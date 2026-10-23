# Scénarios de test — SYS-02 · Sauvegarde automatique hors-ligne

Statut : ✅ exécuté le 2026-10-23 — reste la recette manuelle (🧪) · Ticket : [#16](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/16) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · 🧪 recette = à faire à la main. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | tout compte connecté ; `ADMIN` pour l'écriture de test (`POST /api/v1/users`) |
| Données de départ | un compte actif ; navigateur avec IndexedDB |
| Tickets à avoir déployés avant | AUTH-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Saisies hors ligne mises en file | Réseau coupé · l'utilisateur soumet | non envoyée, en file (`queued: true`) | `outbox-service.spec.ts` « should queue the request without sending it when offline » | ✅ auto |
| 1b | idem | La requête échoue en cours d'envoi (statut 0) | mise en file | « should queue the request when the network fails while sending » | ✅ auto |
| 1c | idem | Le serveur répond 503 | mise en file puis relancée | « should queue and retry a transient server error » | ✅ auto |
| 1d | Synchronisées au retour du réseau | Le réseau revient | rejeu dans l'ordre (FIFO) | « should replay queued requests in order once back online » | ✅ auto |
| 1e | Ordre préservé | Nouvelle saisie alors que d'autres attendent | mise derrière elles | « should queue a new request behind older pending ones to keep the order » | ✅ auto |
| 1f | Relances bornées | Erreur transitoire persistante | délais 1, 2, 4, 8 s puis `FAILED`, la suite continue | « should retry transient failures with exponential backoff then fail and continue FIFO » | ✅ auto |
| 2 | Architecture posée avant les modules | En ligne | la requête part avec une `Idempotency-Key` | « should send the request with an idempotency key when online » | ✅ auto |
| 2b | idem (serveur) | Même requête rejouée avec la même clé | même réponse, aucun doublon | `IdempotencyIT.createUser_replayedWithSameKey_returnsSameResponseWithoutDuplicate` | ✅ auto |
| 2c | idem (démarrage) | L'application démarre | le service de file est créé et rejoue | `app.config.ts` : `provideAppInitializer(() => void inject(OutboxService))` | 🔍 code |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Soumission sans session | utilisateur déconnecté | 401 `UNAUTHENTICATED`, rien en file | `outbox-service.spec.ts` « should reject a submission without an authenticated account » | ✅ auto |
| Saisies d'un compte, autre compte connecté | A hors ligne, B se connecte | non envoyées sous B | « should not send one account’s queued requests while another account is connected » | ✅ auto |
| Saisies conservées jusqu'au retour de leur compte | A se reconnecte | envoyées sous A | « should retain queued requests until their creating account reconnects » | ✅ auto |
| Erreur métier à l'envoi direct | 409 nom déjà pris | relancée à l'appelant, pas de file | « should not queue a business error and rethrow it » | ✅ auto |
| Refus serveur pendant le rejeu | 422 | `FAILED`, la suivante part | « should mark a refused request as failed without blocking the next one » | ✅ auto |
| Renvoyer ou abandonner une saisie refusée | — | renvoyée ou supprimée | « should resend a failed request on retry and drop it on discard » | ✅ auto |
| Vider la file | `clear()` | file vide, pas de réapparition | « should empty the queue on clear », « should not requeue a failed request after the account queue is cleared » | ✅ auto |
| Même contenu sans clé | 2 `POST` sans en-tête | 2 comptes créés (pas de déduplication) | `IdempotencyIT.createUser_sameBodyWithoutKey_isNotDeduplicated` | ✅ auto |
| Clé réutilisée pour une autre requête | même clé, autre URL | 422 | `IdempotencyIT.createUser_keyReusedForAnotherRequest_returns422` | ✅ auto |
| Clé qui n'est pas un UUID | `Idempotency-Key: abc` | 400 | `IdempotencyIT.createUser_invalidKey_returns400` | ✅ auto |
| Clé sans jeton | — | 401 | `IdempotencyIT.createUser_keyWithoutToken_returns401` | ✅ auto |
| 401 sur un appel d'API | jeton expiré | un refresh puis rejeu | `session-interceptor.spec.ts` « should refresh once and replay the request with the new token after a 401 » | ✅ auto |

## Cas limites

| Cas | Exemple | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Même clé chez deux utilisateurs | A et B envoient la clé `K` | indépendantes (clé primaire `(user_id, idempotency_key)`) | 2 insertions acceptées en base, la 2ᵉ du même utilisateur ignorée (`ON CONFLICT DO NOTHING`) | ✅ base |
| Requête identique pendant son traitement | 2ᵉ envoi avant la fin du 1ᵉʳ | 409 « Requête en cours de traitement. » | `IdempotencyFilter.replay` (ligne sans `response_status`) | 🔍 code |
| Réponse 5xx | erreur serveur | clé libérée, nouvel essai possible | `IdempotencyFilter.execute` : `store.release` si statut ≥ 500, 401 ou 403 | 🔍 code |
| Clé de plus de 24 h | rejeu le lendemain | traité comme une nouvelle requête | `IdempotencyStore.claim` purge `created_at < now - 24 h` | 🔍 code |
| En-tête `Location` d'une création rejouée | rejeu d'un `POST` 201 | statut et corps rejoués, `Location` absent | `IdempotencyFilter.writeStored` | 🔍 code |
| Rechargement de la page avec des saisies en file | F5 hors ligne | saisies conservées (IndexedDB `vvs-outbox`) | `OutboxStorage` | 🔍 code · 🧪 recette |
| Navigateur sans IndexedDB | navigation privée stricte | file en mémoire pour la session seulement | `OutboxStorage.useMemory` | 🔍 code |
| Erreur de contrainte au rejeu | deux saisies concurrentes qui créent le même nom | 500, traité comme transitoire : 5 essais puis `FAILED` | `isTransient` (≥ 500) | 🔍 code |
| Affichage de l'état | en ligne sans attente ; hors ligne ; 2 en attente + 1 refusée | « tout est synchronisé » ; avertissement ; compteurs | `sync-status.spec.ts` (3 tests), `sync-status.a11y.spec.ts`, `connectivity-service.spec.ts` | ✅ auto |
| Page « Synchronisation » | 1 saisie refusée | liste avec la raison, Renvoyer, Abandonner | `sync-page.spec.ts` (4 tests) | ✅ auto |

## Tests existants

- Backend : `security/idempotency/IdempotencyIT.java`
- Frontend : `core/offline/outbox-service.spec.ts`, `connectivity-service.spec.ts`, `sync-status.spec.ts`, `sync-status.a11y.spec.ts`, `features/sync/pages/sync-page/sync-page.spec.ts`, `core/http/session-interceptor.spec.ts`

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Outils du navigateur → hors ligne, 2 saisies, rechargement, retour en ligne | les 2 partent dans l'ordre, une ligne de journal chacune | 🧪 recette |
| Couper le réseau au milieu d'un envoi puis rétablir | pas de doublon en base | 🧪 recette |
| Provoquer un refus au rejeu (nom déjà pris) | entrée visible dans « Synchronisation », Renvoyer et Abandonner fonctionnent | 🧪 recette |

## Résultat de l'exécution

- **Date** : 2026-10-23.
- **Backend** : CI GitHub `mvn verify` vert sur un code identique à `develop`. **Frontend** : `ng test` local, 102 tests verts sur 102. **Base** : table `idempotency_keys` vérifiée (clé primaire par utilisateur).
- **Anomalies** :
  1. ⚠ Écart de documentation : l'ADR `0001` dit que la file est vidée à la déconnexion ; le code la conserve par compte. Mettre l'ADR à jour.
  2. ⚠ Les cas 409 « en cours », libération sur 5xx et purge 24 h n’ont pas de test : tests à écrire dans SYS-12 (ouvert).
  3. ⚠ Aucun formulaire métier n'utilise encore `submit` (objet de SYS-07).
- **Recette manuelle** : 3 étapes 🧪.
