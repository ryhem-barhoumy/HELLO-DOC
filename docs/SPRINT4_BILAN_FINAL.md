# HelloDoc — bilan final du Sprint 4

Date de recette : 24 novembre 2026. Ce document distingue les tests automatisés réellement exécutés des essais qui exigent une base MySQL et deux sessions utilisateur réelles.

## Partie 1 — Présentation générale

Le Sprint 4 regroupe la consultation présentielle et vidéo, la fiche clinique, les ordonnances facultatives, le dossier patient, l’historique des consultations et les recommandations de suivi.

- Le médecin dispose d’un espace de travail ouvert depuis le détail du rendez-vous confirmé. L’ouverture de cet espace ne démarre pas la caméra.
- Le patient ouvre la page de téléconsultation depuis son rendez-vous vidéo confirmé. Il n’a accès ni aux outils de rédaction du médecin, ni aux notes privées, ni aux documents en brouillon.
- En présentiel, le médecin ouvre le même espace clinique sans créer de session vidéo.
- WebRTC et le chat HelloDoc forment le canal vidéo principal. Le lien Google Meet est un recours manuel externe, réservé aux démonstrations avec des données fictives.
- La fin de l’appel vidéo et la clôture métier du rendez-vous sont deux actions séparées. La clôture met le rendez-vous à `COMPLETED` et ne supprime aucun brouillon sauvegardé.

## Partie 2 — Architecture technique

### Entités et relations principales

- `Appointment` relie un médecin, un patient, un horaire, un mode, un statut et, pour la démonstration, un lien Meet et l’horodatage/auteur de sa déclaration de fin.
- `TeleconsultationSession` appartient à un rendez-vous et porte les états `WAITING`, `ACTIVE`, `COMPLETED`.
- `TeleconsultationMessage` appartient à une session et à un expéditeur.
- `ConsultationNote` appartient à un rendez-vous et contient le compte rendu, les recommandations de suivi et la note privée du médecin.
- `ConsultationRevision` conserve les événements de versionnement. Lors d’une correction d’un rapport finalisé, elle conserve aussi le contenu antérieur et le motif de correction.
- `Prescription` appartient à un rendez-vous, peut référencer la version remplacée et contient des `PrescriptionItem`. Son état distingue `DRAFT` et `FINALIZED`.

### Services, API et composants

- `TeleconsultationService` contrôle les participants, l’ouverture de session et la fenêtre vidéo. `TeleconsultationChatService` gère les messages.
- `TeleconsultationSignalHandler` et `TeleconsultationWebSocketConfiguration` portent le canal de signalisation `/api/teleconsultation-signal`. WebRTC négocie les flux entre navigateurs ; l’accès aux périphériques exige HTTPS ou localhost.
- `AppointmentMeetLinkService` garde la validation HTTPS/hôte Google Meet et les contrôles médecin/patient. Une route additive permet au médecin d’enregistrer explicitement la fin du recours externe.
- `AppointmentService` gère les transitions du rendez-vous, l’historique, les droits et les notifications. Une clôture vidéo exige une session WebRTC terminée ou une déclaration Meet faite par le médecin ; elle ne crée pas de fausse session WebRTC.
- `ConsultationService` gère brouillons, finalisation, corrections avec versions antérieures, contrôle d’accès au dossier et liste « Mes patients ».
- `PrescriptionService` gère les ordonnances facultatives, leurs brouillons, leur finalisation et leur versionnement.
- `PatientClinicalService` ne transmet au patient que les notes partageables et finalisées ; `privateNote` n’est pas exposée.
- Les contrôleurs REST conservent les routes patient existantes. Les nouvelles informations de suivi et le retour Meet étendent les réponses de façon additive.
- `teleconsultation.component.ts` regroupe l’identité du rendez-vous, le canal vidéo, le chat, le recours Meet, la clôture et le composant `ConsultationNoteComponent` directement dans la même route et le même DOM Angular. Le panneau médical reste monté lorsqu’il est masqué.
- `consultation-note.component.ts` contient le compte rendu, son brouillon, l’ordonnance facultative, les recommandations de suivi et l’historique clinique autorisé.

