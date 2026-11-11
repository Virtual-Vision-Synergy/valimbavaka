# Scénarios de test — RPT-03 · Graphique d'évolution du CA

Statut : ✅ rédigé le 2026-11-12 · Ticket : [#100](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/100) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur `ADMIN` (a `reports.read`) ; vendeuse et livreur refusés (cas de RPT-01) |
| Données de départ | Celles de RPT-01 : journée du jour ouverte, deux articles à 8 500 Ar de deux gammes, vendeuse, canaux Boutique et Facebook. Exemple : en novembre 2026, 8 500 Ar le 3 et 17 000 Ar le 20 |
| Tickets à avoir déployés avant | RPT-01 (`ReportPeriod`, `ReportFilter`, `ReportSaleRepository`) |

Les tests d'intégration partagent la base avec d'autres classes : aucune assertion ne suppose que la base ne contient que ses données (différences avant/après du point du jour, somme de la série comparée au CA de la même réponse, ou période sans aucune vente comme l'an 2000).

## Scénarios issus des critères d'acceptation

Critère : « Le graphique reflète les valeurs journalières de la période choisie. » Série : un point par jour pour un jour ou un mois, un point par mois pour une année.

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Mois : un point par jour, trous à 0 | Novembre 2026, ventes le 3 (8 500) et le 20 (17 000) | 30 points du 01/11 au 30/11 ; le 3 vaut 8 500, le 20 vaut 17 000, les autres 0 | `ReportSalesServiceImplTest.getTrend_month_hasOnePointPerDayAndZeroForDaysWithoutSale` | ☐ |
| 2 | Année : un point par mois | 2026, ventes le 2 et le 30 mars (1 000 et 2 500,50), le 31 décembre (700) | 12 points au 1er de chaque mois ; mars = 3 500,50 ; décembre = 700 ; janvier = 0 | `ReportSalesServiceImplTest.getTrend_year_sumsTheDaysOfEachMonthIntoOnePoint` | ☐ |
| 3 | Jour : un seul point | Le 11/11/2026 sans vente | 1 point à 0 | `ReportSalesServiceImplTest.getTrend_day_isASinglePoint` | ☐ |
| 4 | Filtre gamme | Gamme Botanika, novembre | Lecture de `dailyRevenueByRange` (CA des lignes de la gamme) | `ReportSalesServiceImplTest.getTrend_withRange_readsTheDailyRevenueOfThatRange` | ☐ |
| 5 | Points de la période | Février 2026 ; année 2026 ; un jour | 28 jours ; 12 mois au 1er du mois ; 1 jour | `ReportPeriodTest.points_month_isEveryDayOfTheMonthWithoutGap`, `points_year_isTheFirstDayOfEachOfTheTwelveMonths`, `points_day_isThatSingleDay` | ☐ |
| 6 | Rattachement d'un jour à son point | 17 juin dans une année ; dans un mois | 1er juin ; le 17 juin lui-même | `ReportPeriodTest.pointOf_yearAndMonth_isTheMonthStartOrTheDayItself` | ☐ |
| 7 | Mêmes filtres que la synthèse | Mars 2026, canal et gamme choisis | La série est lue sur mars 2026 avec le même canal et la même gamme ; `trend` = série renvoyée | `ReportServiceImplTest.getReport_trend_isTheCurveOfThePeriodWithTheSameFilters` | ☐ |
| 8 | Mois de bout en bout | Deux ventes aujourd'hui (8 500 en boutique, 17 000 sur Facebook) | Le point d'aujourd'hui augmente de 25 500 ; 28 à 31 points ; premier point = 1er du mois ; somme des points = `sales.revenue` | `ReportIT.getReport_monthTrend_hasOnePointPerDayAndItsSumIsTheRevenue` | ☐ |
| 9 | Année de bout en bout | Une vente de 8 500 aujourd'hui | Le point du mois en cours augmente de 8 500 ; 12 points commençant au 1er janvier ; somme = CA | `ReportIT.getReport_yearTrend_hasTwelveMonthPointsThatIncludeTheSale` | ☐ |
| 10 | Jour de bout en bout | `granularity=DAY`, une vente de 8 500 | Un seul point (aujourd'hui) qui augmente de 8 500 | `ReportIT.getReport_dayTrend_isASinglePointOfTheDay` | ☐ |
| 11 | Filtres canal et gamme de bout en bout | Vente Facebook (savon + crème), vente boutique (savon) | Série Facebook + gamme savon : +8 500 ; série gamme crème : +17 000 | `ReportIT.getReport_trendWithFilters_appliesTheChannelAndTheRange` | ☐ |
| 12 | Série continue sans vente | Février 2000 ; année 2000 | 29 points à 0 du 2000-02-01 au 2000-02-29 ; 12 points à 0 | `ReportIT.getReport_pastPeriodWithoutSale_hasAContinuousTrendOfZeros` | ☐ |
| 13 | Écran : une barre par jour | L'API renvoie 3 jours (0, 8 500, 17 000) | 3 colonnes, hauteurs 0 %, 42,5 %, 85 % (échelle 20 000) ; tableau de données de 3 lignes | `reports-page.spec.ts` « should draw one bar per day of the period, a day without sale being an empty bar » | ☐ |
| 14 | Écran : une barre par mois | Période « Année », 12 points | 12 colonnes ; en-tête de colonne « Mois » | `reports-page.spec.ts` « should draw one bar per month for a year, with a month column in the data table » | ☐ |

## Règles métier

| Règle | Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Ventes annulées exclues | Vente `CANCELLED` | Le point du jour ne bouge pas | `ReportIT.getReport_cancelledSale_isNotInTheTrend` | ☐ |
| Série bornée par la période | Mois ou année | Une seule requête SQL `between from and to`, au plus 366 lignes lues | `ReportSalesServiceImplTest.getTrend_month_hasOnePointPerDayAndZeroForDaysWithoutSale` (`verifyNoMoreInteractions`) | ☐ |
| Mêmes règles de CA que la synthèse | Somme de la série | Somme des points = `sales.revenue` (sans filtre de gamme) | `ReportIT.getReport_monthTrend_hasOnePointPerDayAndItsSumIsTheRevenue`, `getReport_yearTrend_hasTwelveMonthPointsThatIncludeTheSale` | ☐ |

## Sécurité et erreurs

Même endpoint que RPT-01, aucun nouveau paramètre ni corps : `ReportIT.getReport_withoutToken_returns401`, `getReport_sellerAndDeliveryDriver_return403`, `getReport_futureDay_returns422`, `getReport_unknownGranularity_returns400`, `getReport_malformedDateOrId_returns400` couvrent aussi la série (même réponse).

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Période qui n'a pas commencé | 422, aucune série lue | `ReportServiceImplTest.getReport_dayInTheFuture_doesNotReadTheCurve` | ☐ |
| Sans jeton, vendeuse, livreur | 401, 403 | `ReportIT` (RPT-01) | ☐ |
| Mass assignment, 404, 409 | Sans objet : lecture seule | — | — |

## Cas limites et hors ligne

- Mois en cours : la série va jusqu'à la fin du mois, les jours à venir valent 0 ; l'écran met en gras le jour d'aujourd'hui.
- Année : un point par mois, daté du 1er du mois ; février bissextile compte 29 jours dans la série d'un mois.
- Gamme inconnue ou désactivée : série à 0 (pas d'erreur), comme la synthèse.
- Lecture seule : pas de file hors ligne ; hors ligne, la section affiche l'état d'erreur avec « Réessayer » (même ressource que la synthèse).
- Accessibilité : barres décoratives, tableau équivalent pour les lecteurs d'écran (`/reports` dans `demo-pages.a11y.spec.ts`).

## Tests existants

- Backend : `ReportPeriodTest`, `ReportSalesServiceImplTest`, `ReportServiceImplTest`, `ReportIT` (complétés).
- Frontend : `reports-page.spec.ts` (complété), `report-service.spec.ts` (réponse enrichie), `features/demo-pages.a11y.spec.ts` (réponse `/api/v1/reports` enrichie, changement minimal).

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Admin, « Rapports », période « Mois » | Une barre par jour du mois, le jour d'aujourd'hui en relief, les jours sans vente à 0 | 🧪 recette |
| Passer sur « Année » | Douze barres, une par mois (janv. à déc.) | 🧪 recette |
| Passer sur « Jour » | Une seule barre | 🧪 recette |
| Choisir un canal puis une gamme | La courbe se recalcule avec les mêmes filtres que la synthèse | 🧪 recette |
| Comparer la somme des barres au CA de la synthèse | Identiques (hors filtre de gamme : voir RPT-01) | 🧪 recette |

## Résultat de l'exécution
- Date : non exécuté.
- Environnement : à lancer (CI ou local).
- Anomalies ouvertes : aucune connue.
