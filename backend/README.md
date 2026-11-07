# Hello Doc Spring Boot backend

This backend uses Java 21, Spring Boot, Spring Security, Spring Data JPA, BCrypt, and the MySQL server supplied by Laragon. It implements database-backed sign-in/session restoration/logout, patient registration as USER, doctor applications as pending USER accounts, admin review/approval, one-time ADMIN seeding, and protected admin profile/dashboard endpoints. Other business APIs listed in the backend specification are not implemented yet.

## Configure Laragon and start

1. Start Laragon's MySQL service. To create or verify only the `hello_doc` schema and start Spring Boot with passwords entered as secure prompts (not written to the project), run:

   ```powershell
   cd backend
   .\run-local.ps1
   ```

   The script targets MySQL at `127.0.0.1:3306`, username `root`, and runs `CREATE DATABASE IF NOT EXISTS hello_doc`. It does not change other databases. It asks for the MySQL password without echoing it; press Enter to use an empty password. It then separately requires the initial Admin password. Both values use process-only environment variables, which the script removes when Spring Boot stops.

   You can alternatively set environment variables manually in the backend process. Do not commit real secrets:

   ```powershell
   $env:HELLODOC_DB_URL = 'jdbc:mysql://localhost:3306/hello_doc?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
   $env:HELLODOC_DB_USERNAME = '<your Laragon MySQL username>'
   $env:HELLODOC_DB_PASSWORD = '' # leave empty for a root account with no password
   $env:HELLODOC_ADMIN_INITIAL_PASSWORD = '<initial administrator password>'
   ```

The initial Admin password is separate from the MySQL password. It is read only by the backend process, is never compiled into Angular, and is BCrypt-hashed before insertion. The seed runs only when `admin@hellodoc.com` is absent. On later starts it leaves the existing row and password untouched.

2. The initial sign-in is `admin@hellodoc.com` with the password supplied at the prompt. Immediately after login, open Admin > Sécurité and change it using the current and new password. The new password must contain at least eight characters. The run script removes the initial-password environment variable when stopped; the saved account is unaffected.

3. To run Maven commands separately, install Maven 3.9+ if not already installed:

   ```powershell
   cd backend
   mvn spring-boot:run
   ```

Hibernate creates/updates the `users` table. Database credentials and the initial password are intentionally required from environment variables and are not committed.

## Security and current API

- `GET /api/auth/csrf` issues the Angular-compatible `XSRF-TOKEN` cookie and token response.
- `POST /api/auth/login`, `GET /api/auth/me`, and `POST /api/auth/logout` use an HTTP-only server session cookie and CSRF protection for mutations. `POST /api/auth/password` checks the current password and stores a new BCrypt hash.
- Public patient/doctor registration assigns a regular `USER` role in the service; no request DTO accepts a role. There is no public admin creation endpoint.
- `GET /api/admin/me` and `GET /api/admin/dashboard` require `ROLE_ADMIN`. Security returns 401 for no session and 403 for an authenticated non-admin.
- Dashboard account and pending doctor application counts are read from persisted rows. Appointment metrics, activity logs, and notifications remain unavailable until those database features exist.
- Configure `HELLODOC_FRONTEND_ORIGIN` when Angular is served from another origin. Keep credentials enabled only for that exact trusted origin.

The Angular application models PATIENT, DOCTOR, ADMIN, and USER. Patient registrations create active USER accounts, which are admitted to the patient workspace. Doctor registrations create pending USER accounts and are blocked from login until an administrator approves them; approval assigns DOCTOR/APPROVED in the database. Doctor business endpoints are not implemented yet.
