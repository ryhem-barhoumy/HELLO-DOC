# Audit HELLO DOC

Date : 14 novembre 2026. Périmètre : analyse préalable à la refonte, sans modification du code source.

## Diagnostic

HELLO DOC dispose d'une base Angular et Spring Boot exploitable, avec authentification par session, inscriptions, validation des médecins, recherche, disponibilités et gestion des rendez-vous. Une réécriture de la pile technique n'est pas justifiée. En revanche, plusieurs écrans dépassent les capacités réelles des API, certains formulaires promettent des enregistrements incomplets, et la présentation est trop petite et trop générique pour une utilisation quotidienne en cabinet.

Le Sprint 0 décrit un produit plus vaste : téléconsultation, dossier médical partagé, ordonnances, cabinets, facturation, paiement et modération. Ces capacités ne peuvent pas être considérées comme livrées parce qu'une page ou une intention documentaire existe.

## Sources et limites de vérification

- Code actuel de `frontend/src` et `backend/src`, configuration, dépendances installées, contrôleurs, services, entités, requêtes de persistance et tests.
- Document fourni : `C:/Users/Win 11 Pro/Desktop/Sprint0_HelloDoc_Final.docx`, dont les paragraphes et tableaux ont été extraits. Aucun PDF distinct n'a été fourni. Les diagrammes incorporés n'ont pas fait l'objet d'une inspection visuelle.
- README racine, README des deux applications et `backend/docs/HELLO_DOC_BACKEND_SPECIFICATION.md`. Ces textes contiennent des descriptions historiques ou proposées ; le code actuel prévaut pour établir l'état d'implémentation.
- Compilation Angular de production réussie. Le premier essai échouait sur la récupération des polices Google dans l'environnement restreint ; le nouvel essai avec accès réseau a réussi.
- `mvn test -q` : **36 tests, 13 suites, 0 échec, 0 erreur, 0 test ignoré**. Rapports dans `backend/target/surefire-reports`. Tests unitaires et tests MVC avec dépendances simulées ; aucune preuve de concurrence MySQL réelle ou de parcours navigateur complet.
- MySQL Laragon démarré : version réelle **8.4.3**, base `hello_doc` présente. Spring Boot établit sa connexion JDBC et initialise JPA, puis le serveur Tomcat échoue au démarrage : `Unable to establish loopback connection`, causé par `SocketException: Invalid argument: connect` dans `UnixDomainSockets`. Deux essais, dont un avec IPv4 et répertoire temporaire court, donnent le même blocage. Ce symptôme ne démontre pas à lui seul un défaut du code métier.
- Frontend démarré : `http://localhost:4200/`, réponse HTTP **200**. Accueil, connexion, validation à vide et inscription médecin examinés dans le navigateur. Contrôle mobile à **390 × 844** : pas de débordement horizontal mesuré sur la connexion et l'inscription médecin. Les espaces authentifiés et les autres dimensions restent à vérifier visuellement.
- Aucun compte créé, aucune réservation créée, aucun mot de passe modifié dans le cadre de l'audit. Aucune validation CRUD réelle avec session n'est revendiquée. Le démarrage standard du backend exécute la configuration existante `ddl-auto=update` et les initialisations prévues par l'application ; ce n'est pas un démarrage garanti sans écriture en base.
- Les changements locaux préexistants ont été conservés : `AppointmentService.java`, `DoctorProfileService.java`, `DoctorProfileServiceTest.java`, `booking.component.ts`, ainsi que `backend/.idea/`. Le seul fichier ajouté pour cet audit est ce rapport.

## Architecture réellement présente

