# JWT with Refresh Token Rotation — Workflow

## Architecture Overview

This app uses a **stateless JWT** system with **refresh token rotation**. Every request is authenticated via a short-lived JWT. When the JWT expires, the client exchanges a long-lived refresh token (stored in the database) for a new pair. If a refresh token is replayed after use, all sessions for that user are immediately invalidated.

---

## Components

### `SecurityConfig.java`
The central Spring Security configuration class. Responsibilities:
- Disables CSRF (not needed for stateless JWT)
- Declares public vs protected endpoints
- Inserts `JwtAuthFilter` before Spring's default auth filter
- Configures logout handling
- Exposes `AuthenticationManager` and `DaoAuthenticationProvider` as beans

### `JwtAuthFilter.java`
A `OncePerRequestFilter` that runs on every request. Responsibilities:
- Reads the `Authorization: Bearer <token>` header
- Extracts and validates the JWT
- If valid, loads the user from DB and sets the authentication in `SecurityContextHolder`
- Calls `filterChain.doFilter()` unconditionally — authorization is handled downstream

### `JwtService.java`
Handles all JWT operations:
- **Generate**: builds a signed JWT with `sub`, `iat`, `exp` claims using HS256
- **Extract**: parses the JWT and reads individual claims
- **Validate**: checks that the username in the token matches the DB user, and the token is not expired
- Secret key is Base64-decoded from the `JWT_SECRET` environment variable and used as an HMAC-SHA256 key

### `RefreshToken.java` (Entity)
Maps to the `refresh_rotation` table. Fields:
- `id` — auto-incremented primary key
- `token` — random UUID string sent to and stored by the client
- `expiryDate` — when this token stops being valid (20 minutes from creation)
- `userEntity` — FK to the user who owns this token (`@ManyToOne`, one user → many tokens)
- `generationTime` — timestamp of creation (for auditing)

### `RefreshTokenRepository.java`
Extends `JpaRepository`. Custom queries:
- `findByToken(String)` — look up a token row by its UUID string
- `findTokenByUserId(Integer)` — find a user's token by user ID (native SQL join)
- `findActiveTokensByUserId(Integer)` — count token rows for a user where `expiry_date < NOW()` (used for session limiting)
- `upQuery(int)` — immediately expire all tokens for a user by setting `expiry_date = NOW()`

### `RefreshTokenService.java`
Service layer wrapping the repository:
- `createRefreshToken(username)` — generates a UUID token, sets 20-min expiry, saves to DB
- `findByToken(String)` — delegates to repository
- `verifyExpiration(RefreshToken)` — checks expiry and deletes the token if expired (defined but not used in the main refresh endpoint; controller does inline check)
- `ExpireAll(int userId)` — calls `upQuery` to nuke all sessions for a user
- `ActiveSessions(int userId)` — returns session count from repository

### `UserController.java`
Two key endpoints for the token lifecycle: `/authenticate` and `/refreshToken`.

---

## Token Lifetimes

| Token | Type | Lifetime | Storage |
|---|---|---|---|
| Access token | Signed JWT | 10 minutes | Client memory / header |
| Refresh token | Opaque UUID | 20 minutes | Client storage + DB (`refresh_rotation`) |

---

## Flow 1: Login (`POST /authenticate`)

**Request:**
```json
{ "userName": "pravar", "password": "secret" }
```

**Steps:**

1. `AuthenticationManager.authenticate()` is called with username + password.
   - Internally calls `CustomUserDetailsService.loadUserByUsername()` to fetch the user from DB.
   - Calls `BCryptPasswordEncoder.matches()` to verify the password against the stored hash.
   - Throws `BadCredentialsException` if either check fails.

2. If authenticated, fetch the `UserEntity` from DB and get their `id`.

3. Call `refreshTokenService.ActiveSessions(userId)` — counts existing token rows for this user.
   - If count > 5: block login and return an error message.
   - If count ≤ 5: proceed.

4. Call `refreshTokenService.createRefreshToken(username)`:
   - Look up `UserEntity` by username.
   - Generate `UUID.randomUUID().toString()` as the token value.
   - Set `expiryDate = now + 20 minutes`.
   - Save to `refresh_rotation` table. Returns the saved entity.

5. Call `jwtService.generateToken(username)`:
   - Build a JWT with claims: `sub=username`, `iat=now`, `exp=now+10min`.
   - Sign with HS256 using the secret key.
   - Return the compact `xxxxx.yyyyy.zzzzz` string.

6. Return both tokens to the client:
```json
{
  "accessToken": "<JWT>",
  "token": "<UUID refresh token>"
}
```

---

## Flow 2: Authenticated Request

**Request:**
```
GET /getusers/user
Authorization: Bearer <JWT>
```

**Steps (inside `JwtAuthFilter`):**

1. Read `Authorization` header. If missing or not starting with `Bearer `, skip authentication entirely (request will be rejected by the authorization layer if the endpoint is protected).

2. Strip `Bearer ` prefix (`substring(7)`) to get the raw JWT.

3. Call `jwtService.extractUsername(token)` — parses the JWT payload and reads the `sub` claim.

4. Check `SecurityContextHolder.getContext().getAuthentication() == null` — skip if already authenticated.

5. Call `userDetailsService.loadUserByUsername(username)` — fetch user from DB.

