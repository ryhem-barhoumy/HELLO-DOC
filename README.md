# Hello Doc monorepo

Hello Doc contains an Angular 20 frontend and the API contract for a teammate's Java Spring Boot backend. The backend implementation and database are not included in this repository.

## Repository layout

- `frontend/` — complete Angular application, dependencies, configuration, and frontend README.
- `backend/docs/HELLO_DOC_BACKEND_SPECIFICATION.md` — proposed Spring Boot API, domain, security, and integration contract.
- `.git/` — this repository's Git history and metadata remain at the monorepo root.

## Run the Angular frontend

Requirements: Node.js 20.19+ or 22.12+ and npm.

```powershell
cd frontend
npm ci
npm start
```

Open `http://localhost:4200`. Angular CLI commands run from `frontend/`:

```powershell
npm run build
npm run build -- --configuration production
```

The frontend expects a Spring Boot API at `http://localhost:8080/api` by default. Until that service is implemented and configured for credentialed CORS and CSRF, API-backed sign-in, persistence, and patient/doctor/admin workflows will report the service as unavailable. The frontend does not include a Node.js backend, mock database, or fake authentication.

See [frontend/README.md](frontend/README.md) for frontend features and limitations, and the [backend specification](backend/docs/HELLO_DOC_BACKEND_SPECIFICATION.md) for the API contract.