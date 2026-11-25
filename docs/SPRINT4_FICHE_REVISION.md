# HelloDoc — fiche de révision Sprint 4

> Résumé court fondé sur le code présent. Les heures/exemples sont fictifs et doivent être évalués avec l’horloge serveur de recette.

## Parcours en un coup d’œil

- Routes patient et médecin : /patient/rendez-vous/:appointmentId/teleconsultation et /medecin/rendez-vous/:appointmentId/teleconsultation.
- Les deux routes chargent TeleconsultationComponent.
- L’ouverture de la salle médecin est séparée de la création/démarrage d’une session.
- Fenêtre serveur : début du rendez-vous moins 15 min à début plus 30 min.
- Session : WAITING → ACTIVE → COMPLETED.
- WebRTC : RTCPeerConnection, permissions média, signalisation SDP/ICE sur WebSocket.
- Chat : WS CHAT, persistance via service, historique REST, maximum 2 000 caractères.
- Note : version optimiste, historique de révisions, finalisation après rendez-vous COMPLETED.
- Ordonnance : DRAFT/FINALIZED; amendement crée un nouvel enregistrement lié.
- Dossier patient : rendez-vous terminés, documents finalisés; note privée exclue.
- Meet : URL externe validée; HelloDoc ne crée ni ne contrôle l’appel Meet.
- Suivi : notification de recommandation; aucune réservation automatique.
- Sécurité : session/CSRF, rôle, propriétaire, statut et fenêtre contrôlés backend.
- Temps : rendez-vous Instant; calendrier métier Africa/Tunis.
- Dépendances principales : Angular 20; Spring Boot 3.5.7 / Java 21; JPA/MySQL.
- Tests : scripts Node frontend et tests JUnit/Spring; les tests navigateur restent importants.

## 20 notions à retenir

1. **Route de salle** — écran Angular de travail.
2. **Session téléconsultation** — état serveur distinct du rendez-vous.
3. **Rendez-vous VIDEO** — mode requis pour le flux vidéo.
4. **CONFIRMED** — état nécessaire au flux médecin.
5. **WAITING** — session non démarrée.
6. **ACTIVE** — session démarrée; chat/signaux autorisés.
7. **COMPLETED** — session terminée; nouveaux messages bloqués.
8. **Fenêtre d’accès** — début moins quinze, début plus trente minutes.
9. **roleGuard** — rôle attendu sur la route.
10. **consultationAccessGuard** — éligibilité et propriété du rendez-vous.
11. **SessionIntegrityFilter** — révoque accès si compte ou rôle invalide.
12. **CSRF** — protection des écritures sous cookie session.
13. **RTCPeerConnection** — connexion média WebRTC navigateur.
14. **SDP** — description offre/réponse de session.
15. **ICE** — candidats de connectivité réseau.
16. **STUN/TURN** — découverte de chemin / relais éventuel.
17. **SignalHandler** — relais WebSocket de signaux et CHAT.
18. **Version optimiste** — refuse écriture depuis version obsolète.
19. **Note privée** — visible médecin, exclue de la vue patient.
20. **supersedes** — relation entre nouvelle ordonnance amendée et ancienne.

## 15 questions rapides

1. **Le clic ouvre-t-il WebRTC?** Non, il ouvre la route; démarrage soumis à la fenêtre.
2. **Qui crée une session?** Le médecin via endpoint protégé, lorsque les règles backend le permettent.
3. **Le patient entre-t-il avant l’heure?** Le parcours patient permet la préparation anticipée.
4. **Quelle borne d’ouverture?** 15 minutes avant le début.
5. **Quelle borne de fin?** 30 minutes après le début selon le service.
6. **Le front seul protège-t-il?** Non; le backend répète les vérifications.
7. **Où passe la vidéo?** WebRTC entre navigateurs; le backend relaie la signalisation.
8. **Où est persisté le chat?** Table de messages via TeleconsultationChatService.
9. **Quel transport envoie le chat UI?** CHAT par WebSocket; historique REST de resynchronisation.
10. **Un brouillon est-il visible patient?** Non, seuls les éléments finalisés sont exposés.
11. **La note privée sort-elle?** Non.
12. **Comment corriger une ordonnance?** Créer un amendement lié; l’original reste conservé.
13. **Meet est-il créé par HelloDoc?** Non, l’application valide et conserve l’URL externe.
14. **Le suivi réserve-t-il?** Non, le patient utilise le parcours existant de réservation.
15. **Que vérifier manuellement?** Navigation anticipée, caméra/micro, ICE/TURN et rendu navigateur.

## Repères de fichiers

- Routes / guards : frontend/src/app/app.routes.ts, frontend/src/app/core/guards/.
- Salle et détails : frontend/src/app/features/appointments/teleconsultation.component.ts, appointment-detail.component.ts.
- Backend vidéo/chat : backend/src/main/java/com/hellodoc/service/TeleconsultationService.java, TeleconsultationChatService.java, websocket/TeleconsultationSignalHandler.java.
- Clinique : ConsultationService.java, PrescriptionService.java, PatientClinicalService.java.
- Configuration/tests : frontend/package.json, backend/pom.xml, backend/src/test/java/com/hellodoc/.

