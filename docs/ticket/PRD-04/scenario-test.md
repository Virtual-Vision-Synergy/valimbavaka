# Scénarios de test — PRD-04 · Indicateurs de production

Statut : ✅ rédigé le 2026-11-08 — **tests exécutés en CI, verts** · Ticket : [#41](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/41) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur (`ADMIN`, user story), vendeuse (`SELLER`, voit aussi l'écran Production), livreur (`DELIVERY_DRIVER`, 403), anonyme (401) |
| Données de départ | Novembre 2026 (heure de Madagascar) : `LOT-00001` terminé le 03/11, 30 savons pour 2 kg d'huile à 1 200 Ar + 0,5 kg de base à 12 000 Ar (8 400 Ar) ; `LOT-00002` terminé le 05/11, 10 savons pour 1 kg d'huile (1 200 Ar) ; `LOT-00003` en cours ; octobre : 32 savons terminés |
| Migration | aucune (lecture seule sur `production_batches` et `production_batch_inputs`, permission `production.read` existante) |
| Tickets à avoir déployés avant | PRD-01 (#38), PRD-02 (#39 : `completed_at`, `unit_cost`), PRD-03 (#40 : statut `IN_PROGRESS` ; PR empilée) |

## Scénarios issus des critères d'acceptation

Règles de calcul (`ProductionIndicatorServiceImpl.getIndicators`) : mois en cours **à l'heure de Madagascar** ; un lot
compte dans le mois de sa **validation** (`completed_at`, règle de PRD-02) ; coût matière = Σ `quantité × unit_cost`
des entrants, **uniquement** pour les lots dont tous les entrants ont un coût figé ; coût moyen = coût matière ÷ unités
de ces lots, arrondi à 2 décimales.

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Les 3 indicateurs sont affichés en tête de l'écran | lots, unités et coût du fixture (18 lots, 3 en cours, 864 unités, +12 %, 3 410 Ar) | trois premières cartes « Lots terminés ce mois » 18 « 3 lot(s) en cours », « Unités produites » 864 « +12 % vs mois dernier », « Coût matière moyen » 3 410 Ar | `production-page-kpis.spec.ts` « affiche les 3 indicateurs en tête : lots terminés, unités produites, coût matière moyen » | ✅ auto |
| 1b | Calcul serveur : lots, unités, coût par unité | données de départ | `completedBatches` 2, `inProgressBatches` 3, `unitsProduced` 40, `previousMonthUnits` 32, `unitsTrendPercent` 25, `materialCost` 9 600, `materialCostPerUnit` 240.00 | `ProductionIndicatorServiceImplTest.getIndicators_completedBatchesOfTheMonth_countsBatchesUnitsAndMaterialCostPerUnit` | ✅ auto |
| 1c | Bout en bout : un lot validé ce mois | vendeuse ; « Valider le lot » de 10 savons pour 2 kg d'huile à 1 200 Ar | écarts avant / après : +1 lot, +10 unités, +2 400 Ar de coût matière, mois `2026-11` | `ProductionIndicatorsIT.getIndicators_batchCompletedThisMonth_addsTheBatchItsUnitsAndItsMaterialCost` | ✅ auto |
| 1d | Un lot démarré n'est pas un lot produit | `POST /batches/{id}/start` | +1 en cours, +0 lot terminé, +0 unité | `ProductionIndicatorsIT.getIndicators_startedBatch_countsItInProgressOnly` | ✅ auto |
| 1e | Écran branché sur l'API | `context()` | `GET /api/v1/production/indicators` appelé avec les autres lectures ; indicateurs du serveur recopiés ; matières à racheter = entrants sous un seuil non nul | `production-service.spec.ts` « assemble l'écran : produits finis, entrants avec le stock de l'entrepôt, vendeuses, lots récents, indicateurs » | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/production/indicators` sans `Authorization` | 401 `UNAUTHENTICATED` | `ProductionSecurityIT.getIndicators_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `ProductionSecurityIT.getIndicators_withDeliveryDriverRole_returns403` | ✅ auto |
| Vendeuse | jeton vendeuse (`production.read`) | 200 | `ProductionIndicatorsIT.getIndicators_batchCompletedThisMonth_addsTheBatchItsUnitsAndItsMaterialCost` | ✅ auto |
| Mass assignment, 422, 404, 409 | — | sans objet : lecture sans corps ni paramètre ni identifiant | — | ⏭ hors périmètre (aucune écriture) |
| Journal | — | sans objet : lecture seule, rien n'est tracé | — | ⏭ hors périmètre (aucune écriture) |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Entrant sans coût | lot de 10 savons dont la base n'a pas de `unit_cost` | lot compté (lots, unités) mais **hors** coût matière ; `uncostedBatches` 1 ; coût moyen = 8 400 ÷ 30 = 280.00 | `ProductionIndicatorServiceImplTest.getIndicators_batchWithAnInputWithoutCost_leavesItOutOfTheCostAndCountsItAsUncosted`, `ProductionIndicatorsIT.getIndicators_inputWithoutCost_countsTheBatchAsUncostedAndLeavesItOutOfTheCost` | ✅ auto |
| Écran : lots sans coût | `uncostedLots` 2, `averageCost` null | carte coût : « — » sans « Ar », note « 2 lot(s) sans coût d'entrant, non compté(s) » | `production-page-kpis.spec.ts` « affiche « — » sans unité quand aucun lot n’est chiffré et signale les lots sans coût » | ✅ auto |
| Mois vide | aucun lot terminé ce mois ni le mois dernier | 0 lot, 0 unité, coût 0, `materialCostPerUnit` null, `unitsTrendPercent` null | `ProductionIndicatorServiceImplTest.getIndicators_noBatchThisMonthNorLastMonth_returnsZerosAndNoAverageNorTrend`, `production-mapping.spec.ts` « garde « pas de tendance » et « pas de coût moyen » quand le serveur les renvoie à null » | ✅ auto |
| Baisse | 30 unités contre 40 le mois dernier | `unitsTrendPercent` −25 ; écran « -25 % vs mois dernier » (sans « + », en rouge) | `ProductionIndicatorServiceImplTest.getIndicators_fewerUnitsThanLastMonth_returnsANegativeTrend`, `production-page-kpis.spec.ts` « affiche une baisse sans « + » et signale l’absence de production le mois dernier » | ✅ auto |
| Rien le mois dernier | `previousMonthUnits` 0 | pas de division par zéro : tendance null, écran « aucune production le mois dernier » | idem | ✅ auto |
| Fuseau, minuit, fin de mois | 30/11/2026 21:30 UTC = 01/12 00:30 à Madagascar | mois `2026-12`, bornes `[2026-11-30T21:00Z, 2026-12-31T21:00Z)`, mois précédent `[2026-10-31T21:00Z, 2026-11-30T21:00Z)` | `ProductionIndicatorServiceImplTest.getIndicators_firstMinutesOfTheMonthInMadagascar_readsTheMonthOfMadagascar` | ✅ auto |
| Lot validé le mois dernier | `completed_at` = 1er du mois 00:00 Madagascar − 1 h | non compté ce mois ; compté dans `previousMonthUnits` | `ProductionIndicatorsIT.getIndicators_batchCompletedLastMonth_countsInThePreviousMonthOnly` | ✅ auto |
| Matières à racheter : bornes du seuil | stock 4,999 / seuil 5 ; stock 5 / seuil 5 ; seuil 0 | seul le premier compte (même règle que STK-04) ; un produit fini sous son seuil ne compte pas | `production-mapping.spec.ts` « compte un entrant à racheter sous son seuil, jamais à son seuil ni avec un seuil à zéro », `production-service.spec.ts` (1e) | ✅ auto |
| Hors ligne | écran ouvert sans réseau | lecture seule : l'écran ne se charge pas (comme les autres lectures de PRD-01) ; aucune saisie en file | — | ⏭ hors périmètre (lecture, SYS-07) |
| Accessibilité | page Production chargée avec des réponses vides | aucune violation axe | `src/app/features/demo-pages.a11y.spec.ts` « production should have no detectable accessibility violation » | ✅ auto |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/production/service/impl/ProductionIndicatorServiceImplTest.java` (créé),
  `.../controller/ProductionIndicatorsIT.java` (créé), `.../controller/ProductionSecurityIT.java`.
- Frontend : `valimbavaka-ui/src/app/features/production/pages/production-page/production-page-kpis.spec.ts` (créé),
  `.../data-access/production-service.spec.ts`, `.../data-access/production-mapping.spec.ts`,
  `src/app/features/demo-pages.a11y.spec.ts` (page production).

## Recette manuelle (🧪)

1. En administrateur, noter les indicateurs, puis « Valider le lot » de 10 savons : recharger, « Lots terminés ce mois » +1, « Unités produites » +10, coût matière moyen recalculé.
2. « Démarrer » un lot planifié : la note « lot(s) en cours » augmente, les lots terminés ne bougent pas.
3. Retirer le coût d'achat d'une matière, valider un lot qui l'utilise : note « 1 lot(s) sans coût d'entrant, non compté(s) ».
4. Vérifier en vendeuse que les mêmes indicateurs s'affichent ; en mobile, que les 4 cartes passent à la ligne.

## Résultat de l'exécution

- Date : 2026-11-08 (CI GitHub `ci.yml`, run 42416628836, et `conventions.yml`, run 42416630737, commit `9546c30`)
- Environnement : CI GitHub — backend `mvn verify` : 337 tests unitaires et 334 tests d'intégration, 0 échec (dont
  `ProductionIndicatorServiceImplTest` 5, `ProductionIndicatorsIT` 4, `ProductionSecurityIT` 15) ; frontend `ng test` :
  63 fichiers, 375 tests, 0 échec (dont `production-page-kpis.spec.ts` 3, `production-service.spec.ts` 5,
  `production-mapping.spec.ts` 9, `demo-pages.a11y.spec.ts` 9) ; Spotless, Checkstyle, Prettier, ESLint, gitleaks verts
- Anomalies ouvertes : aucune ; 4 étapes 🧪 restantes