### Persistance et évolutions de schéma

Les colonnes suivantes sont ajoutées de façon nullable ou facultative : `appointments.meet_call_ended_at`, `appointments.meet_call_ended_by`, `consultation_notes.recommended_delay`, `consultation_notes.follow_up_instructions` et les champs de snapshot/correction de `consultation_revisions`. Le lien `appointments.meet_link` existant est réutilisé. Aucun `DROP`, reset ou accès à la base personnelle n’a été exécuté.

Ces mappings ont été initialisés et testés sur H2 temporaire. Ils n’ont pas été appliqués ni vérifiés sur le MySQL du poste ; cette recette ne vaut donc pas validation de `ddl-auto=update` sur MySQL 8.4.

## Partie 3 — Règles métier

- Un rendez-vous passe notamment de `PENDING` à `CONFIRMED`, puis à `COMPLETED`. Seul le médecin concerné peut le clôturer.
- Une session vidéo passe de `WAITING` à `ACTIVE`, puis à `COMPLETED`. La déconnexion, une panne réseau ou le dépassement du créneau ne clôturent pas la consultation.
- Le médecin peut ouvrir la page de travail dès confirmation. Les connexions vidéo sont autorisées de 15 minutes avant le début prévu à 30 minutes après. Une session déjà active n’est pas coupée à la fin de cette fenêtre.
- Pour terminer un rendez-vous vidéo, le médecin termine WebRTC ou déclare explicitement avoir terminé l’appel Meet. La déclaration Meet est réservée au médecin propriétaire, horodatée, et ne prétend pas fermer Google Meet.
- Le patient peut récupérer un lien Meet uniquement pendant la fenêtre vidéo d’un rendez-vous confirmé. Les liens restent sous la politique d’admission de Google et sont réservés à une démonstration fictive.
- Le compte rendu peut rester en brouillon après la clôture ; il n’est pas publié automatiquement. La finalisation requiert un résumé clinique et le statut `COMPLETED`.
- La correction d’un rapport finalisé exige un motif. La version antérieure, son auteur, la date et la version restent dans le journal médecin. Le patient voit le document courant finalisé, sans note privée.
- L’ordonnance est facultative. Une ordonnance finalisée peut être corrigée par une nouvelle version ; aucune prescription vide n’est créée automatiquement.
- Le médecin peut recommander une date ou un délai et ajouter des instructions. Cette recommandation ne réserve pas un créneau ; le patient choisit une disponibilité et suit la confirmation habituelle.
- Le patient ne voit pas les brouillons de compte rendu ni les ordonnances en brouillon. Les dossiers sont limités aux participants autorisés.

## Partie 4 — Parcours utilisateur

### Consultation présentielle

1. Depuis le rendez-vous confirmé, le médecin ouvre « Ouvrir l’espace de consultation ».
2. La page affiche le patient et le rendez-vous, sans session WebRTC.
3. Le médecin saisit le compte rendu, sa note privée, le suivi et éventuellement une ordonnance.
4. Il sauvegarde le brouillon, puis termine la consultation. L’absence d’ordonnance n’empêche pas la clôture.
5. Il finalise le compte rendu et, s’il le souhaite, l’ordonnance. Le patient retrouve les documents partageables dans son dossier.

### Consultation WebRTC

1. Le médecin clique « Commencer la téléconsultation » sur un rendez-vous vidéo confirmé. La salle est préparée mais la caméra ne démarre pas.
2. La page de travail et les champs médicaux sont accessibles avant l’ouverture de la salle ; la date d’ouverture est affichée dans cette page.
3. Dans la fenêtre autorisée, le médecin démarre l’appel. Le patient ouvre sa page et rejoint l’appel ; caméra et microphone nécessitent l’autorisation du navigateur.
4. Le médecin utilise le chat et la fiche clinique dans la même page. Masquer le panneau ne détruit ni son formulaire ni l’état WebRTC.
5. « Terminer l’appel vidéo » met fin à la session vidéo seulement. Le médecin peut ensuite continuer son brouillon, puis clôturer le rendez-vous et finaliser les documents.

