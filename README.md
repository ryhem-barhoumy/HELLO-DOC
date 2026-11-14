# Hello Doc monorepo

Hello Doc contains an Angular 20 frontend and a Java Spring Boot backend. The backend implements MySQL-backed session authentication, patient registration and profiles, doctor applications and approval with operational profiles, doctor discovery, specialties, recurring availability, absences, and dynamically calculated appointment slots. Appointment booking, status management, patient notifications and absence-related rescheduling are implemented. Clinical records, teleconsultation, billing and several administrative modules remain to be delivered.

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

Configure your own MySQL connection in the ignored `backend/.env.local` file (copy `backend/.env.example` as a guide), then follow [backend/README.md](backend/README.md). The backend API uses port `8081` by default; each collaborator supplies their own MySQL host and port. Sign-in, profiles, doctor discovery, approval, availability, absences, appointment booking and core patient/doctor appointment workflows use the configured MySQL database. Administrative appointment analytics, activity logs, notification management, and other explicitly listed out-of-scope features remain planned.

See [frontend/README.md](frontend/README.md) for frontend details and [backend/docs/HELLO_DOC_BACKEND_SPECIFICATION.md](backend/docs/HELLO_DOC_BACKEND_SPECIFICATION.md) for the API contract. No Node.js backend, mock database, or fake authentication is included.

## Refonte et parcours actuels

Voir [le bilan de la première livraison](REFONTE_AVANCEMENT.md). L’inscription médecin porte sur les coordonnées et le justificatif ; les services et horaires sont configurés après approbation. Une absence affectant des rendez-vous exige un avertissement puis une confirmation du médecin avant d’inviter les patients à reprogrammer.
