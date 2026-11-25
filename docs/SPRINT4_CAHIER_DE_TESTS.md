# HelloDoc — cahier de tests Sprint 4

> Inventaire documentaire du dépôt au 24 novembre 2026. Les scénarios manuels sont des cas à exécuter; leur présence dans ce fichier ne vaut pas preuve d’exécution. Aucun test de bout en bout navigateur n’est déclaré dans package.json.

## 1. Automatisation présente

### Scripts frontend

| Script | Fichier | Objet |
|---|---|---|
| npm run test:booking | frontend/scripts/test-booking.mjs | Parcours et règles de réservation |
| npm run test:approval | frontend/scripts/test-approval.mjs | Approbation |
| npm run test:appointments | frontend/scripts/test-appointments.mjs | Actions rendez-vous |
| npm run test:clinical | frontend/scripts/test-clinical.mjs | Parcours clinique |
| npm run test:consultation-workspace | frontend/scripts/test-consultation-workspace.mjs | Tests Node de l’espace clinique/salle |
| npm run test:agenda-account | frontend/scripts/test-agenda-account.mjs | Agenda et compte |
| npm run test:doctor-editors | frontend/scripts/test-doctor-editors.mjs | Éditeurs médecin |

Ce sont des tests Node ciblés; ils ne démontrent pas l’accès effectif à une caméra, le réseau ICE ni le rendu dans un navigateur réel.

### Tests backend

Les tests JUnit/Spring/Mockito présents incluent notamment : TeleconsultationServiceTest, AppointmentServiceTest, AppointmentParticipantActionsTest, AppointmentMeetLinkServiceTest, ConsultationServiceTest, PrescriptionServiceTest, PatientClinicalServiceTest, AppointmentMeetLinkSecurityTest, AppointmentRescheduleSecurityTest, PatientAppointmentApiCompatibilityTest, SessionIntegrityFilterTest, SpaCsrfTokenRequestHandlerTest, tests de dépôt et contextes Spring. Inventaire exhaustif : backend/src/test/java/com/hellodoc/.

AdminSpecialtyMySqlIntegrationTest est conditionné par hellodoc.mysql.integration=true; l’exécution Maven ordinaire ne doit pas cibler MySQL. Sprint4ApplicationContextTest utilise H2 en mémoire. Ne pas définir le flag MySQL sur une base personnelle/de développement.

### Limites de couverture

Un test de service ne prouve pas le parcours clic → guard Angular → navigation → API → rendu complet. Aucun Playwright/Cypress/e2e n’est déclaré dans package.json. Les permissions média, l’heure simulée et le comportement navigateur demandent une recette manuelle.

## 2. Données et prérequis

Utiliser des comptes fictifs séparés patient/médecin, actifs, avec rendez-vous VIDEO. Pour les tests temporels, figer l’horloge ou préparer une heure reproductible. Jeu type : médecin A, patient A, médecin B, patient B; rendez-vous vidéo confirmé à 15 h 30, rendez-vous présentiel, rendez-vous annulé, sessions WAITING/ACTIVE dédiées. Les données cliniques ci-dessous sont fictives.

## 3. Statuts

| Statut | Sens |
|---|---|
| À exécuter | Prévu mais non exécuté lors de la rédaction |
| Réussi / Échoué | À remplir avec date, commit et environnement |
| N/A | Prérequis indisponible; expliquer sans conclure que le scénario passe |

## 4. Scénarios manuels