| Couche | État constaté |
|---|---|
| Frontend | Angular standalone ; Angular installé 20.3.19, CLI et builder 20.3.24 ; TypeScript 5.9.3, RxJS 7.8.2, Zone.js 0.15.1 |
| Interface | Templates HTML intégrés aux composants, CSS personnalisé global, DM Sans et Manrope chargées via Google Fonts ; Bootstrap absent des dépendances |
| Navigation | Routes publiques, patient, médecin et administrateur ; guards de rôle ; imports directs des composants, sans découpage des routes par chargement différé |
| Intégration | `ApiService`, services métier, requêtes avec cookies, intercepteurs CSRF et erreurs ; nombreux appels directs dans `PortalComponent` |
| Backend | Java 21, Java exécuté 21.0.10 ; Spring Boot 3.5.7, Spring MVC, Security, Data JPA, Bean Validation et Mail ; Hibernate observé 6.6.33.Final |
| Persistance | MySQL ; entités utilisateurs, patients, médecins, candidatures, spécialités, disponibilités, absences, rendez-vous, historique des statuts, avis, notifications |
| Documents | Justificatifs professionnels stockés dans un répertoire privé, téléchargement réservé à l'administration |
| Authentification | BCrypt, session serveur `JSESSIONID`, rotation de l'identifiant à la connexion, CSRF par cookie et en-tête ; aucun JWT implémenté |
| Exploitation | Ports documentés 4200 / 8081 / 3308 ; variables d'environnement pour configuration et secrets ; schéma géré avec Hibernate `update`, sans migrations versionnées présentes |

L'architecture à trois couches correspond au Sprint 0. L'écart JWT/session et l'absence de Bootstrap doivent être documentés comme décisions techniques. Il n'est pas nécessaire d'introduire JWT ou Bootstrap pour obtenir le résultat demandé.

### Points de maintenabilité

`PortalComponent` cumule navigation, tableaux de bord, recherches, listes, administration, horaires, absences et formulaires de profil. Les conditions par rôle et par rubrique, les objets `any` et les templates très longs rendent les régressions difficiles à isoler. Le CSS possède déjà des variables de couleur, mais ses règles sont très compactées et les dimensions restent dispersées.

La séparation progressive en shell partagé, composants visuels et pages métier peut se faire dans l'architecture actuelle. Une migration générale de framework serait une dépense sans bénéfice établi.

## Correspondance avec les 33 user stories du Sprint 0

« Présent » désigne une implémentation identifiée, pas une recette de bout en bout réussie. Les tests ne couvrent pas toutes les lignes.