### Recours Google Meet (démonstration)

1. Le médecin saisit un lien Meet existant dans l’espace de consultation. Le backend valide son hôte, son chemin et l’absence de paramètres/fragment.
2. Le patient participant ne peut lire le lien que pendant la fenêtre vidéo. Un tiers et les comptes non participants sont refusés.
3. Les participants ouvrent la réunion externe dans un nouvel onglet. Le médecin peut poursuivre la saisie HelloDoc.
4. À son retour, le médecin confirme « J’ai terminé l’appel Google Meet ». HelloDoc enregistre l’auteur et l’heure, sans prétendre fermer ou contrôler la réunion externe.
5. Cette confirmation ne met pas le rendez-vous à `COMPLETED`. Le médecin procède séparément à la clôture métier.

### Brouillons, documents et suivi

- Le compte rendu ou l’ordonnance peut rester en brouillon après la clôture ; une alerte le signale et le médecin peut revenir les finaliser.
- Une ordonnance n’est jamais requise pour clôturer. Une correction de prescription produit une nouvelle version au lieu d’écraser l’ancienne.
- Après finalisation, le patient consulte les documents dans son dossier médical. Une recommandation de suivi affiche un accès à la réservation mais ne crée pas de rendez-vous.
- Une correction du compte rendu finalisé requiert un motif et conserve le contenu précédent dans le journal réservé au médecin.

## Partie 5 — Scénarios de tests manuels

Tous les parcours de cette section nécessitent une recette locale avec les comptes fictifs du propriétaire. Aucun parcours navigateur réel ni appel caméra n’a été observé dans cette session.

| Test | Compte | Prérequis | Actions | Résultat attendu | Statut |
|---|---|---|---|---|---|
| A — WebRTC complet | Médecin + patient dans deux navigateurs/appareils | Rendez-vous vidéo confirmé, backend et Angular démarrés, caméra/micro autorisés, TURN si nécessaire | Ouvrir la page en avance, démarrer à l’heure, rejoindre côté patient, échanger chat, saisir/sauvegarder un brouillon, terminer l’appel, clôturer, finaliser | La page médicale est accessible avant vidéo ; WebRTC/chat fonctionnent ; la fin vidéo ne clôt pas le rendez-vous ; patient retrouve les documents finalisés | Non testé dans navigateur |
| B — Meet et autorisations | Médecin + patient + compte tiers | Rendez-vous vidéo confirmé et lien Meet fictif valide | Ajouter depuis l’espace de consultation, ouvrir côté patient dans la fenêtre, tenter depuis tiers/hors fenêtre, déclarer la fin Meet, puis clôturer | Participant autorisé seulement ; fin Meet tracée ; pas de fausse session WebRTC ; statut reste confirmé jusqu’à clôture | Partiellement testé par tests de service/sécurité ; Meet réel non testé |
| C — Présentiel | Médecin + patient | Rendez-vous présentiel confirmé et heure dépassée ou clôture autorisée | Ouvrir le même espace, enregistrer brouillon, clôturer et finaliser | Aucun canal vidéo requis, le dossier est historisé | Automatisé en services/UI statique ; navigateur non testé |
| D — Ordonnance facultative | Médecin + patient | Rendez-vous confirmé puis terminé | Clôturer sans ordonnance ; refaire avec brouillon et finalisation ; créer une version corrigée | Pas de prescription vide, pas de blocage de clôture, patient voit seulement les prescriptions finalisées, version antérieure conservée | Tests backend de service exécutés ; navigateur non testé |
| E — Correction du compte rendu | Médecin + patient | Compte rendu finalisé | Modifier avec motif, consulter journal médecin, ouvrir dossier patient | Motif/version/auteur/date tracés ; précédente version conservée ; patient ne reçoit pas `privateNote` | Services et persistance H2 testés ; navigateur non testé |
| F — Sécurité | Médecin propriétaire, autre médecin, patient étranger, tiers | Comptes fictifs | Appeler les endpoints cliniques et Meet avec les différents comptes ; vérifier `privateNote` | Refus aux comptes non autorisés ; aucun brouillon ou note privée patient | Tests de sécurité/services automatisés ; tests HTTP réels non testés |
| G — Navigation et caméra | Médecin + patient, idéalement deux appareils | Rendez-vous vidéo confirmé, navigateurs compatibles | Vérifier bouton d’entrée avant l’heure, ouvrir/masquer outils, quitter avec formulaire non enregistré, déconnecter/rétablir le réseau | Aucun démarrage caméra à l’ouverture ; formulaire avertit avant navigation ; panneau ne démonte pas WebRTC ; perte réseau ne clôture rien | Assertions statiques et builds réussis ; navigateur réel non testé |

