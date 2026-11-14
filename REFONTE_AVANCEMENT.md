# Première livraison de la refonte HELLO DOC

14 novembre 2026. Ce document décrit les changements réalisés après l'audit et les demandes complémentaires de l'utilisateur. Il ne remplace pas le backlog des modules restant à développer.

## Parcours d'inscription

L'inscription médecin contient désormais les coordonnées, la spécialité, le numéro d'inscription à l'Ordre, la ville, l'attestation professionnelle et les informations de connexion. Elle ne demande plus de services, tarifs ou horaires. Le compte reste en attente ; l'approbation crée un profil professionnel sans services ni disponibilités inventés.

Après connexion, un médecin dont la configuration est incomplète voit une invitation à ajouter ses services/tarifs dans son profil puis ses plages dans l'éditeur hebdomadaire. Les créneaux ne sont proposés qu'en présence de services et d'horaires.

Les téléphones d'inscription sont obligatoires et comportent 8 à 15 chiffres. L'interface élimine les lettres lors de la saisie ou du collage ; le serveur refuse les valeurs invalides, y compris lorsqu'un client contourne le formulaire. Les e-mails exigent une adresse avec domaine complet. Les champs obligatoires, tailles, consentement et mots de passe possèdent des messages de validation. Le justificatif conserve ses contrôles de contenu et de taille côté serveur.

## Absences et reprogrammation

Règle confirmée par l'utilisateur : enregistrer l'absence et demander une reprogrammation au patient, avec avertissement au médecin pour éviter cette période si l'absence n'est pas nécessaire.

1. Le médecin saisit une absence entière ou partielle.
2. Une prévisualisation serveur compte les rendez-vous à venir en attente ou confirmés qui la recouvrent, sans modifier de donnée.
3. L'interface affiche le nombre concerné, les conséquences et le conseil de revoir la période si possible.
4. Le médecin peut revenir à son formulaire ou confirmer explicitement. Une requête directe sans confirmation est aussi refusée par le serveur si des rendez-vous sont concernés.
5. L'enregistrement de l'absence, le statut `TO_RESCHEDULE`, l'historique et les notifications internes sont transactionnels. Le motif et le commentaire privés de l'absence ne sont transmis ni dans les notifications ni dans l'historique partagé.
6. Le patient conserve l'accès au rendez-vous et choisit un nouveau créneau. Le serveur le revalide ; la demande repasse à `PENDING` pour confirmation du médecin.
7. Retirer l'absence ne restaure pas automatiquement les anciennes réservations.

Les mutations d'horaires et d'absence utilisent le verrou médecin employé par la réservation. Réservation et reprogrammation utilisent READ COMMITTED pour relire l'état après l'acquisition de ce verrou sur MySQL. La contrainte unique et le verrou optimiste des rendez-vous restent en place.

## Interface et navigation

- Corps de texte, labels, champs et actions agrandis ; palette et états communs ; styles de focus et réduction des animations.
- Navigation latérale et en-tête harmonisés ; icônes SVG communes ; menu mobile refermable et navigation hors écran rendue inerte.
- Agenda dédié avec vues jour, semaine et mois, navigation par période/date, recherche patient/service et filtre par statut. Les données proviennent de l'API ; toutes les pages de la période sont chargées avant filtrage.
- Présentation chronologique adaptée au téléphone ; vue mensuelle détaillée sur ordinateur ; horaires de Tunis explicités.
- Pagination des listes exposée. Leur recherche locale est explicitement limitée à la page chargée ; l'annuaire patient conserve ses filtres de recherche serveur.
- Templates d'inscription et de portail extraits dans des fichiers HTML, composants agenda et icônes séparés, routes chargées à la demande.
- Polices système pour supprimer la dépendance du build à Google Fonts.

## Corrections des parcours

- Profil administrateur : GET et PUT réels, vérification du rôle actif et du mot de passe actuel.
- Profil médecin : suppression des champs horaires qui étaient envoyés puis ignorés. L'éditeur hebdomadaire reste leur source de configuration.
- Paramètres : suppression du chargement des routes de préférences inexistantes ; le changement de mot de passe conserve son API.
- Rubrique patients médecin sans API : état d'indisponibilité explicite au lieu d'une requête systématiquement en erreur.
- Calendrier de disponibilité mensuelle accessible publiquement comme les créneaux.
- Erreur de connexion distinguée d'une session expirée.
- Réservation : mois initial aligné avec la date demandée ; erreurs conservées pendant le rafraîchissement des créneaux.
- Reprogrammation : le formulaire reste utilisable après une erreur ; anciennes et nouvelles dates consignées dans l'historique.
- Lanceur Windows local : répertoire de sockets JDK configuré pour éviter le blocage Tomcat constaté lors de l'audit.

## Validation

- Compilation Angular de production réussie : **352,75 ko** de chargement initial, sous le budget de 500 ko. Avant refonte : 531,90 ko. Aucun avertissement de budget sur la version livrée.
- **42 tests backend** réussis, dont les nouveaux tests de validation et de prévisualisation/confirmation d'absence.
- **10 scénarios HTTP/MySQL réels** dans `backend/verify-workflows.py`, sur une base temporaire indépendante et supprimée après recette : validations invalides, inscription minimale avec document, approbation, configuration après activation, permissions, double réservation concurrente, absence sans confirmation, notification et historique confidentiels, reprogrammation et persistance du profil administrateur.
- Contrôles navigateur : inscription simplifiée, erreurs de champs, affichage mobile à 390 px, connexion du compte médecin de recette, agenda et changement de mois/période, filtre de statut, prévisualisation d'une absence affectant un rendez-vous et retour à l'édition sans notification.

Les données de recette étaient clairement identifiées comme tests et stockées dans une autre base. Aucune donnée médicale réelle n'a été utilisée pour ces scénarios.

## Suite de la mission

Cette livraison constitue la première étape de la refonte, pas la livraison de l'ensemble du Sprint 0. Restent notamment les dossiers et notes cliniques réellement persistés, ordonnances, téléconsultation, cabinets, facturation/paiement, modération, journal administratif et gestion administrative des rendez-vous. Les anciens composants cliniques ne sont pas déclarés fonctionnels.

À poursuivre également : remplacement des confirmations natives restantes, tableaux d'annuaires dédiés et recherche serveur généralisée, politique de suppression/conservation des comptes, révocation complète des sessions désactivées, migrations versionnées et configuration de déploiement. Le backend utilise toujours le fuseau métier Africa/Tunis ; la configuration d'origine et l'URL d'API doivent être adaptées au déploiement cible.

## Intégration des mises à jour GitHub

La gestion administrative des spécialités ajoutée sur GitHub a été conservée et raccordée au nouveau portail. Le lanceur et la documentation conservent également la configuration MySQL individuelle via `.env.local` et `.env.example`. Les constats antérieurs concernant l’absence de ce module décrivent l’état précédant cette intégration.
