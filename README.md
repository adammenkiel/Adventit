# Adventit

Adventit is a trip-planning application for comparing public-transit and walking routes between two places. Create an account, sign in, and review route durations, distances, maps, and available journey details.

## Technology overview

- **Backend:** Java 21 and Spring Boot 3.5, organized around application services and ports/adapters. Spring Security provides stateless authentication with signed JWTs in HTTP-only cookies; accounts are stored in SQLite and passwords are BCrypt-hashed.
- **Route planning:** The backend requests transit and walking alternatives from the Google Routes API and maps the response to application route models, including transit steps and encoded polylines.
- **AI:** The backend includes an Ollama/Qwen-backed question-answering service. Its AI service and integration remain in the codebase.
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS, and Base UI. The UI calls the backend with credentialed requests and can draw returned route polylines using the Google Maps JavaScript API.

## Run locally

### Prerequisites

- Java 21
- Node.js and npm
- A Google Routes API key with the Routes API enabled to plan trips
- A Google Maps JavaScript API key to display route maps (optional)

### 1. Configure and run the API

Open PowerShell in the repository root and start the backend:

```powershell
cd .\Adventit-Backend
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$env:AUTH_JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:GOOGLE_ROUTES_API_KEY = "your-google-routes-api-key"
.\gradlew.bat bootRun
```

The API starts at `http://localhost:8080`. SQLite creates `adventit.db` in the backend working directory by default. Keep the API process running while using the frontend. The environment variables above apply to the current PowerShell window; use a new key for production.

### 2. Configure and run the frontend

In a second PowerShell window, from the repository root:

```powershell
cd .\Adventit-Frontend
npm ci
```

To display routes on a map, create `Adventit-Frontend/.env.local` with a browser-restricted key that has the Google Maps JavaScript API enabled:

```env
VITE_GOOGLE_MAPS_API_KEY=your-google-maps-javascript-api-key
```

This is a browser key; do not put the backend Routes API key in this file. The map key is optional, but route planning requires the backend key.

Start Vite:

```powershell
npm run dev
```

Open the local URL printed by Vite (by default, `http://localhost:5173`). The frontend uses `http://localhost:8080` as its API base by default; set `VITE_API_BASE_URL` in `.env.local` if the API runs elsewhere.

### Tests and production build

Run backend tests from `Adventit-Backend` with `.\gradlew.bat test`. Run the frontend production build from `Adventit-Frontend` with `npm run build`.

## Development assistance

GitHub Copilot Agent was used to assist with the development process.

## Video
Link: https://youtu.be/IqrnP5_zy9U
