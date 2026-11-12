# Scénarios de test — RPT-05 · Classement des produits les plus vendus

Statut : ✅ rédigé et exécuté le 2026-11-12 · Ticket : [#102](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/102) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur `ADMIN` (a `reports.read`) ; vendeuse et livreur refusés (cas de RPT-01, même endpoint) |
| Données de départ | Journée du jour ouverte, vendeuse, canaux Boutique et Facebook, articles à 8 500 Ar (coût d'achat 3 120,50 Ar) d'une gamme créée par le test puis désactivée. Exemple : une vente boutique de 3 savons, 1 huile et 1 crème = 25 500 + 8 500 + 8 500 = 42 500 Ar |
| Tickets à avoir déployés avant | RPT-01 (`ReportPeriod`, `ReportFilter`, `ReportSaleRepository`), RPT-02 à RPT-04 (`ReportRDto` enrichi) |

Les tests d'intégration partagent la base avec d'autres classes : tous les articles de `ReportRankingIT` appartiennent à une gamme créée par la classe, et le filtre gamme les isole du reste de la base (aucune assertion ne suppose que la base ne contient que ses données).

## Scénarios issus des critères d'acceptation

Critère : « Le tableau est trié par CA décroissant avec quantité, marge et % du CA. »

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Tri par CA décroissant, colonnes | Une vente : 3 savons, 1 huile, 1 crème (gamme du test) | `ranking.items` : savon (quantité 3, CA 25 500, marge 16 138,50 = 25 500 − 3 × 3 120,50, part 60,0), puis huile et crème à 20,0 ; `ranking.revenue` 42 500 ; `size` 10 | `ReportRankingIT.getReport_ranking_sortsByRevenueWithQuantityMarginAndShare` | ✅ auto |
| 1b | Calcul : quantité décimale, marge, part | Savon 3 (9 000 Ar, coût 3 000) et huile 1,5 (4 000 Ar, coût 1 000,50) sur 16 000 Ar de lignes | Marges 6 000 et 2 999,50 ; parts 56,3 % (56,25 arrondi) et 25,0 % : la part est sur le CA de toutes les lignes (16 000), pas sur les 13 000 listés | `ReportSalesServiceImplTest.getRanking_twoArticles_givesQuantityRevenueMarginAndShareOfAllTheLines` | ✅ auto |
| 1c | Égalité de CA : ordre stable | Deux articles à 8 500 Ar, créés dans l'ordre inverse de leurs noms | Le nom le plus petit d'abord (puis le code) | `ReportRankingIT.getReport_ranking_tiedRevenueIsOrderedByName` | ✅ auto |
| 1d | Coût inconnu : pas de marge | Article sans coût d'achat, 1 vendu | `margin` = `null` (jamais 100 % de marge) ; à l'écran « — » et texte lu par les lecteurs d'écran | `ReportRankingIT.getReport_ranking_unknownPurchaseCost_hasNoMargin`, `ReportSalesServiceImplTest.getRanking_unknownCostOnALine_hasNoMargin`, `reports-ranking.spec.ts` « should show a dash and a spoken explanation when the margin is unknown » | ✅ auto |
| 1e | Taille bornée, base des parts | 11 articles à 8 500 Ar dans une vente | 10 lignes seulement ; `ranking.revenue` 93 500 ; part du premier 9,1 (8 500 / 93 500, pas 8 500 / 85 000) | `ReportRankingIT.getReport_ranking_isCutAtTenArticlesButTheSharesUseTheWholeTurnover` | ✅ auto |
| 1f | Rapport : bloc et filtres | `getReport` avec canal et gamme | `ranking` = résultat de `getRanking` avec la même période, le même canal et la même gamme | `ReportServiceImplTest.getReport_ranking_isTheRankingOfThePeriodWithTheSameFilters` | ✅ auto |
| 1g | Écran : tableau | L'API renvoie Savon noir (2,5 ; 17 000 ; marge 9 200,50 ; 66,7) et Huile de coco (marge `null`) | Une ligne par article dans l'ordre de l'API : rang, nom et code, quantité « 2,5 », CA, marge, « 66,7 % » ; aucune donnée de démonstration (« Gommage café ») ; pas de « NaN » | `reports-ranking.spec.ts` « should list the best-selling articles with quantity, revenue, margin and share », « should not keep any demonstration data in the ranking » | ✅ auto |

## Règles métier

| Règle | Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Ventes `VALIDATED` seulement | Une vente de 5 huiles passée à `CANCELLED` | L'huile n'apparaît pas ; `ranking.revenue` = 8 500 | `ReportRankingIT.getReport_ranking_cancelledSaleIsNotCounted` | ✅ auto |
| Filtre canal | Savon vendu en boutique, huile ×2 sur Facebook | Canal Facebook : huile seule (quantité 2, part 100,0) ; canal Boutique : savon seul | `ReportRankingIT.getReport_ranking_channelFilterKeepsOnlyThatChannel` | ✅ auto |
| Filtre gamme (s'applique ici) | Article d'une autre gamme que celle du filtre | Absent du classement | `ReportRankingIT.getReport_ranking_rangeFilterKeepsOnlyTheArticlesOfThatRange` | ✅ auto |
| Filtres rejoués à l'écran | Choisir le canal Facebook | Nouvel appel avec `salesChannelId=c-fb` ; classement vide : « Aucun article vendu sur la période » | `reports-ranking.spec.ts` « should ask the ranking again with the filters and show an empty state when nothing sold » | ✅ auto |
| Période sans vente | Année 2000 | `items` vide, `revenue` 0, pas de division par zéro | `ReportRankingIT.getReport_ranking_pastPeriodWithoutSale_isEmpty`, `ReportSalesServiceImplTest.getRanking_noSale_isEmptyWithZeroRevenue` | ✅ auto |
| CA nul | Article à 0 Ar, total 0 | Part 0,0 | `ReportSalesServiceImplTest.getRanking_zeroRevenueArticle_hasZeroShareWithoutDividingByZero` | ✅ auto |

## Sécurité et erreurs

Aucun nouvel endpoint : le classement est un bloc de `GET /api/v1/reports` (`reports.read`), sans corps ni écriture ni journal.

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `ReportRankingIT.getReport_ranking_withoutToken_returns401` | ✅ auto |
| Vendeuse ou livreur | 403 `FORBIDDEN` | `ReportIT.getReport_sellerAndDeliveryDriver_return403` (RPT-01, même endpoint) | ✅ auto |
| Période pas commencée | 422 (règle de RPT-01) | `ReportServiceImplTest.getReport_dayInTheFuture_doesNotReadTheCurve` (le classement n'est pas lu) | ✅ auto |

## Cas limites et hors ligne

- Égalité de CA : nom puis code, donc un ordre identique à chaque appel (cas 1c).
- 10 articles ou moins : tous listés ; 11 : coupé à 10 en base (`limit`), les parts restent sur le CA de toutes les lignes (cas 1e).
- Quantité décimale (1,5 ; 2,5) : trois décimales côté API, affichée sans zéro inutile.
- Pas de saisie ni de file hors ligne : lecture seule ; hors ligne, la section affiche l'erreur avec « Réessayer » (comme la synthèse).

## Tests existants

- Backend : `feature/report/service/impl/ReportSalesServiceImplTest`, `ReportServiceImplTest`, `feature/report/controller/ReportRankingIT` (nouveau), `ReportIT` (accès).
- Frontend : `features/reports/pages/reports-page/reports-ranking.spec.ts` (nouveau), `reports-page.spec.ts`, `data-access/report-service.spec.ts`, `features/demo-pages.a11y.spec.ts` (réponse `/api/v1/reports` avec `ranking` vide).

## Recette manuelle 🧪 (Njara)

| # | Étapes | Attendu | OK |
|---|---|---|---|
| R1 | Ouvrir `/reports`, période « Mois » | Section « Produits les plus vendus » : tableau trié par CA décroissant, au plus 10 lignes, avec quantité, CA, marge et % du CA | 🧪 |
| R2 | Choisir un canal, puis une gamme | Le tableau se restreint au canal et à la gamme | 🧪 |
| R3 | Période sans vente | État vide « Aucun article vendu sur la période » | 🧪 |
| R4 | Article vendu sans coût d'achat | Marge « — » | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-12
- Environnement : CI GitHub Actions sur la branche (ci.yml, conventions.yml, Qodana : verts, 0 annotation Qodana) ; backend 1038 tests dont `ReportRankingIT` 9 et `ReportSalesServiceImplTest` 16, frontend 142 fichiers de test (908 tests), tous passés. Recette manuelle 🧪 : à passer par Njara après le merge
- Anomalies ouvertes : aucune connue