| US | Besoin | État dans le code |
|---|---|---|
| 1 | Authentification administrateur | Présente ; contrôles d'accès administrateur testés en MVC |
| 2 | Authentification patient et médecin | Présente ; sessions et BCrypt ; connexion réelle à recetter |
| 3 | Inscription patient | Présente ; rôle attribué côté serveur, profil patient créé |
| 4 | Inscription médecin et justificatif | Présente ; multipart, contrôle de signature PDF/JPEG/PNG, limite 5 Mo |
| 5 | Approbation et refus médecin | Présents ; création du profil et activation ; motif de refus générique dans l'interface |
| 6 | CRUD et blocage des utilisateurs | Partiel ; consultation, activation et suppression présentes ; création et modification administratives générales absentes |
| 7 | Gestion du profil utilisateur | Partielle ; profils patient et médecin présents ; mise à jour du profil administrateur sans route correspondante |
| 8 | Gestion des spécialités | Lecture et initialisation présentes ; CRUD administratif absent |
| 9 | Gestion des cabinets | Absente comme module ; une adresse de cabinet dans un profil ne constitue pas sa gestion |
| 10 | Profil professionnel médecin | Partiel ; services et tarifs présents ; horaires du formulaire de profil ignorés à la mise à jour ; expérience non modélisée explicitement |
| 11 | Horaires et disponibilités | Présents ; deux plages par jour, absences totales et partielles ; conflits avec rendez-vous existants à traiter |
| 12 | Recherche de médecins | Présente ; spécialité, ville et disponibilité ; pagination UI et passage à l'échelle incomplets |
| 13 | Profil et disponibilités publics | Présents ; créneaux et calendrier calculés ; incohérence d'accès anonyme au calendrier mensuel |
| 14 | Prise de rendez-vous | Présente ; statut initial PENDING, validation serveur et mécanismes de concurrence |
| 15 | Modification et annulation patient | Présentes ; reprogrammation, vérification des créneaux et appartenance |
| 16 | Rendez-vous à venir et historique | Présents ; listes et compteurs ; pagination non exposée dans l'interface |
| 17 | Acceptation et refus médecin | Présents ; transitions contrôlées côté serveur |
| 18 | Agenda et modification médecin | Partiels ; vue mensuelle et reprogrammation présentes ; ergonomie, filtres et complétude des résultats à corriger |
| 19 | Notifications des rendez-vous | Notifications internes persistantes présentes ; absence sans traitement des rendez-vous impactés ; pas de SMS ni rappel planifié identifié |
| 20 | Gestion administrative des rendez-vous | Absente ; rubrique explicitement indisponible |
| 21 | Téléconsultation visio et chat | Absente ; pas d'implémentation WebRTC/WebSocket identifiée |
| 22 | Liste des patients du médecin | Écran présent ; API `/doctors/me/patients` absente |
| 23 | Dossier médical patient | Écran présent ; modèle et API correspondants absents |
| 24 | Compte rendu de consultation | Formulaire présent ; API `consultation-note` et persistance absentes |
| 25 | Ordonnance | Absente |
| 26 | Dossier partagé consultable par le patient | Absent ; l'historique de rendez-vous ne remplit pas ce besoin |
| 27 | Historique des consultations | Partiel ; rendez-vous terminés visibles, sans dossier clinique associé |
| 28 | Factures et tarifs médecin | Tarifs présents comme chaînes dans les services ; facturation absente |
| 29 | Factures patient et paiement | Absents |
| 30 | Suivi financier administrateur | Absent |
| 31 | Note et avis après consultation | Présents ; consultation terminée exigée, avis unique par rendez-vous et modification autorisée |
| 32 | Modération et signalements | Absents |
| 33 | Tableaux de bord et statistiques | Partiels ; agrégats patient/médecin et comptes administratifs présents, statistiques administratives détaillées absentes |

### Corrections à apporter au référentiel

Le document Sprint 0 actuel reconnaît bien trois acteurs. La spécification technique du dépôt cite encore un ancien rapport à deux rôles : cette source historique ne doit pas remplacer le document fourni maintenant.

Les charges affichées par sprint correspondent aux estimations des user stories : **81, 76, 88, 102 et 90 heures**, soit **437 heures** au total. Ces estimations restent des prévisions du document, sans mesure d'avancement technique associée.

## Défauts et risques prioritaires

### 1. Écrans reliés à des API absentes

Preuves : `features/portal/portal.component.ts`, `features/doctor/patient-dossier.component.ts`, `features/doctor/consultation-note.component.ts`, contrôleurs `DoctorController`, `PatientController`, `AdminController`, `AppointmentController`.

- La liste des patients médecin et le dossier détaillé appellent des routes non exposées.
- Les notes de consultation effectuent GET et PUT sur une route absente. Le composant ignore l'erreur initiale 404, ce qui laisse un formulaire vide ressemblant à un parcours disponible.
- Les paramètres patient/médecin chargent `/patients/me/preferences` et `/doctors/me/preferences`, routes absentes, alors que le changement de mot de passe dispose d'une autre API existante.
- Le formulaire de profil administrateur appelle PUT `/admin/me` ; le contrôleur ne définit que GET.
- Les rubriques rendez-vous, notifications et journal administratifs affichent une indisponibilité. Elles ne sont pas opérationnelles.

Conséquence : une interface soignée ne suffira pas à restaurer ces parcours. Corriger le contrat API ou présenter explicitement l'état indisponible avant de permettre une saisie.

### 2. Profil médecin et horaires incohérents

Preuve : `DoctorProfileService.update` et `PortalComponent.saveProfile`.

Le frontend transmet `workStart`, `workEnd` et `workingDays`, mais la mise à jour serveur ne les applique pas. Le serveur conserve les fenêtres existantes et change leur durée/pause. L'utilisateur peut recevoir une confirmation sans obtenir les jours ou heures demandés. Le profil résume aussi plusieurs plages par un début minimal et une fin maximale, masquant les coupures.

