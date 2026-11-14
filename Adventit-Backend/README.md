# Adventit backend

Spring Boot backend with a hexagonal structure and SQLite-backed account authentication.

## Run

Set `AUTH_JWT_SECRET` to a Base64-encoded random key of at least 32 bytes, then run `.\gradlew.bat bootRun` from this directory. The default database is `adventit.db` in the working directory; set `DATABASE_URL` to use a different SQLite JDBC URL. `AUTH_JWT_EXPIRATION` accepts a Java duration (default `PT1H`). Set `APP_CORS_ALLOWED_ORIGINS` to a comma-separated list of exact frontend origins (default `http://localhost:5173`). For a frontend on a different site, set `AUTH_COOKIE_SAME_SITE=None` and `AUTH_COOKIE_SECURE=true` (requires HTTPS); otherwise the cookie will not be sent by browsers on cross-site requests.

## Authentication API

All endpoints use JSON. Login returns an HTTP-only `token` cookie; browsers send it automatically on subsequent requests when the client enables credentials (for example, `fetch(url, { credentials: "include" })`). Authentication is cookie-based and does not use an `Authorization` header. The API is available only under `/api/auth`.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Create an account with `{ "email": "...", "password": "..." }` |
| `POST` | `/api/auth/login` | Authenticate and set the signed token cookie |
| `GET` | `/api/auth/me` | Return the authenticated account |
| `POST` | `/api/auth/logout` | Clear the token cookie |

Emails are normalized to lowercase. Passwords must contain 8-72 characters and are stored as BCrypt hashes. Registration returns `409` for an existing email; invalid credentials return `401`.

## Trip planning

Set `GOOGLE_ROUTES_API_KEY` to an API key with the Google Routes API enabled. `POST /app/trip` requires an authenticated user and accepts `{ "startPlace": "...", "endPlace": "..." }`, where both values are place addresses. It returns route options for public transport (`TRANSIT`) and walking (`WALK`), with duration, distance, an encoded polyline, and ordered journey steps for each available route. Transit steps include stop names, departure and arrival times, line and vehicle information, and headsign when provided by Google. The endpoint can be overridden with `GOOGLE_ROUTES_API_ENDPOINT` when needed.

## Temporary Qwen ask endpoint

`POST /app/ask` is available only to authenticated users and accepts `{ "question": "..." }` (up to 4000 characters). It returns `{ "answer": "..." }` using the local Ollama chat API. Start Ollama with a Qwen model available (for example, `ollama run qwen2.5`); the default model is `qwen2.5:latest`. Set `QWEN_MODEL` to use another installed model and `QWEN_OLLAMA_ENDPOINT` to override the default `http://localhost:11434/api/chat`.

## Test

Run `.\gradlew.bat test`.