| ID | Précondition / action | Résultat attendu | Statut initial |
|---|---|---|---|
| T01 | Médecin propriétaire, VIDEO CONFIRMED à 15:30; ouvrir à 14:00 avec le bouton original | Route médecin ouverte; aucune session créée/démarrée; message d’ouverture visible | À exécuter |
| T02 | Même rendez-vous, 14:00 | Deux cadres vidéo et chat visibles; chat désactivé; Meet développé; fiche médicale et ordonnance visibles selon états | À exécuter |
| T03 | Avant 15:15, vérifier réseau et base | Aucun POST create/start; aucune ligne session créée prématurément | À exécuter |
| T04 | Après navigation anticipée, attendre 15:15 | Même page; démarrage activable selon le composant | À exécuter |
| T05 | Médecin démarre à 15:15 | Session créée/récupérée et ACTIVE; aucune navigation vers une autre page | À exécuter |
| T06 | Médecin démarre; patient rejoint une session ACTIVE | Signalisation, vidéo/audio selon permissions et réseau | À exécuter |
| T07 | Patient ouvre avant 15:15 le rendez-vous vidéo confirmé | Salle patient accessible; attente affichée | À exécuter |
| T08 | Utilisateur connecté non participant ouvre l’URL d’autrui | Refus guard/API; aucune donnée ou session divulguée | À exécuter |
| T09 | Patient tente route médecin et médecin route patient | roleGuard refuse ou redirige | À exécuter |
| T10 | Participant ouvre un rendez-vous annulé | Salle refusée; aucune session créée | À exécuter |
| T11 | Médecin ouvre un rendez-vous présentiel | Refus salle vidéo | À exécuter |
| T12 | Rendez-vous vidéo PENDING | Pas d’accès patient à la salle; médecin suit la confirmation | À exécuter |
| T13 | Session WAITING avant fenêtre, GET session | Démarrage non autorisé; page de travail conservée suivant le traitement d’erreur | À exécuter |
| T14 | Médecin démarre à startAt - 16 min | Backend refuse; état non ACTIVE | À exécuter |
| T15 | Médecin démarre à startAt - 15 min | Fenêtre autorisée; vérifier heure serveur | À exécuter |
| T16 | Démarrage à startAt + 30 min | Frontière inclusive autorisée selon service | À exécuter |
| T17 | Démarrage après startAt + 30 min | Backend refuse; UI ne contourne pas le contrôle | À exécuter |
| T18 | Compte médecin désactivé après connexion | Session invalidée/refusée par filtre d’intégrité | À exécuter |
| T19 | Requête write REST sans token CSRF | Requête refusée | À exécuter |
| T20 | API avec rôle non autorisé | 403/guard; aucune écriture | À exécuter |
| T21 | URL Meet HTTPS valide meet.google.com | Médecin propriétaire sauvegarde; participant lit selon règle Meet | À exécuter |
| T22 | URL Meet autre hôte, HTTP ou paramètres non permis | Validation refuse le lien | À exécuter |
| T23 | Déclarer Meet terminé pendant WebRTC ACTIVE | Refus métier; WebRTC inchangé | À exécuter |
| T24 | En session ACTIVE, envoyer message via UI | Chat temps réel et persistant visible des deux participants | À exécuter |
| T25 | Message vide, >2 000 caractères, puis après COMPLETED | Refus validation, taille et état terminé | À exécuter |
| T26 | Perdre/rétablir WS | Messages persistés; polling resynchronise l’historique | À exécuter |
| T27 | Recharger une salle ACTIVE | Historique conservé; reconnexion peut demander un réessai manuel | À exécuter |
| T28 | Médecin termine la session | État COMPLETED; aucun nouveau message accepté | À exécuter |
| T29 | Modifier la fiche puis naviguer sans sauvegarder | Guard avertit avant sortie | À exécuter |
| T30 | Sauvegarder note avec version courante | Version incrémentée; rechargement restitue la saisie | À exécuter |
| T31 | Sauvegarder une version périmée depuis deux onglets | Conflit contrôlé; pas d’écrasement silencieux | À exécuter |
| T32 | Finaliser avec rendez-vous non terminé ou résumé vide | Backend refuse; note non finalisée | À exécuter |
| T33 | Finaliser note d’un rendez-vous terminé avec résumé | Note finalisée et entrée d’historique | À exécuter |
| T34 | Modifier note finalisée sans motif | Refus; pas de révision invalide | À exécuter |
| T35 | Créer puis amender une ordonnance | Nouvelle ordonnance liée à l’ancienne; original préservé | À exécuter |
| T36 | Patient ouvre son historique | Consultations terminées, éléments finalisés seulement; note privée absente | À exécuter |
| T37 | Médecin ouvre dossier d’un patient jamais suivi | Refus/404; dossier non révélé | À exécuter |
| T38 | Modifier recommandation de suivi | Notification sans contenu privé et sans réservation automatique | À exécuter |
| T39 | Poste dans un autre fuseau | Heure affichée et fenêtre cohérentes avec Instant et Africa/Tunis | À exécuter |
| T40 | Deux patients réservent même créneau simultanément | Une seule réservation réussit; l’autre reçoit conflit | À exécuter |

## 5. Vérifications chat à préserver

Contrôler l’endpoint WS /api/teleconsultation-signal, les types JOIN/OFFER/ANSWER/ICE/CHAT, la persistance par TeleconsultationChatService, l’historique REST, le polling, la déduplication par identifiant, la limite 2 000 caractères et le refus après COMPLETED. Cette tâche documentaire n’exige aucune modification du handler, des payloads ou du composant chat.

## 6. Commandes disponibles

Depuis frontend : npm run test:booking, npm run test:approval, npm run test:appointments, npm run test:clinical, npm run test:consultation-workspace, npm run test:agenda-account, npm run test:doctor-editors; build : npm run build.

Depuis backend : mvn test avec configuration de test du dépôt. Ne pas activer -Dhellodoc.mysql.integration=true sur une base non dédiée. Consigner version, date, commit, statut DB et rapports Surefire.