Préconisation : le profil gère l'identité et l'activité ; l'éditeur hebdomadaire devient l'unique lieu de gestion des horaires, avec lien explicite depuis le profil.

### 3. Absences et rendez-vous existants

Preuve : `AbsenceService` ne modifie que les absences ; `AppointmentStatus` ne contient pas `TO_RESCHEDULE`, pourtant utilisé dans le frontend.

Une absence retire les futurs créneaux proposés mais laisse les rendez-vous existants dans leur état. Aucun traitement des patients impactés ni notification spécifique n'est déclenché ici. La réservation verrouille la ligne médecin, mais les mutations d'absence et d'horaires ne suivent pas ce même protocole : leur concurrence avec une réservation reste à tester et à protéger.

La décision de conserver, annuler ou demander une reprogrammation doit être explicite. Elle ne peut pas être inventée à partir d'un statut frontend inutilisé.

### 4. Listes et agenda incomplets

Preuves : `PortalComponent.load`, `filteredRecords`, `appointmentsOn`, `DoctorController.appointments`.

Les réponses paginées sont réduites à `content` sans commandes de pagination. Limites actuelles notamment : rendez-vous patient 30, annuaires administratifs 50, agenda médecin 100 par défaut. Les éléments suivants peuvent être invisibles sans indication claire. Les filtres opèrent sur les seuls éléments chargés.

La vue mensuelle utilise `records` directement : les filtres recherche/statut ne filtrent pas les événements du calendrier. Les événements ont une présentation identique quelle que soit leur situation. Une liste générique de cartes s'affiche aussi pour ces données, ce qui duplique l'information autour de l'agenda.

### 5. Réservation et reprogrammation

Points solides : patient déduit de la session, médecin actif obligatoire, service vérifié dans son catalogue, revalidation des horaires et absences, verrou pessimiste médecin, contrainte unique `(doctor_id, reservation_key)`, transaction et contrôle de propriété.

À corriger ou éprouver :

- `book` traite le conflit d'intégrité lors d'un flush ; `reschedule` n'a pas le même traitement explicite ni de journal dédié de l'ancien/nouveau créneau.
- Les transitions de statut sont historisées, mais une reprogrammation ne conserve pas un historique des anciennes dates.
- Un changement de durée du médecin peut modifier la durée d'un ancien rendez-vous lors de sa reprogrammation : règle à décider.
- Les demandes passées encore PENDING ne disposent pas d'une politique d'expiration explicite.
- `booking.component.ts` lance le calendrier avant d'aligner le mois sur la date reçue en paramètre. Un lien vers le mois suivant peut présenter le mauvais mois.
- Les appels de créneaux et de listes ne sont pas systématiquement annulés quand la sélection change. Des réponses tardives peuvent remplacer un choix plus récent.
- Dans `reschedule.component.ts`, une erreur masque le formulaire entier, ce qui empêche de changer facilement de date pour réessayer. Certains messages d'erreur sont effacés par le rechargement immédiat des créneaux.
- Le frontend affiche en dur un préavis de 60 minutes et un horizon de 60 jours alors que le backend les rend configurables.

### 6. Suppression des comptes

Preuves : `AdminDirectoryService`, `AdminAccountDeletionRepository`, associations obligatoires des rendez-vous, avis, notifications et historique.

La suppression native retire profils et comptes sans traiter toutes les références issues des rendez-vous et notifications. Un compte ayant un historique peut donc rencontrer un conflit de clé étrangère. Les tests existants simulent les dépôts ; ils ne valident pas les contraintes MySQL réelles.

Préconisation : décider de la désactivation, de l'anonymisation et de la conservation des historiques avant de compléter une suppression définitive. Ne pas résoudre ce point par une cascade générale non justifiée.

### 7. Authentification et rôles

