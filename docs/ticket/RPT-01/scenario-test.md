# Scénarios de test — RPT-01 · Rapports filtrables journaliers/mensuels/annuels

Statut : ✅ rédigé le 2026-11-11 · Ticket : [#98](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/98) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur `ADMIN` (a `reports.read`) ; vendeuse `SELLER` et livreur `DELIVERY_DRIVER` refusés (pas de `reports.read`) |
| Données de départ | Journée du jour **ouverte** (JR-01) ; deux articles finis à 8 500 Ar, « Savon » (gamme A) et « Crème » (gamme B), 50 unités chacun en Boutique ; vendeuse Miora Rakoto ; canaux Boutique et Facebook |
| Tickets à avoir déployés avant | VTE-01 (ventes), VTE-07/VTE-08 (`reports.read`, ventes `VALIDATED`, `business_date`), SYS-05 (canaux, gammes) |

Les tests d'intégration partagent la base avec d'autres classes : chaque assertion est une **différence** entre deux lectures du même rapport, jamais une valeur absolue.

## Scénarios issus des critères d'acceptation

Critère : « Le rapport se met à jour selon la période et les filtres gamme/canal. »

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Mois sans filtre | 1 Savon en Boutique (8 500) et 2 Crèmes sur Facebook (17 000) ; `GET /api/v1/reports?granularity=MONTH` | CA du mois +25 500, ventes +2 | `ReportIT.getReport_monthWithoutFilter_sumsEveryValidatedSale` | ✅ auto |
| 2 | Filtre canal | 1 Savon en Boutique, 2 Savons sur Facebook ; `&salesChannelId=<Facebook>` | CA +17 000, ventes +1 (la vente Boutique est exclue) | `ReportIT.getReport_channelFilter_countsOnlyThatChannel` | ✅ auto |
| 3 | Filtre gamme | Une vente de 1 Savon (gamme A) et 2 Crèmes (gamme B) ; `&rangeId=<A>` puis `<B>` | Gamme A : CA +8 500 ; gamme B : CA +17 000 ; la vente compte 1 fois dans chaque gamme | `ReportIT.getReport_rangeFilter_countsOnlyTheLinesOfThatRange` | ✅ auto |
| 4 | Canal et gamme ensemble | Boutique-Crème, Facebook-Crème, Boutique-Savon ; `&salesChannelId=<Boutique>&rangeId=<B>` | CA +8 500 (seule la première vente) | `ReportIT.getReport_channelAndRangeTogether_areBothApplied` | ✅ auto |
| 5 | Jour, mois, année | 1 Savon vendu aujourd'hui ; `granularity=DAY`, `MONTH`, `YEAR` | La même vente s'ajoute (+8 500) aux trois rapports | `ReportIT.getReport_dayMonthAndYear_includeTheSameSale` | ✅ auto |
| 6 | Valeurs par défaut | Aucun paramètre | 200 ; `granularity` `MONTH`, `from`/`to` renseignés, aucun filtre (`salesChannelId` nul) | `ReportIT.getReport_withoutParameters_isTheCurrentMonth` | ✅ auto |
| 7 | Période par défaut en heure de Madagascar | Horloge fixée à 21:30 UTC le 30/11 (= 00:30 le 1/12 à Madagascar) | Rapport de décembre 2026 : du 2026-12-01 au 2026-12-31 | `ReportServiceImplTest.getReport_withoutParameters_isTheCurrentMonthInMadagascarTime` | ✅ auto |
| 8 | Bornes de la période | Jour 2026-11-11 ; mois de novembre ; février 2028 (bissextile) ; année 2026 | Jour 11/11–11/11 ; 01/11–30/11 ; fin au 29/02/2028 ; 01/01–31/12 | `ReportPeriodTest.containing_day_isThatSingleDay`, `containing_month_spansTheCalendarMonth`, `containing_februaryOfALeapYear_endsOnThe29th`, `containing_year_spansTheCalendarYear` | ✅ auto |
| 9 | Période précédente (socle de RPT-02) | Jour 01/01/2026 ; mois de mars ; année 2026 | 31/12/2025 ; février 2026 (01–28) ; année 2025 | `ReportPeriodTest.previous_dayOnNewYearsDay_isDecember31`, `previous_month_isThePreviousCalendarMonth`, `previous_year_isThePreviousCalendarYear` | ✅ auto |
| 10 | Agrégat sans gamme | Filtre canal Boutique, 30 000 Ar en 3 ventes | Requête `totals` ; CA 30 000, 3 ventes, panier moyen 10 000 | `ReportSalesServiceImplTest.getSales_withoutRange_readsTheTotalsOfValidatedSalesOfThePeriod` | ✅ auto |
| 11 | Agrégat avec gamme | Filtre gamme A, 17 000 Ar en 2 ventes | Requête `totalsByRange` | `ReportSalesServiceImplTest.getSales_withRange_readsTheTotalsOfThatRange` | ✅ auto |
| 12 | Écran : synthèse | L'admin ouvre `/reports` ; l'API renvoie 25 500 Ar, 2 ventes, panier 12 750 | Section « Synthèse de la période » : « Du 2026-11-01 au 2026-11-30 » et trois indicateurs | `reports-page.spec.ts` « should show the revenue, the number of sales and the basket of the current month » | ✅ auto |
| 13 | Écran : filtres | L'admin choisit un canal puis une gamme | Nouvel appel avec `salesChannelId`, puis avec `salesChannelId` et `rangeId` | `reports-page.spec.ts` « should reload the report with the selected channel and range » | ✅ auto |
| 14 | Écran : période et date | L'admin choisit « Année », puis la date 2026-10-15 | Appels avec `granularity=YEAR`, puis `date=2026-10-15` | `reports-page.spec.ts` « should reload the report for a year when the period changes », « should reload the report for the chosen date » | ✅ auto |
| 15 | Écran : réinitialiser | Un canal est choisi, puis « Réinitialiser » | Nouvel appel sans `salesChannelId` | `reports-page.spec.ts` « should clear the channel and range filters on reset » | ✅ auto |
| 16 | Appel HTTP | Paramètres complets, puis filtres vides | Paramètres d'URL envoyés ; filtres vides omis | `report-service.spec.ts` « sends the granularity, the date and the filters as query parameters », « omits empty filters so that the server applies its defaults » | ✅ auto |
| 17 | Accessibilité | Page `/reports` chargée | Aucune violation axe | `demo-pages.a11y.spec.ts` (`reports`) | ✅ auto |

## Règles métier

| Règle | Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Seules les ventes `VALIDATED` comptent | Une vente passe à `CANCELLED` | Le CA du mois ne bouge pas | `ReportIT.getReport_cancelledSale_isNotCounted` | ✅ auto |
| Aucune vente sur la période | Année 2000 | 200, CA 0, ventes 0, panier moyen 0 (pas de division par zéro) | `ReportIT.getReport_pastPeriodWithoutSale_returnsZeros`, `ReportSalesServiceImplTest.getSales_noSale_returnsZerosWithoutDividingByZero` | ✅ auto |
| Panier moyen arrondi au centime | 10 000 Ar en 3 ventes | 3 333,33 | `ReportSalesServiceImplTest.getSales_basketNotRound_isRoundedToTheCent` | ✅ auto |
| Filtre sur un identifiant inconnu | `rangeId` inexistant | 200, rapport à zéro (pas d'erreur) | `ReportIT.getReport_unknownRange_returnsZeros` | ✅ auto |
| Les filtres sont rappelés dans la réponse | Année 2025 avec canal et gamme | La réponse porte `salesChannelId` et `rangeId` et la période effective | `ReportServiceImplTest.getReport_yearWithFilters_passesThePeriodAndFiltersToTheAggregate` | ✅ auto |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `ReportIT.getReport_withoutToken_returns401` | ✅ auto |
| Vendeuse ou livreur | 403 `FORBIDDEN` | `ReportIT.getReport_sellerAndDeliveryDriver_return403` | ✅ auto |
| Granularité inconnue (`WEEK`) | 400 `MALFORMED_REQUEST` | `ReportIT.getReport_unknownGranularity_returns400` | ✅ auto |
| Date ou identifiant mal formé | 400 | `ReportIT.getReport_malformedDateOrId_returns400` | ✅ auto |
| Période qui n'a pas commencé (jour futur, mois suivant) | 422 `VALIDATION_FAILED` | `ReportIT.getReport_futureDay_returns422`, `ReportServiceImplTest.getReport_dayInTheFuture_throwsBusinessException`, `getReport_monthAfterTheCurrentOne_throwsBusinessException` | ✅ auto |
| Jour futur d'un mois en cours | Accepté (la période a commencé) | `ReportServiceImplTest.getReport_dayLaterInTheCurrentMonth_isAccepted` | ✅ auto |
| Mass assignment | Sans objet : lecture seule, aucun corps | — | — |
| 404 / 409 | Sans objet : aucune ressource nommée, aucune écriture | — | — |

## Cas limites et hors ligne

- Aucun agrégat sans borne : la période est toujours un jour, un mois ou une année civile.
- Une vente à deux gammes compte dans chacune ; une vente à deux lignes de la même gamme ne compte qu'une fois pour cette gamme (`count(distinct)`).
- Gamme filtrée : le CA est la somme des lignes de la gamme **avant remise globale** de la vente (la remise de vente n'est pas répartie par ligne) ; sans filtre gamme, c'est le total de la vente après remise.
- Lecture seule : pas de file hors ligne ni d'`Idempotency-Key` ; hors ligne, la synthèse affiche l'état d'erreur avec « Réessayer ».
- Jour = `sales.business_date` en `Indian/Antananarivo`, jamais la date UTC de création.

## Tests existants

- Backend : `feature/report/domain/ReportPeriodTest.java`, `feature/report/service/impl/ReportSalesServiceImplTest.java`, `feature/report/service/impl/ReportServiceImplTest.java`, `feature/report/controller/ReportIT.java`.
- Frontend : `features/reports/data-access/report-service.spec.ts`, `features/reports/pages/reports-page/reports-page.spec.ts`, `features/demo-pages.a11y.spec.ts`.

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Connecté en admin, ouvrir « Rapports » | Synthèse du mois en cours (CA, ventes, panier moyen), période affichée | 🧪 recette |
| Passer sur « Jour » puis « Année », changer la date | Synthèse recalculée, période effective affichée | 🧪 recette |
| Choisir un canal, puis une gamme, puis « Réinitialiser » | Chiffres recalculés à chaque choix ; retour au total | 🧪 recette |
| Choisir une date future | Le sélecteur refuse les dates postérieures à aujourd'hui | 🧪 recette |
| Connecté en vendeuse | Entrée « Rapports » absente ; `/reports` redirige | 🧪 recette |

## Résultat de l'exécution

- Date : 2026-11-11.
- Environnement : CI GitHub (`ci.yml`, `conventions.yml`, `qodana_code_quality.yml`) sur le commit `70759c09` : backend 932 tests, 0 échec (dont 5 `ReportServiceImplTest`, 4 `ReportSalesServiceImplTest`, 8 `ReportPeriodTest` et 14 `ReportIT`) ; frontend 136 fichiers de test, 850 tests verts (dont `report-service.spec.ts`, `reports-page.spec.ts` et l'accessibilité de `/reports`) ; Spotless/Checkstyle, Prettier/ESLint, gitleaks verts ; Qodana for JVM : 0 annotation.
- Anomalies ouvertes : aucune connue ; sections de comparaison, courbe, gammes et classement encore en démonstration (RPT-02 à RPT-05).
