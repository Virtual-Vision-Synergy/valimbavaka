# Scénarios de test — SYS-02 · Sauvegarde automatique hors-ligne

Statut : ✔ rempli depuis les tests existants au 2026-10-23 — à valider par le traiteur · Ticket : [#16](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/16) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | tout compte connecté ; `ADMIN` pour les écritures de test (`POST /api/v1/users`) |
| Données de départ | un compte actif ; navigateur avec IndexedDB |
| Tickets à avoir déployés avant | AUTH-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Saisies hors ligne mises en file | Étant donné le réseau coupé · Quand l'utilisateur soumet une saisie | non envoyée, stockée en file (`queued: true`) | `outbox-service.spec.ts` « should queue the request without sending it when offline » | ☐ |
| 1b | idem | Quand la requête échoue en cours d'envoi (réseau) | mise en file | « should queue the request when the network fails while sending » | ☐ |
| 1c | idem | Quand le serveur répond par une erreur transitoire | mise en file puis relancée | « should queue and retry a transient server error » | ☐ |
| 1d | Synchronisées au retour du réseau | Quand le réseau revient | saisies rejouées dans l'ordre (FIFO) | « should replay queued requests in order once back online » | ☐ |
| 1e | Ordre préservé | Quand une saisie arrive derrière des saisies en attente | mise en file derrière elles | « should queue a new request behind older pending ones to keep the order » | ☐ |
| 1f | Relances bornées | Quand l'erreur transitoire persiste | délais exponentiels puis `FAILED`, la suite continue | « should retry transient failures with exponential backoff then fail and continue FIFO » | ☐ |
| 2 | Architecture posée avant les modules | Quand en ligne | la requête part avec une `Idempotency-Key` | « should send the request with an idempotency key when online » | ☐ |
| 2b | idem (serveur) | Quand la même requête est rejouée avec la même clé | même réponse, aucun doublon | `IdempotencyIT.createUser_replayedWithSameKey_returnsSameResponseWithoutDuplicate` | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Soumission sans session | 401 `UNAUTHENTICATED`, rien en file | « should reject a submission without an authenticated account » | ☐ |
| Saisies d'un compte, autre compte connecté | non envoyées sous l'autre compte | « should not send one account’s queued requests while another account is connected » | ☐ |
| Saisies conservées jusqu'au retour de leur compte | conservées | « should retain queued requests until their creating account reconnects » | ☐ |
| Erreur métier à l'envoi direct | relancée, pas de file | « should not queue a business error and rethrow it » | ☐ |
| Refus serveur au rejeu | `FAILED`, la suivante part | « should mark a refused request as failed without blocking the next one » | ☐ |
| Renvoyer / abandonner une saisie refusée | renvoyée ou supprimée | « should resend a failed request on retry and drop it on discard » | ☐ |
| Vider la file | file vide | « should empty the queue on clear » | ☐ |
| Pas de remise en file après vidage | pas de réapparition | « should not requeue a failed request after the account queue is cleared » | ☐ |
| Même clé, requête sans l'en-tête | non dédupliquée | `IdempotencyIT.createUser_sameBodyWithoutKey_isNotDeduplicated` | ☐ |
| Clé réutilisée pour une autre requête | 422 | `IdempotencyIT.createUser_keyReusedForAnotherRequest_returns422` | ☐ |
| Clé qui n'est pas un UUID | 400 | `IdempotencyIT.createUser_invalidKey_returns400` | ☐ |
| Clé sans jeton d'accès | 401 | `IdempotencyIT.createUser_keyWithoutToken_returns401` | ☐ |
| 401 sur un appel d'API | un refresh puis rejeu | `session-interceptor.spec.ts` « should refresh once and replay the request with the new token after a 401 » | ☐ |

## Cas limites et hors ligne

- Interface : « tout est synchronisé » quand rien n'attend (`sync-status.spec.ts`), avertissement hors ligne, compteurs de saisies en attente et refusées ; absence de violation d'accessibilité (`sync-status.a11y.spec.ts`) ; page de synchronisation : liste, renvoi, abandon (`sync-page.spec.ts`) ; suivi de la connectivité (`connectivity-service.spec.ts`).
- Requête identique pendant son traitement : 409 (pas de test dédié repéré).
- Réponse 5xx : clé libérée puis nouvel essai (pas de test dédié repéré).
- Purge des clés de plus de 24 h (pas de test dédié repéré).
- Rechargement de la page avec des saisies en file : conservées dans IndexedDB (pas de test automatique, voir recette).

## Tests existants (repérés dans le code)

- Backend : `security/idempotency/IdempotencyIT.java`
- Frontend : `core/offline/outbox-service.spec.ts`, `connectivity-service.spec.ts`, `sync-status.spec.ts`, `sync-status.a11y.spec.ts`, `features/sync/pages/sync-page/sync-page.spec.ts`, `core/http/session-interceptor.spec.ts`

## À faire à la main (recette QA)

- Dans les outils du navigateur, passer hors ligne, créer deux saisies, recharger la page, puis repasser en ligne : les deux partent dans l'ordre et une seule ligne de journal est créée par saisie.
- Couper le réseau au milieu d'un envoi, rétablir : pas de doublon en base.
- Provoquer un refus (nom de fournisseur déjà pris) pendant le rejeu : l'entrée apparaît dans « Synchronisation » avec sa raison ; « Renvoyer » et « Abandonner » fonctionnent.
- Changer de compte sans connexion intermédiaire : les saisies du premier compte ne partent pas.

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
