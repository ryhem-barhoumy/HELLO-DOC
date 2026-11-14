# Hello Doc Spring Boot backend

This backend uses Java 21, Spring Boot, Spring Security, Spring Data JPA, BCrypt, and MySQL. It implements session authentication, patient profiles as USER, pending doctor applications, admin approval with Doctor profile creation, specialties and public doctor discovery, recurring availability, absences, calculated free slots, and appointment booking/management. Backend API port: 8081 by default. MySQL host and port are local settings chosen by each collaborator.

## Configure Laragon and start

1. Start your local MySQL service. Copy `.env.example` to `.env.local` and set your own MySQL host, port, database name, and username. `.env.local` is ignored by Git; do not share it. Leave `HELLODOC_DB_PASSWORD=` empty if the account has no password. Then create or verify only your configured schema and start Spring Boot:

   ```powershell
   cd backend
   .\run-local.ps1
   ```

   The script reads local settings from `.env.local`; defined process environment variables override that file. It runs `CREATE DATABASE IF NOT EXISTS` only for the configured schema and leaves all other databases untouched. It prompts for the MySQL password without echoing it; press Enter to use an empty password. It separately requires the initial Admin password. These credentials are not stored by the script.

   You can alternatively set environment variables manually in the backend process. Do not commit real secrets:

   ```powershell
   $env:HELLODOC_DB_URL = 'jdbc:mysql://<your-host>:<your-port>/<your-database>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
   $env:HELLODOC_DB_USERNAME = '<your MySQL username>'
   $env:HELLODOC_DB_PASSWORD = '' # leave empty for a root account with no password
   $env:HELLODOC_ADMIN_INITIAL_PASSWORD = '<initial administrator password>'
   ```

Set `SERVER_PORT` only if you need to override the shared backend default of `8081`. The initial Admin password is separate from the MySQL password. It is read only by the backend process, is never compiled into Angular, and is BCrypt-hashed before insertion. The seed runs only when `admin@hellodoc.com` is absent. On later starts it leaves the existing row and password untouched.

2. The initial sign-in is `admin@hellodoc.com` with the password supplied at the prompt. Immediately after login, open Admin > Sécurité and change it using the current and new password. The new password must contain at least eight characters. The run script removes the initial-password environment variable when stopped; the saved account is unaffected.

3. To run Maven commands separately, install Maven 3.9+ if not already installed:

   ```powershell
   cd backend
   mvn spring-boot:run
   ```

Hibernate creates/updates the `users` table. Database credentials and the initial password are intentionally required from environment variables and are not committed.

## Profile and scheduling API

- `GET/PUT /api/patients/me` reads or updates the signed-in patient profile; updates require the current password.
- `GET/PUT /api/doctors/me` reads or updates the signed-in doctor profile; updates require the current password.
- `GET /api/specialties`, `GET /api/doctors`, and `GET /api/doctors/{id}` expose specialties, paginated search, and public approved profiles.
- `GET /api/doctors/{id}/slots?date=YYYY-MM-DD` calculates free slots from weekly schedules, absences, and active appointments.
- `GET /api/doctors/{id}/availability-calendar?month=YYYY-MM` returns each date’s `AVAILABLE`, `FULL`, `DAY_OFF`, `ABSENT`, or `PAST` status, free-slot count, and first free time. Free slots are grouped as the first or second window, exclude pending/confirmed appointments and partial absences, omit past times, and enforce a configurable 60-minute minimum notice and 60-day booking horizon (`HELLODOC_MIN_BOOKING_NOTICE_MINUTES`, `HELLODOC_MAX_BOOKING_HORIZON_DAYS`).
- `GET /api/doctors/me/availabilities` and `PUT /api/doctors/me/availabilities/weekly` manage up to two non-overlapping weekly windows per ISO weekday. Existing availability rows remain the first window; replacing the weekly schedule only changes future slot calculation and never edits saved appointments. Window edits validate five-minute increments and require enough time for at least one consultation.
- `GET/POST/PUT/DELETE /api/doctors/me/availabilities[/{id}]` manages weekly recurring windows using ISO weekdays (Monday 1 through Sunday 7).
- `GET/POST/PUT/DELETE /api/doctors/me/absences[/{id}]` manages date-bounded all-day absences or one-date partial absences with start/end times. Absence reasons remain private.
- Public doctor profiles include professional phone, clinic address, and city. They never include the account email.

