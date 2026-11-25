# HelloDoc — préparation de soutenance Sprint 4

> Support dérivé du dépôt. Utiliser des comptes et données fictifs. Distinguer explicitement le code présent, la configuration d’environnement et les pistes futures.

## 1. Fil conducteur

Présenter le besoin : patient et médecin préparent une téléconsultation, échangent au moment prévu et conservent les contrôles de rendez-vous, le dossier clinique et la confidentialité. La solution présente est une page Angular partagée par deux routes, des guards frontend, des règles de propriété/statut dans les services backend, une session WebRTC séparée du rendez-vous, une signalisation WS et une persistance clinique JPA.

La page de travail et la communication vidéo sont deux étapes. Le médecin peut ouvrir la salle avant l’heure autorisée; l’ouverture ne doit pas démarrer WebRTC. Le service backend fait respecter la fenêtre de session. Au démarrage, le navigateur demande les permissions et échange SDP/ICE par WebSocket. Le chat est persistant. Les notes sont versionnées, les ordonnances amendables; la vue patient ne montre que les éléments finalisés et exclut la note privée.

Ne pas dire que HelloDoc génère une conférence Google Meet, stocke les médias, ou réserve automatiquement un suivi. TURN dépend de la configuration déployée.

## 2. Script de 2 minutes

« HelloDoc gère une téléconsultation à partir d’un rendez-vous confirmé. Le front Angular possède une route patient et une route médecin qui chargent le même composant de salle. Les guards contrôlent le rôle et l’éligibilité; le backend répète le contrôle de propriété et du statut.

Le point important est de distinguer la page de travail de la communication vidéo. Le médecin peut ouvrir la salle avant l’heure autorisée et préparer la fiche; cette ouverture ne doit pas commencer WebRTC. La fenêtre commence quinze minutes avant le rendez-vous et le service backend la fait respecter. Au démarrage, le navigateur obtient les permissions, crée une connexion RTCPeerConnection et échange offre, réponse et candidats ICE sur WebSocket.

Le chat est persisté et son historique est resynchronisé. Le dossier contient des notes versionnées et des ordonnances amendables. La vue patient ne montre que les consultations terminées et éléments finalisés, sans note privée. La démonstration vérifie l’accès anticipé, le contrôle horaire et la confidentialité. »

## 3. Script de 5 minutes

1. **Besoin (40 s).** Préparer l’espace de téléconsultation sans exiger un flux média avant l’heure.
2. **Architecture (60 s).** Angular 20, Spring Boot 3.5/Java 21, JPA, MySQL par environnement; session HTTP/CSRF, WS natif et WebRTC navigateur.
3. **Sécurité (50 s).** Montrer roleGuard, consultationAccessGuard et contrôles de propriétaire côté service. Cacher un bouton ne sécurise pas une API.
4. **Salle (60 s).** Ouvrir le rendez-vous avant la fenêtre. Montrer cadres, chat désactivé, heure, Meet et fiche; réseau sans POST de création/start.
5. **Démarrage (50 s).** Avancer l’horloge de recette à 15:15 pour rendez-vous 15:30; démarrer dans la même page et montrer l’état.
6. **Clinique (50 s).** Sauvegarder un brouillon, montrer la version, finaliser après complétion, ouvrir une ordonnance amendée et la vue patient.
7. **Qualité/limites (30 s).** Montrer tests Node et JUnit; préciser que caméra/ICE réels requièrent recette navigateur. Meet reste externe; TURN dépend de l’environnement.

## 4. Script de 10 minutes

- **0:00–1:00 — contexte :** besoin, utilisateurs, périmètre.
- **1:00–2:00 — architecture :** composant partagé, routes par rôle, API/services/repositories et MySQL.
- **2:00–3:00 — sécurité :** session, CSRF, rôle, propriétaire, filtre session, codes 401/403/404/409.
- **3:00–5:00 — scénario temporel :** rendez-vous à 15:30; accès médecin à 14:00; salle visible sans session; ouverture à 15:15; patient rejoint.
- **5:00–6:00 — WebRTC :** getUserMedia, ICE config, RTCPeerConnection, SDP/ICE WS, pair à pair; limites TURN.
- **6:00–7:00 — chat :** CHAT WS, validation, persistance et historique REST; limite et session terminée.
- **7:00–8:00 — clinique :** note versionnée, historique, finalisation après rendez-vous; amendement ordonnance.
- **8:00–9:00 — dossier patient :** éléments terminés/finalisés, privateNote exclue; suivi sans réservation.
- **9:00–10:00 — qualité :** automatisation, recette navigateur et questions.

## 5. Démonstrations reproductibles

### A. Préparation avant l’heure
1. Utiliser un compte médecin actif et rendez-vous VIDEO confirmé à 15:30.
2. Régler l’environnement de recette sur 14:00.
3. Cliquer « Commencer la téléconsultation ».
4. Montrer la route, les deux cadres, chat désactivé, message horaire, Meet et espaces cliniques.
5. Dans le réseau, confirmer l’absence d’appel create/start avant l’heure.

### B. Ouverture de fenêtre
1. Dans la même page, avancer l’horloge de recette à 15:15.
2. Démarrer avec le contrôle existant et vérifier le contrôle backend.
3. Avec deux navigateurs de recette, autoriser caméra/micro et vérifier signalisation et chat.

### C. Confidentialité
1. Utiliser des données fictives.
2. Montrer qu’un brouillon n’apparaît pas dans le dossier patient.
3. Finaliser après statut COMPLETED.
4. Vérifier les éléments finalisés visibles et la note privée absente.

## 6. Dix explications de code à préparer

