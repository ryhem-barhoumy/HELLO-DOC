# Hello Doc monorepo

Hello Doc contains an Angular 20 frontend and a Java Spring Boot backend. The backend implements MySQL-backed session authentication, patient registration and profiles, doctor applications and approval with operational profiles, doctor discovery, specialties, recurring availability, absences, and dynamically calculated appointment slots. Appointment booking and the remaining patient, doctor, and admin business APIs are being delivered in later lots.

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

Start Laragon MySQL on port `3308` and follow [backend/README.md](backend/README.md) to securely create/verify `hello_doc` and run the backend. The backend defaults to `http://localhost:8081/api`. Sign-in, profiles, doctor discovery, approval, availability, and absence APIs use the configured MySQL database. Appointment booking and the remaining workflows are planned in later lots.

See [frontend/README.md](frontend/README.md) for frontend details and [backend/docs/HELLO_DOC_BACKEND_SPECIFICATION.md](backend/docs/HELLO_DOC_BACKEND_SPECIFICATION.md) for the API contract. No Node.js backend, mock database, or fake authentication is included.
