# Évolution produit HelloDoc — audit du 21 novembre 2026

Le périmètre couvre le web Angular, le backend Spring Boot/MySQL et la compatibilité des contrats d’authentification Android. Les décisions déjà validées sont conservées : inscription médecin minimale avec justificatif, configuration après approbation, choix du nouveau créneau réservé au patient, avertissement d’absence affectant des rendez-vous.

## Constats avant intervention

| Priorité | Constat vérifié dans le code | Intervention |
|---|---|---|
| Haute | Les écrans de fiche de consultation et de dossier patient appellent des API inexistantes. | Persistance des notes, relation de soin limitée au médecin ayant réalisé la consultation, contrôle de version et historique de modification. |
| Haute | Les sessions conservent leurs anciennes autorités après une désactivation ou un changement de rôle. | Revalidation du compte avant les appels authentifiés, invalidation des sessions devenues obsolètes. |
| Haute | Déplacer un rendez-vous déjà confirmé conserve son ancien statut confirmé. | Nouvelle confirmation du médecin après déplacement choisi par le patient. |
| Haute | Des suppressions de comptes ayant un historique aboutissent à une erreur de contrainte générique. | Conservation de l’historique et refus explicite de suppression ; désactivation disponible. |
| Moyenne | Le portail assemble de nombreux parcours dans un composant dense ; des rubriques administrateur restent sans API. | Composants métier dédiés et données opérationnelles réelles sans accès administratif aux notes cliniques. |
| Moyenne | Dates, acteurs et motifs se collent dans les historiques ; la navigation varie entre détails, listes et formulaires. | Présentations structurées, formats de cabinet cohérents et navigation partagée. |
| Moyenne | La reprogrammation peut recevoir une réponse réseau obsolète ; certains boutons restent utilisables pendant le chargement. | Annulation des requêtes obsolètes, validation du créneau courant, préservation des saisies et garde d’envoi. |
| Moyenne | Les styles sont répartis entre règles historiques et surcharges ; certaines cartes contiennent des informations sans libellés. | Fondations visuelles communes, cartes de rendez-vous explicites, tableaux et formulaires lisibles et responsives. |

L’audit repose sur les routes, contrôleurs, services, requêtes JPA, entités, composants, formulaires, styles et tests existants. Les anciennes documentations décrivant des fonctionnalités prévues ne sont pas utilisées comme preuve de leur livraison. Les tests de recette utilisent une base temporaire et des données synthétiques identifiées.

## Limite de qualification

Les améliorations et tests ne constituent pas une certification de conformité ni une validation d’exploitation commerciale. L’hébergement, les sauvegardes/restaurations, le chiffrement et la gestion des clés, la politique de conservation, l’exploitation et les obligations applicables doivent être évalués pour le déploiement réel. Aucun diagnostic ou contenu médical n’est généré par l’application.

## Fonctionnement livré

### Rendez-vous et parcours patient

Le détail distingue service, date complète, horaire du cabinet, durée, coordonnées, tarif et motif. L’historique est une chronologie avec statut, auteur, date et motif sur des lignes distinctes. Le médecin confirme ou refuse une demande en attente ; il peut annuler un rendez-vous confirmé. Le patient choisit lui-même le nouveau créneau. Un déplacement remet la demande en attente de confirmation, y compris si son ancien créneau était confirmé. Une consultation déjà commencée ne peut plus être déplacée, sauf lorsqu’une absence a explicitement demandé sa reprogrammation.

La reprogrammation annule les anciennes recherches de disponibilités, efface l’heure lors d’un changement de date, déduplique les horaires et bloque les doubles envois. Une erreur conserve le motif. Un conflit actualise les créneaux. Les dates sont présentées en français et dans le fuseau `Africa/Tunis`.

Les résultats de recherche médecin n’affichent plus de filtres de statut propres aux rendez-vous. Les champs de recherche ont un nom accessible. Les filtres de comptes et de rendez-vous proposent les statuts de leur propre domaine. Le calendrier du profil distingue recherche en cours, erreur et jours sans disponibilité ; une ancienne réponse ne remplace plus le mois courant.

### Consultations et suivi du médecin