La base est saine : secrets hors frontend, hachage BCrypt, pas de stockage local de session fictive, CSRF actif, CORS limité à une origine, vérifications de rôle et de propriété pour les rendez-vous.

Points à compléter :

- `USER` sert aux patients et aux candidatures médecins en attente ; `PATIENT` reste aussi accepté. Clarifier cette convention et vérifier systématiquement le statut actif.
- La désactivation empêche une nouvelle connexion et plusieurs services revérifient le statut, mais elle n'invalide pas explicitement les sessions existantes. Tous les points d'entrée ne revérifient pas l'état courant de la même façon.
- Les paramètres de session de production, cookies sécurisés, expiration explicite, révocation après changement de mot de passe et limitation des tentatives ne sont pas définis dans les fichiers examinés. Les valeurs effectives du conteneur doivent être vérifiées avant déploiement.
- Le calendrier mensuel est déclaré `permitAll()` dans le contrôleur, mais son chemin n'est pas dans la liste publique du filtre HTTP, contrairement aux créneaux. Son accès anonyme reste donc bloqué par ce filtre.
- L'intercepteur transforme tout HTTP 401 en « session expirée », même lors d'une mauvaise connexion ou d'un mot de passe actuel incorrect. Adapter le message au contexte.
- Des messages backend restent en anglais et des détails techniques sont affichés à l'utilisateur, notamment dans l'inscription médecin et la récupération du compte.

### 8. Performance, configuration et qualité

- Bundle initial de production : **531,90 ko**, budget d'avertissement **500 ko**, soit **31,90 ko** au-dessus. Les routes chargent les composants directement.
- URL API compilée en dur : `http://localhost:8081/api`, y compris pour le build de production actuel. Prévoir une configuration de déploiement cohérente.
- Polices externes nécessaires à l'inlining du build : dépendance réseau constatée. Héberger les fichiers utiles localement rendrait le résultat reproductible.
- La recherche par disponibilité évalue au plus les 500 premiers médecins correspondant aux autres critères, puis filtre en mémoire. Les résultats au-delà peuvent être omis.
- La recherche et le calendrier calculent des disponibilités jour par jour avec plusieurs accès aux dépôts. Le mapping d'un rendez-vous charge séparément historique et avis. Risque de multiplication des requêtes à mesurer sur données représentatives.
- Pas de migrations versionnées ; `ddl-auto=update` ne remplace pas une stratégie de migration, restauration et évolution du schéma.
- Aucun script de tests frontend ni suite de recette navigateur dans la configuration livrée.
- Le README racine annonce encore les rendez-vous comme futurs, bien qu'ils soient codés. La spécification mélange propositions et réalisations, dont PostgreSQL dans une feuille de route alors que MySQL est réellement utilisé.

## Audit visuel et ergonomique

La palette verte, les surfaces claires et les deux familles typographiques constituent une base cohérente. L'accueil est déjà plus travaillé qu'un simple CRUD. Le principal défaut transversal est la lisibilité et l'organisation des tâches métier.

Mesures CSS : labels de connexion à **10 px**, navigation à **11 px**, actions secondaires et badges à **9 px**, événements de calendrier à **8 px**, jusqu'à **7 px** sur mobile. Ces tailles ne conviennent pas à une consultation rapide et prolongée.

L'inscription médecin conserve une grille dense sur téléphone. À 390 px, le message de backend indisponible occupe une colonne étroite à côté de la spécialité, avec un bouton natif visuellement différent. La connexion et l'inscription n'ont pas débordé horizontalement sur les vues mesurées, mais cela ne vaut pas validation de l'ensemble du produit.

Autres points identifiés dans les templates : icônes faites de caractères Unicode de styles différents ; dates parfois affichées brutes ; filtres génériques proposant des statuts sans rapport avec certaines listes ; cartes peu adaptées aux annuaires administratifs ; confirmations `window.confirm` ; absence de fermeture du menu mobile lors de la navigation et de gestion explicite de son focus ; messages d'état et champs sans traitement d'accessibilité uniforme.

### Architecture visuelle proposée

