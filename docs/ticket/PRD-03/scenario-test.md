# Scénarios de test — PRD-03 · Statut d'un lot de production

Statut : ✅ rédigé le 2026-11-08 — **tests exécutés en CI, verts** · Ticket : [#40](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/40) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Vendeuse (`SELLER`, démarre et termine), administrateur (`ADMIN`, idem), livreur (`DELIVERY_DRIVER`, 403), anonyme (401) |
| Données de départ | Produit fini `ART-0031 · Savon noir 100 g` ; matière première `ART-0008 · Base savonneuse` (5 kg en entrepôt `WAREHOUSE`) ; lots `LOT-00001` planifié, `LOT-00002` en cours, `LOT-00003` terminé ; vendeuse active « Soa Rakoto » |
| Migration | `2026_11_08-42-changelog.sql` : permission `production.update` (administrateur, vendeuse) ; `production.complete` donnée aussi à la vendeuse |
| Tickets à avoir déployés avant | PRD-01 (#38), PRD-02 (#39, PR empilée : `completeBatch`, `production.complete`) |

## Scénarios issus des critères d'acceptation

Cycle de vie (en avant seulement) : `PLANNED` (Planifié) → `IN_PROGRESS` (En cours) → `COMPLETED` (Terminé) ; un lot
planifié peut aussi être terminé directement (PRD-02).

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Le statut est visible | lots `LOT-00001` planifié, `LOT-00002` en cours, `LOT-00003` terminé dans « Lots récents » | badges « Planifié », « En cours », « Terminé » | `production-page-status.spec.ts` « affiche le statut de chaque lot : Planifié, En cours, Terminé » | ✅ auto |
| 1b | Le statut est consultable par filtre | `GET /batches?status=IN_PROGRESS&search=LOT-…` | seul le lot démarré est listé, le lot planifié n'apparaît pas | `ProductionStatusIT.getBatches_statusInProgress_listsTheStartedBatchOnly` | ✅ auto |
| 1c | Modifiable : démarrer un lot planifié | vendeuse, `POST /batches/{id}/start` sur un lot de 10 savons | 200, `status` `IN_PROGRESS`, `completedAt` null ; aucun mouvement de stock ; 1 ligne de journal `UPDATE` « Démarrage du lot LOT-xxxxx : planifié → en cours » | `ProductionStatusIT.startBatch_plannedBatchBySeller_returns200InProgressAndMovesNothing`, `ProductionBatchServiceImplTest.startBatch_plannedBatch_savesItInProgressAndTracesTheStart`, `ProductionBatchTest.start_plannedBatch_becomesInProgressWithoutCompletionDate` | ✅ auto |
| 1d | Modifiable jusqu'à Terminé : terminer un lot en cours | vendeuse, lot démarré, 5 kg de base en stock pour 2 consommés ; `POST /batches/{id}/completion` | 200 `COMPLETED`, `completedAt` renseigné ; 2 mouvements `PRODUCTION` ; journal : démarrage + validation | `ProductionStatusIT.completeBatch_inProgressBatchBySeller_returns200CompletedAndMovesTheStock`, `ProductionBatchTest.complete_inProgressBatch_becomesCompleted` | ✅ auto |
| 1e | Écran : boutons selon le statut | vendeuse | planifié : « Démarrer » et « Terminer » ; en cours : « Terminer » ; terminé : aucun bouton | `production-page-status.spec.ts` « propose Démarrer et Terminer sur un lot planifié, Terminer seul sur un lot en cours, rien sur un lot terminé » | ✅ auto |
| 1f | Écran : « Démarrer » | clic sur « Démarrer » de `LOT-00001` | appel `startLot('lot-00001')`, badge « En cours », bouton « Démarrer » retiré, toast « Lot LOT-00001 en cours… » | `production-page-status.spec.ts` « Démarrer passe le lot en cours sans recharger la liste », `production-service.spec.ts` « démarre un lot planifié par POST sur /batches/{id}/start avec une clé d’idempotence » | ✅ auto |
| 1g | Écran : « Terminer » | clic sur « Terminer » de `LOT-00002` | appel `completeLot('lot-00002')`, badge « Terminé », plus aucun bouton | `production-page-status.spec.ts` « Terminer un lot en cours le passe à Terminé et retire ses boutons » | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST /batches/{id}/start` sans `Authorization` | 401 `UNAUTHENTICATED` | `ProductionSecurityIT.startBatch_withoutToken_returns401` | ✅ auto |
| Mauvais rôle (démarrer) | jeton livreur | 403 `FORBIDDEN` | `ProductionSecurityIT.startBatch_withDeliveryDriverRole_returns403` | ✅ auto |
| Mauvais rôle (terminer) | jeton livreur sur `/completion` et `/completed-batches` | 403 `FORBIDDEN` (la vendeuse, elle, est désormais autorisée) | `ProductionSecurityIT.completeBatch_withDeliveryDriverRole_returns403`, `ProductionSecurityIT.createCompletedBatch_withDeliveryDriverRole_returns403` | ✅ auto |
| Mass assignment | corps `{"status":"COMPLETED","completedAt":"2020-01-01T00:00:00Z"}` sur `/start` | ignoré : 200 `IN_PROGRESS`, `completedAt` null, aucun mouvement | `ProductionStatusIT.startBatch_bodyWithStatusAndCompletionDate_ignoresThem` | ✅ auto |
| Données invalides | — | sans objet : `/start` n'a pas de corps (identifiant non UUID → 400 `MALFORMED_REQUEST`, gestion commune) | — | ⏭ hors périmètre (gestion d'erreurs commune, SYS) |
| Lot inconnu | UUID aléatoire | 404 `NOT_FOUND` | `ProductionStatusIT.startBatch_unknownBatch_returns404`, `ProductionBatchServiceImplTest.startBatch_unknownBatch_throwsNotFoundAndTracesNothing` | ✅ auto |
| État incompatible : déjà en cours | deuxième `/start` | 409 `CONFLICT` « Le lot est déjà en cours. » ; une seule ligne de journal | `ProductionStatusIT.startBatch_alreadyInProgress_returns409AndTracesTheStartOnce`, `ProductionBatchServiceImplTest.startBatch_alreadyStartedBatch_throwsConflictAndSavesNothing`, `ProductionBatchTest.start_inProgressBatch_throwsConflict` | ✅ auto |
| État final : lot terminé | `/start` sur un lot `COMPLETED` | 409 `CONFLICT` « Le lot est déjà terminé… » ; statut inchangé | `ProductionStatusIT.startBatch_completedBatch_returns409AndStaysCompleted`, `ProductionBatchTest.start_completedBatch_throwsConflictAndStaysCompleted` | ✅ auto |
| Journal | démarrage | ligne `UPDATE`, ressource `PRODUCTION_BATCH` | `ProductionStatusIT.startBatch_plannedBatchBySeller_returns200InProgressAndMovesNothing` | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Retour en arrière | lot en cours → planifié | impossible : aucun endpoint, `ensureCanStart` refuse tout autre statut que `PLANNED` | `ProductionBatchTest.start_inProgressBatch_throwsConflict` | ✅ auto |
| Terminer refusé par le stock | « Terminer » avec base savonneuse insuffisante | toast d'erreur « Le statut du lot … n'a pas changé : … », badge inchangé | `production-page-status.spec.ts` « garde le statut et affiche l’erreur quand le serveur refuse (stock insuffisant) » ; serveur : PRD-02 `ProductionCompletionIT.completeBatch_oneInputShort_returns409AndMovesNothing` | ✅ auto |
| Hors ligne | « Démarrer » sans réseau | saisie en file (`Idempotency-Key`), toast « Saisie enregistrée hors connexion… », badge inchangé jusqu'à la synchro | `production-page-status.spec.ts` « hors ligne : la saisie part en file et le statut affiché ne change pas » | ✅ auto |
| Rejeu de la file | même `Idempotency-Key` rejouée | réponse mémorisée par le filtre d'idempotence (SYS-07), pas de 409 | file : tests SYS-07 | ⏭ hors périmètre (SYS-07) |
| Deux personnes démarrent le même lot | vendeuse et administrateur en même temps | verrou `PESSIMISTIC_WRITE` (`findByIdForUpdate`) : le premier passe, le second reçoit 409 | revue de `ProductionBatchServiceImpl.startBatch` ; pas de test de concurrence | 🔍 code |
| Double clic | deux clics rapides sur « Démarrer » | boutons du lot désactivés pendant l'envoi (`changing`) | `production-page.ts` `advance` | 🔍 code |
| Livreur sur l'écran | jeton livreur | route `/production` refusée (`production.read`) | guard existant (PRD-01) | ⏭ hors périmètre (PRD-01) |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/production/domain/ProductionBatchTest.java`,
  `.../service/impl/ProductionBatchServiceImplTest.java`, `.../controller/ProductionStatusIT.java` (créé),
  `.../controller/ProductionSecurityIT.java`.
- Frontend : `valimbavaka-ui/src/app/features/production/pages/production-page/production-page-status.spec.ts` (créé),
  `.../data-access/production-service.spec.ts`, `.../data-access/production-mapping.spec.ts`,
  `src/app/features/demo-pages.a11y.spec.ts` (page production).

## Recette manuelle (🧪)

1. En vendeuse, créer un lot « Enregistrer en brouillon », puis « Démarrer » dans « Lots récents » : badge « En cours », stock inchangé (écran Stocks).
2. Sur le même lot, « Terminer » : badge « Terminé », matières sorties et produit fini entré à l'entrepôt.
3. Recharger la page : les statuts sont conservés ; un lot terminé n'a plus de bouton.
4. Couper le réseau, « Démarrer » un lot planifié : toast « hors connexion », puis synchro au retour du réseau.

## Résultat de l'exécution

- Date : 2026-11-08 (CI GitHub `ci.yml`, run 42414235707, et `conventions.yml`, run 42414237395, commit `c4e88ce`)
- Environnement : CI GitHub — backend `mvn verify` : 305 tests unitaires et 299 tests d'intégration, 0 échec (dont
  `ProductionBatchTest` 9, `ProductionBatchServiceImplTest` 11, `ProductionStatusIT` 7, `ProductionSecurityIT` 13,
  `ProductionCompletionIT` 7) ; frontend `ng test` : 59 fichiers, 349 tests, 0 échec (dont
  `production-page-status.spec.ts` 6, `production-service.spec.ts` 5, `production-mapping.spec.ts` 7,
  `demo-pages.a11y.spec.ts`) ; Spotless, Checkstyle, Prettier, ESLint verts
- Anomalies ouvertes : aucune ; lignes 🔍 code : concurrence sur le même lot, double clic (pas de test dédié) ; 4 étapes 🧪 restantes
