# HelloDoc — flux et diagrammes Sprint 4

> Lecture issue du code présent. Les diagrammes montrent les principaux parcours, pas tous les composants de l’application. Les contrôles métier du serveur restent applicables même si un client affiche un bouton.

## 1. Vue de composants

```mermaid
flowchart LR
  Doctor[Navigateur médecin] --> Angular[Angular / Router / Guards]
  Patient[Navigateur patient] --> Angular
  Angular --> REST[Contrôleurs REST]
  Angular --> WS[WebSocket / TeleconsultationSignalHandler]
  REST --> Services[Services métier]
  WS --> Services
  Services --> JPA[Repositories JPA]
  JPA --> DB[(MySQL)]
  Doctor <-->|WebRTC média| Patient
```
Le navigateur prend en charge l’interface, les permissions média et la connexion pair à pair. Le serveur contrôle les droits, la session applicative, la persistance des notes et messages, et relaie les informations de négociation.

## 2. Cycle d’un rendez-vous vidéo

```mermaid
stateDiagram-v2
  [*] --> PENDING: réservation
  PENDING --> CONFIRMED: médecin confirme
  PENDING --> REJECTED: médecin refuse
  CONFIRMED --> COMPLETED: médecin termine
  CONFIRMED --> CANCELLED: annulation autorisée
  CONFIRMED --> NO_SHOW: absence après l'heure
  CONFIRMED --> TO_RESCHEDULE: demande de changement
  TO_RESCHEDULE --> CONFIRMED: nouveau créneau
```
Les transitions sont vérifiées dans AppointmentService. Le rendez-vous et la session vidéo sont des états distincts.

## 3. Ouverture médecin avant l’heure

```mermaid
sequenceDiagram
  actor M as Médecin
  participant D as AppointmentDetailComponent
  participant R as Angular Router/Guard
  participant API as Appointment API
  participant T as TeleconsultationComponent
  M->>D: Cliquer « Commencer »
  D->>R: Naviguer vers la route médecin
  R->>API: Charger rendez-vous / valider accès
  API-->>R: Rendez-vous vidéo confirmé + participant
  R->>T: Autoriser la page
  T->>API: Charger Meet, contexte, état de session
  T-->>M: Salle, cadres vidéo, chat désactivé et heure d’ouverture
  Note over T,API: Aucune création/démarrage WebRTC avant la fenêtre
```
Le code documenté sépare l’affichage de la salle et l’ouverture de session. Le guard valide identité, propriété, statut et mode.

## 4. Ouverture patient

```mermaid
sequenceDiagram
  actor P as Patient
  participant A as Rendez-vous patient
  participant G as Guards
  participant S as Salle commune
  P->>A: Ouvrir la téléconsultation confirmée
  A->>G: Route patient
  G->>G: Rôle, propriétaire, VIDEO, statut
  G-->>S: Accès autorisé
  S->>S: Afficher la salle et surveiller la session
  Note over S: Pas de création prématurée par le patient
```
La route utilise le même composant de salle que le médecin; les commandes varient selon le rôle.

## 5. Fenêtre de téléconsultation

```mermaid
flowchart LR
  Start[Début rendez-vous] --> Minus[Début moins 15 min]
  Minus -->|avant| Waiting[Page visible, média fermé]
  Minus -->|à partir de| Window[Fenêtre ouverte]
  Window --> Create[Créer/récupérer session selon service]
  Create --> StartSession[Médecin démarre session]
  StartSession --> Active[ACTIVE / signalisation et média]
  Active --> End[Terminer]
  End --> Done[COMPLETED]
  Window -->|jusqu'à début + 30 min| Window
```
TeleconsultationService applique startAt moins 15 minutes jusqu’à startAt plus 30 minutes.

## 6. Démarrage WebRTC

```mermaid
sequenceDiagram
  actor M as Médecin
  actor P as Patient
  participant API as REST session
  participant WS as SignalHandler
  M->>API: POST /start
  API-->>M: état ACTIVE
  M->>WS: JOIN puis OFFER (SDP)
  P->>WS: JOIN
  WS-->>P: OFFER en attente
  P->>WS: ANSWER
  WS-->>M: ANSWER
  M->>WS: ICE candidates
  WS-->>P: ICE candidates
  P->>WS: ICE candidates
  WS-->>M: ICE candidates
  Note over M,P: Le média passe ensuite par WebRTC/ICE
```
Le serveur relaie les signaux, mais ne mélange pas les flux vidéo.

## 7. Chat temps réel et persistance

```mermaid
sequenceDiagram
  actor U as Participant
  participant UI as TeleconsultationComponent
  participant WS as SignalHandler
  participant C as TeleconsultationChatService
  participant DB as MessageRepository
  U->>UI: Envoyer texte
  UI->>WS: CHAT sur WebSocket
  WS->>C: Valider état, taille, participant
  C->>DB: Persister
  DB-->>C: Message DTO
  C-->>WS: Message
  WS-->>UI: Diffuser message
  UI->>C: GET périodique de l’historique
  C-->>UI: Historique récent (max 200)
```
L’API REST POST existe également; l’interface actuelle privilégie le WS pour l’envoi. La synchronisation REST périodique s’exécute toutes les trois secondes environ.

## 8. Authentification et CSRF

```mermaid
sequenceDiagram
  actor U as Utilisateur
  participant UI as Angular
  participant Auth as AuthController
  participant Sec as SecurityConfig
  participant F as SessionIntegrityFilter
  U->>UI: Connexion
  UI->>Auth: GET /auth/csrf puis POST /auth/login
  Auth->>Sec: Authentifier et créer session
  Sec-->>UI: Cookie session + utilisateur
  UI->>Auth: GET /auth/me
  F->>F: Vérifier statut/role actif
  UI->>Auth: Écriture avec token CSRF
```
Les détails de cookie et CORS dépendent de la configuration d’exécution.

