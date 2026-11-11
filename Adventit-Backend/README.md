# Adventit backend

Spring Boot backend with a hexagonal structure. The first feature is session-based account authentication, backed by SQLite.

## Run

From this directory, run `.\gradlew.bat bootRun`. The default database is `adventit.db` in the working directory. Set `DATABASE_URL` to use a different SQLite JDBC URL. For HTTPS deployments, set `SESSION_COOKIE_SECURE=true`.

## Authentication API

All endpoints use JSON except where noted. Register and login require an active CSRF token:

1. Call `GET /api/auth/csrf` and retain the session cookie and returned `token`.
2. Send that same cookie and an `X-CSRF-TOKEN` header with each `POST` request.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/auth/csrf` | Start a session and return a CSRF token |
| `POST` | `/api/auth/register` | Create an account with `{ "email": "...", "password": "..." }` |
| `POST` | `/api/auth/login` | Authenticate and establish the session cookie |
| `GET` | `/api/auth/me` | Return the authenticated account |
| `POST` | `/api/auth/logout` | Invalidate the session |

Emails are normalized to lowercase. Passwords must contain 8-72 characters and are stored as BCrypt hashes. Registration returns `409` for an existing email; invalid credentials return `401`.

The browser session cookie is HTTP-only and uses `SameSite=Lax`. CSRF protection remains enabled for state-changing requests.

## Test

Run `.\gradlew.bat test`.