Après une consultation terminée, son médecin peut enregistrer un résumé obligatoire, des observations, des recommandations, une date de contrôle facultative et une note privée. Aucun de ces contenus n’est rempli automatiquement. Les notes sont persistées en base. Le médecin doit soumettre la version qu’il a lue pour modifier une fiche ; un conflit ne remplace pas silencieusement le contenu enregistré. Les saisies restent visibles après une erreur ou un conflit. Un avertissement protège une fiche modifiée avant de quitter son écran.

La date de contrôle envoie une notification générale invitant le patient à choisir un rendez-vous ; elle ne réserve aucun créneau et ne transmet aucun contenu clinique. Une mise à jour conservant la même date ne produit pas de notification en double. Une ancienne date de contrôle peut rester dans une fiche historique sans bloquer les autres corrections.

« Mes patients » recherche et pagine les patients ayant une consultation terminée avec le médecin connecté. Le dossier montre uniquement ses propres consultations terminées avec ce patient. Le patient, l’administrateur et un autre médecin ne peuvent pas accéder à la fiche. L’historique des 50 dernières modifications conserve auteur, date, action et numéro de version ; il ne contient pas de copies des anciennes notes et ne permet pas de restauration.

### Administration et comptes

Les tableaux de rendez-vous, leurs filtres, le journal et les indicateurs utilisent des requêtes backend réelles. La vue administrative exclut les motifs, les notes privées et les comptes rendus médicaux. Les décisions d’approbation/refus et de gestion des comptes sont consignées dans la même transaction que l’action. Le journal commence à l’activation de cette fonctionnalité ; les anciennes actions ne sont pas reconstituées artificiellement.

Les notifications administrateur affichent les candidatures en attente et les courriels d’approbation en échec, avec accès au profil pour réessayer l’envoi. Le refus d’une candidature demande un motif dans une boîte de dialogue. La désactivation demande confirmation. La suppression d’un compte possédant des rendez-vous est refusée avec un message proposant sa désactivation, pour préserver l’historique.

Les appels d’une session authentifiée vérifient que son compte existe encore, reste actif et possède toujours le même rôle. Une désactivation ou un changement de rôle invalide cette session au prochain appel. L’interface revient à la connexion. Les accès publics aux profils sont limités aux identifiants médecins et ne comprennent plus accidentellement `/doctors/me`.

### Présentation commune

Le logo et les couleurs HelloDoc sont conservés. Une navigation commune aux pages de détail et de suivi adapte ses destinations au rôle. Sur mobile, le menu expose son état, se ferme avec Échap et restitue le focus. Les fondations visuelles communes harmonisent titres, formulaires, cartes, boutons, états, chronologies et tableaux dans les trois espaces. Les tableaux restent dans une zone de défilement identifiée sur petit écran. Aucun jeu de démonstration n’est injecté dans l’application normale.

## Principaux fichiers de cette évolution

Les chemins ci-dessous sont relatifs à la racine du dépôt ; les fichiers d’approbation SMTP déjà modifiés précédemment restent conservés.

| Partie | Fichiers / composants |
|---|---|
| Sécurité backend | `backend/src/main/java/com/hellodoc/config/SecurityConfig.java`, `SessionIntegrityConfiguration.java`, `security/SessionIntegrityFilter.java` |
| Consultation | Contrôleur et service `ConsultationController`, `ConsultationService` ; entités `ConsultationNote`, `ConsultationRevision` ; leurs repositories et DTO `ConsultationNoteRequest`, `ConsultationNoteDto`, `DoctorPatientSummary`, `DoctorPatientDossier` |
| Administration | `AdminOperationsController`, `AdminOperationsService`, `AdministrativeAuditService`, `AdminDirectoryService`, `DoctorApprovalService` ; entité/repository `AdministrativeActivity`, DTO `AdministrativeActivityDto`, `AdminAppointmentDto` |
| Réservation et requêtes | `entity/Appointment.java`, `service/AppointmentService.java`, `repository/AppointmentRepository.java`, `repository/DoctorRepository.java` |
| Fondations frontend | `frontend/src/product.css`, `angular.json`, `shared/workspace-header.component.ts`, `shared/cabinet-date.pipe.ts`, `core/services/session-state.service.ts`, `auth.service.ts`, `core/interceptors/api-error.interceptor.ts`, `core/models/api.models.ts`, `appointment-permissions.ts` |
| Écrans médecin | `features/doctor/consultation-note.component.ts`, `patient-dossier.component.ts`, `doctor-patients.component.ts` |
| Écrans administrateur | `features/admin/admin-operations.component.ts`, `admin-inbox.component.ts`, `admin-doctor-detail.component.ts` |
| Rendez-vous et patient | `features/appointments/appointment-detail.component.ts` et `.html`, `reschedule.component.ts`, `features/patient/doctor-detail.component.ts`, `review.component.ts` |
| Navigation et authentification | `features/portal/portal.component.ts` et `.html`, `features/auth/login.component.ts`, `forgot-password.component.ts`, `app.routes.ts`, `core/guards/unsaved-note.guard.ts` |
| Vérifications | `backend/verify-workflows.py`, tests `ConsultationServiceTest`, `SessionIntegrityFilterTest`, `AppointmentParticipantActionsTest`, adaptations des tests de services ; `frontend/scripts/test-clinical.mjs`, `features/doctor/clinical-workflows.test.ts`, `features/appointments/appointment-actions.test.ts` |
| Recette isolée | `frontend/src/environments/environment.qa.ts`, `frontend/proxy.qa.json`, scripts npm dans `frontend/package.json` |

