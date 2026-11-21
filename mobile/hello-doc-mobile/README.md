# HelloDoc Android — accueil et authentification

Ouvrir **C:\L3PI\HELLO-DOC\mobile\hello-doc-mobile** dans Android Studio (**File → Open**). Attendre la synchronisation Gradle, choisir un émulateur, puis **Run app**. Le dossier à ouvrir contient `settings.gradle.kts`, `gradlew.bat` et `app/`.

## Lancer sur ce PC

1. Démarrer MySQL dans Laragon.
2. Démarrer **HelloDoc Backend** dans IntelliJ, avec vos configurations locales habituelles. Le port par défaut est **8081**.
3. Ouvrir ce projet mobile dans Android Studio et lancer **app** sur l’émulateur.
4. Si Android demande l’accès au réseau local, l’autoriser pour joindre le serveur du PC.

Le frontend Angular n’a pas besoin de tourner pour le mobile. `npm start` ne lance pas une application Android.

L’émulateur utilise déjà **http://10.0.2.2:8081/api/**. Les comptes et demandes d’inscription sont enregistrés dans le backend HelloDoc actuel, comme sur le web. Aucun serveur « étudiants » ni compte local ne sont utilisés.

Prérequis de ce projet : SDK 37, Android 8.1/API 27 minimum, JDK 21 ou JBR compatible fourni par Android Studio. Le wrapper Gradle et les versions des dépendances sont conservés dans le dépôt. Android Studio génère `local.properties` sur chaque PC ; ce fichier personnel n’est pas versionné.

## Écrans et architecture

- **HomeActivity** : accueil public, identité HelloDoc, connexion, création de compte patient ou médecin.
- **LoginActivity** : formulaire de connexion, validation, affichage des erreurs et retour à l’accueil.
- **RegisterActivity → NavHostFragment → RegisterFragment** : inscription patient/médecin.
- **AccountActivity → NavHostFragment → AccountFragment** : vrai nom, e-mail, rôle et statut du compte, déconnexion serveur.

Les deux premiers écrans sont bien des **Activities**, et non des Fragments. Les Activities utilisent des Intent explicites pour naviguer entre elles ; les Fragments sont hébergés dans des graphes Navigation.

La structure reprend les principes MVVM présentés dans vos supports **chap5V2026**, **TP6 SQLite/Room** et **TP7 Retrofit** :

```text
ui/ (Activities et Fragments avec ViewBinding)
    → viewmodel/AuthViewModel (état, LiveData en lecture seule, coroutines)
        → repository/AuthRepository (authentification et erreurs API)
            → network/AuthApi + RetrofitClient (Retrofit, Gson, OkHttp)
                → backend Spring Boot HelloDoc
model/ : modèles JSON et contraintes des formulaires
HelloDocApplication : conteneur Repository et AuthViewModelFactory
```

Les vues observent les LiveData avec leur cycle de vie ; les Fragments utilisent `activityViewModels()`. Le ViewModel conserve les opérations en cours pendant une rotation et bloque les doubles soumissions. Les appels suspend de Retrofit ne bloquent pas le thread d’interface.

Les PDF servent de références pédagogiques. Leur exercice de gestion d’étudiants n’est pas une fonctionnalité HelloDoc. Les écrans Étudiants, leurs DAO, bases Room, adaptateurs, tests, ressources et le module de documents locaux ont été retirés de **ce projet mobile**. Les autres dossiers de TP du PC ne sont pas modifiés. Room n’est pas nécessaire pour cette étape d’authentification, dont la source est le backend.

## Inscription et session

- Patient : prénom, nom, téléphone (8–15 chiffres, lettres filtrées même au collage), adresse, e-mail valide, mot de passe de 8–72 caractères, confirmation et consentement.
- Médecin : mêmes coordonnées, spécialité chargée du backend, numéro d’inscription à l’Ordre, ville et justificatif **PDF/JPEG/PNG ≤ 5 Mo**. Le backend vérifie également le contenu.
- Le médecin attend l’approbation de l’administrateur dans le web. Les horaires, services et informations du cabinet ne sont pas demandés à l’inscription mobile.
- Après création du compte, le mobile affiche le résultat réel et propose la connexion. Un médecin en attente ne peut pas se connecter.
- Les mêmes comptes fonctionnent sur le web et sur Android. `USER` correspond au patient, `DOCTOR` au médecin approuvé. Aucun JWT ni utilisateur de démonstration n’a été ajouté.

