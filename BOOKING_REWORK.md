# Prise de rendez-vous patient

La page Angular existante `/patient/reserver/:doctorId` a été reconstruite à partir des exigences et de la capture fournie. Le site de maquette nécessitait une connexion ; la capture a servi de référence visuelle. Aucun site externe n'a été modifié et aucune donnée de démonstration n'est intégrée à la page.

## Comportement livré

- Navigation vert foncé avec logo existant, recherche médecin, rendez-vous, notifications, menu de compte, profil, paramètres et déconnexion ; destinations déjà présentes dans les routes Angular. Menu mobile avec état accessible et navigation repliée rendue inerte.
- Médecin déjà sélectionné : identité, spécialité et adresse issues de son profil, lien de retour à ce profil.
- Formulaire et récapitulatif côte à côte sur ordinateur, empilés sur mobile. Le tarif reste « — » jusqu'au choix du service ; la date et l'heure restent également vides jusqu'à leur sélection.
- Services chargés uniquement depuis le médecin concerné. Les services sans nom ou sans tarif ne sont pas réservables. Chargement, catalogue vide et erreur de chargement sont distincts.
- Cinq jours compacts, navigation par groupe et calendrier mensuel ouvrable au clavier ; une date sélectionnée commune à l'ensemble de la page. Statuts non limités à la couleur.
- Horaires affichés en boutons, exclusivement issus des vrais créneaux disponibles renvoyés par le serveur. Aucune borne de plage n'est transformée en réservation par le frontend. Identifiants conservés dans les objets de créneau ; `startAt` envoyé conformément au contrat existant.
- Horaires dédoublonnés par instant, triés et affichés dans le fuseau métier retourné par l'API. Le changement de service ou de date efface l'heure. Les requêtes précédentes sont annulées et les réponses tardives sont ignorées.
- Envoi uniquement lorsque le service, la date et un créneau encore présent dans les résultats sont valides. Motif facultatif conservé en cas d'erreur, coordonnées patient déduites de la session côté serveur.
- Double envoi bloqué pendant la requête et après succès. Le message de succès respecte le statut PENDING. Une erreur 409 conserve le motif, recharge le profil/tarif et les disponibilités, et exige une nouvelle sélection d'heure.

## Compléments serveur

GET `/api/doctors/{id}/booking-policy` expose le fuseau, les dates limites et le préavis réellement configurés. Les dates du frontend ne reposent plus sur une limite de 60 jours écrite en dur.

POST `/api/appointments` continue de vérifier le médecin actif, le service, les horaires, les absences et les réservations concurrentes. Le tarif enregistré reste celui du catalogue serveur. La nouvelle propriété facultative `expectedServicePrice` permet aussi de refuser avec HTTP 409 un changement de tarif entre l'affichage et l'envoi, plutôt que d'enregistrer silencieusement un autre prix. Le constructeur et les clients existants restent compatibles.

Les libellés « Première plage » et « Deuxième plage » restent disponibles dans les données et la gestion médecin ; seul leur affichage patient a été supprimé.

## Fichiers de cette intervention

Frontend :

- `frontend/src/app/features/patient/booking.component.ts` : état du parcours et orchestration HTTP.
- `frontend/src/app/features/patient/booking.component.html` : navigation, formulaire, calendrier, horaires et récapitulatif.
- `frontend/src/patient-booking.css` et `frontend/angular.json` : présentation dédiée, intégrée avec les feuilles globales existantes.
- `frontend/src/app/core/models/api.models.ts` : contrat du tarif attendu.
- `frontend/src/app/features/patient/booking.component.test.ts`, `frontend/scripts/test-booking.mjs`, `frontend/package.json` : tests de l'état réel du composant via `npm run test:booking`.

Backend :

- `DoctorController.java`, `DoctorProfileService.java`, `SecurityConfig.java` : politique de réservation publique du médecin actif.
- `AppointmentCreateRequest.java`, `AppointmentService.java` : comparaison du tarif affiché au tarif serveur.
- `AppointmentServiceTest.java` et `backend/verify-workflows.py` : validation du conflit tarifaire, de la politique et des réservations existantes.

Les modifications préexistantes dans le dépôt ont été conservées.

## Vérifications

- `npm run test:booking` : 8 tests réussis. Sélection/prix, changement de service/date, réponse tardive, doublons et créneaux invalides, limites et jours fermés, catalogue vide, panne réseau et nouvelle tentative, double soumission, succès PENDING et conflit.
- `npm run build -- --configuration production` : compilation vérifiée ; chargement initial d'environ 362 ko, sous le budget existant.
- Tests Maven : 43 tests réussis, dont la nouvelle vérification du tarif ; les tests existants des deux plages médecin restent verts.
- Recette HTTP/MySQL : 10 scénarios réussis sur une base temporaire, incluant désormais la politique publique et le refus d'un tarif divergent. Base de recette supprimée après vérification.
- Navigateur : parcours patient connecté → profil médecin → réservation, état initial sans tarif et bouton désactivé, service réel à 50 DT dans la base de test, date et heures réelles, changement de date avec heure effacée, bouton activé uniquement après sélection, envoi réel et message d'attente de confirmation. Aucun motif n'a été nécessaire pour envoyer la demande.
- Vues ordinateur 1280 px et mobile 390 px : largeur de contenu inférieure ou égale à la largeur visible ; navigation mobile ouvrable et récapitulatif synchronisé. Aucun libellé de plage dans le texte patient rendu.

Les fixtures et services simulés des tests unitaires ne sont pas utilisés par l'application. Le contrôle navigateur utilisait une autre base MySQL, uniquement pour la recette, sans données médicales réelles.

## Limites conservées

Le modèle métier actuel définit une durée et une pause par médecin/fenêtre, et non une durée propre à chaque service. Les créneaux conservent cette règle serveur ; aucune durée de service n'a été inventée. Les tarifs sont des chaînes déjà formatées dans le catalogue (par exemple en DT), sans nouveau système monétaire ajouté. Les dates limites sont configurables ; le fuseau métier actuel reste Africa/Tunis.

Le calendrier utilise les boutons natifs avec navigation Tab/Entrée et libellés explicites, sans prétendre implémenter une grille ARIA avec toutes les commandes fléchées d'un calendrier spécialisé. Les pannes réseau et réponses tardives sont couvertes par les tests du composant ; aucune interruption de réseau système n'a été imposée pendant la recette visuelle.
