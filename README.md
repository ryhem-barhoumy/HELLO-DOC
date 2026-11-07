# Hello Doc monorepo

Hello Doc contains an Angular 20 frontend and a Java Spring Boot backend in progress. The backend currently implements MySQL-backed session authentication, patient registration, pending doctor applications with administrator approval, role-protected administrator endpoints, and an idempotent initial admin seed. The broader patient, doctor, and appointment API remains specified but unfinished.

## Repository layout

- `frontend/` - Angular application, dependencies, and frontend documentation.
- `backend/` - Spring Boot source, Maven build, and setup instructions.
- `backend/docs/HELLO_DOC_BACKEND_SPECIFICATION.md` - full API/domain contract and implementation status.
- `.git/` - repository history at the monorepo root.

## Run the Angular frontend

Requirements: Node.js 20.19+ or 22.12+ and npm.

```powershell
cd frontend
npm ci
npm start
```

Open `http://localhost:4200`. Build with `npm run build` or `npm run build -- --configuration production`.

## Run the backend

Start Laragon MySQL and follow [backend/README.md](backend/README.md) to securely create/verify `hello_doc` and run the backend. The backend defaults to `http://localhost:8080/api`. Sign-in, registration, doctor application approval, and admin endpoints use the configured MySQL database. Other patient, doctor, and appointment workflows remain unavailable until those APIs are implemented.

See [frontend/README.md](frontend/README.md) for frontend details and [backend/docs/HELLO_DOC_BACKEND_SPECIFICATION.md](backend/docs/HELLO_DOC_BACKEND_SPECIFICATION.md) for the API contract. No Node.js backend, mock database, or fake authentication is included.
