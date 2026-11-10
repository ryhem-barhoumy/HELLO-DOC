# Hello Doc — Spring Boot Backend Specification

**Status:** account/session and profile foundations, specialty discovery, up to two weekly availability windows, full-day and partial absences, public availability calendar, appointment booking and management, persisted notifications, and post-consultation reviews are implemented. Patient and active-doctor dashboards expose persisted appointment and unread-notification data. Administrative appointment analytics, activity logs, and notification management are outside this implemented scope. **Base URL:** `http://localhost:8081/api` (Spring Boot defaults to port `8081`; set `SERVER_PORT` to override locally). **Database:** MySQL host, port, schema, username, and password are per-developer settings loaded from the ignored `backend/.env.local`; see `backend/.env.example`. No Laragon-specific port is shared. The Angular workspace accepts active `USER` accounts as patients and active `DOCTOR` accounts for the doctor area.

## 1. Product and frontend contract

Hello Doc connects patients seeking care, doctors managing consultations, and platform administrators operating the service. The existing repository contains a standalone Angular 20 application with Router, reactive forms, role-aware navigation, centralized `ApiService`, domain services, a functional-call error interceptor, and an HttpOnly-cookie integration target. The main pages are public home, sign in, patient and doctor registration, and protected patient, doctor, and admin workspaces. The workspace navigation covers doctor discovery, appointment tracking/history, availability, absence, patient lists, notifications, profile/settings, doctor approvals, account administration, and activity logs.

The backend owns identity, authorization, validation, persistence, scheduling, doctor approval, privacy, notification delivery, and the truth of every status/statistic. Angular never persists a user database, password, or session token. UI summaries are shown only when supplied by the API; unavailable API data must be reported as unavailable, never substituted with sample counts.

## 2. Existing and proposed frontend screens

| Screen / tab | Purpose and information | Actions and fields | Role | Backend operation and expected result |
|---|---|---|---|---|
| Public home `/` | Hello Doc identity, service explanation and role entry points | Open patient registration, doctor registration, login | Public | No private data. Optional public doctor search can call `GET /doctors` when wired. |
| Login `/connexion` | Sign-in for existing account | Email, password; submit; logout from workspace | Public / authenticated | `POST /auth/login` returns safe session user; session cookie set. Logout `POST /auth/logout`, 204. |
| Patient registration `/inscription/patient` | Create patient account | First/last name, email, phone, password, consent | Public | `POST /patients/registrations`, 201; unique email; password hashed; never return password. |
| Doctor registration `/inscription/medecin` | Submit professional account for review | Patient identity fields plus specialty, license number, city, practice address, consent | Public | `POST /doctors/registrations`, 201; status always `PENDING`; notify admins; response includes `PENDING`. |
| Access denied | Explain insufficient permission | Navigate back/sign in | Any | No operation; backend remains the security boundary. |
| Patient overview `/patient` | Future patient summary and next appointment | Open doctor search, appointments, history, notifications | PATIENT | `GET /patients/me/dashboard`; aggregate counts and next appointment; no fabricated values. |
| Doctor discovery `/patient/medecins` | Search approved active providers | Search by name, specialty, city; status filters; open doctor | PATIENT | `GET /doctors?q=&specialty=&city=&page=&size=`; paginated `Page<DoctorSummary>`. Current UI shows API state and result cards. |
| Doctor detail (linked from discovery) | Public professional identity, specialties, practice, available slots and review summary | Select slot, request booking | PATIENT | `GET /doctors/{id}`, `GET /doctors/{id}/availability?from=&to=`; only approved active doctor data. |
| Appointment request | Select doctor/slot, reason for visit | Submit booking; display outcome | PATIENT | `POST /appointments`; server checks slot, access, lead time and conflicts; 201 pending/confirmed according to agreed policy. |
| Patient appointments `/patient/rendez-vous` | Upcoming and current bookings | Search/filter status/date, details, permitted cancellation | PATIENT | `GET /appointments/mine`; `GET /appointments/{id}`; `POST /appointments/{id}/cancellation`. |
| Follow-up/history `/patient/historique` | Past and cancelled bookings | Filter and inspect details; review eligible completed visit | PATIENT | `GET /appointments/mine?history=true`; `POST /appointments/{id}/review` after completion, once per appointment. |
| Patient notifications | Alerts and booking updates | Read, mark as read | PATIENT | `GET /notifications`; `PATCH /notifications/{id}/read`. |
| Patient profile `/patient/profil` | Contact/profile information | Edit permitted fields | PATIENT | `GET/PUT /patients/me`; reject role/status/email privilege edits. |
| Patient settings `/patient/parametres` | Account preferences and security actions | Update notification preferences; request account deactivation/password change | PATIENT | Proposed `GET/PUT /patients/me/preferences`, `POST /auth/password`; confirm before destructive account action. |
| Doctor overview `/medecin` | Workday summary, next consultations and appointment counts | Open schedule/appointment modules | ACTIVE DOCTOR | `GET /doctors/me/dashboard`; status counts and next appointments. Pending/rejected/inactive accounts denied. |
| Doctor appointments and calendar | Day/week/month appointment schedule and status | Filter dates/status, inspect, confirm, cancel or complete where policy permits | ACTIVE DOCTOR | `GET /doctors/me/appointments?from=&to=&status=&page=`; `PATCH /appointments/{id}/status`. |
| Availability | Published recurring/dated slots | Add/update/delete slot; duration and timezone | ACTIVE DOCTOR | `GET/POST/PUT/DELETE /doctors/me/availability[/{id}]`; prevent overlap with appointments/absence. |
| Absences | Declared time off and affected appointments | Create absence, review proposed rescheduling, cancel if policy permits | ACTIVE DOCTOR | `GET/POST /doctors/me/absences`; `POST /absences/{id}/reschedule-plan`; `POST /appointments/{id}/reschedule`. Notify affected patients. |
| Doctor patient list | Patients with a valid care relationship/appointment | Search own patient list; open limited dossier | ACTIVE DOCTOR | `GET /doctors/me/patients`; `GET /doctors/me/patients/{patientId}`. Enforce relationship and minimum necessary fields. |
| Consultation note | Record clinical note for own appointment | Save note, update within policy | ACTIVE DOCTOR | `POST/PUT /appointments/{id}/consultation-note`; strict authorization/audit; excluded from admin DTOs. |
| Doctor reviews | Reviews associated with own profile | Read and optionally respond if product policy allows | ACTIVE DOCTOR | `GET /doctors/me/reviews`; optional response endpoint needs product decision. |
| Doctor notifications | Appointment, absence and admin decision messages | Mark read | ACTIVE DOCTOR / PENDING registration notices | `GET /notifications`; `PATCH /notifications/{id}/read`. Pending doctor may only access registration status/notifications. |
| Doctor profile/settings | Professional details and preferences | Edit authorized fields; request account change | ACTIVE DOCTOR | `GET/PUT /doctors/me`; credentials and approval status are server-managed. |
| Admin overview `/admin` | Counts for patients, doctors, pending approvals and appointments; recent activity | Review queues and navigate modules | ADMIN | `GET /admin/dashboard`; server-computed counts with as-of time; no consultation details. |
| Admin doctors | Provider directory and account state | Search, status/specialty filters, inspect safe profile, activate/deactivate | ADMIN | `GET /admin/doctors`; `GET /admin/doctors/{id}`; `PATCH /admin/doctors/{id}/status`. |
| Doctor approvals | Pending requests | Inspect submitted professional data, approve or reject with reason | ADMIN | `GET /admin/doctors?status=PENDING`; `POST /admin/doctors/{id}/approve` or `/reject`; atomic decision, audit and notification. |
| Admin patients | Patient accounts and minimal appointment metadata | Search/filter, view profile, activate/deactivate | ADMIN | `GET /admin/patients`; `GET /admin/patients/{id}`; `PATCH /admin/patients/{id}/status`. Do not expose clinical notes. |
| Admin appointments | Operational appointment list | Search doctor/patient/date/status; inspect; allowed status correction/cancellation | ADMIN | `GET /admin/appointments`; `GET /admin/appointments/{id}`; `PATCH /admin/appointments/{id}/status`, audit all changes. |
| Admin notifications | Pending approvals, service alerts and history | Filter/read/mark read | ADMIN | `GET /admin/notifications`; `PATCH /notifications/{id}/read`. |
| Activity log | Administrative actions, actor and timestamp | Filter actor/action/date, paginate | ADMIN | `GET /admin/activity-logs`; immutable, paginated audit records. |
| Admin profile/logout | Safe administrator profile and session exit | View/update allowed contact fields, logout | ADMIN | `GET/PUT /admin/me`; `POST /auth/logout`. No role escalation. |