- Palette : vert profond pour les actions, fond gris très clair, texte sombre, couleurs sémantiques distinctes pour attente, confirmation, annulation, absence et fin de consultation, toujours accompagnées d'un libellé.
- Corps de texte **16 px**, labels **14–15 px**, métadonnées **13–14 px**, titres de page **28–32 px**. Ajuster après contrôle des tableaux et du mobile.
- Échelle d'espacement 4 / 8 / 12 / 16 / 24 / 32 ; rayons communs, bordures sobres, ombres légères ; cibles tactiles confortables d'environ 44 px pour les actions principales.
- Shell commun : navigation latérale, barre supérieure, titre et actions de page. Le rôle modifie les entrées autorisées sans changer la grammaire visuelle.
- Composants communs : boutons, champs et erreurs, badges, états de chargement/vide/erreur, tableaux paginés, confirmations et retours d'action accessibles.
- Annuaires : tableaux sur grand écran, fiches synthétiques sur mobile ; recherche et pagination serveur ; filtres propres à chaque métier.
- Formulaire médecin organisé par identité, activité, justificatif et horaires, avec progression et conservation des saisies en cas d'erreur.

### Nouvel agenda proposé

Sur ordinateur : vue semaine pour le travail courant, vue jour pour le détail et vue mois pour l'orientation. Sur téléphone : liste chronologique par journée. L'ajout d'une vue doit répondre au besoin observé ; la vue mois ne doit pas imposer l'affichage de tous les détails.

Barre d'outils : période, Aujourd'hui, changement de vue, recherche et filtres pertinents. Pour le médecin connecté, le périmètre reste son propre agenda. Un filtre multi-médecins appartient à un espace administratif ou cabinet disposant de l'autorisation correspondante.

Chaque rendez-vous montre heure, patient, service et statut. Le détail et les actions s'ouvrent dans un panneau cohérent. Les plages occupées, fermées et absentes sont distinguées. Le parcours patient reste médecin → service → date → créneau libre → récapitulatif → confirmation avec revalidation serveur.

## Plan de réalisation par priorité

Chaque lot doit être vérifié avant d'élargir le périmètre. Aucun délai de réalisation n'est fixé sans découpage de tickets et décisions métier.

| Lot | Modifications et raison | Fichiers ou zones concernés | Résultat et validation attendus |
|---|---|---|---|
| P0 — Référence et environnement | Lever le blocage de serveur HTTP, établir le contrat réel, corriger la documentation | README, spécification backend, configuration locale et de déploiement | API accessible ; liste exacte des routes et états ; version de référence reproductible |
| P0 — Parcours défectueux | Corriger les profils, les préférences, les formulaires sans API et les erreurs trompeuses | `portal.component.ts`, profils backend, composants dossier/consultation, intercepteurs | Aucun formulaire actif sans sauvegarde réelle ; valeurs relues identiques aux valeurs enregistrées |
| P0 — Cohérence métier | Décider et protéger absences, reprogrammations, sessions désactivées et suppression | `AppointmentService`, `AbsenceService`, `DoctorProfileService`, administration, repositories, sécurité | Tests MySQL des conflits et contraintes ; contrôle d'accès avec comptes actifs/inactifs ; historique conservé selon règle validée |
| P1 — Fondations visuelles | Créer tokens, composants communs et shell ; augmenter la lisibilité | `styles.css`, shell extrait de `PortalComponent`, composants partagés | Navigation clavier et mobile utilisables ; mêmes dimensions et états sur les pages prioritaires |
| P1 — Tableaux de bord et annuaires | Hiérarchiser les informations et exposer pagination/recherche serveur | Pages tableau de bord, médecins et patients ; services et requêtes | Données réelles, compteurs définis, résultats au-delà de la première page accessibles |
| P1 — Rendez-vous | Recomposer agenda, réservation, détail et reprogrammation | Pages rendez-vous, booking, reschedule, API de listes et disponibilité | Recette patient/médecin complète ; deux réservations concurrentes donnent un seul succès ; changements visibles après relecture |
| P2 — Administration complète | Compléter spécialités, cabinets et rendez-vous selon backlog ; journaliser les actions | Contrôleurs/services admin, nouvelles pages ciblées | Droits explicites, pagination, historique d'administration et absence d'accès clinique implicite |
| P2 — Dossier médical | Implémenter les modèles, autorisations, notes, comptes rendus et ordonnances | Nouvelles entités/API cliniques ; pages dossier/consultation existantes | Accès patient et médecin autorisé vérifié ; note privée distincte ; persistance et audit testés |
| P3 — Autres modules Sprint 0 | Téléconsultation, messagerie, facturation, paiement et modération après cadrage | Modules dédiés et intégrations externes | Tests d'intégration, erreurs, retours de paiement et autorisations propres à chaque module |