6. Call `jwtService.validateToken(token, userDetails)`:
   - Username in token == username from DB?
   - `expiryDate > now`?
   - If both pass: valid.

7. If valid: create `UsernamePasswordAuthenticationToken(userDetails, null, authorities)` and set it on `SecurityContextHolder`. The request is now authenticated.

8. Call `filterChain.doFilter()` — pass to the next filter and eventually the controller.

9. Spring's authorization layer checks if the endpoint requires a role (e.g., `@PreAuthorize("hasRole('ROLE_USER')")`). The authorities from step 7 are used for this check.

---

## Flow 3: Token Refresh (`POST /refreshToken`)

Called when the client's access token has expired. The client sends its stored refresh token UUID.

**Request:**
```json
{ "token": "<UUID refresh token>" }
```

**Steps:**

1. Look up the UUID in `refresh_rotation` table via `refreshTokenService.findByToken()`.
   - If not found: throw `RuntimeException("Refresh token is not in the database!")`. Token was deleted (logout) or never existed.

2. Check expiry: compare `refreshToken.getExpiryDate()` to `Instant.now()`.

**Branch A — Token is expired (`expiryDate < now`):** split on the `revoked` flag.
- **A1 — `revoked = true`**: this token was already consumed by a rotation or logout and is being presented again. This is the **reuse detection / token theft path** — either the client is replaying an old token, or an attacker stole it after it was rotated.
  Response: `refreshTokenService.ExpireAll(userId)` sets `expiry_date = NOW(), revoked = true` on every live token for this user. All sessions on all devices must re-authenticate. Returns 401.
- **A2 — `revoked = false`**: the token simply timed out without ever being used. Not an attack signal — returns 401 "please login again" and leaves the user's other sessions alone.

**Branch B — Token is valid (`expiryDate > now`):**
- **Rotation step 1**: immediately invalidate the old token — `setExpiryDate(Instant.now())` and `setRevoked(true)`. The old UUID is now dead, and any later replay of it lands in Branch A1.
- **Rotation step 2**: generate a new access token via `jwtService.generateToken(username)`.
- **Rotation step 3**: create a new refresh token via `refreshTokenService.createRefreshToken(username)` — new UUID, new refresh window (`auth.refresh-token.ttl-ms`, default 20 min), saved to DB.
- Return the new pair to the client.

**Response (Branch B):**
```json
{
  "accessToken": "<new JWT>",
  "token": "<new UUID refresh token>"
}
```

The client must discard the old refresh token UUID and store the new one. On the next expiry, it uses the new UUID.

---

## Flow 4: Logout (`POST /logout`)

Handled by Spring Security's logout pipeline configured in `SecurityConfig`.

**Steps:**

1. Request hits `/logout`.
2. Spring Security intercepts it before it reaches any controller.
3. `LogoutHandlerService.logout()` reads the refresh token from `Authorization: Bearer <refreshToken>`:
   - still valid → expire + revoke it (single-device logout)
   - already revoked → replay detected, expire all of this user's tokens
   - naturally expired → no-op
4. `logoutSuccessHandler` clears `SecurityContextHolder` — removes the authenticated user from the current thread's context.
5. No redirect (the default `/login?logout` redirect is suppressed by the lambda handler).

After logout the refresh token row stays in the DB, marked revoked. Any later attempt to use it at `/refreshToken` hits Branch A1 and logs the user out everywhere.

---

## JWT Token Structure

A JWT has three Base64-encoded parts separated by dots: `header.payload.signature`.

**Header:**
```json
{ "alg": "HS256" }
```

**Payload (claims added by `createToken()`):**
```json
{
  "sub": "pravar",
  "iat": 1711500000,
  "exp": 1711500600
}
```

No roles or email are embedded in the token. Roles are always loaded fresh from the DB by `CustomUserDetailsService` during token validation.

**Signature:**
```
HMAC-SHA256(base64(header) + "." + base64(payload), secretKeyBytes)
```

The secret key is derived by Base64-decoding `token.secret.key` (env `JWT_SECRET`) and wrapping the bytes as an HMAC-SHA256 key.

---

## Security Model Summary

| Scenario | Behaviour |
|---|---|
| Valid JWT on protected endpoint | Passes through — user authenticated from token |
| Expired JWT | `JwtAuthFilter` does not set auth → 401 Unauthorized |
| Valid refresh token presented | Old token invalidated, new access + refresh token pair issued |
| Revoked (already-used) refresh token replayed | All sessions for that user nuked — forced re-login everywhere |
| Naturally expired refresh token presented | 401, re-login on that device only |
| Refresh token not in DB | Rejected — forged or unknown |
| Login beyond `MAX_SESSIONS` (default 5) | 409 — blocked until one existing session ends |
| Logout | Refresh token expired + revoked; a replay triggers reuse detection |
| Token payload tampered | HMAC signature mismatch → JWT rejected during parsing |

---

## Known Issues / Improvement Areas

1. **`verifyExpiration()` unused** — `RefreshTokenService.verifyExpiration()` is defined but the `/refreshToken` endpoint does its own inline expiry check. Dead code.

2. **`usernameFromUser` unused** — line 34 of `JwtAuthFilter` reads a request attribute `"uname"` that is never used.

3. **Session limit not enforced on refresh** — the 5-device cap is only checked at login (`/authenticate`). A client can keep refreshing indefinitely without ever hitting the cap check again.