**Current implementation boundary (after report recovery):** Angular includes actual route-level components and HTTP-backed forms for sign-in, patient/doctor registration, doctor search and profile, appointment booking and details, patient review, appointment cancellation/status changes, appointment rescheduling, doctor schedule/availability, absence declaration/removal, doctor patient dossier, consultation notes, account profile, password change, notifications/read state, and admin directory/approval/status actions/dashboard projections. The doctor monthly calendar is implemented: it requests appointments bounded by the visible month, renders them on a Monday-first calendar grid, and provides previous/next month and today controls. These actions are wired to the proposed endpoints and display API errors honestly; without Spring Boot they cannot complete successfully against persistent data. Admin chart/activity/registration panels render only when the dashboard response contains those fields. Profile image upload, admin patient-detail page, delete-account workflow, notification preferences editor, and a full appointment/absence history audit UI remain unimplemented. The report documents no platform-admin role; the platform-admin area is an additional requirement from the later Hello Doc brief and remains distinct from the historical doctor role. Do not infer an implemented screen from a backend endpoint alone.

## 3. Domain and database model

Use the Laragon MySQL server with UUID primary keys, UTC timestamps, explicit clinic timezone (IANA identifier), `created_at`, `updated_at`, optimistic version where concurrent edits are possible, and migrations (Flyway/Liquibase). Store only necessary clinical and identity data. Use soft deactivation/status where audit history must remain. The current auth implementation uses Hibernate schema update for the initial `users` table; migration-managed schema is still recommended before production.

### User (identity and login)

`id UUID PK`, `email VARCHAR(254) UNIQUE NOT NULL` (normalized lowercase), `password_hash VARCHAR(255) NOT NULL`, `first_name VARCHAR(100)`, `last_name VARCHAR(100)`, `role VARCHAR(16) NOT NULL CHECK IN ADMIN/DOCTOR/PATIENT/USER`, `account_status VARCHAR(16) NOT NULL`. The current Java entity persists this core. Planned profile and audit columns are not implemented yet. Password hash only, never a password.

### Patient

`id UUID PK`, `user_id UUID FK UNIQUE NOT NULL`, optional `birth_date DATE`, `preferred_language VARCHAR(10)`, `notification_preferences JSONB` (or normalized preference columns), timestamps. Avoid collecting health history unless an explicit clinical need and retention policy are approved. 1:1 with User; 1:N appointments and notifications.

### Doctor

`id UUID PK`, `user_id UUID FK UNIQUE NOT NULL`, `specialty VARCHAR(120) NOT NULL`, `license_number VARCHAR(100) NOT NULL`, `license_country VARCHAR(2)`, `practice_name VARCHAR(180)`, `practice_address VARCHAR(300)`, `city VARCHAR(120)`, `location VARCHAR(300)`, `bio TEXT` (bounded), `appointment_duration_minutes SMALLINT`, `work_start TIME`, `work_end TIME`, `working_days SMALLINT[]` (0 Sunday through 6 Saturday), `break_minutes SMALLINT`, `approval_status VARCHAR(16) NOT NULL` (`PENDING/APPROVED/REJECTED`), `rejection_reason TEXT NULL`, `reviewed_by UUID FK User NULL`, `reviewed_at TIMESTAMPTZ NULL`, `is_active BOOLEAN NOT NULL`, timestamps, version. Require work end after start, positive duration, nonnegative break and at least one workday. Unique license number within issuing jurisdiction; indexes `(approval_status, is_active)`, city, specialty and normalized names. 1:N services, availability, absences, appointments, reviews.

### MedicalService (called ServiceMedical in the report)

`id UUID PK`, `doctor_id UUID FK NOT NULL`, `name VARCHAR(160) NOT NULL`, `price_amount NUMERIC(10,2) NOT NULL CHECK >= 0`, `currency CHAR(3) NOT NULL` (initial UI displays DT), `is_active BOOLEAN NOT NULL`, timestamps. Index `(doctor_id,is_active)`. A Doctor has many services; each appointment references its chosen service and also snapshots displayed name, price and currency so editing a tariff does not rewrite historical bookings. No separate category/service-type entity is needed for the report’s v1 catalog.

### Appointment