## Appointment API

- `GET /api/patients/me/dashboard` and `GET /api/doctors/me/dashboard` provide persisted appointment counts and upcoming appointment summaries.
- `POST /api/appointments` books a selected doctor/service/start time for the patient identified by the session. Contact details and patient identifiers are never accepted from the request.
- `GET /api/appointments/mine?history=false|true`, `GET /api/appointments/{id}`, `POST /api/appointments/{id}/cancellation`, `PATCH /api/appointments/{id}/status`, and `PUT /api/appointments/{id}/reschedule` support patient and doctor workflows with participant ownership checks.
- `GET /api/doctors/me/appointments` returns only the active doctor’s agenda. Rebooking and rescheduling revalidate the doctor’s weekly schedule, absences, duration, current time, and overlapping active appointments inside a transaction. A doctor row lock serializes competing requests and a unique reservation key prevents duplicate start times at the database level.
- Statuses are `PENDING`, `CONFIRMED`, `TO_RESCHEDULE`, `COMPLETED`, `REJECTED`, `CANCELLED`, and `NO_SHOW`; every transition is stored in `appointment_status_history`.
- `GET /api/notifications` lists only the signed-in patient’s or active doctor’s notifications; `PATCH /api/notifications/{id}/read` marks only that recipient’s notification as read. Appointment requests, decisions, cancellations, rescheduling, and new reviews create persisted notifications in the same transaction.
- `POST /api/appointments/{id}/review` creates or edits the signed-in patient’s single review after a completed consultation. `GET /api/doctors/me/reviews` lists reviews only for the active doctor’s own profile.

## Security and current API

- `GET /api/auth/csrf` issues the Angular-compatible `XSRF-TOKEN` cookie and token response.
- `POST /api/auth/login`, `GET /api/auth/me`, and `POST /api/auth/logout` use an HTTP-only server session cookie and CSRF protection for mutations. `POST /api/auth/password` checks the current password and stores a new BCrypt hash.
- Public patient/doctor registration assigns a regular `USER` role in the service; no request DTO accepts a role. There is no public admin creation endpoint.
- `GET /api/admin/me` and `GET /api/admin/dashboard` require `ROLE_ADMIN`. Security returns 401 for no session and 403 for an authenticated non-admin.
- Patient and active-doctor dashboards include persisted appointment counts, upcoming appointments, and unread notification counts. Admin appointment metrics, activity logs, and admin notification views remain unavailable until those later administrative features exist.
- Configure `HELLODOC_FRONTEND_ORIGIN` when Angular is served from another origin. Keep credentials enabled only for that exact trusted origin.

The Angular application models PATIENT, DOCTOR, ADMIN, and USER. Patient registrations create active USER accounts and profiles. Doctor approval assigns DOCTOR/ACTIVE, creates the Doctor profile, and imports valid specialties and weekly availability. Invalid historical service, hour, or day data is logged and does not prevent profile creation. The optional `dev` profile seeds two demo doctors only when `HELLODOC_DEMO_PASSWORD` is configured (minimum 12 characters). For a local run, set `SPRING_PROFILES_ACTIVE=dev` and `HELLODOC_DEMO_PASSWORD` in the backend process environment; the password is BCrypt-hashed and is not committed.

### Inscription médecin et justificatif

`GET /api/specialties` est public en lecture seule. Les spécialités de référence sont initialisées de façon idempotente dans tous les profils Spring Boot.

`POST /api/doctors/registrations` accepte `multipart/form-data` avec une partie JSON `registration` (coordonnées, spécialité, numéro professionnel, ville, mot de passe et consentement ; services et horaires configurés après approbation) et exactement une partie `document`. Le justificatif obligatoire est l’attestation d’inscription à l’Ordre des médecins avec cachet. Le serveur vérifie les signatures PDF/JPEG/PNG et limite le fichier à 5 Mo; le nom et le type déclarés par le navigateur ne servent pas à valider le contenu. Les fichiers sont stockés sous un nom aléatoire hors du dossier public, par défaut dans `${user.home}/.hellodoc/doctor-documents`; `HELLODOC_DOCUMENTS_DIR` permet de choisir un autre dossier privé.

