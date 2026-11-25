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

## Configuration IntelliJ pour chaque membre de l’équipe

Après un clone ou un pull, chaque personne conserve ses propres réglages MySQL et SMTP. Les paramètres communs du code restent dans `src/main/resources/application.properties` ; les valeurs personnelles sont définies dans une configuration de lancement IntelliJ locale. Un pull partage le code, pas les comptes ni les données MySQL d’un autre PC.

### 1. Préparer le backend

1. Démarrer le service MySQL local (Laragon, XAMPP ou votre installation MySQL).
2. Vérifier son port et créer la base voulue si elle n’existe pas. Exemple pour une base appelée `hello_doc` :

   ```sql
   CREATE DATABASE IF NOT EXISTS hello_doc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

   Si votre base porte un autre nom, utilisez ce nom dans la commande et dans l’URL JDBC. Hibernate crée/met à jour les tables après connexion ; le lancement direct depuis IntelliJ ne crée pas la base MySQL elle-même.
3. Ouvrir `backend/pom.xml` dans IntelliJ comme projet Maven, puis laisser les dépendances se charger. Utiliser le JDK **21**.

### 2. Créer sa configuration de lancement

Dans **Run → Edit Configurations… → + → Spring Boot**, renseigner :

| Champ | Valeur |
|---|---|
| Name | `HelloDoc Backend` |
| Main class | `com.hellodoc.HelloDocApplication` |
| JRE / JDK | Java 21 |
| Use classpath of module | Le module Maven du backend |
| Working directory | Votre dossier `backend` local |
| Active profiles | Laisser vide ; aucun profil `smtp` n’est défini |
| Store as project file | Laisser décoché pour conserver les identifiants localement |

Afficher **Environment variables** depuis **Modify options** si nécessaire, puis ouvrir son éditeur de variables. Voir [la documentation IntelliJ](https://www.jetbrains.com/help/idea/run-debug-configuration-spring-boot.html).

### 3. Renseigner ses propres variables MySQL

| Variable | Valeur à renseigner sur votre PC |
|---|---|
| `HELLODOC_DB_URL` | `jdbc:mysql://127.0.0.1:PORT_MYSQL/NOM_BASE?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `HELLODOC_DB_USERNAME` | Votre utilisateur MySQL, par exemple `root` |
| `HELLODOC_DB_PASSWORD` | Votre mot de passe MySQL ; vide si le compte n’en a pas |
| `SERVER_PORT` | `8081` recommandé pour le backend |
| `HELLODOC_ADMIN_INITIAL_PASSWORD` | Un mot de passe personnel d’au moins 12 caractères pour créer l’administrateur initial |

Remplacer `PORT_MYSQL` et `NOM_BASE` par de vraies valeurs. Exemple : MySQL sur **3306** et base **hello_doc** donnent :