`id UUID PK`, `patient_id UUID FK NOT NULL`, `doctor_id UUID FK NOT NULL`, `medical_service_id UUID FK NULL` (retain nullable for a discontinued service), `service_name_snapshot VARCHAR(160) NOT NULL`, `price_snapshot NUMERIC(10,2) NULL`, `currency CHAR(3) NULL`, `starts_at TIMESTAMPTZ NOT NULL`, `ends_at TIMESTAMPTZ NOT NULL`, `timezone VARCHAR(64) NOT NULL`, `status VARCHAR(24) NOT NULL` (`PENDING/CONFIRMED/CANCELLED/TO_RESCHEDULE/COMPLETED/RESCHEDULED`), `reason VARCHAR(500) NULL` (not a diagnosis), optional patient contact snapshot fields as legally justified, `cancellation_reason VARCHAR(500) NULL`, `cancellation_comment VARCHAR(1000) NULL`, `created_by UUID FK User`, `cancelled_by UUID FK User NULL`, `rescheduled_from UUID FK Appointment NULL`, `absence_id UUID FK Absence NULL`, timestamps, version. `ends_at > starts_at`; index doctor/start/status and patient/start/status. Prevent overlapping active appointments for a doctor transactionally (PostgreSQL exclusion constraint on `tstzrange` is preferred); a unique lock/serializable transaction can be used. Cardinality: Patient 1:N Appointment, Doctor 1:N Appointment, MedicalService 1:N Appointment.

### Availability

`id UUID PK`, `doctor_id UUID FK NOT NULL`, `starts_at TIMESTAMPTZ`, `ends_at TIMESTAMPTZ`, `timezone VARCHAR(64)`, `status VARCHAR(16)` (`AVAILABLE/BOOKED/BLOCKED`), `absence_id UUID FK NULL`, timestamps. Require positive interval, no overlap, and unique slot reservation. Index `(doctor_id, starts_at, ends_at, status)`. Generate dated slots from Doctor working days/hours/duration/break on bounded demand or persist a rolling window. Never return past, booked, absent, or blocked slots as bookable. Define timezone/DST behavior explicitly.

### Absence

`id UUID PK`, `doctor_id UUID FK NOT NULL`, `starts_at`, `ends_at`, `reason VARCHAR(500) NULL`, `status VARCHAR(16)` (`ACTIVE/CANCELLED`), `created_at`, `updated_at`. On creation, find overlapping appointments, create a rescheduling plan and notify patient/doctor. Never silently move appointments. Index doctor and interval.

### Review

`id UUID PK`, `appointment_id UUID FK UNIQUE NOT NULL`, `patient_id UUID FK NOT NULL`, `doctor_id UUID FK NOT NULL`, `rating SMALLINT CHECK 1..5`, `comment VARCHAR(2000) NULL`, `recommended BOOLEAN NOT NULL`, `visibility VARCHAR(16)` (`PUBLISHED/HIDDEN/PENDING_MODERATION`), timestamps. Only the patient on a completed appointment can create/edit; one review per appointment. Index doctor/visibility/date. Admin sees moderation metadata, not unrelated medical data.

### Notification

`id UUID PK`, `recipient_user_id UUID FK NOT NULL`, `type VARCHAR(40)`, `title VARCHAR(180)`, `body VARCHAR(1000)`, `resource_type VARCHAR(40) NULL`, `resource_id UUID NULL`, `action_url VARCHAR(500) NULL`, `action_label VARCHAR(80) NULL`, `read_at TIMESTAMPTZ NULL`, `created_at`. Index recipient/read/date. Do not include diagnosis or consultation note text in notification content.

### ConsultationNote (optional clinical feature)

`id UUID PK`, `appointment_id UUID FK UNIQUE NOT NULL`, `doctor_id UUID FK NOT NULL`, `patient_id UUID FK NOT NULL`, `resume TEXT`, `observation TEXT`, `treatment TEXT`, `next_check DATE NULL`, `private_note TEXT NULL`, `created_at`, `updated_at`, `version`. Treat as sensitive health data: restrict to treating doctor; patient access should be an explicit product/legal decision. Admin APIs must never serialize it. Encrypt database/storage backups and audit reads/changes. Confirm applicable health-data law, retention, consent and hosting before production.

### ActivityLog

`id UUID PK`, `actor_user_id UUID FK NULL` (retain actor snapshot if account later removed), `action VARCHAR(80) NOT NULL`, `target_type VARCHAR(60)`, `target_id UUID NULL`, `occurred_at TIMESTAMPTZ NOT NULL`, `correlation_id VARCHAR(80)`, `safe_metadata JSONB` with allow-listed fields only. Append-only; index actor/date and action/date. Never copy passwords, session IDs, tokens, consultation text or unnecessary personal data into metadata.

**Relationships:** User 1:1 Patient/Doctor by role; User 1:N Notification; Doctor 1:N MedicalService/Availability/Absence/Appointment/Review; Patient 1:N Appointment/Review; MedicalService 1:N historical appointments; Appointment 0:1 Review and 0:1 ConsultationNote. `ActivityLog.actor_user_id` references User. Consider a separate session table only if the chosen server-side session store requires persistence; Spring Security session cookies must not contain credentials.

## 4. REST API conventions and common DTOs

All endpoints below are **proposed future Spring Boot endpoints**. JSON is UTF-8; timestamps are ISO-8601 UTC (`2026-05-01T09:30:00Z`). Paginated responses:

```json
{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0}
```

Safe session user:

```json
{"id":"5d66a2d9-18f2-4618-b4e0-1e6e75d56727","firstName":"Maya","lastName":"Ben Salah","email":"maya@example.tn","role":"PATIENT","status":"ACTIVE"}
```

Validation errors return `application/problem+json` or a stable equivalent:

```json
{"status":400,"code":"VALIDATION_ERROR","message":"Some fields are invalid.","fieldErrors":{"email":"Email is already registered."},"timestamp":"2026-05-01T09:30:00Z"}
```

Use `400` malformed/field validation, `401` unauthenticated/expired session, `403` role or ownership denied, `404` unknown resource (and optionally concealed unauthorized resources), `409` duplicate/conflict/slot taken/decision already applied, `422` semantically invalid business transition, `429` throttling, `500` unexpected error with correlation ID only. `201` create, `200` response, `204` successful no-content update/delete. Never reveal stack traces.

### 4.1 Authentication