**Deux appareils recommandés.** Sur un seul ordinateur, deux sessions peuvent se gêner : navigateur ou système peut réserver la caméra à la première session, et un retour audio peut apparaître si les haut-parleurs captent le micro. Pour la recette, utiliser deux appareils ou un casque, des profils/navigateurs séparés et vérifier les permissions caméra/micro des deux côtés. Ne pas présenter un aperçu local ou une simulation comme validation vidéo.

## Partie 6 — Tests automatisés

### Backend

Commande complète exécutée avec la distribution Maven locale et des propriétés H2 temporaires :

```powershell
& 'C:\Users\DELL\.m2\wrapper\dists\apache-maven-3.10.0-bin\39q0qgmngdu329psje7bjgne79\apache-maven-3.10.0\bin\mvn.cmd' `
  '-Dspring.datasource.url=jdbc:h2:mem:hellodoc_final_suite' `
  '-Dspring.datasource.driver-class-name=org.h2.Driver' `
  '-Dspring.datasource.username=sa' `
  '-Dspring.datasource.password=' `
  '-Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect' `
  '-Dspring.jpa.hibernate.ddl-auto=create-drop' test
```

Résultat final : **148 tests, 0 échec, 0 erreur, 3 ignorés — BUILD SUCCESS**. La campagne ciblée qui précède a également réussi : 28 tests, 0 échec, 0 erreur. Elle inclut le démarrage complet Spring Boot sur Tomcat/H2, les repositories, l’URL Meet, les contrôles d’accès, les clôtures et les corrections cliniques. H2 n’est pas une validation MySQL.

### Frontend et build

Les six scripts existants ont été exécutés :

```powershell
npm run test:booking
npm run test:approval
npm run test:appointments
npm run test:clinical
npm run test:agenda-account
npm run test:doctor-editors
npm run test:consultation-workspace
npm run build
```

Résultats antérieurs : **62 tests passent** (10 + 5 + 8 + 12 + 7 + 13 + 7) ; le build Angular production passe. À la finition du 24 novembre 2026, les sept scripts frontend ont de nouveau été lancés : leurs résultats sont récapitulés ci-dessous. Les tests `consultation-workspace` vérifient les routes, l’intégration du formulaire clinique dans le DOM, l’absence de démarrage vidéo automatique, la déclaration Meet, la clôture séparée et la protection contre la perte de saisies. Ces assertions sont statiques et ne remplacent pas un test navigateur.

### Compatibilité API patient et tests ignorés

Les tests backend incluent les contrats existants d’authentification, profil, recherche et rendez-vous. Aucun endpoint historique, méthode HTTP ou mécanisme d’authentification/CORS n’a été modifié pour intégrer la page de travail. Les ajouts JSON sont facultatifs/additifs. Le projet Android/Retrofit n’est pas présent dans ce workspace : la comparaison directe de ses interfaces reste à faire.