API utilisée : `auth/csrf`, `auth/login`, `auth/me`, `auth/logout`, `specialties`, `patients/registrations` et `doctors/registrations`. Les inscriptions médecin sont multipart (`registration` + `document`). Les cookies JSESSIONID et XSRF-TOKEN restent en mémoire ; le cookie CSRF brut est envoyé dans l’en-tête attendu par Spring. Aucun mot de passe ni cookie n’est enregistré dans une base locale ou une sauvegarde. Un arrêt complet du processus nécessite une nouvelle connexion.

## Changer le serveur sur un autre PC

Pour modifier l’adresse dans Android Studio, ajouter une propriété personnelle dans **le fichier utilisateur** `%USERPROFILE%\.gradle\gradle.properties`, puis synchroniser Gradle :

```properties
HELLODOC_API_URL=http://10.0.2.2:8081/api/
```

Adapter le port au backend de ce PC. L’URL doit se terminer par `/api/` ; elle ne doit pas pointer sur Angular. Les identifiants MySQL et SMTP restent exclusivement dans la configuration locale du backend.

Pour un téléphone USB, activer le débogage USB :

```powershell
adb reverse tcp:8081 tcp:8081
cd C:\L3PI\HELLO-DOC\mobile\hello-doc-mobile
.\gradlew.bat :app:installDebug '-PHELLODOC_API_URL=http://127.0.0.1:8081/api/'
```

Pour un serveur sur le réseau local, remplacer l’URL par l’adresse IP accessible du PC. Le HTTP et l’autorisation de réseau local sont réservés au debug. Une version release doit utiliser une API HTTPS.

## Vérifications

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK : `app\build\outputs\apk\debug\app-debug.apk`. Une copie prête à installer est déposée dans **C:\L3PI\HELLO-DOC\mobile\HelloDoc-auth-debug.apk** ; les APK sont ignorés par Git.

Les tests JVM vérifient les contrats JSON, les cookies, le CSRF, le multipart médecin, les validations et les erreurs. Le test réel est ignoré tant que `HELLODOC_TEST_API_URL` n’est pas défini. Utiliser exclusivement une base de test jetable :

```powershell
# Terminal 1 : fixture séparée, MySQL Laragon local sur 3308.
cd C:\L3PI\HELLO-DOC\backend
$env:HELLODOC_QA_PORT='8083'
python verify-workflows.py --hold

# Terminal 2 : attendre QA_BROWSER_READY avant ces commandes.
cd C:\L3PI\HELLO-DOC\mobile\hello-doc-mobile
$env:HELLODOC_TEST_API_URL='http://localhost:8083/api/'
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest '-PHELLODOC_API_URL=http://10.0.2.2:8083/api/' '-Pandroid.testInstrumentationRunnerArguments.hellodocQa=true'
```

Appuyer sur Entrée dans le terminal 1 pour arrêter la fixture et supprimer sa base temporaire. Recompiler ensuite sans l’option de port 8083 avant de tester avec votre backend habituel.

Les tests instrumentés couvrent l’accueil → Activity de connexion, l’e-mail invalide, les lettres dans le téléphone, les champs médecin et la restauration du rôle après recréation, l’inscription patient avec double clic, la connexion/déconnexion patient et médecin, et une erreur de mot de passe suivie d’une nouvelle tentative. Des captures sont enregistrées dans Downloads de l’émulateur pour revue visuelle.

Périmètre de cette étape : accueil, inscription, connexion et espace du compte. La prise de rendez-vous, les dossiers et la gestion du cabinet restent sur le web. Limites de vérification : téléphone physique et sélection manuelle complète du justificatif à contrôler ; l’envoi multipart et la validation médecin sont testés avec le vrai backend.

## Résultats de vérification sur ce PC

- `assembleDebug` : compilation réussie.
- 9 tests JVM de contrat/validation + 1 test réel Retrofit/Spring/MySQL réussis.
- 7 tests instrumentés réussis sur Pixel 7 / Android 17.
- `lintDebug` réussi sans erreur ; avertissements non bloquants de versions et de style.
- Revue des captures de l’accueil, de la connexion, des inscriptions et de l’espace médecin : pas de débordement horizontal ni de contrôle masqué par les barres système.
- Le backend utilisé pour ces essais est celui du dépôt actuel, avec une base jetable sur le port 8083. L’APK livré revient au port habituel 8081.