| Method / path | Role | Request and response | Rules/errors |
|---|---|---|---|
| `POST /auth/login` | Public | `{ "email":"maya@example.tn", "password":"…" }` → safe session user; set session cookie | Normalize email; rate limit; generic invalid-credential message; 401; PENDING doctor returns registration state or 403 `DOCTOR_PENDING` (settle client UX). |
| `GET /auth/me` | Authenticated | No body → safe session user including role/status | 401 if absent/expired. Used by guards after reload. |
| `POST /auth/logout` | Authenticated | CSRF-protected, empty → 204; invalidate server session and clear cookie | Idempotent logout policy may return 204. |
| `GET /auth/csrf` | Public/session bootstrap | Return `{ "headerName":"X-XSRF-TOKEN", "token":"…" }` or issue readable CSRF cookie per Spring setup | Angular interceptor must send token for unsafe methods. Do not make auth exempt by default. |
| `POST /auth/password` | Authenticated | `{ "currentPassword":"…", "newPassword":"…" }` → 204 | Verify current password, policy and revoke other sessions; errors 400/401. |

`GET /auth/csrf` must issue a readable `XSRF-TOKEN` cookie and return the matching header name. Angular calls it before login and public registration, then sends `X-XSRF-TOKEN` on POST/PUT/PATCH/DELETE. CORS must allow credentials for the configured frontend origin.

Patient registration request: `{ "firstName":"Maya", "lastName":"Ben Salah", "email":"maya@example.tn", "phone":"+216…", "address":"…", "password":"…", "consent":true }`. Response `201 {"id":"…","status":"ACTIVE"}`. Patient phone is optional in the legacy registration; reservation contact fields must be available when required. Doctor registration adds `{ "specialty":"Cardiology", "licenseNumber":"TN-…", "city":"Tunis", "address":"…", "serviceTitle":"Dr. Maya…", "durationMinutes":30, "workStart":"08:00", "workEnd":"17:00", "workingDays":["1","2","3","4","5","6"], "pauseMinutes":15, "location":"…", "description":"…", "services":[{"name":"Consultation","price":"50 DT"}] }`, response `201 {"id":"…","status":"PENDING"}`. The registration UI requires at least one valid named service/price and one working day. Consent version/time must be recorded if required by policy; never echo password or password confirmation.

### 4.2 Patient

| Method / path | Access | Contract |
|---|---|---|
| `POST /patients/registrations` | Public | Patient registration above; 201; duplicate email 409; invalid consent/fields 400. |
| `GET /patients/me` | PATIENT | Safe patient profile DTO; never include password hash or unrelated medical record. |
| `PUT /patients/me` | PATIENT | `{firstName,lastName,phone,address,currentPassword}`; 200; current password is required by the historical account-edit flow. Validate ownership/field lengths. Email change needs verified workflow. |
| `GET /patients/me/dashboard` | PATIENT | `{upcomingAppointment,upcomingCount,pastCount,unreadNotificationCount}`; server as-of time. |
| `GET /patients/me/preferences` / `PUT /patients/me/preferences` | PATIENT | Notification preferences, language; validate allowed keys. |
| `GET /patients/me/appointments` | PATIENT (legacy alias if used) | Prefer shared `GET /appointments/mine`; paginated own records. |

### 4.3 Doctor and public discovery

| Method / path | Access | Contract |
|---|---|---|
| `POST /doctors/registrations` | Public | Doctor registration; always creates PENDING; 409 duplicate email/license. |
| `GET /doctors` | Public or PATIENT | Query `q,specialty,city,availableOn,page,size,sort`; only APPROVED and active providers; paginated safe summary. `availableOn` must test a real bookable slot, not just a usual workday. |
| `GET /doctors/{doctorId}` | Public or PATIENT | Public profile with displayed title, specialty, description, location, duration, working days/hours, service names/prices and approved public reviews; omit private email/license documents. 404 if not published. |
| `GET /doctors/{doctorId}/availability` | Public or PATIENT | `from,to,timezone`; return bookable slots only; bounded date range. |
| `GET /doctors/me` / `PUT /doctors/me` | ACTIVE DOCTOR | Safe own profile. Update permits name/contact, service title, specialty, duration, work hours/days, break, location, description and service list; require `currentPassword` for the profile edit as in the legacy flow. License changes require re-review. |
| `GET /doctors/me/dashboard` | ACTIVE DOCTOR | `{todayCount,pendingCount,confirmedCount,completedCount,nextAppointments:[]}`. |
| `GET /doctors/me/appointments` | ACTIVE DOCTOR | Query `from,to,status,page,size`; own doctor appointments only. |
| `GET /doctors/me/patients` | ACTIVE DOCTOR | Paginated patients with prior/active appointment relationship only; minimal fields. |
| `GET /doctors/me/patients/{patientId}` | ACTIVE DOCTOR | Dossier summary only if care relationship and policy authorize; otherwise 404/403. |
| `GET /doctors/me/reviews` | ACTIVE DOCTOR | Published reviews for this doctor, paginated. |

### 4.4 Appointments

Appointment create JSON: `{ "doctorId":"uuid", "availabilityId":"uuid", "startsAt":"2026-05-04T09:30:00Z", "reason":"Consultation", "timezone":"Africa/Tunis" }`. The server derives patient from session, validates that doctor is approved/active and slot still free, and atomically reserves it. Response 201:

```json
{"id":"…","startsAt":"2026-05-04T09:30:00Z","endsAt":"2026-05-04T10:00:00Z","status":"PENDING","doctorName":"Dr. …","patientName":"Maya …","reason":"Consultation"}
```

