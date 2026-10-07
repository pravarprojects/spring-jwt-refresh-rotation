# Architecture — JWT with Refresh Token Rotation

Spring Boot 3.1.5 · Spring Security 6 · PostgreSQL · JJWT 0.11.5 · Java 17

---

## What This Project Does

A stateless REST API authentication backend. Users register, log in, and receive a short-lived JWT access token paired with a longer-lived refresh token. The access token authorizes protected requests; the refresh token is used to obtain a new pair when the access token expires. Every refresh rotates the refresh token, and any attempt to reuse an already-consumed token triggers a full session wipeout across all devices.

---

## Project Structure

```
src/main/java/com/jwt/jwt/
├── JwtApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── JwtAuthFilter.java
│   ├── CustomUserDetailsService.java
│   └── CustomUser.java
├── controller/
│   └── UserController.java
├── dto/
│   ├── JwtResponse.java
│   ├── RefreshTokenRequest.java
│   ├── UserRequest.java
│   └── UserResponse.java
├── entity/
│   ├── UserEntity.java
│   ├── RefreshToken.java
│   ├── role.java
│   ├── AuthRequest.java
│   └── MatchReq.java
├── repository/
│   ├── JwtRepository.java
│   └── RefreshTokenRepository.java
└── services/
    ├── JwtService.java
    ├── RefreshTokenService.java
    ├── UserService.java
    └── LogoutHandlerService.java

src/main/resources/
└── application.properties
```

---

## Package Responsibilities

### `config/` — Security Wiring

| Class | Responsibility |
|---|---|
| `SecurityConfig` | Defines the Spring Security filter chain. Marks public vs protected endpoints, disables CSRF, registers `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter`, and wires `LogoutHandlerService` to `POST /logout`. Exposes `BCryptPasswordEncoder` and `AuthenticationManager` as beans. |
| `JwtAuthFilter` | `OncePerRequestFilter`. On every request: extracts the Bearer token from the `Authorization` header, calls `JwtService` to extract and validate the username, loads `UserDetails` from `CustomUserDetailsService`, and if valid, injects a `UsernamePasswordAuthenticationToken` into `SecurityContextHolder`. |
| `CustomUserDetailsService` | Implements `UserDetailsService`. Queries `JwtRepository.findByUsername()` and wraps the result in a `CustomUser`. Throws `UsernameNotFoundException` if not found. |
| `CustomUser` | Implements `UserDetails`. Wraps `UserEntity` and adapts it to Spring Security — maps the `role` enum to a `SimpleGrantedAuthority`, delegates `getPassword()` and `getUsername()` to the entity. |

### `controller/` — HTTP Layer

All endpoints live in `UserController`.

| Endpoint | Method | Auth | Description |
|---|---|---|---|
| `/create_user` | POST | No | Register a new user |
| `/authenticate` | POST | No | Login — returns `JwtResponse` with access + refresh token |
| `/refreshToken` | POST | No | Exchange a valid refresh token for a new token pair |
| `/logout` | POST | Yes | Expire current session (or all sessions on reuse detection) |
| `/getusers/user` | GET | `ROLE_USER` | Get all users |
| `/usersById/id/{id}/admin` | GET | `ROLE_ADMIN` | Get user by ID |
| `/usersById/hiadmin` | POST | `ROLE_ADMIN` | Validate token ownership for admin |
| `/match` | POST | No | Match a username against a token |
| `/validate/{token}` | POST | No | Validate token by path variable |
| `/validateWithObject` | POST | No | Validate token from request body |
| `/hi` | GET | No | Health check |
| `/redirect` | GET | No | Redirects to `/hi` |

### `services/` — Business Logic

