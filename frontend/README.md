# Hello Doc — Angular frontend

Frontend Angular du prototype Hello Doc, plateforme de rendez-vous entre patients et médecins. L’interface est en français et prépare une intégration avec l’API Java Spring Boot. Aucun backend ni jeu de données de démonstration n’est inclus.

## Prérequis et commandes

- Node.js 20.19+ ou 22.12+ avec npm, compatibles Angular 20.
- `npm ci` pour installer les dépendances verrouillées.
- `npm start` pour lancer le serveur Angular sur `http://localhost:4200`.
- `npm run build` pour compiler en mode développement; `npm run build -- --configuration production` pour la version de production.

## Fonctionnalités de l’interface

- Accueil Hello Doc; connexion; inscription patient; inscription médecin avec spécialité, numéro professionnel, services/tarifs, horaires, jours travaillés, pause et lieu.
- Espace patient : recherche filtrée, profil médecin, réservation par service/date/créneau, rendez-vous, annulation, historique, avis et reprogrammation.
- Espace médecin : tableau de bord, demandes et changements d’état, agenda par rendez-vous, disponibilités, absences, dossier patient autorisé, fiche de consultation, avis et profil professionnel.
- Espace `ADMIN` séparé : tableau de bord alimenté par API, approbations des médecins, répertoires et journal. Ce rôle de plateforme est ajouté au prototype historique; il ne faut pas confondre avec le compte médecin appelé `admin` dans l’ancien JavaScript.
- Les composants métier appellent le backend. Sans API, l’interface affiche ses états d’indisponibilité au lieu de créer ou d’afficher des données fictives.

## Architecture

- Angular 20.3, TypeScript 5.9, Router, composants autonomes et Reactive Forms.
- `src/app/features`: pages d’accueil, authentification, patient, médecin, rendez-vous et administration.
- `src/app/core/services` et `models`: clients HTTP, session et contrats TypeScript.
- `src/app/core/guards`: contrôle de rôle et blocage des comptes médecin non approuvés.
- `src/environments/environment.ts`: URL centrale `http://localhost:8080/api`.
- Les requêtes utilisent les cookies (`withCredentials`). Le frontend initialise la protection CSRF via `GET /auth/csrf`, lit le cookie `XSRF-TOKEN` et envoie `X-XSRF-TOKEN` sur les méthodes de modification.

## Backend nécessaire

Spring Boot doit fournir les routes décrites dans `HELLO_DOC_BACKEND_SPECIFICATION.md`, utiliser des sessions `HttpOnly`, CSRF, CORS avec credentials et autorisation serveur. Les gardes Angular ne protègent pas les données à elles seules. Le frontend ne conserve ni mot de passe ni session métier dans `localStorage`/`sessionStorage`.

## Limites et validation

Les parcours sont câblés côté Angular mais les résultats serveur, la connexion, les notifications, les réservations, l’approbation médecin et la persistance ne seront utilisables qu’après livraison du backend. Les graphes et statistiques sont affichés seulement si l’API les renvoie. Le calendrier mensuel du médecin affiche les rendez-vous renvoyés par `GET /doctors/me/appointments?from=&to=` et propose la navigation entre les mois. L’envoi d’images, la suppression de compte, le détail patient côté administrateur et l’éditeur de préférences de notifications restent à compléter. `npm run build` passe. Une vérification navigateur Playwright couvre les routes publiques, les validations locales, l’absence de débordement horizontal aux largeurs 390, 768 et 1440 px, et les redirections de routes privées sans session. Les parcours authentifiés restent à vérifier avec le backend Spring Boot disponible. `npm ci` a signalé des advisories de dépendances à analyser avant production.
