# Scénarios de test — PRD-02 · Cohérence stock lors de la validation d'un lot

Statut : ✅ rédigé le 2026-11-08 — **tests exécutés en CI, verts** · Ticket : [#39](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/39) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur (`ADMIN`, valide), vendeuse (`SELLER`, crée mais ne valide pas : 403), anonyme (401) |
| Données de départ | Produit fini `ART-0031 · Savon noir 100 g` ; matière première `ART-0008 · Base savonneuse` (coût 1 200 Ar, 10 kg en entrepôt) ; semi-fini `ART-0040 · Base karité` (coût 1 200 Ar, 5 kg en entrepôt) ; emplacement `WAREHOUSE` actif ; vendeuse active « Soa Rakoto » |
| Migration | `2026_11_08-41-changelog.sql` (colonnes `production_batch_inputs.unit_cost` et `production_batches.completed_at`, permission `production.complete` donnée à l'administrateur) |
| Tickets à avoir déployés avant | PRD-01 (#38, PR empilée), JNL-01, STK-01 (`StockMovementService.record`) |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Les deux mouvements sont générés dans la même opération : validation d'un lot planifié | lot de 30 savons, entrants base savonneuse `0.45` et base karité `1.5` ; `POST /batches/{id}/completion` | 200, statut `COMPLETED`, `completedAt` renseigné ; entrepôt : base savonneuse 9,55, base karité 3,5, savon +30 ; 3 mouvements `PRODUCTION` avec `reference_id` = id du lot | `ProductionCompletionIT.completeBatch_enoughStock_returns200CompletedWithMovementsAndFrozenCosts` | ✅ auto |
| 1b | Jamais séparément : un entrant manquant annule tout | base karité à 1 kg pour un besoin de 1,5 | 409 `CONFLICT` ; base savonneuse reste à 10, savon à 0, aucun mouvement du lot, statut `PLANNED` | `ProductionCompletionIT.completeBatch_oneInputShort_returns409AndMovesNothing`, `ProductionCompletionTest.complete_insufficientStock_propagatesTheConflictWithoutRecordingTheOutput`, `ProductionBatchServiceImplTest.completeBatch_refusedMovement_tracesNothing` | ✅ auto |
| 1c | Un mouvement négatif par entrant puis un positif pour le produit, type `PRODUCTION`, emplacement `WAREHOUSE` | 2 entrants | 3 commandes dans l'ordre : entrants triés par identifiant, produit en dernier | `ProductionCompletionTest.complete_plannedBatch_recordsOneOutgoingMovementPerInputThenTheIncomingOutput` | ✅ auto |
| 1d | Coût unitaire des entrants figé à la validation | base savonneuse à 12 000 Ar, base karité sans coût | `unitCost` 12 000 puis `null` ; `completedAt` = horloge | `ProductionCompletionTest.complete_plannedBatch_freezesTheCostsAndMarksTheBatchCompleted`, `ProductionBatchTest.complete_plannedBatch_becomesCompletedWithFrozenCosts`, `ProductionBatchTest.complete_inputWithoutCost_keepsANullUnitCost` | ✅ auto |
| 1e | « Valider le lot » de l'écran : création et validation en une seule requête | `POST /completed-batches`, 12 savons, 3 kg de base (3 en stock) | 201 `COMPLETED` ; base à 0, savon 12 ; 2 mouvements ; journal `CREATE` + `UPDATE` | `ProductionCompletionIT.createCompletedBatch_enoughStock_returns201CompletedAndMovesTheStock`, `ProductionBatchServiceImplTest.createCompletedBatch_validRequest_plansThenCompletesTheSameBatch` | ✅ auto |
| 1f | Création et validation refusées ensemble | 3 kg demandés, 1 en stock | 409 ; aucun lot créé, stock inchangé | `ProductionCompletionIT.createCompletedBatch_insufficientStock_returns409AndCreatesNoBatch` | ✅ auto |
| 1g | Écran : « Valider le lot » appelle le bon endpoint par la file hors ligne | `validate: true` | `POST /api/v1/production/completed-batches` avec `Idempotency-Key`, lot affiché « Validé » | `production-service.spec.ts` « « Valider le lot » crée et valide en une seule requête… » | ✅ auto |
| 1h | Écran / data-access : valider un lot planifié existant | lot `b2` | `POST /api/v1/production/batches/b2/completion` avec `Idempotency-Key` | `production-service.spec.ts` « valide un lot planifié existant… » | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST /batches/{id}/completion` | 401 `UNAUTHENTICATED` | `ProductionSecurityIT.completeBatch_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton vendeuse sur `/completion` et `/completed-batches` | 403 `FORBIDDEN` | `ProductionSecurityIT.completeBatch_withSellerRole_returns403`, `ProductionSecurityIT.createCompletedBatch_withSellerRole_returns403` | ✅ auto |
| Mass assignment | `"id"`, `"status":"PLANNED"`, `"completedAt":"2020-01-01T00:00:00Z"`, `"unitCost":1` dans le corps | ignorés : id serveur, `COMPLETED`, date serveur, coût 1 200 de la fiche article | `ProductionCompletionIT.createCompletedBatch_bodyWithIdStatusAndCost_ignoresThem` | ✅ auto |
| Données invalides | corps de `/completed-batches` | mêmes règles et mêmes 422 que `POST /batches` (même `ProductionBatchWDto`, même `ProductionBatchPolicy`) | tests 422 de PRD-01 (`ProductionBatchIT.createBatch_*_returns422`) | ✅ auto |
| Entrepôt inconnu ou désactivé | `WAREHOUSE` inactif | 422 `VALIDATION_FAILED`, aucun mouvement | `ProductionCompletionTest.complete_unknownWarehouse_recordsNoMovement` | ✅ auto |
| Ressource introuvable | `POST /batches/{inconnu}/completion` | 404 `NOT_FOUND`, rien n'est mouvementé | `ProductionCompletionIT.completeBatch_unknownBatch_returns404`, `ProductionBatchServiceImplTest.completeBatch_unknownBatch_throwsNotFoundAndMovesNothing` | ✅ auto |
| Conflit : déjà validé | deuxième validation du même lot | 409 « Le lot est déjà validé… », stock mouvementé une seule fois | `ProductionCompletionIT.completeBatch_alreadyCompleted_returns409AndMovesTheStockOnlyOnce`, `ProductionBatchTest.complete_alreadyCompletedBatch_throwsConflictAndKeepsTheFirstCompletion` | ✅ auto |
| Conflit : stock insuffisant | voir 1b, 1f | 409 `CONFLICT` « Stock insuffisant à cet emplacement : 1 disponible. » | voir 1b, 1f | ✅ auto |
| Journal | lot validé | une ligne `UPDATE` `PRODUCTION_BATCH` « Validation du lot LOT-00003 : 1 entrant(s) sortis, 30 unité(s) entrées à l'entrepôt », plus une ligne `CREATE` `STOCK_MOVEMENT` par mouvement (STK-01) | `ProductionBatchServiceImplTest.completeBatch_plannedBatch_movesTheStockThenFlushesAndTracesTheValidation`, `ProductionCompletionIT.completeBatch_enoughStock_returns200CompletedWithMovementsAndFrozenCosts` | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Stock exactement suffisant | 3 kg en stock, 3 kg demandés | accepté, stock à 0 | `ProductionCompletionIT.createCompletedBatch_enoughStock_returns201CompletedAndMovesTheStock` | ✅ auto |
| Stock juste insuffisant | 1 kg pour 1,5 | 409, rien ne bouge | `ProductionCompletionIT.completeBatch_oneInputShort_returns409AndMovesNothing` | ✅ auto |
| Entrant sans coût d'achat | `cost` null sur la fiche | validation acceptée, `unitCost` null (PRD-05 le signalera) | `ProductionBatchTest.complete_inputWithoutCost_keepsANullUnitCost` | ✅ auto |
| Deux validations simultanées du même lot | deux clics, deux postes | la seconde attend le verrou (`findByIdForUpdate`, `PESSIMISTIC_WRITE`) puis reçoit 409 « déjà validé » | `ProductionBatchRepository.findByIdForUpdate` ; pas de test de concurrence | 🔍 code `ProductionBatchServiceImpl.completeBatch` |
| Deux lots qui consomment les mêmes matières en même temps | lots A et B sur la même base | verrous de stock pris dans un ordre stable (entrants triés, produit en dernier) : pas d'interblocage ; le second voit le solde mis à jour | `ProductionCompletionTest.complete_plannedBatch_recordsOneOutgoingMovementPerInputThenTheIncomingOutput` (ordre) | ✅ auto |
| Hors ligne | « Valider le lot » sans réseau | une seule saisie en file (`/completed-batches`), rejouée avec la même `Idempotency-Key` ; toast « en file » | `production-service.spec.ts` (clé envoyée) ; file : tests SYS-07 | ✅ auto |
| Vendeuse sur l'écran | jeton vendeuse | bouton « Valider le lot » masqué, Entrée enregistre un lot planifié | `production-page.ts` `canComplete` ; pas de spec de page | 🧪 recette |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/production/domain/ProductionBatchTest.java`,
  `.../service/impl/ProductionCompletionTest.java`, `.../service/impl/ProductionBatchServiceImplTest.java`,
  `.../controller/ProductionCompletionIT.java`, `.../controller/ProductionSecurityIT.java`.
- Frontend : `valimbavaka-ui/src/app/features/production/data-access/production-service.spec.ts`.

## Recette manuelle (🧪)

1. En administrateur, créer un lot avec « Valider le lot » : toast « Lot LOT-xxxxx validé : matières sorties et produit
   fini entré à l'entrepôt », puis vérifier sur l'écran Stocks que l'entrepôt a perdu les matières et gagné le produit.
2. En administrateur, demander plus de matière que le disponible (en contournant l'écran, ex. stock modifié entre-temps) : toast d'erreur 409, rien ne bouge.
3. En vendeuse : le bouton « Valider le lot » n'apparaît pas ; « Enregistrer en brouillon » crée un lot planifié.

## Résultat de l'exécution

- Date : 2026-11-07 (CI GitHub `ci.yml`, run 42412440990, commit `46b2e51`)
- Environnement : CI GitHub — backend `mvn verify` : 280 tests unitaires et 291 tests d'intégration, 0 échec (dont
  `ProductionBatchTest` 5, `ProductionCompletionTest` 4, `ProductionBatchServiceImplTest` 8, `ProductionCompletionIT` 7,
  `ProductionSecurityIT` 11) ; frontend `ng test` : 53 fichiers, 294 tests, 0 échec (dont `demo-pages.a11y.spec.ts`)
- Anomalies ouvertes : aucune ; 3 étapes 🧪 restantes. Lignes 🔍 code : concurrence sur le même lot (pas de test dédié)