Trois tests sont ignorés dans la suite. La base de recette utilisée est H2 temporaire ; aucun test de cette campagne ne certifie la migration ni le comportement sur le MySQL personnel.

## Partie 7 — Modifications effectuées

### Intégration de la consultation

- `frontend/src/app/features/appointments/appointment-detail.component.html` et `.ts` : rétablissement de « Commencer la téléconsultation » dès confirmation, indépendamment de la fenêtre réseau ; accès présentiel au même espace.
- `frontend/src/app/features/appointments/teleconsultation.component.ts` et `.css` : identité, vidéo, chat, fenêtre, Meet, outils cliniques intégrés, confirmation de clôture et état des sauvegardes. Le composant clinique reste monté lorsqu’il est masqué.
- `frontend/src/app/features/doctor/consultation-note.component.ts` et `.css` : mode intégré, sauvegarde, ordonnance, suivi, historique des corrections et motif obligatoire pour corriger un rapport finalisé.
- `frontend/src/app/app.routes.ts` et `frontend/src/app/core/guards/unsaved-consultation-workspace.guard.ts` : confirmation de sortie avec saisies cliniques ou ordonnance non sauvegardées.

### Backend, tests et fichiers créés

- Backend modifié pour la fin Meet explicite, la clôture vidéo et le versionnement des corrections : `Appointment.java`, `AppointmentService.java`, `AppointmentMeetLinkService.java`, `AppointmentMeetLinkController.java`, `MeetLinkDto.java`, `ConsultationNoteRequest.java`, `ConsultationService.java`, `ConsultationRevision.java`.
- Tests ajustés ou ajoutés : `AppointmentMeetLinkServiceTest`, `AppointmentMeetLinkSecurityTest`, `AppointmentServiceTest`, `ConsultationServiceTest`, `Sprint4RepositoryContextTest`, `Sprint4ApplicationContextTest`.
- Tests frontend : nouveau `frontend/scripts/test-consultation-workspace.mjs` et commande `test:consultation-workspace` dans `frontend/package.json`.
- Livrable créé : `docs/SPRINT4_BILAN_FINAL.md`.

Les fonctionnalités et fichiers construits avant cette passe de finition sont conservés. Le workspace fourni ne contient pas de métadonnées Git ; il n’est donc pas possible d’établir un diff Git complet ou de distinguer les éditions antérieures à cette recette.

## Partie 8 — Guide de lancement et recette locale

### Lancement

1. Dans IntelliJ, ouvrir le module `backend` et démarrer `HelloDocApplication` avec la configuration MySQL personnelle existante. Garder les paramètres personnels inchangés. Vérifier les logs Spring et l’initialisation des repositories.
2. Depuis `frontend`, exécuter `npm install` si nécessaire, puis `npm start`. Ouvrir l’origine Angular déjà configurée.
3. Utiliser uniquement des comptes fictifs de médecin et de patient disposant de rendez-vous/réservations compatibles. Ce rapport ne fournit ni ne change aucun mot de passe.
4. Dans l’espace médecin, ouvrir **Mes rendez-vous → rendez-vous vidéo confirmé → Commencer la téléconsultation** ; pour le présentiel, utiliser **Ouvrir l’espace de consultation**.
5. Dans l’espace patient, ouvrir le rendez-vous vidéo confirmé dans la fenêtre de salle. Accorder caméra et micro lorsque le navigateur les demande.

### Recette des données