## Vérifications exécutées

| Vérification | Résultat |
|---|---|
| Maven `package` | 93 tests : 90 réussis, 0 échec, 0 erreur, 3 tests optionnels ignorés ; JAR construit |
| Frontend `test:appointments` | 8 tests réussis |
| Frontend `test:booking` | 8 tests réussis |
| Frontend `test:approval` | 5 tests réussis |
| Frontend `test:clinical` | 10 tests réussis |
| Compilation Angular production | Réussie |
| Recette HTTP / sessions / MySQL `verify-workflows.py --mail` | 22 scénarios réussis sur un schéma temporaire : concurrence, tarifs, absences, approbation et échec/reprise SMTP, notes et versions, dossier réservé au médecin, vues administratives et révocation de session |
| Navigateur ordinateur et 390 px | Connexion, listes, dossier, enregistrement d’une fiche, détail et historique du rendez-vous, menu et Échap, tableau et filtre administrateur, alertes administrateur, recherche médecin, profil et réservation patient vérifiés |
| Réservation depuis le navigateur | Bouton inactif avant les champs requis, tarif réellement renvoyé, activation après sélection d’une heure, réponse « Demande envoyée, en attente de confirmation du médecin » |
| Confidentialité dans la vue administrative | Le motif clinique de la recette n’apparaît pas dans le détail administrateur |

Les courriels de recette sont interceptés par un serveur SMTP synthétique local ; aucun courriel de recette n’est envoyé à Gmail. Les données utilisateur existantes ne sont ni utilisées comme fixtures ni remplacées. Les contrats d’authentification Android sont conservés ; l’APK Android n’a pas été recompilé pour cette évolution web/backend.

## Lancer et vérifier sur votre PC

1. Démarrer MySQL avec votre configuration habituelle.
2. Arrêter puis relancer **HelloDoc Backend** dans IntelliJ : les nouvelles API et protections nécessitent ce redémarrage. Conserver les variables JDBC, MySQL et SMTP personnelles déjà définies. Le lancement reste sur le port configuré, normalement 8081.
3. Dans `C:\L3PI\HELLO-DOC\frontend`, lancer `npm start`, puis ouvrir `http://localhost:4200`. Un serveur Angular déjà lancé recharge les sources ; actualiser le navigateur si nécessaire.
4. Côté médecin, ouvrir un rendez-vous terminé → **Ouvrir la fiche de consultation**, puis **Mes patients**. Côté administrateur, vérifier **Rendez-vous**, **Journal d’activité** et **Notifications**.

Pour reproduire la recette isolée sur cette machine : construire le JAR, puis définir `$env:HELLODOC_QA_PORT='8083'` dans PowerShell et lancer `python verify-workflows.py --mail --hold` dans le backend. Dans un autre terminal frontend, lancer `npm run start:qa`, ouvrir `http://localhost:4201` et utiliser uniquement les comptes de fixture définis dans le script. Appuyer sur Entrée dans le terminal de recette à la fin : le serveur et son schéma temporaire sont supprimés. Ce profil de recette n’est pas utilisé par `npm start` ni par la compilation de production.

