# Hello Doc monorepo

Hello Doc contains an Angular 20 frontend, a Java Spring Boot backend and a native Kotlin Android application. The backend implements MySQL-backed session authentication, patient registration and profiles, doctor applications and approval with operational profiles, doctor discovery, specialties, recurring availability, absences, and dynamically calculated appointment slots. Appointment booking, status management, patient notifications and absence-related rescheduling are implemented, together with consultation notes, authorized patient directories and administrative activity. The Android milestone covers the public home, patient/doctor registration, login and account session. Teleconsultation, billing and the remaining mobile workflows are outside this milestone.

## Repository layout

- `frontend/` - Angular application, dependencies, and frontend documentation.
- `backend/` - Spring Boot source, Maven build, and setup instructions.
- `mobile/hello-doc-mobile/` - native Android Kotlin project; open this directory in Android Studio. See [the mobile guide](mobile/hello-doc-mobile/README.md).
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

Configure your own MySQL connection in the ignored `backend/.env.local` file (copy `backend/.env.example` as a guide), then follow [backend/README.md](backend/README.md). The backend API uses port `8081` by default; each collaborator supplies their own MySQL host and port. The local startup script reads this file; when starting from IntelliJ, supply the same variables in your personal Run configuration. Sign-in, profiles, doctor discovery, approval, availability, absences, appointments, consultation notes and administrative operations use the configured MySQL database.

See [frontend/README.md](frontend/README.md) for frontend details and [backend/docs/HELLO_DOC_BACKEND_SPECIFICATION.md](backend/docs/HELLO_DOC_BACKEND_SPECIFICATION.md) for the API contract. No Node.js backend, mock database, or fake authentication is included.

## Refonte et parcours actuels

Voir [le bilan de la première livraison](REFONTE_AVANCEMENT.md). L’inscription médecin porte sur les coordonnées et le justificatif ; les services et horaires sont configurés après approbation. Une absence affectant des rendez-vous exige un avertissement puis une confirmation du médecin avant d’inviter les patients à reprogrammer.


## Évolution produit et recette

Le rapport [EVOLUTION_PRODUIT.md](EVOLUTION_PRODUIT.md) détaille les parcours livrés, les fichiers concernés, les vérifications, les étapes de lancement et les limites avant exploitation commerciale. Les notes de consultation nécessitent le redémarrage du backend et sont accessibles uniquement au médecin de la consultation terminée.

## Partager le projet avec l’équipe

Conservez ce dépôt : il n’est pas nécessaire de créer un nouveau dossier avant de pousser une version. Les sources à partager sont dans `backend/`, `frontend/` et `mobile/`, accompagnées des guides et des fichiers de compilation du projet.

Les dossiers `output/`, `tmp/`, `.angular/`, `node_modules/`, `target/`, les caches Gradle et les répertoires `build/` sont des fichiers de travail ou des résultats générés. Ils restent locaux grâce à `.gitignore`. Il en va de même pour les APK, `.env.local`, `local.properties`, les réglages IntelliJ/Android Studio et les clés de signature. Les supprimer n’est pas nécessaire pour partager le projet.

Pour récupérer cette version depuis une copie existante :

```powershell
git pull origin main
```

Si Git signale des modifications locales, enregistrez-les dans un commit ou un stash avant de tirer la nouvelle version. Chaque membre conserve sa propre configuration MySQL et SMTP ; les fichiers personnels ignorés ne sont pas remplacés par un pull.

Après le pull :

1. Backend : ouvrir `backend/` dans IntelliJ et renseigner les variables locales décrites dans [le guide backend](backend/README.md).
2. Web : exécuter `npm ci`, puis `npm start` dans `frontend/`.
3. Android : ouvrir `mobile/hello-doc-mobile/` dans Android Studio et synchroniser Gradle. Android Studio crée le fichier SDK `local.properties` sur chaque PC. Voir [le guide mobile](mobile/hello-doc-mobile/README.md).

La refonte récente de Mon compte et des disponibilités médecin, les vérifications et les limites sont détaillées dans [le guide frontend](frontend/README.md).