| Class | Responsibility |
|---|---|
| `JwtService` | Generates JWTs (HS256, configurable expiry — default 10 min). Extracts claims (`extractUsername`, `extractExpiration`, `extractAllClaims`). Validates tokens (`validateToken` — username match + expiry check). Provides `getSignKey()` which decodes the BASE64 HMAC-SHA secret. |
| `RefreshTokenService` | Owns the full lifecycle of refresh tokens: `createRefreshToken` (UUID value, configurable expiry — default 20 min, persisted to DB), `findByToken`, `verifyExpiration`, `ExpireAll` (mass-expire all tokens for a user), `ActiveSessions` (count of current sessions). |
| `UserService` | User CRUD. `createUser` encodes the password with `BCryptPasswordEncoder` before persisting. `getAllUsers` and `getById` map `UserEntity` to `UserResponse` DTOs. |
| `LogoutHandlerService` | Implements Spring's `LogoutHandler`. Extracts the token from the `Authorization` header. If the token is still valid: expires and revokes that single token. If it was already revoked (rotated or logged out before): calls `upQuery()` to expire all tokens for that user (reuse detection). If it simply timed out: no-op. Always clears `SecurityContextHolder`. |

### `entity/` — Database Models

| Class | Table | Notes |
|---|---|---|
| `UserEntity` | `jwt_rotation` | id, name, username (unique), password (BCrypt), role (enum). Has `@OneToMany` → `RefreshToken` with cascade delete. |
| `RefreshToken` | `refresh_rotation` | UUID token string, `generationTime` (Instant), `expiryDate` (Instant), `@ManyToOne` FK → `UserEntity`. Uses a Builder pattern for construction. |
| `role` | — | Enum: `ROLE_USER`, `ROLE_ADMIN` |
| `AuthRequest` | — | Login request payload: `userName`, `password` |
| `MatchReq` | — | Username + token pair for validation endpoints |

### `dto/` — API Payloads

| Class | Fields |
|---|---|
| `JwtResponse` | `accessToken` (JWT), `token` (refresh token UUID) |
| `RefreshTokenRequest` | `token` (refresh token UUID) |
| `UserRequest` | `name`, `username`, `role`, `password` |
| `UserResponse` | `id`, `name`, `username`, `role`, `password` |

### `repository/` — Data Access

| Class | Notable Methods |
|---|---|
| `JwtRepository` | `findByUsername(String username)` |
| `RefreshTokenRepository` | `findByToken(String token)` · `findTokenByUserId(Integer id)` · `findActiveTokensByUserId(Integer id)` (native count query) · `upQuery(int id)` (native bulk UPDATE — sets `expiry_date = NOW()` for all active tokens of a user) |

---

## Database Schema

Two tables, auto-created by Hibernate (`ddl-auto=update`).

```
jwt_rotation
┌─────────────┬──────────────┬──────────┐
│ id (PK)     │ name         │ username │
│ role        │ password     │          │
└─────────────┴──────────────┴──────────┘

refresh_rotation
┌─────────────┬────────────────────┬─────────────────┐
│ id (PK)     │ token (UUID string)│ expiry_date     │
│ generation_time                  │ user_entity_id  │ ← FK → jwt_rotation.id
└─────────────┴────────────────────┴─────────────────┘
```

---

## JWT Lifecycle

### Token Specifications

| Token | Type | Algorithm | Expiry | Storage |
|---|---|---|---|---|
| Access token | JWT (signed) | HS256 | 10 minutes | Client only (never persisted) |
| Refresh token | UUID string | — | 20 minutes | `refresh_rotation` table in DB |

---

### Step 1 — Registration

```
POST /create_user  { name, username, role, password }
  └─► UserService.createUser()
        └─► BCryptPasswordEncoder.encode(password)
        └─► JwtRepository.save(UserEntity)
```

---

### Step 2 — Login

```
POST /authenticate  { userName, password }
  └─► AuthenticationManager.authenticate()          ← validates credentials
  └─► RefreshTokenService.ActiveSessions(userId)
        └─► if count >= auth.max-sessions (default 5)  →  409 CONFLICT
  └─► RefreshTokenService.createRefreshToken(username)
        └─► UUID token value
        └─► generationTime = Instant.now()
        └─► expiryDate     = now + auth.refresh-token.ttl-ms
        └─► persisted to refresh_rotation table
  └─► JwtService.generateToken(username)
        └─► subject = username
        └─► issuedAt = now
        └─► expiration = now + auth.access-token.ttl-ms
        └─► signed with HS256
  └─► return JwtResponse { accessToken, token }
```

---

### Step 3 — Authorized Request

