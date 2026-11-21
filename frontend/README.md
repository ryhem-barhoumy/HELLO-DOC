# Hello Doc — Angular frontend

Frontend Angular du prototype Hello Doc, plateforme de rendez-vous entre patients et médecins. L’interface est en français et prépare une intégration avec l’API Java Spring Boot. Aucun backend ni jeu de données de démonstration n’est inclus.

## Prérequis et commandes

- Node.js 20.19+ ou 22.12+ avec npm, compatibles Angular 20.
- `npm ci` pour installer les dépendances verrouillées.
- `npm start` pour lancer le serveur Angular sur `http://localhost:4200`.
- `npm run build` pour compiler en mode développement; `npm run build -- --configuration production` pour la version de production.

## Fonctionnalités de l’interface

- Accueil Hello Doc; connexion; inscription patient; inscription médecin avec coordonnées, spécialité, numéro professionnel et justificatif. Le cabinet, les services et les horaires sont complétés après approbation.
- Espace patient : recherche filtrée, profil médecin, réservation par service/date/créneau, rendez-vous, annulation, historique, avis et reprogrammation.
- Espace médecin : tableau de bord, demandes et changements d’état, agenda par rendez-vous, disponibilités, absences, dossier patient autorisé, fiche de consultation, avis et profil professionnel.
- Espace `ADMIN` séparé : tableau de bord alimenté par API, approbations des médecins, répertoires et journal. Ce rôle de plateforme est ajouté au prototype historique; il ne faut pas confondre avec le compte médecin appelé `admin` dans l’ancien JavaScript.
- Les composants métier appellent le backend. Sans API, l’interface affiche ses états d’indisponibilité au lieu de créer ou d’afficher des données fictives.

## Actions sur les rendez-vous

- Demande en attente : le médecin peut **Confirmer** ou **Refuser**.
- Rendez-vous confirmé à venir : le médecin peut **Annuler** ; le patient est alors notifié.
- La **reprogrammation est réservée au patient** : aucun bouton ni route de reprogrammation n’est proposé au médecin, et le backend refuse aussi ses appels directs.
- Après une absence, le médecin voit que le patient doit choisir un nouveau créneau. La demande reprogrammée revient ensuite en attente de confirmation.
- Les boutons de décision sont désactivés pendant leur envoi.

Vérifier les règles et les actions des écrans avec `npm run test:appointments`. Les tests `npm run test:booking` couvrent le formulaire patient et `npm run test:approval` l’approbation des médecins.

## Architecture

- Angular 20.3, TypeScript 5.9, Router, composants autonomes et Reactive Forms.
- `src/app/features`: pages d’accueil, authentification, patient, médecin, rendez-vous et administration.
- `src/app/core/services` et `models`: clients HTTP, session et contrats TypeScript.
- `src/app/core/guards`: contrôle de rôle et blocage des comptes médecin non approuvés.
- `src/environments/environment.ts`: URL centrale `http://localhost:8081/api`.
- Les requêtes utilisent les cookies (`withCredentials`). Le frontend initialise la protection CSRF via `GET /auth/csrf`, lit le cookie `XSRF-TOKEN` et envoie `X-XSRF-TOKEN` sur les méthodes de modification.

## Backend nécessaire

Le backend Spring Boot du projet fournit les routes décrites dans `HELLO_DOC_BACKEND_SPECIFICATION.md` et utilise des sessions `HttpOnly`, CSRF, CORS avec credentials et autorisation serveur. Les gardes Angular ne protègent pas les données à elles seules. Le frontend ne conserve ni mot de passe ni session métier dans `localStorage`/`sessionStorage`.

## Refonte des interfaces médecin — 21 novembre 2026

Les routes `/medecin/profil` et `/medecin/disponibilites` utilisent maintenant deux éditeurs dédiés, intégrés dans le portail existant. Les écrans patient et administrateur conservent leurs composants. Le backend et le schéma de données n’ont pas été modifiés pour cette refonte.

### Résultat et audit

Le frontend est en Angular 20.3 avec composants autonomes, Router, Reactive Forms, FormsModule et HttpClient/RxJS. L’audit a identifié la superposition de styles génériques (`styles.css`, `workspace.css`, `product.css`, `cabinet.css`), des libellés de 10–12 px et un composant portail regroupant de nombreux domaines.

Les nouveaux styles sont limités à `.doctor-ui` et aux deux pages `.doctor-workspace`. Les champs mesurent 46–48 px ; les cartes, actions, messages et modales partagent les mêmes règles. La typographie locale existante est conservée, sans dépendance de police externe.

- **Mon compte** : carte avec photo, nom, spécialité et cabinet ; trois onglets accessibles au clavier ; profil personnel, cabinet/services et sécurité séparés. Les brouillons sont conservés entre les onglets.
- **Cabinet et services** : formulaire en deux colonnes lorsque la place le permet, erreurs sous les champs, liste structurée des services, formulaire en modale, confirmation de suppression, tarif exact conservé. Les modifications des services sont publiées avec l’enregistrement du compte.
- **Sécurité** : trois champs alignés, affichage/masquage individuel, règle de 8–72 caractères, confirmation correspondante, blocage des formulaires invalides et des doubles envois, remise à zéro des champs après succès uniquement.
- **Disponibilités** : sept journées avec switches et état textuel, plages structurées, ajout/suppression, modale de copie et accord explicite avant remplacement. Contrôles locaux des heures manquantes, des pas de cinq minutes, de la durée minimale et des chevauchements, en plus de la validation serveur.
- **Sauvegarde** : chargement et erreur distincts, aucune confirmation sur échec, brouillons conservés pour réessayer ; avertissement lors d’une navigation ou d’une déconnexion avec des modifications en attente, et protection contre la fermeture de l’onglet.

