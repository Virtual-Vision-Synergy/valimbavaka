# Scénarios de test — PRD-01 · Création d'un lot de production

Statut : ✍ rédigé le 2026-11-08 — **tests écrits, non exécutés** · Ticket : [#38](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/38) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Vendeuse (`SELLER`, crée et consulte), administrateur (`ADMIN`, crée et consulte), livreur (`DELIVERY_DRIVER`, refusé) |
| Données de départ | Produit fini `ART-0031 · Savon noir 100 g` (actif) ; matière première `ART-0008 · Base savonneuse` (kg, 12,5 kg en entrepôt) ; semi-fini `ART-0040 · Base karité` ; vendeuse active « Soa Rakoto » ; une vendeuse désactivée ; un livreur |
| Migration | `2026_11_08-40-changelog.sql` (tables `production_batches`, `production_batch_inputs`, séquence du numéro, permission `production.create`, `production.read` donnée à la vendeuse) |
| Tickets à avoir déployés avant | ART-04, STK-01, JNL-02, JNL-01 |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Le formulaire permet de sélectionner produit obtenu, quantité, vendeuse responsable et entrants : création par la vendeuse | Savon noir × `30`, responsable Soa, entrants base savonneuse `0.45` et base karité `1.5` | 201, `Location` `/api/v1/production/batches/{id}`, numéro `LOT-00001`…, statut `PLANNED`, 2 entrants dans l'ordre saisi | `ProductionBatchIT.createBatch_sellerWithValidBatch_returns201PlannedWithNumberAndInputs` | ☐ |
| 1b | Le produit obtenu ne peut être qu'un produit fini actif | matière première `ART-0008` comme produit obtenu | 422 `VALIDATION_FAILED` | `ProductionBatchIT.createBatch_rawMaterialAsOutput_returns422` | ☐ |
| 1c | Les entrants ne peuvent être que des MP ou SF actifs | produit fini comme entrant | 422 `VALIDATION_FAILED` | `ProductionBatchIT.createBatch_finishedProductAsInput_returns422`, `ProductionBatchPolicyTest.check_inputRefusedByArticleRules_propagatesTheError` | ☐ |
| 1d | Chaque ligne est contrôlée par le serveur (ART-04) | 2 entrants | `requireProductionArticle` appelé pour la sortie et pour chaque entrant | `ProductionBatchPolicyTest.check_validBatch_checksOutputAndEveryInputAndReturnsTheOutput` | ☐ |
| 1e | La responsable est une vendeuse active | livreur, puis vendeuse désactivée | 422 « La personne responsable doit être une vendeuse active. » | `ProductionBatchIT.createBatch_operatorIsNotASeller_returns422`, `ProductionBatchIT.createBatch_deactivatedSeller_returns422`, `ProductionBatchPolicyTest.check_operatorNotAnActiveSeller_throwsBusinessException` | ☐ |
| 1f | La liste des responsables ne propose que les vendeuses actives, sans données sensibles | 1 vendeuse active, 1 désactivée, 1 livreur au même prénom | 1 résultat, ni `email` ni `commissionRate` | `ProductionBatchIT.getOperators_sellerToken_listsActiveSellersOnly` | ☐ |
| 1g | Un lot créé est planifié : le stock ne bouge pas (PRD-02) | lot de 12 | 0 ligne `stock_movements` avec `reference_id` = id du lot | `ProductionBatchIT.createBatch_validBatch_isTracedInJournalAndMovesNoStock` | ☐ |
| 1h | Le numéro vient de la séquence du serveur | séquence à 7 | `LOT-00007`, statut `PLANNED`, 2 entrants | `ProductionBatchServiceImplTest.createBatch_validRequest_savesAPlannedBatchNumberedFromTheSequence`, `ProductionBatchTest.plan_validDetails_createsAPlannedBatchWithItsInputs` | ☐ |
| 1i | Écran : produits finis, entrants avec le disponible de l'entrepôt, vendeuses et lots récents viennent du serveur | `GET` articles `OUTPUT` / `INPUT`, stocks, opérateurs, 10 derniers lots | contexte assemblé, lot `PLANNED` affiché « Planifié » | `production-service.spec.ts` « assemble l'écran… », `production-mapping.spec.ts` | ☐ |
| 1j | Écran : l'enregistrement envoie le contrat serveur | 1 entrant à `2.4`, 1 à `0` | `POST` avec `Idempotency-Key`, ligne à 0 non envoyée, `validate` non envoyé | `production-service.spec.ts` « crée le lot par POST… », `production-mapping.spec.ts` « envoie les entrants… » | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST`, `GET /batches`, `GET /operators` | 401 `UNAUTHENTICATED` | `ProductionSecurityIT.createBatch_withoutToken_returns401`, `getBatches_withoutToken_returns401`, `getOperators_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `ProductionSecurityIT.createBatch_withDeliveryDriverRole_returns403`, `getBatches_withDeliveryDriverRole_returns403`, `getOperators_withDeliveryDriverRole_returns403` | ☐ |
| Lecture par la vendeuse | jeton vendeuse | 200 | `ProductionSecurityIT.getBatches_withSellerRole_returns200` | ☐ |
| Mass assignment | `"id"`, `"status":"COMPLETED"`, `"number":"LOT-HACK"`, `"role":"ADMIN"` dans le corps | ignorés : id serveur, `PLANNED`, numéro de la séquence | `ProductionBatchIT.createBatch_bodyWithIdStatusAndNumber_ignoresThem` | ☐ |
| Données invalides | quantité `0`, `1.2345`, `1000000000` ; entrant `-1` ; `inputs` vide | 422 `VALIDATION_FAILED` | `ProductionBatchIT.createBatch_zeroQuantity_returns422`, `createBatch_quantityWithFourDecimals_returns422`, `createBatch_quantityAboveNineIntegerDigits_returns422`, `createBatch_negativeInputQuantity_returns422`, `createBatch_noInput_returns422` | ☐ |
| Doublon d'entrant | même MP deux fois | 422 « Un entrant ne peut apparaître qu'une fois dans un lot. », aucun appel au catalogue | `ProductionBatchIT.createBatch_sameInputTwice_returns422`, `ProductionBatchPolicyTest.check_sameInputTwice_throwsBusinessExceptionBeforeAnyLookup` | ☐ |
| Ressource introuvable | entrant UUID inconnu ; `GET /batches/{inconnu}` | 404 `NOT_FOUND` | `ProductionBatchIT.createBatch_unknownInputArticle_returns404`, `getBatchById_unknownBatch_returns404`, `ProductionBatchServiceImplTest.getBatchById_unknownBatch_throwsNotFound` | ☐ |
| Conflit 409 | — | sans objet : pas de modification dans PRD-01 (la version servira à PRD-03) | — | ⏭ PRD-03 |
| Journal | lot créé | une ligne `CREATE` `PRODUCTION_BATCH` « Création du lot LOT-00012 : 30 × ART-0031 · Savon noir 100 g (2 entrant(s)) » | `ProductionBatchServiceImplTest.createBatch_validRequest_tracesTheCreationInTheJournal`, `ProductionBatchIT.createBatch_validBatch_isTracedInJournalAndMovesNoStock` | ☐ |
| Refus = rien d'écrit | règle violée | ni lot, ni numéro consommé, ni journal | `ProductionBatchServiceImplTest.createBatch_rejectedByThePolicy_savesNothingAndTracesNothing` | ☐ |
| Pas de suppression | `DELETE /batches/{id}` | 4xx (aucun endpoint, JNL-02) | `ProductionSecurityIT.deleteBatch_isNotExposed` | ☐ |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Quantité minimale | `0.001` | acceptée (3 décimales) | couvert par la contrainte `@Digits` ; pas de test dédié | 🔍 code `ProductionBatchWDto` |
| Quantité maximale | `999999999.999` puis `1000000000` | acceptée puis 422 | `ProductionBatchIT.createBatch_quantityAboveNineIntegerDigits_returns422` | ☐ |
| Nombre d'entrants | 50 puis 51 | accepté puis 422 (`@Size(max = 50)`) | pas de test dédié | 🔍 code `ProductionBatchWDto` |
| Deux lots de suite | même corps posté deux fois (clés différentes) | deux numéros distincts | `ProductionBatchIT.createBatch_twoBatches_receiveDistinctNumbers` | ☐ |
| Recherche | numéro en minuscules `lot-00003` | trouvé, entrants chargés | `ProductionBatchIT.getBatches_searchOnNumber_returnsTheMatchingBatch` | ☐ |
| `size` = 101 | `GET /batches?size=101` | 422 | `ProductionBatchIT.getBatches_sizeAboveMaximum_returns422` | ☐ |
| Lecture d'un lot | `GET /batches/{id}` | lot avec ses entrants | `ProductionBatchIT.getBatchById_createdBatch_returnsItWithInputs` | ☐ |
| Liste de la saisie protégée | modification de la liste passée à `plan` | le lot garde ses entrants, `getInputs()` non modifiable | `ProductionBatchTest.getInputs_listChangedByTheCaller_doesNotChangeTheBatch` | ☐ |
| Hors ligne | réseau coupé à l'enregistrement | saisie en file, toast `common.queuedSaved`, envoyée au retour avec la même `Idempotency-Key` (SYS-07) | mécanisme commun `OutboxService` | 🧪 recette |
| Entrant sans stock | MP à 0 en entrepôt | ligne en rouge, « Valider » grisé, « Enregistrer » possible (le lot est planifié) | — | 🧪 recette |
| Article ou vendeuse hors liste | lot d'un produit désactivé depuis | « — » dans « Lots récents » | `production-mapping.spec.ts` « affiche « — »… » | ☐ |
| Concurrence | deux vendeuses créent un lot en même temps | numéros distincts (séquence PostgreSQL) | `createBatch_twoBatches_receiveDistinctNumbers` (séquentiel) | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/production/`
  - `domain/ProductionBatchTest`
  - `service/impl/ProductionBatchPolicyTest`, `service/impl/ProductionBatchServiceImplTest` (+ `ProductionTestData`)
  - `controller/ProductionBatchIT`, `controller/ProductionSecurityIT` (+ `ProductionApiTestSupport`)
- Frontend : `valimbavaka-ui/src/app/features/production/data-access/production-mapping.spec.ts`,
  `production-service.spec.ts` ; `core/layout/menu.spec.ts` (la vendeuse voit « Production »).
- Transverse : `ArchitectureTest` (repository sans suppression, contrôleur avec `@HasPermission`).

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Vendeuse : menu « Production », choisir Savon noir, 30, Soa Rakoto, ajouter 2 matières, « Enregistrer » | toast « Lot LOT-000xx enregistré : planifié… », lot en tête de « Lots récents » avec le badge « Planifié » | 🧪 |
| Même parcours, réseau coupé (DevTools hors ligne) | toast « Saisie enregistrée hors connexion… », lot créé au retour du réseau | 🧪 |
| Livreur | menu « Production » absent, `/production` refusé | 🧪 |
| Vérifier en base | `production_batches` + `production_batch_inputs` remplies, aucune ligne `stock_movements` | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-08
- Ce qui a tourné : `tsc` (app et spec), ESLint et Prettier sur les fichiers front modifiés : OK.
  **Tests automatiques : à lancer (CI ou local)** — `mvn verify` et `ng test` non exécutés (règle n°3 ; Maven
  Central inaccessible dans l'environnement de développement, compilation Java non vérifiée en local).
- Anomalies ouvertes : aucune connue à ce stade.
- Étapes 🧪 restantes : 4 (+ 2 cas limites).