1. app.routes.ts : chemins médecin/patient chargent le même TeleconsultationComponent avec guards.
2. consultationAccessGuard : vérifie rôle, propriété, mode et statut, indépendamment de l’existence d’une session.
3. AppointmentDetailComponent.openTeleconsultation : navigue vers la salle; ne doit pas confondre navigation et POST create.
4. TeleconsultationComponent.loadSession : lit la session, gère l’absence anticipée et suit l’heure d’ouverture.
5. TeleconsultationService.ensureWindow : règle serveur de début moins quinze minutes à début plus trente.
6. TeleconsultationService.start : médecin propriétaire, rendez-vous vidéo confirmé et fenêtre valide.
7. TeleconsultationSignalHandler : valide authentification, type et taille puis relaie SDP/ICE et transmet CHAT au service.
8. TeleconsultationChatService : valide le participant, la limite de 2 000 caractères, la persistance et l’état complété.
9. ConsultationService : versions/révisions; finalisation exige rendez-vous terminé et résumé non vide.
10. PatientClinicalService : consulte seulement éléments terminés/finalisés et exclut privateNote.

## 7. Questions/réponses (50)

1. **Pourquoi Angular?** C’est le framework du frontend et gère composants, formulaires et routes.
2. **Pourquoi Spring?** Le backend repose sur Spring Boot, services, sécurité et JPA.
3. **Où sont les règles métier?** Principalement dans les services backend.
4. **Route égale session WebRTC?** Non, la route ouvre l’espace de travail; la session a son propre état.
5. **Pourquoi deux routes?** Elles distinguent le rôle et le contexte de navigation.
6. **Un seul composant de salle?** Oui, patient et médecin chargent TeleconsultationComponent.
7. **Qui ouvre la route médecin?** Le médecin participant admissible, après garde et contrôle serveur.
8. **Un autre patient peut-il ouvrir la salle?** Non, le serveur vérifie la propriété.
9. **Le bouton masqué suffit-il?** Non, l’API aussi vérifie le rôle et la propriété.
10. **Pourquoi ouverture anticipée?** Préparer le travail sans démarrer le média.
11. **Quand ouvre la fenêtre?** Quinze minutes avant le début.
12. **Quand s’arrête?** Jusqu’à trente minutes après le début selon le service.
13. **Quelle horloge fait foi?** Le contrôle backend, pas l’horloge du navigateur.
14. **Qui crée une session?** Le parcours médecin appelle le endpoint prévu; le service vérifie les conditions.
15. **Le patient crée-t-il la session?** Non, l’endpoint de création est réservé DOCTOR.
16. **Que signifie WAITING?** Session créée mais pas démarrée.
17. **Qui peut démarrer?** Le médecin propriétaire, dans la fenêtre et sur rendez-vous confirmé.
18. **Comment négocier le média?** SDP offre/réponse et candidats ICE.
19. **Où circule le média?** Entre navigateurs selon le réseau et ICE.
20. **Le backend stocke-t-il les vidéos?** Aucun stockage média n’est décrit.
21. **À quoi sert STUN?** Découvrir des adresses candidates de connectivité.
22. **À quoi sert TURN?** Relayer quand le pair-à-pair direct échoue.
23. **TURN est-il garanti?** Non, URLs/secret sont optionnels.
24. **Pourquoi WebSocket?** Signaux WebRTC et chat temps réel.
25. **Est-ce STOMP?** Le dépôt déclare un WebSocket natif, pas STOMP.
26. **Le chat persiste-t-il?** Oui, via TeleconsultationChatService.
27. **Pourquoi REST historique?** Charger et resynchroniser les messages enregistrés.
28. **Pourquoi polling?** L’interface relit périodiquement l’historique.
29. **Limite de longueur?** 2 000 caractères par message.
30. **Chat après la fin?** Refusé quand la session est COMPLETED.
31. **Meet intégré à WebRTC?** Non, c’est un lien externe.
32. **HelloDoc crée Meet?** Non, le service valide et sauvegarde l’URL.
33. **Fin d’appel Meet veut dire quoi?** Marqueur applicatif, pas contrôle de la conférence.
34. **Qui voit le dossier médecin?** Médecin autorisé dans son périmètre de suivi.
35. **Le patient voit-il un brouillon?** Non, seuls les éléments finalisés sont servis.
36. **Note privée visible?** Non, elle est exclue du DTO patient.
37. **Prévention écrasement?** Version optimiste et conflit si version périmée.
38. **Amendement ordonnance?** Nouvelle prescription liée à l’ancienne.
39. **Conditions finalisation note?** Rendez-vous terminé et résumé présent.
40. **Le suivi réserve-t-il?** Non, recommandation distincte de réservation.
41. **Comment dater?** Instant pour rendez-vous; Africa/Tunis pour règles calendaires concernées.
42. **Pourquoi 404?** Ne pas révéler l’existence d’une ressource tierce.
43. **Pourquoi CSRF?** Les cookies de session authentifient les écritures; CSRF protège ces requêtes.
44. **Comment détecter session révoquée?** SessionIntegrityFilter vérifie statut et rôle.
45. **Migrations versionnées?** Aucun Flyway/Liquibase repéré; JPA ddl-auto update.
46. **Tests frontend?** Sept scripts Node nommés dans package.json.
47. **Tests backend?** JUnit/Spring/Mockito, y compris services, sécurité et repositories.
48. **Vraie caméra testée?** Les scripts ne prouvent pas un test matériel; recette manuelle.
49. **Limites à annoncer?** TURN conditionnel, Meet externe, pas de média serveur ni upload médical.
50. **Suite possible?** Ajouter E2E navigateur, horloge contrôlée et recette TURN.

## 8. Question inconnue

Répondre depuis le fichier vérifié. Distinguer « implémenté », « dépend de l’environnement » et « piste à valider ». Ne pas extrapoler depuis un nom de fonction.