| Method / path | Access | Business contract |
|---|---|---|
| `POST /appointments` | PATIENT | Create own booking; request includes `doctorId`, `availabilityId`, `serviceId`, `startsAt`, `timezone`, `reason` and a contact snapshot (`patientName`, `phone`, `email`, `address`) if the backend does not derive it from the profile. The server loads the authoritative service name/price; never trust a client-supplied price. Store a price/service snapshot on the appointment. Transactionally reject past dates, non-workdays, absences, blocked/taken slots and conflicts; create in `PENDING`; notify the doctor. |
| `GET /appointments/mine` | PATIENT | Query `history,from,to,status,page,size`; only current patient records. |
| `GET /appointments/{id}` | Owning PATIENT, assigned DOCTOR, ADMIN | DTO is role-sensitive; admin DTO excludes clinical note/reason if unnecessary. Conceal unauthorized IDs as 404 if appropriate. |
| `POST /appointments/{id}/cancellation` | Owning PATIENT or assigned DOCTOR; ADMIN only authorized workflow | `{ "reason":"…", "comment":"…" }`; enforce the agreed cancellation cutoff and state transition; notify counterpart; retain cancellation reason/comment and release slot if still valid; 409/422 otherwise. |
| `PATCH /appointments/{id}/status` | Assigned DOCTOR or ADMIN | `{ "status":"CONFIRMED" }`; allow `PENDING → CONFIRMED/CANCELLED` and `CONFIRMED → COMPLETED/CANCELLED`; absence service may also transition active states to `TO_RESCHEDULE`. Audit actor and notify patient. A past confirmed slot does not auto-complete: the doctor marks completion. |
| `GET /appointments/{id}/reschedule-options` | Owning PATIENT or assigned DOCTOR | Return available slots for the same provider after absence/conflict; enforce ownership. |
| `POST /appointments/{id}/reschedule` | Owning PATIENT or assigned DOCTOR per policy | `{ "availabilityId":"uuid", "reason":"…" }`; lock destination, retain old appointment/status history, change to `RESCHEDULED` or new pending booking by agreed policy, notify both parties. |
| `POST /appointments/{id}/review` | Owning PATIENT | `{ "rating":5,"recommend":"YES","comment":"…" }`; completed appointment only. One review per appointment; permit editing that review with the same endpoint or `PUT`. |
| `GET/PUT /appointments/{id}/consultation-note` | Assigned ACTIVE DOCTOR | `{ "resume":"…","observation":"…","treatment":"…","nextCheck":"2026-05-20","privateNote":"…" }`; only after completed appointment; highly sensitive, strict audit and access checks. A changed next-check date creates an in-app patient notification with an action link to choose a slot. |

Suggested status transitions matching the report: `PENDING → CONFIRMED | CANCELLED | TO_RESCHEDULE`; `CONFIRMED → COMPLETED | CANCELLED | TO_RESCHEDULE`; a doctor-created absence sets overlapping active appointments to `TO_RESCHEDULE`. Completing an explicit reschedule may create a linked replacement appointment in `PENDING` while retaining the original for history, or transition the original to `RESCHEDULED`; choose one transactional policy. `COMPLETED` and `CANCELLED` are terminal for ordinary actions. A new patient booking starts `PENDING` and requires doctor confirmation.

### 4.5 Availability and absences

| Method / path | Access | Contract |
|---|---|---|
| `GET /doctors/me/availability` | ACTIVE DOCTOR | List own availability; optionally query from/to. |
| `POST /doctors/me/availability` | ACTIVE DOCTOR | `{ "startsAt":"…", "endsAt":"…", "timezone":"Africa/Tunis" }`; 201; reject overlap/past slots. The doctor profile’s duration, workdays, work interval and pause are the source for generated slots; avoid incompatible manual slots. |
| `PUT /doctors/me/availability/{id}` | ACTIVE DOCTOR | Update own unbooked slot; 409 if appointment conflicts. |
| `DELETE /doctors/me/availability/{id}` | ACTIVE DOCTOR | 204 only if no confirmed booking; otherwise 409. |
| `GET /doctors/me/absences` | ACTIVE DOCTOR | Own absence intervals and rescheduling outcome summary. |
| `POST /doctors/me/absences` | ACTIVE DOCTOR | `{ "startsAt":"…", "endsAt":"…", "reason":"…", "comment":"…" }`; block unreserved slots, set overlapping pending/confirmed appointments to `TO_RESCHEDULE`, notify affected patients and return affected appointment IDs. Do not silently cancel or move appointments. |
| `POST /absences/{id}/reschedule-plan` | Assigned DOCTOR | `{ "strategy":"PROPOSE_ALTERNATIVES" }`; return affected booking IDs and candidate slots, never auto-commit. |
| `POST /appointments/{id}/reschedule` | Assigned parties | Select an offered slot and explicitly confirm. |
| `DELETE /doctors/me/absences/{id}` | ACTIVE DOCTOR | Mark absence cancelled if policy allows; reopen only future unreserved slots blocked solely by that absence. Previously affected appointments remain `TO_RESCHEDULE` and are not restored automatically. |

### 4.6 Reviews and notifications

`GET /doctors/me/reviews` returns `{content:[{id,rating,comment,createdAt,patientDisplayName}],...}` with privacy-preserving display name. `POST /appointments/{id}/review` uses appointment constraints above. Review moderation endpoints may be added for ADMIN after moderation policy is decided.

| Method / path | Access | Contract |
|---|---|---|
| `GET /notifications` | Authenticated | Current user’s paginated notifications only. Admin also has operational admin feed. |
| `PATCH /notifications/{id}/read` | Owning recipient | Set `read_at`; 204; reject another user’s notification. |
| `POST /admin/notifications` | ADMIN, optional | `{ "recipientUserId":"…", "title":"…", "body":"…", "type":"…" }`; restrict manual broadcast and audit. |

### 4.7 Admin

| Method / path | Access | Contract |
|---|---|---|
| `GET /admin/dashboard` | ADMIN | `{asOf,patientsCount,doctorsCount,pendingDoctorApprovals,appointments:{total,confirmed,pending,cancelled},recentRegistrations:[],recentActivities:[],importantNotifications:[]}`. Counts from database, documented date range/time zone. |
| `GET /admin/doctors` | ADMIN | `q,status,specialty,city,page,size,sort`; paginated safe doctor summaries. |
| `GET /admin/doctors/{id}` | ADMIN | Registration/professional review data; no passwords or clinical note. |
| `POST /admin/doctors/{id}/approve` | ADMIN | Optional `{ "comment":"…" }`; only PENDING; atomically APPROVED, set reviewer/time, audit and notify. 409 if already decided. |
| `POST /admin/doctors/{id}/reject` | ADMIN | `{ "reason":"…" }` required; set REJECTED, audit, notify. Re-review policy explicit. |
| `PATCH /admin/doctors/{id}/status` | ADMIN | `{ "status":"ACTIVE" }`; activation does not bypass approval. Deactivation blocks new bookings and has policy for current bookings. |
| `GET /admin/specialties` | ADMIN | List the persisted medical specialties. Public registration still uses `GET /specialties`. |
| `POST /admin/specialties` | ADMIN | Create `{ "name":"Neurologie" }`; trimmed nonblank name, maximum 120 characters, unique after case/accent normalization. Returns 201. |
| `PUT /admin/specialties/{id}` | ADMIN | Rename a specialty using the same validation and uniqueness rules. |
| `DELETE /admin/specialties/{id}` | ADMIN | Delete only when no doctor is associated; otherwise return 409 with the associated doctor count. |
| `GET /admin/patients` / `GET /admin/patients/{id}` | ADMIN | Query `q,status,page,size`; safe account and appointment metadata only; no consultation note or diagnosis. |
| `PATCH /admin/patients/{id}/status` | ADMIN | `{ "status":"INACTIVE" }`; audit actor/time and define existing appointment handling. |
| `GET /admin/appointments` | ADMIN | `doctorId,patientId,from,to,status,page,size`; minimal operational projection. |
| `GET /admin/appointments/{id}` | ADMIN | Operational details; exclude clinical note and sensitive reason unless necessary. |
| `PATCH /admin/appointments/{id}/status` | ADMIN | `{ "status":"CANCELLED","reason":"…" }`; explicit authorized transitions, notify parties and audit. |
| `GET /admin/notifications` | ADMIN | Paginated operational notifications / unread counts. |
| `GET /admin/activity-logs` | ADMIN | `actorId,action,from,to,page,size`; immutable events with safe metadata. |
| `GET /admin/me` / `PUT /admin/me` | ADMIN | Own display/contact profile; cannot change role or create another admin through this API. |

