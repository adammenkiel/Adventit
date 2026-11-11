# Adventit backend

Spring Boot backend with a hexagonal structure and SQLite-backed account authentication.

## Run

Set `AUTH_JWT_SECRET` to a Base64-encoded random key of at least 32 bytes, then run `.\gradlew.bat bootRun` from this directory. The default database is `adventit.db` in the working directory; set `DATABASE_URL` to use a different SQLite JDBC URL. Set `AUTH_COOKIE_SECURE=true` when serving over HTTPS. `AUTH_JWT_EXPIRATION` accepts a Java duration (default `PT1H`).

## Authentication API

All endpoints use JSON. Login returns an HTTP-only `token` cookie with `SameSite=Lax`; browsers send it automatically on subsequent requests. Authentication is cookie-based and does not use an `Authorization` header. The API is available only under `/api/auth`.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Create an account with `{ "email": "...", "password": "..." }` |
| `POST` | `/api/auth/login` | Authenticate and set the signed token cookie |
| `GET` | `/api/auth/me` | Return the authenticated account |
| `POST` | `/api/auth/logout` | Clear the token cookie |

Emails are normalized to lowercase. Passwords must contain 8-72 characters and are stored as BCrypt hashes. Registration returns `409` for an existing email; invalid credentials return `401`.

## Test

Run `.\gradlew.bat test`.