```
GET /getusers/user
  Authorization: Bearer <accessToken>

  └─► JwtAuthFilter.doFilterInternal()
        └─► extract token from Authorization header
        └─► JwtService.extractUsername(token)
        └─► CustomUserDetailsService.loadUserByUsername()
        └─► JwtService.validateToken(token, userDetails)
              └─► username match  AND  not expired
        └─► if valid: set UsernamePasswordAuthenticationToken
                      in SecurityContextHolder
  └─► SecurityConfig enforces role (@PreAuthorize / hasRole)
  └─► request proceeds
```

---

### Step 4 — Token Refresh

```
POST /refreshToken  { token: "<refreshTokenUUID>" }
  └─► RefreshTokenService.findByToken(token)
  └─► compare expiryDate with Instant.now()

        ┌─ EXPIRED + REVOKED (replay of a consumed token) ───────┐
        │  RefreshTokenService.ExpireAll(userId)                  │
        │    └─► RefreshTokenRepository.upQuery(userId)           │
        │          └─► bulk UPDATE: expiry_date = NOW(),          │
        │                revoked = true for all live tokens       │
        │  401 — reuse detected, all sessions terminated          │
        └────────────────────────────────────────────────────────┘

        ┌─ EXPIRED, NOT REVOKED (natural timeout) ───────────────┐
        │  401 — "please login again"; other sessions untouched  │
        └────────────────────────────────────────────────────────┘

        ┌─ VALID ────────────────────────────────────────────────┐
        │  expire + revoke old refresh token                     │
        │  JwtService.generateToken()      → new access token    │
        │  RefreshTokenService.createRefreshToken() → new UUID   │
        │  return JwtResponse { newAccessToken, newRefreshToken } │
        └────────────────────────────────────────────────────────┘
```

---

### Step 5 — Logout

```
POST /logout
  Authorization: Bearer <refreshToken>

  └─► LogoutHandlerService.logout()
        └─► extract token from Authorization header
        └─► find RefreshToken in refresh_rotation table

              ┌─ TOKEN STILL VALID ──────────────────────────────┐
              │  expiryDate = now, revoked = true                 │
              │  (single session logout)                          │
              └──────────────────────────────────────────────────┘

              ┌─ TOKEN ALREADY REVOKED (replay) ─────────────────┐
              │  RefreshTokenRepository.upQuery(userId)           │
              │    └─► expire ALL tokens for this user            │
              │  (logout from all devices)                        │
              └──────────────────────────────────────────────────┘

              ┌─ TOKEN NATURALLY EXPIRED ────────────────────────┐
              │  no-op                                            │
              └──────────────────────────────────────────────────┘

        └─► SecurityContextHolder.clearContext()
```

---

## Security Features Summary

| Feature | Implementation |
|---|---|
| Password hashing | `BCryptPasswordEncoder` |
| Access token validation | `JwtAuthFilter` → `JwtService.validateToken()` on every request |
| Refresh token stateful storage | `refresh_rotation` table — tokens can be explicitly revoked |
| Token rotation | Old refresh token expired and marked `revoked` on every `/refreshToken` call |
| Reuse detection | Revoked token presented again → `ExpireAll()` kills all sessions for that user. Naturally expired tokens are *not* treated as an attack. |
| Max concurrent sessions | Login denied if active session count ≥ `auth.max-sessions` (default 5) |
| Role-based access control | `ROLE_USER` and `ROLE_ADMIN` enforced on specific endpoints |
| Stateless sessions | `SessionCreationPolicy.STATELESS` — no server-side HTTP session |
| Logout | Tokens explicitly expired in DB — not just client-side discard |

---

## Local Setup

**Prerequisites:** Java 17+, Docker (or a local PostgreSQL).

```bash
docker compose up -d                       # PostgreSQL on :5432, database jwt_auth
export JWT_SECRET=$(openssl rand -base64 32)
./mvnw spring-boot:run
```

All settings are environment variables — see `.env.example` and `src/main/resources/application.properties`.

**Typical request sequence:**
1. `POST /create_user` — register
2. `POST /authenticate` — get `accessToken` + `token` (refresh)
3. `GET /getusers/user` with `Authorization: Bearer <accessToken>`
4. `POST /refreshToken` with `{ "token": "<refreshToken>" }` when access token expires
5. `POST /logout` with `Authorization: Bearer <refreshToken>`