## 5. Doctor approval workflow

1. Public doctor registration validates required identity, specialty, license, city/address, consent, password and unique email/license.
2. In one transaction create User(role `USER`, account status `PENDING`) and a Doctor profile with pending approval and Doctor(approval status `PENDING`, inactive until approved). Hash password with BCrypt/Argon2 encoder.
3. Create a notification for administrators; if no notification routing exists yet, the pending query remains authoritative.
4. `POST /auth/login` may establish a limited pending registration session, but pending users must not access any doctor business endpoint. Provide a status/notice endpoint if they need to check review state.
5. Admin reads pending queue, inspects submitted credentials and decides. Approval/rejection must lock/compare status to prevent concurrent decisions.
6. Approval sets `approval_status=APPROVED`, `account_status=ACTIVE`, `is_active=true`, `reviewed_by`, and `reviewed_at`; rejection sets `REJECTED`, records reason, keeps protected access disabled.
7. Insert append-only ActivityLog and doctor notification in the same transaction; external email delivery can run asynchronously after commit.
8. Return 200 with updated safe summary. Duplicate/already reviewed decision returns 409. Every doctor API additionally enforces approved + active at service/security layer, not just Angular guards.

## 6. Spring Boot architecture and class map

Suggested package layout:

```text
com.hellodoc
  config/{CorsConfig,JacksonConfig,ClockConfig}.java
  security/{SecurityConfig,UserPrincipal,SessionUser,CurrentUser,RoleAuthorization}.java
  controller/{AuthController,PatientController,DoctorController,AppointmentController,
              AvailabilityController,AbsenceController,ReviewController,NotificationController,
              AdminController,ActivityLogController}.java
  dto/{auth,patient,doctor,appointment,availability,absence,review,notification,admin}/...
  entity/{User,Patient,Doctor,Appointment,Availability,Absence,Review,Notification,
          ConsultationNote,ActivityLog}.java
  repository/{UserRepository,PatientRepository,DoctorRepository,AppointmentRepository,
              AvailabilityRepository,AbsenceRepository,ReviewRepository,NotificationRepository,
              ConsultationNoteRepository,ActivityLogRepository}.java
  service/{AuthService,PatientService,DoctorService,DoctorApprovalService,AppointmentService,
           SchedulingService,AbsenceService,ReviewService,NotificationService,AdminService,
           ActivityLogService}.java
  mapper/{PatientMapper,DoctorMapper,AppointmentMapper,AdminMapper}.java
  exception/{ApiExceptionHandler,NotFoundException,ConflictException,ForbiddenException}.java
  validation/{StrongPassword,ValidAppointmentWindow,...}.java
```