Le téléchargement est disponible uniquement à l’administration via `GET /api/admin/doctors/{applicationId}/document`. Les demandes sans document ne peuvent pas être approuvées.

### Courriel d’approbation médecin

Après le commit de l’approbation, Hello Doc envoie au médecin un courriel professionnel confirmant l’activation de son compte et contenant le lien `/connexion`. Aucun mot de passe n’est inclus. L’envoi est désactivé tant qu’un relais SMTP n’a pas été configuré. Dans l’environnement du processus backend, définir `HELLODOC_EMAIL_ENABLED=true`, `HELLODOC_SMTP_HOST`, `HELLODOC_SMTP_PORT` (587 par défaut), `HELLODOC_SMTP_USERNAME`, `HELLODOC_SMTP_PASSWORD` et `HELLODOC_EMAIL_FROM`; `HELLODOC_SMTP_AUTH` et `HELLODOC_SMTP_STARTTLS` sont activés par défaut. `HELLODOC_FRONTEND_URL` choisit le domaine du lien de connexion. Ne pas committer ces secrets. Une erreur SMTP est journalisée après commit et ne révoque pas l’approbation.

Pour un envoi Gmail en local, lancer `./run-local.ps1` depuis `backend`, répondre `y` à la question Gmail, saisir l’adresse expéditrice puis son mot de passe d’application à l’invite masquée. Le script configure `smtp.gmail.com:587` avec authentification et STARTTLS pour cette seule exécution, puis retire les variables à l’arrêt du backend. Il ne stocke pas le secret sur disque.

### Répertoires administratifs disponibles

- `GET /api/admin/doctors/directory?page=0&size=50` renvoie les profils médecins approuvés (y compris les comptes inactifs) avec leurs coordonnées.
- `GET /api/admin/patients?page=0&size=50` renvoie les profils patients persistés.
- `PATCH /api/admin/doctors/{doctorId}/status` et `PATCH /api/admin/patients/{patientId}/status` acceptent `{"status":"ACTIVE"}` ou `{"status":"INACTIVE"}`. Toutes ces routes sont réservées à ADMIN.
- `GET /api/admin/doctors?status=PENDING` reste le répertoire des candidatures à approuver. Les écrans admin des rendez-vous, notifications et journal d’activité attendent encore leurs lots respectifs; l’interface les signale au lieu d’appeler des routes absentes.

## Contrat d'inscription et d'absence actualisé

La partie JSON `registration` de l’inscription médecin contient uniquement `firstName`, `lastName`, `email`, `phone`, `address`, `password`, `consent`, `specialty`, `licenseNumber` et `city`. La partie fichier `document` reste obligatoire. Aucun service, tarif ou horaire n’est créé pendant l’inscription. Le médecin approuvé les configure via PUT `/api/doctors/me` (profil et services) et PUT `/api/doctors/me/availabilities/weekly` (horaires et durées). Le profil ne reçoit plus de champs horaires ignorés.

POST `/api/doctors/me/absences/preview` accepte le même objet que la création d’absence et renvoie `{ "affectedAppointments": n }` sans mutation. Si des rendez-vous sont touchés, POST/PUT d’absence exigent `confirmRescheduling: true`, sinon renvoient HTTP 409. Les patients sont invités par notification interne à reprogrammer ; aucun motif privé d’absence n’est partagé. La nouvelle réservation issue de `TO_RESCHEDULE` repasse à `PENDING`.

GET/PUT `/api/admin/me` lisent/modifient les coordonnées de l’administrateur actif ; PUT demande `currentPassword`.

`python verify-workflows.py` lance dix scénarios HTTP sur une base MySQL temporaire `hello_doc_qa_*`, puis arrête le serveur et supprime uniquement cette base. Il nécessite le jar construit, MySQL Laragon local et le port 8081 libre. Ce script n’est pas destiné à la production ; ses identifiants fixes n’existent que dans sa base jetable.
