# Spring Boot JWT Auth with Refresh Token Rotation

A drop-in authentication service built on **Spring Boot 3 + Spring Security 6 + PostgreSQL**. It issues short-lived JWT access tokens paired with database-backed refresh tokens. Every refresh rotates the refresh token, and **any replay of an already-used refresh token logs that user out of every device**.

Built as the auth service for a larger multi-service app and hardened there against real edge cases, then extracted here as a standalone, fully env-configurable module.

## Features

- **Stateless access tokens**: HS256-signed JWTs carrying `sub` and `role` claims, checked on every request by a `OncePerRequestFilter`.
- **Refresh token rotation**: each `/refreshToken` call expires and revokes the presented token and issues a new pair.
- **Reuse (theft) detection**: a `revoked` flag separates "this token was already consumed" (attack → revoke every session for the user) from "this token just timed out" (harmless → ask the user to log in again).
- **Concurrent session cap**: login is refused with `409 CONFLICT` once a user has `MAX_SESSIONS` live refresh tokens.
- **Server-side logout**: revokes the refresh token, and a replayed logout token also triggers reuse detection.
- **Role-based access**: `ROLE_USER` / `ROLE_ADMIN` via `@PreAuthorize`.
- **BCrypt** password hashing.
- **Pluggable**: port, database, signing key, token lifetimes and session cap are all environment variables.

## How rotation & reuse detection work

```
            login                       refresh (RT1)                  refresh (RT1 again)
client ───────────────► AT1 + RT1  ───────────────► AT2 + RT2        ───────────────► 401
                                    RT1: expired, revoked=true       RT1 is revoked → replay!
                                                                     ALL of the user's refresh
                                                                     tokens are expired + revoked
```

| Refresh token state when presented | Result |
|---|---|
| Valid | Rotated: old one revoked, new access + refresh token returned |
| Expired **and** revoked | Reuse attack: every session for that user is killed (401) |
| Expired, not revoked | Natural timeout: 401 "please login again", other sessions untouched |
| Not in DB | 401 |

More detail: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) (components, data model, flows) and [`docs/workflow.md`](docs/workflow.md) (step-by-step request walkthroughs).

## Tech stack

Java 17 · Spring Boot 3.1 · Spring Security 6 · Spring Data JPA / Hibernate · PostgreSQL · JJWT 0.11 · Maven

## Quick start

```bash
# 1. Start PostgreSQL (or point DB_URL at your own instance)
docker compose up -d

# 2. Provide a signing key (Base64, ≥ 256 bits)
export JWT_SECRET=$(openssl rand -base64 32)

# 3. Run
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`. Hibernate creates the tables on first run.

## Configuration

| Env variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/jwt_auth` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` | DB credentials |
| `JWT_SECRET` | *(required)* | Base64 HMAC key for signing access tokens |
| `ACCESS_TOKEN_TTL_MS` | `600000` (10 min) | Access token lifetime |
| `REFRESH_TOKEN_TTL_MS` | `1200000` (20 min) | Refresh token lifetime |
| `MAX_SESSIONS` | `5` | Max live refresh tokens per user |
| `SHOW_SQL` | `false` | Log Hibernate SQL |

See [`.env.example`](.env.example).

## API

| Endpoint | Method | Auth | Description |
|---|---|---|---|
| `/create_user` | POST | – | Register `{ name, username, password, role }` |
| `/authenticate` | POST | – | Login `{ userName, password }` → `{ accessToken, token }` |
| `/refreshToken` | POST | – | `{ token: <refreshToken> }` → new `{ accessToken, token }` |
| `/logout` | POST | Bearer **refresh** token | Revoke this session |
| `/getusers/user` | GET | `ROLE_USER` | List users |
| `/usersById/id/{id}/admin` | GET | `ROLE_ADMIN` | Get user by id |
| `/validate/{token}`, `/validateWithObject`, `/match` | POST | – | Token validation helpers for other services |
| `/tokenByName` | POST | `ROLE_ADMIN` | Live refresh token ids for a user (service-to-service) |

### Example

```bash
# register
curl -X POST localhost:8080/create_user -H 'Content-Type: application/json' \
  -d '{"name":"Alice","username":"alice","password":"s3cret","role":"ROLE_USER"}'

# login
curl -X POST localhost:8080/authenticate -H 'Content-Type: application/json' \
  -d '{"userName":"alice","password":"s3cret"}'
# → {"accessToken":"eyJhbGciOiJIUzI1NiJ9...","token":"7f1c...-uuid"}

# call a protected endpoint
curl localhost:8080/getusers/user -H "Authorization: Bearer <accessToken>"

# rotate
curl -X POST localhost:8080/refreshToken -H 'Content-Type: application/json' \
  -d '{"token":"<refreshToken>"}'

# logout (note: the REFRESH token goes in the header)
curl -X POST localhost:8080/logout -H "Authorization: Bearer <refreshToken>"
```

## Plugging it into another app

Run it as its own service. Your other services can either:

1. **Verify JWTs locally**: share `JWT_SECRET` and check the HS256 signature and `exp`. No network call is needed, and the `role` claim gives you authorization, or
2. **Ask this service**: call `/validateWithObject` or `/match` to confirm a token or token-to-username binding.

Clients store both tokens, send the access token as `Authorization: Bearer …`, and call `/refreshToken` when they get a 401/403. Each response returns a new refresh token, so the client must replace the one it has stored.

## Project layout

```
src/main/java/com/jwt/jwt/
├── config/        SecurityConfig, JwtAuthFilter, UserDetails adapters
├── controller/    UserController (all endpoints)
├── services/      JwtService, RefreshTokenService, LogoutHandlerService, UserService
├── entity/        UserEntity, RefreshToken, role
├── repository/    Spring Data JPA repositories
└── dto/           request/response objects
```