```text
jdbc:mysql://127.0.0.1:3306/hello_doc?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

Pour MySQL sur **3308**, remplacer seulement `3306` par `3308`. `SERVER_PORT` est le port HTTP du backend, distinct du port MySQL : chaque PC peut utiliser son propre port MySQL tout en gardant le backend sur **8081**.

L’administrateur initial est `admin@hellodoc.com`. Si ce compte existe déjà dans votre base, changer `HELLODOC_ADMIN_INITIAL_PASSWORD` ne change pas son mot de passe enregistré.

### 4. Renseigner son SMTP

Pour envoyer les e-mails avec Gmail, ajouter :

| Variable | Valeur |
|---|---|
| `HELLODOC_EMAIL_ENABLED` | `true` |
| `HELLODOC_SMTP_HOST` | `smtp.gmail.com` |
| `HELLODOC_SMTP_PORT` | `587` |
| `HELLODOC_SMTP_USERNAME` | Votre adresse Gmail complète |
| `HELLODOC_SMTP_PASSWORD` | Votre mot de passe d’application Google |
| `HELLODOC_EMAIL_FROM` | La même adresse Gmail |

Le backend active STARTTLS par défaut. Utiliser un mot de passe d’application créé avec la validation en deux étapes, selon [l’aide Google](https://support.google.com/accounts/answer/185833). Pour travailler sans envoi d’e-mails, définir uniquement `HELLODOC_EMAIL_ENABLED=false` pour cette partie.

### 5. Démarrer et conserver les réglages

Cliquer sur **Apply → OK → Run**, puis attendre `Started HelloDocApplication`. Ces variables seront réutilisées lors des prochains lancements avec cette configuration IntelliJ ; elles ne sont pas appliquées au script lancé séparément dans un terminal. Les questions MySQL du script `run-local.ps1` restent inchangées.

Sur Windows, pour utiliser le même réglage JVM que le script local, ajouter dans **Modify options → Add VM options** :

```text
-Djdk.net.unixdomain.tmpdir="VOTRE_CHEMIN_LOCAL/HELLO-DOC/backend/target"
```

Remplacer le chemin par celui de votre propre clone et vérifier que le dossier `backend/target` existe (la compilation Maven le crée). Ne pas recopier le chemin personnel d’un camarade.

Dans un autre terminal, depuis `frontend`, lancer `npm ci` au premier lancement ou après une modification des dépendances, puis `npm start`. Ouvrir `http://localhost:4200`.

Si vous devez changer le port HTTP du backend, par exemple `SERVER_PORT=8082`, adapter aussi `apiBaseUrl` dans `frontend/src/environments/environment.ts` vers `http://localhost:8082/api`. Pour le mobile, l’adresse se règle avec `HELLODOC_API_URL` à la compilation (voir son guide). Garder **8081** sur tous les PC évite ces modifications du frontend. Si Angular change de port, adapter également `HELLODOC_FRONTEND_ORIGIN` et `HELLODOC_FRONTEND_URL` dans les variables du backend.

Les dossiers `.idea/`, `.run/`, les fichiers `.env.local`, `local.properties` Android et les dossiers de compilation sont ignorés par Git. Les configurations contenant des mots de passe restent personnelles ; ne pas les exporter dans un autre fichier suivi par Git.

## Courriel d’approbation médecin : état et relance

L’approbation crée le profil et active le compte dans une transaction. Après validation de cette transaction, un traitement séparé envoie le courriel : une panne SMTP ne transforme pas l’approbation réussie en échec et ne désactive pas le médecin. Les doubles clics sont bloqués dans l’interface ; un nouvel appel d’approbation pour une candidature déjà approuvée renvoie son état sans recréer le profil ni renvoyer le courriel.

L’état d’envoi est enregistré pour chaque candidature : `NOT_SENT`, `QUEUED`, `SENT`, `FAILED` ou `DISABLED`. `SENT` signifie que le serveur SMTP a accepté le courriel, sans garantir son classement dans la boîte de réception. Après l’approbation, l’administration affiche cet état séparément du résultat de la décision.

Pour un médecin déjà approuvé qui n’a pas reçu de courriel : **Administration → Médecins → Consulter → Courriel d’approbation → Envoyer le courriel**. Il n’est pas nécessaire de recréer le compte ni de refaire la décision. Un envoi déjà marqué `SENT` n’est pas répété par ce bouton ; vérifier les courriers indésirables si nécessaire. Un traitement interrompu peut être relancé après la vérification de son état.

Les API suivantes sont réservées à l’administrateur, avec CSRF pour la relance :

- `GET /api/admin/doctors/{applicationId}/approval-email` : état, explication et dates de tentative/envoi.
- `POST /api/admin/doctors/{applicationId}/approval-email` : mise en attente d’un envoi pour un compte approuvé et actif.