## 9. Guards Angular

```mermaid
flowchart TD
  N[Navigation vers salle] --> R{roleGuard}
  R -->|échec| Deny[Refus / redirection]
  R -->|rôle attendu| C{consultationAccessGuard}
  C -->|non connecté / mauvais rôle| Deny
  C -->|rendez-vous non possédé| Deny
  C -->|annulé ou non vidéo| Detail[Détails / refus selon guard]
  C -->|vidéo admissible| Room[TeleconsultationComponent]
  Room --> U{sortie avec modifications?}
  U -->|oui| Warn[unsavedConsultationWorkspaceGuard]
  U -->|non| Next[Navigation suivante]
```
Le serveur répète les vérifications de propriété et de statut.

## 10. Note médicale

```mermaid
sequenceDiagram
  actor M as Médecin
  participant UI as ConsultationNoteComponent
  participant API as ConsultationController
  participant S as ConsultationService
  participant DB as Note et révisions
  M->>UI: Modifier et enregistrer
  UI->>API: PUT avec version
  API->>S: Valider propriétaire et statut
  S->>S: Comparer version et règles d’édition
  S->>DB: Enregistrer note + révision
  DB-->>UI: Nouvelle version
  M->>UI: Finaliser après rendez-vous terminé
  UI->>API: POST finalize
  S->>S: Exiger COMPLETED et résumé non vide
```
Une version périmée produit un conflit plutôt qu’un écrasement silencieux.

## 11. Ordonnance

```mermaid
stateDiagram-v2
  [*] --> DRAFT: création par médecin
  DRAFT --> FINALIZED: finalisation explicite
  FINALIZED --> Replaced: amendement crée une ordonnance liée
  Replaced --> DRAFT: nouvelle version
```
L’amendement crée un nouvel objet référençant l’original. Le patient voit les prescriptions finalisées liées à des consultations terminées.

## 12. Dossier médical

```mermaid
flowchart LR
  A[Consultations du patient] --> Filter[Appointments COMPLETED]
  Filter --> N[Notes FINALIZED seulement]
  Filter --> P[Prescriptions FINALIZED seulement]
  N -->|exclure privateNote| View[PatientConsultationDto]
  P --> View
  D[Accès médecin] --> Own{A déjà suivi ce patient?}
  Own -->|oui| Dossier[Historique dans le périmètre médecin]
  Own -->|non| Refus[404 / refus]
```
Le dossier décrit ici ne contient pas d’import de pièces médicales.

## 13. Google Meet

```mermaid
sequenceDiagram
  actor M as Médecin
  participant UI as Espace de travail
  participant API as AppointmentMeetLinkController
  participant S as AppointmentMeetLinkService
  M->>UI: Saisir URL
  UI->>API: PUT meet-link
  API->>S: Vérifier propriétaire, vidéo, statut, URL
  S->>S: Autoriser HTTPS meet.google.com
  S-->>UI: URL persistée
  M->>API: POST external-call-ended
  Note over API,S: Marqueur applicatif; ne pilote pas Google Meet
```
HelloDoc ne crée pas de conférence Google Meet ni ne mesure sa connectivité.

## 14. Suivi clinique

```mermaid
flowchart LR
  M[Le médecin saisit date / délai] --> Save[Enregistrer note]
  Save --> Compare{Valeur changée et renseignée?}
  Compare -->|oui| Notify[Notification de suivi]
  Notify --> Patient[Patient consulte son historique]
  Patient --> Book[Le patient ouvre le parcours de réservation]
  Compare -->|non| NoNotify[Aucune notification]
```
La recommandation ne réserve pas de rendez-vous automatiquement.

## 15. Contrôles d’accès backend

```mermaid
flowchart TD
  Req[Requête] --> Session{Session active?}
  Session -->|non| E401[401]
  Session -->|oui| Status{Compte actif?}
  Status -->|non| Revoke[Invalider / refuser]
  Status -->|oui| Role{Rôle permis?}
  Role -->|non| E403[403]
  Role -->|oui| Owner{Participant autorisé?}
  Owner -->|non| E404[404]
  Owner -->|oui| Business{État et fenêtre valides?}
  Business -->|non| E409[400 ou 409 selon règle]
  Business -->|oui| Action[Action métier]
```
Les codes exacts sont propres à chaque service et contrôleur.

## 16. Routes frontend

```mermaid
flowchart LR
  Patient[Patient] --> RP[/patient/rendez-vous/:id/teleconsultation]
  Doctor[Médecin] --> RD[/medecin/rendez-vous/:id/teleconsultation]
  RP --> Shared[TeleconsultationComponent]
  RD --> Shared
  Shared --> Note[ConsultationNoteComponent]
```
Il y a deux URL selon le rôle mais une page de salle partagée. Le clic médecin conserve le bouton « Commencer la téléconsultation » sur les détails.

## 17. Persistances concernées

```mermaid
erDiagram
  APPOINTMENT ||--o| TELECONSULTATION_SESSION : has
  TELECONSULTATION_SESSION ||--o{ TELECONSULTATION_MESSAGE : stores
  APPOINTMENT ||--o| CONSULTATION_NOTE : documents
  CONSULTATION_NOTE ||--o{ CONSULTATION_REVISION : revises
  APPOINTMENT ||--o{ PRESCRIPTION : issues
  PRESCRIPTION ||--o{ PRESCRIPTION_ITEM : contains
  APPOINTMENT }o--|| PATIENT : for
  APPOINTMENT }o--|| DOCTOR : with
```
Ce diagramme est logique : il ne remplace pas les annotations JPA ni le schéma MySQL déployé.