Controllers bind/validate DTOs and return HTTP only. Services enforce business rules/transactions and authorization context. Repositories own persistence queries and projections. Entities never serialize directly. Mappers build explicit role-sensitive response DTOs. `@ControllerAdvice` maps domain exceptions to stable problem JSON. Use Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`, custom interval validators) on request DTOs. Use method authorization such as `@PreAuthorize` plus service ownership checks; do not trust a submitted user ID.

## 7. Spring Security and privacy requirements

- Use Spring Security with an appropriate `PasswordEncoder` (BCrypt, calibrated; or Argon2 where operationally supported). Never log submitted credentials.
- Prefer server-side session authentication: random session ID in `Secure; HttpOnly; SameSite=Lax` cookie in production, `Secure` only behind HTTPS; cookie path/domain scoped. Do not place JWT/access tokens in localStorage/sessionStorage.
- Invalidate and rotate session identifiers on login and privilege change. Configure idle/absolute expiry, concurrent-session policy, logout invalidation and session fixation defense.
- Keep CSRF enabled for cookie-authenticated unsafe methods. Expose a CSRF bootstrap endpoint or use Spring's cookie repository with a frontend-compatible header; verify token on POST/PUT/PATCH/DELETE.
- CORS allow only explicit frontend origins; `allowCredentials=true`; never combine credentials with wildcard origin. Allow required methods/headers and expose CSRF header deliberately.
- Protect controller routes by role and enforce record ownership. Doctor role additionally requires APPROVED and ACTIVE; admin role is separate and never inherited by a doctor.
- Rate limit login/registration and sensitive operations; generic login failure responses; optionally verify email and professional credentials.
- Minimize API DTOs, encrypt backups and sensitive fields where appropriate, TLS everywhere in deployment, audit access to consultation notes, define retention/deletion and incident response according to applicable Tunisian and other relevant health/privacy law.
- Admin dashboard/log DTOs must omit consultation notes, diagnosis and unnecessary patient data. Do not put sensitive data in URLs, notifications, audit metadata or logs.

## 8. Angular-to-backend map

The Angular code defines `AuthService`, `ApiService`, `PatientService`, `DoctorService`, `AppointmentService`, `AdminService`, `NotificationService`, and `ActivityLogService`. `ApiService` centralizes the base URL and `withCredentials`; CSRF and error interceptors prepare mutation requests and translate failures. Auth, booking, and review use their domain services; workspace actions call the same centralized `ApiService` directly. The route guard calls `/auth/me` when no in-memory user exists and restricts ADMIN/DOCTOR/PATIENT; server enforcement remains mandatory.

| Angular page/component | User action | Angular service/current call | Proposed endpoint | Controller → service → entity | Role |
|---|---|---|---|---|---|
| LandingComponent | Start patient journey | Router only | optional `GET /doctors` | DoctorController → DoctorService → Doctor | Public |
| AuthComponent `/connexion` | Sign in | AuthService.login | `POST /auth/login` | AuthController → AuthService → User | Public |
| AuthComponent patient mode | Register patient | PatientService.register | `POST /patients/registrations` | PatientController → PatientService → User, Patient | Public |
| AuthComponent doctor mode | Register professional | DoctorService.register | `POST /doctors/registrations` | DoctorController → DoctorService → User, Doctor, Notification | Public |
| roleGuard / protected workspace | Restore session and enforce role | AuthService.me | `GET /auth/me` | AuthController → AuthService → User | Authenticated |
| Patient overview | Dashboard | ApiService GET `/patients/me/dashboard` | `GET /patients/me/dashboard` | PatientController → PatientService → Appointment, Notification | PATIENT |
| Patient discovery | Search/filter doctors | PatientService.doctors | `GET /doctors` | DoctorController → DoctorService → Doctor | PATIENT |
| Patient doctor detail | Profile/slots | ApiService | `GET /doctors/{id}`, `/doctors/{id}/availability` | DoctorController → DoctorService → Doctor, Availability | PATIENT |
| Patient booking | Book/cancel/review | AppointmentService | POST `/appointments`; cancellation/review subroutes | AppointmentController → AppointmentService → Appointment, Review | PATIENT |
| Patient appointments/history | List/filter | PatientService.appointments | `GET /appointments/mine` | AppointmentController → AppointmentService → Appointment | PATIENT |
| Patient profile/settings | Read/edit | PatientService.profile/updateProfile | `GET/PUT /patients/me`, preference routes | PatientController → PatientService → Patient | PATIENT |
| Patient notifications | List/read | NotificationService | `GET /notifications`, `PATCH /notifications/{id}/read` | NotificationController → NotificationService → Notification | PATIENT |
| Doctor overview | Dashboard | ApiService GET `/doctors/me/dashboard` | `GET /doctors/me/dashboard` | DoctorController → DoctorService → Doctor, Appointment | ACTIVE DOCTOR |
| Doctor appointments/calendar | Browse/change state | DoctorService.appointments | `GET /doctors/me/appointments`, `PATCH /appointments/{id}/status` | AppointmentController → AppointmentService → Appointment | ACTIVE DOCTOR |
| Doctor availability | Manage slots | DoctorService.availability | `/doctors/me/availability` CRUD | AvailabilityController → SchedulingService → Availability | ACTIVE DOCTOR |
| Doctor absence | Declare/reschedule | DoctorService.absences | `/doctors/me/absences`, reschedule plan | AbsenceController → AbsenceService → Absence, Appointment | ACTIVE DOCTOR |
| Doctor patients/dossier | Search own patients | ApiService | `/doctors/me/patients` | DoctorController → DoctorService → Patient, Appointment | ACTIVE DOCTOR |
| Doctor consultation | Write note | AppointmentService (extend) | appointment consultation-note route | AppointmentController → AppointmentService → ConsultationNote | ACTIVE DOCTOR |
| Doctor reviews/profile | Browse/edit | DoctorService.profile | `/doctors/me/reviews`, `/doctors/me` | DoctorController → DoctorService → Review, Doctor | ACTIVE DOCTOR |
| Admin overview | Statistics | AdminService.dashboard | `GET /admin/dashboard` | AdminController → AdminService → aggregate repositories | ADMIN |
| Admin doctors | Search/manage | AdminService.doctors/setAccountState | `/admin/doctors` | AdminController → AdminService → Doctor, User | ADMIN |
| Admin approvals | Approve/reject | AdminService.decideDoctor | `/admin/doctors/{id}/approve|reject` | AdminController → DoctorApprovalService → Doctor, ActivityLog, Notification | ADMIN |
| Admin patients | Search/activate | AdminService.patients/setAccountState | `/admin/patients` | AdminController → AdminService → Patient, User | ADMIN |
| Admin appointments | Filter/manage | AdminService.appointments | `/admin/appointments` | AdminController → AppointmentService → Appointment | ADMIN |
| Admin notifications | Browse | AdminService.notifications | `/admin/notifications` | NotificationController → NotificationService → Notification | ADMIN |
| Admin activity | Search audit trail | ActivityLogService.list | `/admin/activity-logs` | ActivityLogController → ActivityLogService → ActivityLog | ADMIN |
| Admin profile/logout | Profile/session end | ApiService/AuthService.logout | `/admin/me`, `/auth/logout` | AdminController/AuthController → services → User/session | ADMIN |

## 9. Development-only seed accounts

Use a `dev` profile-only `CommandLineRunner` or explicit local SQL migration guarded by `@Profile("dev")`. Read demo credentials from local environment variables or generate random passwords and print them once only in local console; hash with the configured `PasswordEncoder`. Make seed idempotent by normalized email, create one account for each `ADMIN`, active `DOCTOR`, and `PATIENT`, and use obviously non-production addresses. Disable the runner in test/production profiles; add a startup assertion that refuses demo seed flags in production. Never commit secrets, never embed accounts in Angular, and never ship seeded credentials to production.

## 10. Backend delivery roadmap

1. **Spring Boot setup:** Java LTS, Spring Boot, Web, Validation, Security, Data JPA, PostgreSQL driver, Flyway, test dependencies; health endpoint and profiles.
2. **Database config:** local/dev/test/prod externalized config, migrations, UTC, connection secrets outside VCS.
3. **Entities:** identity and core relations, constraints/indexes, safe enum values.
4. **Repositories:** paging/filter queries and transactional slot conflict strategy.
5. **DTOs/mappers:** input validation, role-safe projections and error envelope.
6. **Authentication:** registration, login, session, current-user and logout.
7. **Spring Security:** CSRF, cookies, CORS, role policies, ownership tests.
8. **Roles:** patient and doctor registration, distinct admin provisioning.
9. **Doctor approval:** pending queue, decisions, notifications and audit atomically.
10. **Doctor features:** profile, dashboard, appointment views, reviews.
11. **Patient features:** profile, directory and dashboard.
12. **Appointment management:** booking, status transitions, cancellation, history, reviews.
13. **Availability/absences:** slot editor APIs, conflict rules, explicit rescheduling plan.
14. **Admin features:** account, appointment and dashboard projections without clinical data.
15. **Notifications:** in-app records; optional email outbox after transaction commit.
16. **Activity logs:** append-only administrative and sensitive-action events.
17. **Validation/errors:** centralized stable problem format, rate limits and audit-safe logging.
18. **Angular integration:** align DTO names, CSRF bootstrap/header, credentials, pagination and CORS.
19. **Testing:** unit/service, repository constraints, MockMvc security/role matrix, concurrency booking, migration and privacy/DTO tests.
20. **Deployment:** HTTPS, secrets manager, database backups, monitoring, retention policy and production seed disabled.

## 11. Assumptions and decisions to confirm

### Verified from original repository

- The previous Angular shell rendered original HTML through an iframe.
- The original pages/scripts used French labels and localStorage-based `db.js` fake persistence/session keys. Those files were removed from the current worktree before this implementation; the committed versions were inspected read-only.
- The Angular package configuration targets Angular 20.3 and TypeScript 5.9.
- The original role choice exposed patient and doctor; there was not a distinct authenticated admin portal in that login view.
- The supplied 37-page 2025–2026 academic report describes exactly two product roles: patient and doctor. It documents iterations for accounts/authentication, medical services and booking, then doctor availability and patient follow-up. The report is historical product evidence, not an instruction to retain localStorage or its unsafe plaintext-password prototype.
- In the old HTML and JavaScript, the doctor account is stored under the legacy collection/role named `admin` and the menu title says “Espace Médecin”. This is a naming artifact: it represents the doctor/provider role, not a platform administrator. The recovered Angular app uses `DOCTOR`; `ADMIN` is kept separate for the later explicit platform-admin requirement.
- Patient registration collected full name, email, phone, address, password, password confirmation and optional profile image. Doctor registration collected owner name/contact, specialty, displayed profile/cabinet title, appointment duration, working hours, working days, break length, location, one or more named services and prices, description and an image.
- Patient booking collected patient contact/address, chosen service and its price, date, available time slot, and extra notes. It rejected past dates, non-working days, absences, and unavailable or already reserved slots; a new appointment started `PENDING`.
- Doctor schedule generation used duration, start/end hours, working days, and pause. Slot states were available, reserved, or blocked; past slots were blocked, but a confirmed appointment did not become completed automatically.
- Doctors could confirm, cancel, or complete requests; status changes generated patient notifications. Cancellation retained a reason/comment and released the corresponding slot where applicable.
- Declaring an absence blocked unreserved slots and moved overlapping pending/confirmed appointments to “à reprogrammer”; the patient received a notification. Removing the absence did not silently restore those appointments.
- The doctor patient list was based on completed appointments. The dossier grouped completed visits and consultation notes. A note held summary, observation, treatment/recommendation, next-check date and private doctor note; the next-check notification offered a link to book a slot.
- Patients could submit and edit a rating, comment and recommendation after a completed appointment. The old report describes upcoming tracking, appointment history, notifications and account settings for both roles.
- The report’s admin dashboard screenshots are doctor workspace screens, not evidence of platform-wide patient/doctor approval administration. Platform doctor approval and audit features come from the later product brief, not the report.

### Implemented in current frontend

- Angular standalone shell, redesigned public landing, patient and doctor registration forms that preserve the report’s service, tariff and working-schedule fields, login, route-level doctor detail, appointment booking and detail, review, rescheduling, patient history/dossier, doctor consultation note, availability/absence forms, profile settings, and role workspaces with dashboards, filters and empty/loading/error states.
- Admin workspace remains a distinct added platform feature, including dashboard projections, doctor approval actions, patient/doctor status actions, appointment actions, notifications and activity-log views. Its statistics and tables are sourced only from API responses.
- HTTP integration includes centralized API URL, `withCredentials`, CSRF-cookie header handling, services/models, error interceptor, router guards including pending-doctor rejection, and explicit server-owned authorization expectations.
- No localStorage/sessionStorage fake authentication, database, Node backend, or mock server is used.

### Proposed requirements, not verified legacy behavior

- Admin dashboard, approval queue, platform account activation, and activity history are later additions, not historical report features. Admin filters currently filter the loaded response in the browser; server-side search and pagination controls remain to be completed.
- Database/API contracts, secure cookie sessions, CSRF behavior and authorization rules are proposed integration requirements; they need to be implemented and verified by the Spring Boot developer.
- Profile-image upload, patient account deletion, admin patient details and notification-preference editing are not yet implemented. The doctor monthly calendar is implemented in Angular; its rendered appointment content needs a live API response to verify end to end.

### Decisions for the two developers

1. Preserve the report’s initial `PENDING` booking state and doctor confirmation flow. Agree whether any later scenario can auto-confirm, plus cancellation cutoff and whether rescheduling becomes pending confirmation.
2. Which license authority/jurisdiction verifies doctors, and what evidence/document upload is required?
3. The report uses working-day hours, fixed appointment duration and a pause between appointments to generate daily slots. Confirm how the API represents generated slots, clinic timezone and daylight-saving transitions.
4. What is the permitted patient/doctor access and retention policy for consultation notes and health data? Confirm applicable law and hosting requirements.
5. Do patients need email verification, password reset, and verified contact changes in the first release?
6. Can admins manually create accounts, moderate reviews, or access any additional patient data? Default is no clinical-note access and least privilege.
7. Which notification channels are required (in-app only, email, SMS), and what is the delivery/retry policy?
8. Confirm the DTO envelope, CSRF bootstrap implementation, cookie domain/SameSite policy, origin/port, page numbering (0-based here), and expected search/sort names.

Treat assumptions as proposals until both developers agree. The frontend is not a substitute for production security, server authorization, privacy assessment, accessibility review, or backend integration QA.

#### Justificatif d’inscription médecin

L’inscription médecin reste publique et conserve ses champs métier dans la partie JSON `registration`, envoyée avec un unique fichier `document`. Une attestation d’inscription à l’Ordre avec cachet est obligatoire; seuls les contenus PDF, JPG/JPEG ou PNG de 5 Mo maximum sont acceptés. Le binaire est stocké sous un identifiant aléatoire dans un répertoire privé configurable par `HELLODOC_DOCUMENTS_DIR`. Seul un administrateur peut le télécharger via `GET /api/admin/doctors/{applicationId}/document`.

Après l’approbation, le backend publie un événement transactionnel et envoie un message de confirmation au médecin après commit. La livraison nécessite un relais SMTP : `HELLODOC_EMAIL_ENABLED=true`, `HELLODOC_SMTP_HOST`, `HELLODOC_SMTP_PORT`, `HELLODOC_SMTP_USERNAME`, `HELLODOC_SMTP_PASSWORD`, `HELLODOC_EMAIL_FROM`, et éventuellement `HELLODOC_FRONTEND_URL`. Le message n’inclut aucun mot de passe. Une erreur d’envoi ne fait pas échouer l’approbation.