Le serveur distingue les erreurs d’authentification SMTP, de connexion et de configuration, sans exposer les identifiants. Pour Gmail, les espaces de présentation d’un mot de passe d’application de 16 lettres sont retirés automatiquement ; les mots de passe des autres fournisseurs ne sont pas modifiés. Les connexions SMTP ont des délais bornés : 5 secondes pour la connexion et 10 secondes pour la lecture/l’écriture, configurables avec `HELLODOC_SMTP_CONNECTION_TIMEOUT_MS`, `HELLODOC_SMTP_READ_TIMEOUT_MS` et `HELLODOC_SMTP_WRITE_TIMEOUT_MS`.

Après une mise à jour de ce correctif, redémarrer le backend depuis IntelliJ. Hibernate ajoute les colonnes de suivi d’envoi aux candidatures existantes ; leur statut d’approbation et leur compte restent inchangés. Les anciens comptes approuvés commencent avec un état d’envoi `NOT_SENT`, faute d’historique antérieur.

Vérifications : `mvn test`, `npm run test:approval` et `npm run test:booking` dans leurs dossiers respectifs. Pour tester une panne SMTP puis une relance avec de vrais appels HTTP et MySQL, sans courriel externe et sans toucher au serveur habituel :

```powershell
# Depuis backend/, avec MySQL local sur 3308 et le jar compilé.
$env:HELLODOC_QA_PORT='8083'
python verify-workflows.py --mail
```

Ce scénario utilise un serveur SMTP local factice et une base `hello_doc_qa_*` créée puis supprimée automatiquement. Le test `DoctorApprovalSmtpConnectionTest` est désactivé par défaut ; son activation explicite vérifie uniquement l’authentification au SMTP configuré, sans envoyer de message.

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
- Une demande `PENDING` propose au médecin uniquement **Confirmer** et **Refuser**. Le médecin peut **Annuler** un rendez-vous `CONFIRMED` à venir ; il ne peut pas utiliser l’annulation à la place du refus d’une demande en attente.
- La **reprogrammation est réservée au patient propriétaire** du rendez-vous : `GET /api/appointments/{id}/reschedule-options` et `PUT /api/appointments/{id}/reschedule` acceptent uniquement les rôles `USER`/`PATIENT`. Le médecin ne dispose plus de lien ni de route de reprogrammation ; les appels directs avec son compte renvoient HTTP 403. Après une absence, il attend le nouveau créneau choisi par le patient ; la demande issue de `TO_RESCHEDULE` revient à `PENDING` pour confirmation.
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

## Téléconsultation et documents cliniques

La téléconsultation est créée par le médecin à partir d’un rendez-vous confirmé. Le démarrage et la clôture de la séance sont des actions explicites du médecin ; une déconnexion ne change pas son état et la clôture ne modifie pas automatiquement le statut du rendez-vous. Les signaux WebRTC passent par `wss://<domaine-api>/api/teleconsultation-signal` en production et sont autorisés à chaque message selon le rendez-vous et la session authentifiés. Le chat est persisté par séance et relayé sur ce même canal.

Les valeurs ICE sont configurables dans l’environnement du processus backend : `HELLODOC_WEBRTC_STUN_URLS` (liste séparée par des virgules), `HELLODOC_WEBRTC_TURN_URLS` et `HELLODOC_WEBRTC_TURN_SHARED_SECRET`. Le serveur génère des identifiants TURN temporaires signés avec le secret partagé compatible avec l’authentification REST de coturn. Garder ce secret uniquement dans le gestionnaire de secrets ou l’environnement privé du serveur ; ne pas l’inscrire dans le dépôt, le frontend ou `backend/.env.example`. Un serveur TURN joignable en UDP/TCP/TLS doit être déployé et configuré pour les réseaux qui bloquent les connexions directes. Le STUN par défaut ne garantit pas une connexion dans tous les réseaux. Le frontend et l’API doivent utiliser HTTPS/WSS en production.

Le dossier patient est obtenu via `GET /api/patients/me/consultations` et est résolu depuis l’utilisateur authentifié ; il ne prend aucun identifiant patient en paramètre. Son DTO exclut la note privée et l’historique interne des révisions. Les corrections d’ordonnance créent une nouvelle version reliée à la précédente ; elles ne modifient pas le document initial.