Pages à traiter d'abord après les P0 : shell et tableaux de bord → annuaire médecins et profils → réservation et agenda → annuaires administratifs → paramètres et notifications. Le dossier médical doit attendre un contrat de confidentialité explicite.

## Décisions métier à clarifier avant les fonctions dépendantes

1. Lors d'une absence recouvrant des rendez-vous : avertir seulement, bloquer l'absence, demander une reprogrammation ou annuler ? Qui confirme et comment prévenir le patient ?
2. Une reprogrammation d'un rendez-vous confirmé conserve-t-elle sa confirmation ? Quel délai d'annulation et quelle durée conserver si les horaires ont changé ?
3. Le médecin peut-il créer un rendez-vous pour un patient ? Le Sprint 0 décrit surtout la réservation par le patient ; ne pas inventer ce droit.
4. Quels médecins peuvent lire le dossier partagé ? Le médecin d'un rendez-vous, plusieurs praticiens autorisés, ou un cabinet ? Quelles données restent privées ?
5. Quels droits cliniques précis donner à l'administrateur ? Le « plus haut niveau de privilège » du document ne suffit pas à définir un accès aux notes médicales.
6. Que conserve-t-on après suppression ou blocage d'un compte, et comment traiter ses rendez-vous à venir ?
7. Quel modèle de cabinets : simple adresse, plusieurs médecins, plusieurs sites, ressources communes ?
8. Quels canaux et délais de notification, quelle politique de relance, et quels prestataires pour paiement et téléconsultation ?
9. Confirmer le fuseau métier : `Africa/Tunis` est codé côté serveur, mais plusieurs affichages et bornes de dates utilisent le fuseau du navigateur.

## Recette à exécuter avant de déclarer la refonte fonctionnelle

- Connexion, restauration et expiration de session, déconnexion, mauvais identifiants, comptes en attente/refusés/inactifs ; matrice des trois rôles.
- Inscription patient et médecin, fichier absent/incorrect/trop grand, doublon d'e-mail, approbation et refus, profil enregistré puis relu.
- Recherche multi-pages, combinaison des filtres et retour sans résultat ; aucun résultat silencieusement tronqué.
- Réservation, confirmation/refus, annulation et libération d'un créneau ; reprogrammation, avis après consultation terminée ; notification persistée et marquage de lecture.
- Deux sessions réservent le même créneau ; réservation concurrente avec absence ou modification d'horaire ; transactions et contraintes MySQL réellement exercées.
- Suppression ou désactivation avec historique et rendez-vous futurs ; absence d'accès à un dossier ou rendez-vous d'un autre utilisateur.
- Contrôles visuels aux formats 1440, 1280, 1024, 768 et 390 px, zoom 200 %, navigation clavier, focus, messages d'erreur, longues valeurs et grands volumes.
- Build production, tests backend existants et tests ciblés de chaque correction ; validation de la configuration d'API, des migrations et du démarrage complet.

Le passage à la refonte doit conserver les mécanismes métier existants qui sont utiles, corriger les promesses d'interface sans API, puis améliorer l'expérience sur une base vérifiée. Cet audit ne déclare pas achevée la validation fonctionnelle de bout en bout.
