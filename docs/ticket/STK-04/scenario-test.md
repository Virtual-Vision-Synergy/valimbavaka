# Scénarios de test — STK-04 · Alerte de seuil minimum

Statut : ☐ tests écrits, à exécuter · Ticket : [#33](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/33) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` et `SELLER` (permission `stocks.read`) ; `DELIVERY_DRIVER` pour le 403 |
| Données de départ | Article « Savon noir 100 g » (`ART-0031`, pièce, seuil 40) avec 10 à l'entrepôt et 2,5 en boutique ; article au seuil 15 jamais mouvementé ; article au seuil 0 |
| Tickets à avoir déployés avant | STK-01 (mouvements de stock), ART-01 (seuil de l'article) |

## Scénarios issus des critères d'acceptation

### Critère 1 — La liste affiche stock actuel, seuil, manque et quantité suggérée à commander

Règle : un article **actif**, de seuil **> 0**, est en alerte si son stock total (somme des emplacements actifs) est
**strictement inférieur** au seuil. Manque = seuil − stock. Quantité suggérée = manque arrondi à la dizaine
supérieure, minimum 10. Tri : ratio stock / seuil croissant (ruptures d'abord), puis code.

| # | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| 1.1 | `ART-0031` seuil 40, stock 10 (entrepôt) + 2,5 (boutique) ; `GET /api/v1/stocks/alerts?search=<nom>` | 200, une ligne : `quantity` 12.5, `threshold` 40, `shortage` 27.5, `suggestedQuantity` 30, `unit` PIECE | `StockAlertIT.getAlerts_stockSpreadOverLocationsUnderThreshold_returnsShortageAndSuggestion` | ☐ |
| 1.2 | Article seuil 15 jamais mouvementé | Stock 0, manque 15, suggéré 20 | `StockAlertIT.getAlerts_outOfStockArticle_returnsTheWholeThresholdAsShortage` | ☐ |
| 1.3 | Calcul des champs d'une alerte (12,5 / 40) | manque 27,5 ; suggéré 30 | `StockAlertPolicyTest.toAlert_stockUnderThreshold_returnsQuantityThresholdShortageAndSuggestion` | ☐ |
| 1.4 | Trois articles au seuil 40 : 12,5 / 0 / 40 | Seuls le 0 puis le 12,5 sont listés, `totalCount` 2 | `StockServiceImplAlertTest.getAlerts_mixedStocks_returnsOnlyArticlesUnderThresholdMostUrgentFirst` | ☐ |
| 1.5 | Alertes 20/40, 0/10, 0/50, 10/100 | Ordre : `ART-0002` (0/50), `ART-0003` (0/10), 10/100, 20/40 | `StockAlertPolicyTest.mostUrgentFirst_severalAlerts_sortsByCoverageThenByCode` | ☐ |
| 1.6 | Page des stocks (vendeuse), le serveur renvoie l'alerte 12,5 / 40 | Bloc « 1 article(s) sous le seuil minimum », cellules « 12.5 \| 40 \| 27.5 \| 30 » | `stock-page.spec.ts` › « affiche les articles sous le seuil avec stock, seuil, manque et quantité suggérée (STK-04) » | ☐ |
| 1.7 | Composant : colonnes et unité | En-têtes Article / Stock actuel / Seuil / Manque / Quantité suggérée ; « 30 Pièce » ; pastille « Sous le seuil » | `threshold-alerts.spec.ts` › « affiche stock actuel, seuil, manque et quantité suggérée avec son unité » | ☐ |
| 1.8 | Composant : stock nul | Pastille « En rupture », cellules « 0 \| 15 \| 15 \| 20 » | `threshold-alerts.spec.ts` › « signale une rupture quand le stock est nul » | ☐ |
| 1.9 | Service front | `GET /api/v1/stocks/alerts?page=1&size=100`, sans `sortBy` | `stock-service.spec.ts` › « lit les 100 alertes de seuil les plus urgentes sans tri imposé (STK-04) » | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `StockSecurityIT.getAlerts_withoutToken_returns401` | ☐ |
| Livreur (`DELIVERY_DRIVER`, sans `stocks.read`) | 403 `FORBIDDEN` | `StockSecurityIT.getAlerts_withDeliveryDriverRole_returns403` | ☐ |
| Vendeuse (`stocks.read`) | 200 | `StockSecurityIT.getAlerts_withSellerRole_returns200` | ☐ |
| `size=101` | 422 `VALIDATION_FAILED` | `StockAlertIT.getAlerts_sizeAbove100_returns422` | ☐ |
| `page=0` | 422 `VALIDATION_FAILED` | `StockAlertIT.getAlerts_pageZero_returns422` | ☐ |
| Écriture (`POST`/`PUT`/`PATCH`/`DELETE`) | non exposée | `StockSecurityIT.stocks_writeVerbs_areNotExposed` (STK-01) | ☐ |

Non applicable : mass assignment, 404 et 409 (lecture seule, pas d'identifiant dans l'URL, aucune écriture ni journal).

## Cas limites

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Bornes : stock 0 / 39,999 / 40 / 41 pour un seuil 40 ; seuil 0 avec stock 0 ou 5 | Alerte seulement strictement sous un seuil > 0 | `StockAlertPolicyTest.isUnderThreshold_bounds_alertOnlyStrictlyUnderAPositiveThreshold` | ☐ |
| Arrondi : manque 0,5 / 3 / 10 / 10,001 / 12,5 / 40 / 95 | 10 / 10 / 10 / 20 / 20 / 40 / 100 | `StockAlertPolicyTest.suggestedQuantity_shortage_roundsUpToTheNextTenWithAMinimumOfTen` | ☐ |
| Stock égal au seuil (40 / 40) | Pas d'alerte | `StockAlertIT.getAlerts_stockEqualToThreshold_isNotAnAlert` | ☐ |
| Seuil 0 | Jamais d'alerte | `StockAlertIT.getAlerts_thresholdZero_isNeverAnAlert` | ☐ |
| Article désactivé sous son seuil | Absent de la liste | `StockAlertIT.getAlerts_inactiveArticle_isLeftOut` | ☐ |
| Pagination : page 2, taille 1 | La moins urgente, `totalPages` 2 | `StockServiceImplAlertTest.getAlerts_secondPageOfSizeOne_returnsTheLessUrgentAlert` | ☐ |
| Page au-delà de la fin | `items` vide, `totalCount` conservé | `StockServiceImplAlertTest.getAlerts_pagePastTheEnd_returnsEmptyItemsWithTheTotal` | ☐ |
| Recherche « ` art-0002 ` » (espaces, casse) | Seul `ART-0002` | `StockServiceImplAlertTest.getAlerts_search_keepsOnlyTheMatchingCodeOrName` | ☐ |
| Aucun article avec seuil (liste vide) | Page vide, aucun calcul de solde | `StockServiceImplAlertTest.getAlerts_noArticleWithThreshold_returnsEmptyPageWithoutReadingBalances` | ☐ |
| Aucune alerte côté page | Bloc d'alertes absent | `stock-page.spec.ts` › « n'affiche pas le bloc d'alertes quand aucun article n'est sous son seuil » | ☐ |
| Plus de 100 alertes (`totalCount` 140) | Ordre du serveur conservé, « 140 article(s) », « Les 2 plus urgents sont affichés » | `threshold-alerts.spec.ts` › « garde l'ordre du serveur et indique quand seules les plus urgentes sont affichées » | ☐ |
| Hors ligne / erreur serveur sur `/alerts` | Le bloc d'alertes ne s'affiche pas, le reste de la page fonctionne (lecture seule, pas de file `Idempotency-Key`) | — (recette manuelle) | ☐ |

## Tests existants

- Back : `StockAlertPolicyTest`, `StockServiceImplAlertTest` (unitaires) ; `StockAlertIT`, `StockSecurityIT` (Testcontainers).
- Front : `threshold-alerts.spec.ts`, `stock-page.spec.ts`, `stock-service.spec.ts`.
- Non-régression STK-01/STK-03 : `StockReadIT`, `StockValuationIT`, `StockServiceImplTest` (le support `StockApiTestSupport.createArticle` accepte maintenant un seuil, avec 40 par défaut).

## Recette manuelle (🧪)

1. En administrateur, créer un article de seuil 40 et lui donner 12,5 en stock : le bloc « article(s) sous le seuil minimum » apparaît sous les cartes d'emplacement avec 12,5 / 40 / 27,5 / 30.
2. Porter le stock à 40 : l'article disparaît du bloc ; si plus aucune alerte, le bloc disparaît.
3. Se connecter en vendeuse : même bloc visible. En livreur : `GET /api/v1/stocks/alerts` répond 403 (le serveur décide).

## Résultat de l'exécution

- Date : à lancer (CI ou local) — aucun test exécuté par le traiteur.
- Environnement : `mvn -o test-compile` réussi (back) ; `tsc -p tsconfig.spec.json` et `eslint src/app/features/stocks` sans erreur (front).
- Anomalies ouvertes : aucune connue.
