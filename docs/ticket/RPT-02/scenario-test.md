# Scénarios de test — RPT-02 · Indicateurs comparés à la période précédente

Statut : ✅ rédigé le 2026-11-12 · Ticket : [#99](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/99) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur `ADMIN` (a `reports.read`) ; vendeuse `SELLER` et livreur `DELIVERY_DRIVER` refusés |
| Données de départ | Celles de RPT-01 : journée du jour ouverte, deux articles à 8 500 Ar de deux gammes, vendeuse, canaux Boutique et Facebook. Exemple de comparaison : novembre 2026 = 1 200 Ar en 6 ventes (panier 200), octobre 2026 = 1 000 Ar en 4 ventes (panier 250) |
| Tickets à avoir déployés avant | RPT-01 (`ReportPeriod.previous()`, `ReportSalesService`) |

Les tests d'intégration partagent la base avec d'autres classes : aucune assertion ne suppose que la base ne contient que ses données (différences avant/après, ou période sans aucune vente comme l'an 2000).

## Scénarios issus des critères d'acceptation

Critère : « Chaque indicateur affiche sa valeur et sa variation en %. » Indicateurs : chiffre d'affaires, ventes validées, panier moyen (voir « Points d'attention » du guide pour les livraisons).

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Hausse et baisse | Novembre : 1 200 Ar, 6 ventes, panier 200 ; octobre : 1 000 Ar, 4 ventes, panier 250 | Variations : CA +20,0 %, ventes +50,0 %, panier −20,0 % ; `comparison.sales` = valeurs d'octobre | `ReportServiceImplTest.getReport_risingIndicators_giveAPositivePercentageOfThePreviousValue` | ☐ |
| 1b | Période précédente et mêmes filtres | Mars 2026, canal et gamme choisis | Seconde lecture sur février 2026 (01/02 au 28/02) avec le même canal et la même gamme ; `comparison.from`/`to` = 2026-02-01 / 2026-02-28 | `ReportServiceImplTest.getReport_month_comparesWithThePreviousMonthWithTheSameFilters` | ☐ |
| 1c | Pourcentage calculé | 24 600 000 contre 21 540 000 ; 750 contre 1 000 ; 5 contre 5 | +14,2 % ; −25,0 % ; 0,0 % | `ReportVariationTest.percent_rise_isPositive`, `percent_fall_isNegative`, `percent_sameValue_isZero` | ☐ |
| 1d | Période précédente de bout en bout | `GET /api/v1/reports?granularity=DAY` ; une vente est créée aujourd'hui | La période précédente est la veille (`comparison.from` = `comparison.to` = hier) et son CA ne bouge pas | `ReportIT.getReport_comparison_isThePreviousPeriodWithItsOwnIndicators` | ☐ |
| 1e | Variation cohérente avec les deux périodes | Une vente aujourd'hui ; `granularity=DAY` | `variation.revenue` = (CA − CA précédent) / CA précédent × 100 à 0,1 près ; nulle si la veille est à 0 | `ReportIT.getReport_comparison_variationIsConsistentWithBothPeriods` | ☐ |
| 1f | Filtres appliqués à la période précédente | Mars 2000, gamme inconnue | 200 ; `comparison` du 01/02/2000 au 29/02/2000 (année bissextile), 0 vente | `ReportIT.getReport_comparisonWithFilters_appliesTheSameFiltersToThePreviousPeriod` | ☐ |
| 1g | Écran : valeur, variation et valeur précédente | L'API renvoie CA 25 500 (+27,5 %), 2 ventes (−50 %), panier 12 750 (+155 %) | Trois cartes avec badges « +27,5 % », « −50,0 % », « +155,0 % », une ligne « vs … période précédente » par carte et la période précédente affichée | `reports-page.spec.ts` « should show each indicator with its variation in percent against the previous period » | ☐ |
| 1h | Écran : pas de pourcentage | Période précédente à 0 (variations `null`) | Badges « n.d. », jamais « NaN » ni « Infinity » | `reports-page.spec.ts` « should not show a percentage when the previous period has no sale » | ☐ |

## Règles métier

| Règle | Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Division par zéro | Période précédente à 0 Ar, 0 vente | Variations `null` (pas de pourcentage), valeurs précédentes à 0 | `ReportServiceImplTest.getReport_previousPeriodWithoutSale_hasNoPercentage`, `ReportVariationTest.percent_previousZero_isNull`, `ReportIT.getReport_periodAndPreviousWithoutSale_hasNoPercentage` | ☐ |
| Aucune vente des deux côtés | 0 contre 0 | Variations `null` | `ReportServiceImplTest.getReport_bothPeriodsWithoutSale_hasNoPercentage` | ☐ |
| Ventes arrêtées | 0 Ar contre 1 000 Ar | −100,0 % | `ReportServiceImplTest.getReport_salesStoppedAfterAPeriodWithSales_isMinusOneHundredPercent`, `ReportVariationTest.percent_countsDoublingAndCollapse` | ☐ |
| Arrondi à une décimale, demi vers le haut | 9 contre 8 ; 17 contre 16 | +12,5 % ; +6,3 % (6,25 arrondi) | `ReportVariationTest.percent_halfWay_roundsHalfUpToOneDecimal` | ☐ |

## Sécurité et erreurs

Même endpoint que RPT-01, aucun nouveau paramètre ni corps : les cas de RPT-01 (`ReportIT.getReport_withoutToken_returns401`, `getReport_sellerAndDeliveryDriver_return403`, `getReport_futureDay_returns422`, `getReport_unknownGranularity_returns400`, `getReport_malformedDateOrId_returns400`) couvrent aussi la comparaison (même réponse).

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `ReportIT.getReport_withoutToken_returns401` | ☐ |
| Vendeuse ou livreur | 403 `FORBIDDEN` | `ReportIT.getReport_sellerAndDeliveryDriver_return403` | ☐ |
| Période qui n'a pas commencé | 422 `VALIDATION_FAILED` (aucune comparaison calculée) | `ReportIT.getReport_futureDay_returns422`, `ReportServiceImplTest.getReport_dayInTheFuture_throwsBusinessException` | ☐ |
| Mass assignment, 404, 409 | Sans objet : lecture seule, aucun corps | — | — |

## Cas limites et hors ligne

- Période en cours : le mois ou l'année en cours est comparé **en entier** au mois ou à l'année précédente en entier (pas de comparaison « à date égale »).
- Première période d'activité : la période précédente est à 0, donc aucun pourcentage (`null`), l'écran affiche « n.d. ».
- Une vente validée aujourd'hui ne modifie jamais la période précédente.
- Février bissextile : mars 2028 se compare à février 2028 (29 jours) ; jour du 1er janvier : veille = 31 décembre (voir `ReportPeriodTest`, RPT-01).
- Lecture seule : pas de file hors ligne ni d'`Idempotency-Key` ; hors ligne, la section affiche l'état d'erreur avec « Réessayer ».
- Fuseau : les deux périodes se lisent sur `sales.business_date` (jour métier `Indian/Antananarivo`).

## Tests existants

- Backend : `feature/report/domain/ReportVariationTest.java` (nouveau), `feature/report/service/impl/ReportServiceImplTest.java` et `feature/report/controller/ReportIT.java` (complétés) ; `ReportPeriodTest` (RPT-01) couvre `previous()`.
- Frontend : `features/reports/pages/reports-page/reports-page.spec.ts` (complété), `features/reports/data-access/report-service.spec.ts` (réponse enrichie), `features/demo-pages.a11y.spec.ts` (réponse `/api/v1/reports` enrichie).

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Admin, « Rapports », période « Mois » | Trois cartes (CA, ventes, panier moyen) avec la valeur, un badge de variation en % et « vs … période précédente » ; la période précédente est écrite sous la période | 🧪 recette |
| Passer sur « Jour » puis « Année » | Comparaison avec la veille, puis avec l'année précédente | 🧪 recette |
| Choisir un canal puis une gamme | Valeurs et variations recalculées avec le même filtre des deux côtés | 🧪 recette |
| Période dont la précédente est vide (par ex. première année d'activité) | Badges « n.d. », valeur précédente à 0 Ar, aucun « NaN » | 🧪 recette |

## Résultat de l'exécution

- Date : 2026-11-12.
- Environnement : à lancer (CI ou local) ; rien n'a été exécuté au moment de la rédaction.
- Anomalies ouvertes : aucune connue.
