# Scénarios de test — PRD-05 · Détail et coût de revient d'un lot

Statut : ✅ rédigé le 2026-11-08 — tests à lancer (CI) · Ticket : [#42](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/42) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Vendeuse (`SELLER`, user story), administrateur (`ADMIN`), livreur (`DELIVERY_DRIVER`, 403), anonyme (401) |
| Données de départ | `LOT-00003` terminé le 08/11/2026 : 30 « Savon noir 100 g » (vendu 8 500 Ar) pour 2 kg d'huile de coco à 1 200 Ar/kg (coût figé) + 0,5 kg de base savonneuse à 12 000 Ar/kg ; `LOT-00004` planifié : 10 savons pour 1 kg d'huile, dont le coût d'achat actuel est 1 500 Ar/kg |
| Migration | aucune (lecture seule ; permission `production.read` existante, administrateur et vendeuse) |
| Tickets à avoir déployés avant | PRD-01 (#38), PRD-02 (#39 : `unit_cost` figé à la validation, `completed_at`), PRD-03 (#40), PRD-04 (#41 ; PR empilée) |

## Scénarios issus des critères d'acceptation

Règles de calcul (`ProductionCostServiceImpl.getBatchCost`) : coût d'une ligne = quantité × coût unitaire, arrondi à
2 décimales ; coût matière = Σ des lignes ; **coût de revient unitaire = coût matière ÷ quantité produite**, arrondi à
2 décimales (HALF_UP). Lot terminé : coûts **figés** à la validation (PRD-02) ; lot planifié ou en cours : coûts
d'achat **actuels** (`estimated` = true). Un entrant sans coût laisse les totaux à null (jamais compté pour 0).

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Coût de revient unitaire calculé à partir des entrants (serveur) | `LOT-00003`, l'huile coûte 1 500 Ar aujourd'hui | lignes 2 400.00 et 6 000.00 (coût figé 1 200, pas 1 500), `materialCost` 8 400, `unitCost` 280.00, `uncostedInputs` 0, `estimated` false, `marginPercent` 97 | `ProductionCostServiceImplTest.getBatchCost_completedBatch_usesTheFrozenCostsAndComputesTheCostPerUnit` | ☐ |
| 1b | Bout en bout, vendeuse, coût figé | vendeuse ; « Valider le lot » de 10 savons pour 2 kg d'huile à 1 200 Ar, puis coût de l'huile passé à 5 000 Ar | 200 ; `unitCost` ligne 1 200, `lineCost` 2 400, `materialCost` 2 400, `unitCost` 240, `salePrice` 8 500, `marginPercent` 97 | `ProductionCostIT.getBatchCost_sellerOnCompletedBatch_returnsTheCostFrozenAtCompletion` | ☐ |
| 1c | Lot non terminé : estimation | `LOT-00004` planifié, huile à 1 500 Ar | `estimated` true, `completedAt` null, `materialCost` 1 500, `unitCost` 150.00 | `ProductionCostServiceImplTest.getBatchCost_plannedBatch_estimatesWithTheCurrentPurchaseCosts`, `ProductionCostIT.getBatchCost_plannedBatch_returnsAnEstimateFromTheCurrentCost` | ☐ |
| 1d | Écran : le coût de revient affiché est celui des entrants | détail de `LOT-00003` | tableau « Huile de coco · 2 kg · 1 200 Ar / kg · 2 400 Ar », « Base savonneuse … 6 000 Ar » ; « Coût matières » 8 400 Ar, « Coût de revient / u. » 280 Ar, « Prix de vente » 8 500 Ar, « Marge sur prix de vente » 97 % ; bandeau « Coûts figés à la validation » | `production-lot-page.spec.ts` « affiche les entrants et le coût de revient unitaire calculé à partir des entrants » | ☐ |
| 1e | Écran : lot non terminé | `LOT-00004` | bandeau d'avertissement « Lot non terminé : coût estimé… », pas de « terminé le » | `production-lot-page.spec.ts` « signale un lot non terminé : coût estimé avec les prix d’achat actuels » | ☐ |
| 1f | Data-access branché sur l'API | `cost('b2')` | `GET /api/v1/production/batches/b2/cost` ; réponse convertie (statut `VALIDATED`, jour `2026-11-08`, unités `kg`) | `production-service.spec.ts` « lit le détail et le coût de revient d'un lot par GET sur /batches/{id}/cost », `production-mapping.spec.ts` « reprend le coût de revient du serveur, le jour de validation et les unités courtes » | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/production/batches/{id}/cost` sans `Authorization` | 401 `UNAUTHENTICATED` | `ProductionSecurityIT.getBatchCost_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `ProductionSecurityIT.getBatchCost_withDeliveryDriverRole_returns403` | ☐ |
| Vendeuse | jeton vendeuse (`production.read`) | 200 | `ProductionCostIT.getBatchCost_sellerOnCompletedBatch_returnsTheCostFrozenAtCompletion` | ☐ |
| Lot inconnu | `GET /batches/0195…/cost` (UUID aléatoire) | 404 `NOT_FOUND` | `ProductionCostIT.getBatchCost_unknownBatch_returns404`, `ProductionCostServiceImplTest.getBatchCost_unknownBatch_throwsNotFound` | ☐ |
| Identifiant mal formé | `GET /batches/abc/cost` | 400 `MALFORMED_REQUEST` | `ProductionCostIT.getBatchCost_malformedId_returns400` | ☐ |
| Mass assignment, 422, 409 | — | sans objet : lecture sans corps | — | ⏭ hors périmètre (aucune écriture) |
| Journal | — | sans objet : lecture seule, rien n'est tracé | — | ⏭ hors périmètre (aucune écriture) |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Entrant sans coût (serveur) | base sans coût figé à la validation | ligne base : `unitCost` et `lineCost` null ; ligne huile 2 400 ; `uncostedInputs` 1 ; `materialCost`, `unitCost` et `marginPercent` null | `ProductionCostServiceImplTest.getBatchCost_inputWithoutCost_leavesTheTotalsNullAndCountsTheInput`, `ProductionCostIT.getBatchCost_inputWithoutCost_returnsNoTotalAndCountsTheInput` | ☐ |
| Entrant sans coût (écran) | `uncosted` 1 | « — » sur la ligne, le coût de revient et la marge ; avertissement « 1 entrant(s) sans coût d'achat : coût de revient non calculable… » | `production-lot-page.spec.ts` « affiche « — » et un avertissement quand un entrant n’a pas de coût » | ☐ |
| Arrondi | 3 savons pour 0,833 kg d'huile à 1 200 Ar = 999,60 Ar | `unitCost` 333.20 (2 décimales) | `ProductionCostServiceImplTest.getBatchCost_costNotDivisible_roundsTheCostPerUnitToTwoDecimals` | ☐ |
| Produit sans prix de vente | prix de vente null | coût calculé (120.00), `salePrice` et `marginPercent` null, écran « — » | `ProductionCostServiceImplTest.getBatchCost_outputWithoutSalePrice_returnsTheCostWithoutMargin` | ☐ |
| Champs absents de la réponse | `unitCost`, `materialCost`… absents | normalisés à null, statut `PLANNED` → « Planifié » | `production-mapping.spec.ts` « normalise les coûts absents à null (entrant sans coût, lot non validé) » | ☐ |
| Marge négative | coût de revient > prix de vente | marge affichée en rouge (classe `loss`) | — | 🧪 recette |
| Erreur de chargement | lot introuvable ou réseau coupé | état « erreur » avec « Réessayer », pas de tableau | `production-lot-page.spec.ts` « propose de réessayer si le lot ne se charge pas » | ☐ |
| Hors ligne | détail ouvert sans réseau | lecture seule : erreur et « Réessayer », aucune saisie en file | idem | ☐ |
| Accessibilité | détail chargé | aucune violation axe (titre, légende de tableau, en-têtes de ligne) | `production-lot-page.spec.ts` « ne présente aucune violation d’accessibilité détectable » | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/production/service/impl/ProductionCostServiceImplTest.java` (créé),
  `.../controller/ProductionCostIT.java` (créé), `.../controller/ProductionSecurityIT.java` (2 tests ajoutés).
- Frontend : `valimbavaka-ui/src/app/features/production/pages/production-lot-page/production-lot-page.spec.ts` (créé),
  `.../data-access/production-service.spec.ts`, `.../data-access/production-mapping.spec.ts`.

## Recette manuelle (🧪)

1. En vendeuse, sur l'écran Production, cliquer sur le numéro d'un lot terminé dans « Lots récents » : la page détail
   affiche les entrants, le coût matière, le coût de revient unitaire et la marge ; vérifier à la main Σ lignes ÷ quantité.
2. Modifier le coût d'achat d'une matière (fiche article) puis rouvrir le même lot terminé : coûts inchangés ; rouvrir un
   lot planifié qui l'utilise : coût estimé mis à jour.
3. Lot dont le coût de revient dépasse le prix de vente : marge négative en rouge.
4. En mobile : le tableau défile horizontalement, les 4 montants passent à la ligne.

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : à lancer (CI GitHub `ci.yml` et `conventions.yml`)
- Anomalies ouvertes : aucune connue ; 4 étapes 🧪 restantes