## Limites restantes et mise en exploitation

- La récupération d’un mot de passe oublié par e-mail n’est pas implémentée ; son écran le dit explicitement et ne prétend pas envoyer de message. Le changement avec le mot de passe courant reste disponible.
- Les journaux et notes commencent à leur activation. Il n’existe pas de restauration d’anciennes versions cliniques ni de journal exhaustif des lectures.
- Le contrôle de session effectue une lecture du compte par requête authentifiée. Une exploitation à forte charge nécessite des mesures de performance et une stratégie de révocation adaptée.
- La configuration actuelle utilise encore la mise à jour de schéma Hibernate. Un déploiement doit prévoir des migrations versionnées, la sauvegarde et l’essai de restauration sur une copie avant d’appliquer une évolution à des données réelles.
- Les tests navigateur portent sur les parcours indiqués, et ne remplacent pas une matrice exhaustive de navigateurs, d’appareils, de lecteurs d’écran et de charge.
- Les états d’e-mail distinguent l’approbation du médecin et la transmission SMTP. Une acceptation SMTP ne garantit pas la réception dans la boîte principale du destinataire.
- La validation de l’hébergement, des habilitations organisationnelles, de la conservation, des sauvegardes, du chiffrement et de l’exploitation reste nécessaire avant une commercialisation. Aucune certification n’est revendiquée.

## Suite : agenda, mon compte et photos — 21 novembre 2026

### Parcours du médecin après acceptation

L’inscription conserve ses coordonnées et le justificatif professionnel. Après acceptation, l’accueil du médecin affiche un parcours de préparation avec trois destinations : photo, cabinet/services/tarifs et horaires. La photo est recommandée ; elle n’est pas une condition supplémentaire d’inscription ou d’acceptation.

Dans **Mon compte**, le médecin ajoute ou remplace sa photo, complète les informations de son cabinet, sa présentation et ses services. Les coordonnées et le changement de mot de passe sont réunis sur cette même page. Le patient dispose également d’un seul **Mon compte**. Les anciennes routes `/medecin/parametres` et `/patient/parametres` redirigent vers leur profil pour préserver les anciens liens.

La recherche patient affiche la photo réelle du médecin, son nom, sa spécialité, son cabinet, son lieu de consultation et sa prochaine disponibilité. La photo est aussi utilisée sur sa fiche détaillée et dans le récapitulatif de réservation. Les comptes sans photo conservent une présentation avec leurs initiales ; aucun portrait fictif n’est ajouté.

### Agenda et disponibilités

La vue mensuelle est retirée de l’agenda médecin. Les vues **Jour** et **Semaine** restent disponibles. La semaine présente au maximum trois rendez-vous par jour, puis un accès à la journée complète ; aucune consultation n’est supprimée des données. Les statuts disposent de couleurs distinctes et de libellés explicites. Des compteurs présentent les demandes en attente et les consultations confirmées de la période. Les recherches obsolètes sont annulées et toutes les pages de résultats sont chargées.

L’éditeur des disponibilités présente les jours ouverts/fermés avec des interrupteurs, le nombre de jours actifs, la durée et les pauses. Les première et deuxième plages restent nommées dans l’espace médecin. La seconde se déploie lorsqu’elle est activée. Les horaires peuvent être copiés vers d’autres jours. L’enregistrement bloque les commandes pendant le traitement.

Le test sur une semaine déjà configurée a révélé une contrainte d’unicité lors du remplacement des plages. La suppression est maintenant exécutée avant les nouvelles insertions dans la même transaction ; la correction a été vérifiée avec MySQL, y compris la conservation des rendez-vous et des consultations existants.

### Photos : stockage et accès

Les images sont enregistrées dans une table `profile_photos`, liée au compte. Le JPEG ou PNG fourni est vérifié côté serveur, redimensionné à 640 pixels maximum sur son plus grand côté et réencodé en JPEG. La limite d’entrée est de 5 Mo et de 20 mégapixels. Un fichier dont le contenu ne correspond pas à une image décodable est refusé. Une version dans l’URL permet d’actualiser la photo après remplacement.

Les opérations utilisent la session et la protection CSRF existantes :