- Après clôture, vérifier que le patient apparaît dans « Mes patients » et que l’historique comporte le rendez-vous.
- Finaliser un compte rendu et consulter le dossier patient ; vérifier que la note privée et les brouillons ne sont pas visibles.
- Créer une ordonnance de démonstration, la finaliser, puis contrôler son apparition dans le dossier. Vérifier qu’une consultation sans ordonnance se clôture aussi.
- Configurer un lien Meet fictif depuis l’espace médecin ; vérifier le contrôle d’accès patient, la fenêtre et l’action de déclaration de fin. Google Meet reste externe et sa réunion n’est pas pilotée par HelloDoc.
- Avant d’utiliser MySQL, vérifier les sauvegardes et le SQL de schéma avec l’équipe projet. Cette recette n’a exécuté aucune migration sur la base du poste.

## Partie 9 — Bilan final

### 1. Implémenté et vérifié automatiquement

- Page de travail commune et action d’entrée médecin restaurée ; accès patient à la même salle dès confirmation du rendez-vous vidéo, y compris avant la création de session ; en l’absence de session, la salle patient attend sa création côté médecin.
- Intégration Angular du formulaire clinique, de l’ordonnance, du suivi et de l’historique autorisé dans la même route, sans iframe ni nouvel onglet pour les formulaires.
- Gestion distincte des états WebRTC, Meet et rendez-vous ; déclaration Meet traçable et réservée au médecin ; accès patient au lien temporellement limité.
- Corrections de compte rendu avec motif et sauvegarde de la précédente version ; ordonnance facultative et versionnée.
- Démarrage complet Spring/repositories sur H2 ; contrôles de sécurité automatisés ; tests Angular et build production.
- Compatibilité préservée au niveau des routes patient backend vérifiées par tests.

### 2. Implémenté mais nécessitant une recette manuelle

- Caméra, microphone, signalisation et chat entre deux utilisateurs authentifiés sur deux navigateurs/appareils.
- Comportement réel des permissions, reconnexion réseau, appel long et clôture du médecin depuis les interfaces.
- Parcours patient complet de consultation, documents finalisés, dossier et réservation d’un suivi.
- Réunion Google Meet externe et règles d’admission du compte Google de démonstration.
- Mise à jour de schéma MySQL, affichage dans IntelliJ avec les données locales et compatibilité de l’application Android Kotlin/Retrofit.

### 3. Limites connues ou non terminées

- Aucune preuve de démarrage réel sur MySQL 8.4, aucune migration réelle et aucune donnée personnelle MySQL consultée.
- Aucun essai vidéo réel : l’environnement de travail n’avait pas de navigateur interactif disponible. WebRTC n’est donc pas déclaré validé en conditions réelles.
- Android Retrofit absent du dépôt monté : comparaison des DTO/routes avec l’application native à effectuer dans son dépôt.
- Google Meet demeure un secours externe de démonstration uniquement ; aucune garantie de confidentialité médicale n’est avancée.
- Trois tests Maven sont ignorés ; la suite complète restante passe sur H2 temporaire.

**Conclusion :** la finition fonctionnelle demandée est intégrée et passe les vérifications automatisées décrites. Le Sprint 4 ne peut pas être déclaré entièrement validé en conditions réelles avant les recettes locales MySQL, navigateur/WebRTC, Meet et Android listées ci-dessus.

### Vérifications effectuées lors de la reprise — 24 novembre 2026

- Tests frontend : `test:booking` 10/10, `test:approval` 5/5, `test:appointments` 8/8, `test:clinical` 12/12, `test:consultation-workspace` 11/11, `test:agenda-account` 7/7 et `test:doctor-editors` 13/13 (66 tests au total). Les six premiers scripts hors consultation-workspace ont passé lors de la campagne complète avant ce dernier ajustement ; `test:consultation-workspace` a été relancé après et passe à 11/11.
- Build Angular production : réussi.
- Vérification navigation : le détail médecin garde un seul bouton d’entrée qui prépare la session puis ouvre la salle existante ; le patient peut ouvrir cette salle depuis son rendez-vous vidéo confirmé et attend l’apparition de la session si nécessaire.
- WebRTC, chat réel, caméra, microphone, navigateur, Meet réel, base MySQL et compatibilité Android n’ont pas été testés dans cette reprise.