### Fichiers concernés

Nouveaux composants :

- `src/app/features/doctor/doctor-account.component.ts` et `.html` : profil, cabinet, services et sécurité.
- `src/app/features/doctor/weekly-availability.component.ts` et `.html` : semaine type.
- `src/app/features/doctor/doctor-editor.models.ts` : contrats et règles partagées avec les tests.
- `src/app/shared/editor-dialog.component.ts` et `password-field.component.ts` : modales et champs de mot de passe réutilisables.
- `src/app/core/guards/unsaved-doctor-editor.guard.ts` : protection des brouillons.
- `src/doctor-editors.css` : design et adaptation aux différentes largeurs.

Intégration : `portal.component.ts`/`.html`, `app.routes.ts` (garde uniquement, URL conservées), `shared/icon.component.ts`, `angular.json`, `package.json`, ce guide.

Vérifications reproductibles : `doctor-editors.test.ts`, `scripts/test-doctor-editors.mjs` et `scripts/verify-doctor-editors-browser.mjs`.

### Contrats et limites conservés

- L’e-mail est en lecture seule : l’API de profil actuelle ne prévoit pas sa modification.
- Le backend autorise **deux plages par jour** au maximum. Les libellés Première/Deuxième plage restent dans l’espace médecin.
- La durée des rendez-vous est commune aux services ; le modèle ne possède pas de durée par prestation.
- Le tarif est une chaîne contenant le prix et sa devise. Aucun montant, aucune devise et aucun identifiant de service ne sont inventés. Les textes existants restent inchangés tant que le médecin ne les modifie pas.
- Le profil est enregistré par l’API complète existante, qui exige le mot de passe actuel, les informations du cabinet et au moins un service. Pour un nouveau médecin au cabinet incomplet, l’interface dirige vers les champs nécessaires à compléter avant la première sauvegarde.
- Les horaires d’un jour désactivé restent disponibles dans le brouillon courant pour permettre de le réactiver. Après rechargement, seuls les horaires actifs réellement conservés par le backend sont relus.
- Le récapitulatif indique l’amplitude hebdomadaire, sans prétendre calculer des rendez-vous encore disponibles.

### Vérifications réalisées

- Build Angular de production réussi et TypeScript `--noEmit` réussi.
- **51 tests frontend réussis** : les 38 tests existants et 13 tests des nouveaux éditeurs.
- **25 scénarios HTTP Spring/MySQL réussis**, avec une base jetable créée par `backend/verify-workflows.py --hold` sur le port 8083.
- Vérification navigateur avec le vrai backend : changement de photo et lecture publique, profil/cabinet, ajout/modification/suppression des services, changement de mot de passe avec refus d’un mot de passe actuel incorrect, activation/désactivation, deux plages, chevauchement, copie et suppression confirmées, persistance après rechargement.
- Erreur réseau simulée sur l’enregistrement hebdomadaire : saisies conservées et nouvelle tentative réussie. Les identifiants, dates, tarifs et statuts des rendez-vous existants sont comparés avant/après les manipulations et restent inchangés.
- **1440, 1280, 1024, 768 et 390 px à zoom normal** : aucun défilement horizontal et contrôles à hauteur utilisable ; captures des quatre rubriques.
- **Équivalent 200 % sur un écran de 1440 px** : viewport CSS 720 px, rendu à densité 2 ; contrôle des quatre rubriques et captures. Cette méthode teste la redistribution et le grossissement sans dépendre d’un réglage personnel du navigateur.
- Clavier : onglets par flèches, focus initial du formulaire de service, fermeture des modales par Échap, restauration du focus et navigation mobile.
- Aucun script ESLint/Stylelint n’est configuré dans le projet. Les contrôles exécutés sont les vérifications TypeScript/Angular, les tests et la revue du navigateur ; aucune exécution de lint dédiée n’est revendiquée.

Les captures finales sont dans `../output/doctor-interfaces-2026-11-21/`. Elles utilisent uniquement les comptes de la base de test jetable. Les données de démonstration n’ont pas été intégrées dans l’application.

```powershell
cd C:\L3PI\HELLO-DOC\frontend
npm run build -- --configuration production
npm run test:booking
npm run test:approval
npm run test:appointments
npm run test:clinical
npm run test:agenda-account
npm run test:doctor-editors
```

Les tests navigateur exigent la fixture séparée et `HELLODOC_QA_BROWSER=1`. Le script attend Angular QA sur 4201 et son proxy vers 8083 ; Playwright doit être accessible au projet ou indiqué dans `HELLODOC_PLAYWRIGHT_PATH`. `HELLODOC_CHROME_PATH` permet d’utiliser Chrome installé localement. Ne pas exécuter ces manipulations sur une base contenant des comptes réels.

À vérifier séparément : rendu Safari/Firefox et appareils physiques. Les captures et parcours ci-dessus ont été vérifiés dans Chrome sur ce PC.