- `POST /api/account/me/photo` : enregistrer la photo du compte connecté ;
- `DELETE /api/account/me/photo` : retirer sa photo ;
- `GET /api/account/me/photo` : lire sa photo avec authentification ;
- `GET /api/doctors/{id}/photo` : lire la photo publique d’un médecin actif.

Les photos des patients ne sont pas publiées par un endpoint de lecture par identifiant. La suppression d’un compte sans historique supprime aussi sa photo. Les modifications de coordonnées et de photo sont protégées contre l’écrasement mutuel des colonnes du compte. Les colonnes des comptes rendus ont également reçu une capacité explicite pour préserver les textes longs validés par les formulaires.

### Présentation et navigation

L’en-tête du portail comporte la date, l’espace courant et un accès au compte avec son avatar. Le pied de page commun rassemble les liens utiles sans destinations fictives. Les nouvelles pages s’ouvrent en haut ; le retour navigateur peut restaurer la position précédente. Les cartes, les sections de profil et les plages horaires ont été vérifiées sur ordinateur et à 390 px.

### Fichiers principaux de cette suite

| Partie | Fichiers |
|---|---|
| Agenda | `frontend/src/app/features/appointments/agenda.component.ts` et `.html` |
| Portail, compte et disponibilités | `frontend/src/app/features/portal/portal.component.ts` et `.html` |
| Identité visuelle | `frontend/src/cabinet.css`, `frontend/angular.json` |
| Photos et navigation communes | `frontend/src/app/shared/avatar.component.ts`, `profile-photo.component.ts`, `workspace-header.component.ts`, `workspace-footer.component.ts` |
| Fiches et réservation | Intégration de la photo et/ou du pied de page dans les composants de détail patient, médecin, consultation, reprogrammation, avis et administration |
| Routes et démarrage | `frontend/src/app/app.routes.ts`, `frontend/src/main.ts`, modèle `SessionUser` |
| Photo backend | `entity/ProfilePhoto.java`, `repository/ProfilePhotoRepository.java`, `service/ProfilePhotoService.java`, `controller/ProfilePhotoController.java` |
| Données et sécurité | `UserAccount`, DTO de session et de profil/recherche médecin, DTO de profil patient, `PatientProfileService`, `AdminProfileService`, `AdminAccountDeletionRepository`, `SecurityConfig` |
| Corrections de persistance | `DoctorProfileService.replaceWeeklyAvailability`, `entity/ConsultationNote.java` |
| Tests | `ProfilePhotoServiceTest`, `ProfilePhotoSecurityTest`, `agenda-account.test.ts`, `scripts/test-agenda-account.mjs`, `backend/verify-workflows.py` |

### Résultats de vérification de cette suite

Les tests backend totalisent **104 cas : 101 réussis, aucun échec et 3 tests optionnels ignorés**. Le JAR a été reconstruit. Les **38 tests frontend** réussissent, dont 7 nouveaux tests sur l’agenda, les routes du compte et les photos. La compilation Angular de production réussit.

La recette HTTP/MySQL atteint **28 scénarios réussis** : ajout, lecture publique de la photo médecin, confidentialité de la photo patient, remplacement avec nouvelle version, reconnexion, suppression, modifications simultanées de coordonnées et de photo, remplacement des horaires et conservation de l’historique. Le serveur SMTP de recette reste local et ne transmet aucun message à Gmail.

Le navigateur a permis de vérifier le parcours d’un médecin nouvellement accepté : accueil guidé, sélection native d’un fichier, enregistrement de la photo, saisie du cabinet et d’un service, configuration du premier jour de consultation, puis recherche de ce médecin depuis un compte patient. La photo chargée et les informations enregistrées ont été constatées sur la carte, la fiche et la réservation. Les vues semaine/jour ont été contrôlées avec une journée de six rendez-vous. Le compte patient unifié et les disponibilités ont été contrôlés à 390 px, sans débordement horizontal.

Pour essayer cette version, redémarrer le backend IntelliJ et relancer le serveur Angular (`Ctrl+C`, puis `npm start`) afin de charger les nouvelles routes, les styles et la gestion des photos. Dans l’espace médecin : **Agenda**, **Disponibilités**, puis **Mon compte**. Le stockage des photos ne demande pas de nouvelle variable d’environnement.