### Correction de l’interface vidéo et du chat — 24 novembre 2026

- Sur ordinateur, la grille de la salle garde les deux cadres et les contrôles vidéo à gauche, avec le panneau de chat dans la colonne de droite. Sur les écrans de moins de 850 px, les panneaux s’empilent ; sous 560 px, les cadres vidéo s’empilent aussi.
- Le panneau de chat reste dans le DOM quand aucune session n’existe ou qu’elle est en attente. Le champ et l’envoi restent désactivés jusqu’à ce que la session soit active et que le participant ait rejoint. L’historique déjà chargé reste rendu après la fin de l’appel.
- La logique existante de chargement périodique, signalisation CHAT, envoi/réception et persistance n’a pas été modifiée. Aucun fichier backend, service WebSocket ou code de persistance du chat n’a été modifié.
- Avant l’activation WebRTC, la fiche et l’ordonnance restent visibles en lecture seule. Elles deviennent modifiables lorsque la session est active, après la fin de l’appel ou après la clôture du rendez-vous ; les options de finalisation et de correction restent accessibles selon leur statut et leurs règles existantes.
- Meet reste dans la salle. Les contrôles et la structure WebRTC ainsi que le bouton d’entrée du rendez-vous sont conservés.
- Nouvelle recette frontend : assertions statiques sur la présence persistante du chat, ses conditions de saisie/envoi, la grille responsive et l’édition médicale avant/après l’appel. La suite complète finale compte 68 tests frontend réussis (10 + 5 + 8 + 12 + 13 + 7 + 13) ; le build Angular production passe. Ces vérifications ne constituent pas un essai navigateur/WebRTC réel.
- Scénarios manuels encore à exécuter : ouvrir la salle avant le créneau et vérifier la colonne chat en attente ; démarrer avec deux comptes et vérifier envoi, réception et historique ; terminer l’appel puis sauvegarder/finaliser la fiche et l’ordonnance ; vérifier les mêmes dispositions à largeur mobile.


### Correction de l acces medecin avant le creneau - 24 novembre 2026

Cette correction remplace la description anterieure du bouton medecin qui preparait une session avant la navigation.

- Cause confirmee dans le code : le clic appelait d abord POST /appointments/{id}/teleconsultation, puis naviguait seulement apres succes. TeleconsultationService.create applique ensureWindow, qui renvoie un conflit avant les 15 minutes d ouverture. La page n etait donc jamais atteinte.
- Le bouton d origine navigue maintenant directement vers la meme route Angular. Il n envoie aucune requete de creation WebRTC.
- Dans la salle, les requetes GET peuvent renvoyer 404 (aucune session) ou 409 (session WAITING encore hors fenetre). La salle continue d attendre; la creation n est lancee que lorsque la fenetre locale est ouverte, et le backend conserve ses controles. L action existante Demarrer la seance reste disponible dans cette page et reste conditionnee par l horaire.
- Le guard de route autorise les rendez-vous CONFIRMED ou COMPLETED selon le mode et le role. Il charge le rendez-vous via l API proprietaire existante : un rendez-vous inexistant ou non autorise est refuse, un rendez-vous annule est renvoye aux details. Le patient garde l acces confirme a sa salle avant l heure.
- Aucun backend n a ete modifie. Le chat, la signalisation, la persistance et les flux WebRTC n ont pas ete changes.
- Tests de regression : navigation immediate sans POST, attente avant la fenetre, creation et demarrage dans la fenetre, acces patient, refus d acces selon statut et role, et presence du parcours chat. test:consultation-workspace passe 16/16; l ensemble des scripts frontend passe 71 tests; le build de production Angular reussit.
- Ces tests sont des assertions de structure et de conditions Angular. Ils ne simulent pas un clic dans un navigateur ni un appel entre comptes reels; la tentative de recette navigateur a ete interrompue par la touche Echap. La recette locale du medecin avant 15h15 reste a effectuer avec le backend et des comptes de test.