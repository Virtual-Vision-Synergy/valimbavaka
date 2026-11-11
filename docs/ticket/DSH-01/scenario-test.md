# Scénarios de test — DSH-01 · Cockpit journalier

Statut : ✅ tests automatiques verts en CI le 11/11/2026 — reste la recette manuelle (🧪) · Ticket : [#94](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/94) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · 🔍 code = vérifié par lecture · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main · ☐ = test écrit, pas encore exécuté (aucun restant). Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (`reports.read`) ; `SELLER` et `DELIVERY_DRIVER` pour les refus |
| Données de départ | journée du jour ouverte ; un article `SAV-001` à 8 500 Ar reçu en boutique (50 pièces) ; une vendeuse avec canal Boutique et commission 5 % ; un livreur ; ventes et commandes de livraison créées par le test |
| Tickets à avoir déployés avant | JR-01, JR-03, JR-04, JR-05, VTE-08, RPT-01, DSH-03 |

## Scénarios issus des critères d'acceptation

Critère : « Les 4 indicateurs clés sont affichés sur un seul écran. » (chiffre d'affaires, ventes, livraisons, écart de stock). Les tests d'agrégat comparent la lecture avant et après (la base est partagée entre classes de test).

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Les 4 indicateurs sur un écran | `ADMIN` ouvre `/dashboard`, vue d'ensemble | 4 cartes : CA, ventes, livraisons, écart de stock | `dashboard-kpis.spec.ts` « affiche les 4 indicateurs clés sur un seul écran » ; `dashboard-page.spec.ts` « affiche les 4 indicateurs lus sur l’API et la date de la journée du serveur » | ✅ auto |
| 1b | Le serveur renvoie les 4 blocs | `GET /api/v1/dashboard` en `ADMIN` | 200 avec `businessDate` du jour d'Antananarivo, `status` `OPEN`, `revenue`, `sales`, `deliveries`, `stockGap` | `DashboardIT.getDashboard_administrator_returnsTodayAndTheFourIndicators` ; `DashboardServiceImplTest.getDashboard_openDay_composesTheFourIndicators` | ✅ auto |
| 1c | CA et ventes suivent une vente | Une vente de 2 × 8 500 Ar | CA +17 000, ventes +1, canal Boutique +1 | `DashboardIT.getDashboard_newSale_raisesRevenueAndSalesCount` | ✅ auto |
| 1d | Une vente annulée sort des chiffres | Vente passée à `CANCELLED` | CA −8 500, ventes −1 | `DashboardIT.getDashboard_cancelledSale_leavesRevenueAndSalesCount` | ✅ auto |
| 1e | Livraisons du jour | 3 commandes du jour : 1 en tournée, 1 annulée, 1 à préparer | total +2 (annulée exclue), en tournée +1, à préparer +1, annulées +1 | `DashboardIT.getDashboard_ordersToday_countsOnRoundAndPendingButNotCancelledOnesInTheTotal` ; `DashboardServiceImplTest.getDashboard_openDay_composesTheFourIndicators` (total 15 hors 4 annulées) | ✅ auto |
| 1f | Écart de stock | Réception de 7 pièces d'un nouvel article hors des six composantes de la clôture | écart +7 unités, +1 article | `DashboardIT.getDashboard_stockEnteredOutsideTheSixComponents_raisesTheStockGap` | ✅ auto |
| 1g | Écart sur le comptage du soir (JR-05) | ligne théorique 8, comptée 5 ; ligne théorique 3, écart système 2 mais comptée 3 | −3 unités sur 1 article (le comptage prime sur le stock du système) | `DashboardServiceImplTest.getDashboard_countedLine_takesTheGapOfTheCountInsteadOfTheSystemStock` | ✅ auto |
| 1h | Même article manquant dans deux emplacements | −2 en boutique, +3 en livraison | somme des unités +1, article compté une seule fois | `DashboardServiceImplTest.getDashboard_sameArticleShortInTwoLocations_sumsUnitsButCountsTheArticleOnce` | ✅ auto |
| 1i | Évolution du CA | 15 500 Ar contre 10 000 Ar hier | `changePercent` = 55,0 | `DashboardServiceImplTest.getDashboard_openDay_composesTheFourIndicators` | ✅ auto |
| 1j | Évolution à la baisse | 6 000 Ar contre 9 000 Ar hier | `changePercent` = −33,3 (une décimale) ; carte « −33,3 % » avec un vrai signe moins | `DashboardServiceImplTest.getDashboard_revenueDrop_hasNegativeChangePercentRoundedToOneDecimal` ; `dashboard-kpis.spec.ts` « montre une baisse avec un vrai signe moins » | ✅ auto |
| 1k | Hier sans CA | 8 500 Ar contre 0 | `changePercent` = `null`, aucune tendance affichée | `DashboardServiceImplTest.getDashboard_noRevenueTheDayBefore_hasNoChangePercent` ; `dashboard-kpis.spec.ts` « n’affiche pas de tendance du chiffre d’affaires sans base de comparaison » | ✅ auto |
| 1l | Comparaison des ventes | 47 ventes contre 42 hier | tendance « +5 » sur la carte ventes | `dashboard-kpis.spec.ts` « compare le chiffre d’affaires et les ventes avec hier » | ✅ auto |
| 1m | Échecs et annulées | 2 échecs, 1 annulée | « 2 échecs » en tendance, « 1 annulée(s), hors total » | `dashboard-kpis.spec.ts` « montre les échecs de livraison et les annulées hors total » | ✅ auto |
| 1n | Répartition des ventes par canal | Boutique 26, Facebook 21, un canal désactivé à 0 | un élément de légende par canal, libellé de repli pour le canal désactivé | `dashboard-kpis.spec.ts` « liste chaque canal avec son nombre de ventes, même un canal désactivé » | ✅ auto |
| 1o | Journée non ouverte | pas de journée du jour | `status` `null`, `stockGap` `null`, la réconciliation n'est pas lue ; carte « — » sans « À justifier » | `DashboardServiceImplTest.getDashboard_dayNotOpenedYet_hasNoStatusNorStockGapAndReadsNoClosing` ; `dashboard-service.spec.ts` « should keep a day not opened yet as null status and null stock gap » ; `dashboard-kpis.spec.ts` « n’a pas d’écart de stock tant que la journée n’est pas ouverte » | ✅ auto |
| 1p | Écart nul | `units` = 0 | aucune alerte « À justifier » | `dashboard-kpis.spec.ts` « ne signale rien quand l’écart de stock est nul » | ✅ auto |
| 1q | Contrat front | `get()` | `GET /api/v1/dashboard` | `dashboard-service.spec.ts` « should read the four indicators of the day from the API » | ✅ auto |
| 2 | Alertes de stock (DSH-03) conservées | `ADMIN` | le bloc lit toujours `GET /api/v1/stocks/alerts`, à part de l'agrégat | `dashboard-page.spec.ts` (tests de DSH-03 adaptés : « lit les 100 alertes… », « affiche une erreur dans le bloc sans bloquer le cockpit… ») | ✅ auto |
| 3 | Accessibilité de l'écran | page `dashboard` chargée avec l'API simulée | aucune violation axe | `demo-pages.a11y.spec.ts` (entrée `dashboard`) | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/dashboard` | 401 `UNAUTHENTICATED` | `DashboardIT.getDashboard_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton `SELLER`, puis `DELIVERY_DRIVER` | 403 `FORBIDDEN` (le serveur décide, pas le front) | `DashboardIT.getDashboard_sellerAndDeliveryDriver_return403` | ✅ auto |
| Vendeuse ou livreur sur l'écran | `SELLER` ouvre `/dashboard` | aucun appel à l'agrégat, message « réservé à l'administrateur » ; alertes de stock si `stocks.read` | `dashboard-page.spec.ts` « montre aux alertes seules la vendeuse… » et « n’appelle aucune API et ne montre aucun indicateur au livreur » | ✅ auto |
| Échec de l'agrégat | 503 sur `/api/v1/dashboard` | état d'erreur avec « Réessayer », puis rechargement ; les alertes de stock restent indépendantes | `dashboard-page.spec.ts` « propose de réessayer quand l’agrégat du cockpit échoue » | ✅ auto |
| Champ `id` / `role` dans le corps | sans objet : lecture seule, aucune entrée | — | — | ⏭ sans objet |
| 404, 409, 422, journal | sans objet : aucun paramètre, aucune écriture, rien n'est tracé au journal | — | — | ⏭ sans objet |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Minuit à Madagascar | `2026-11-11T21:30:00Z` (00:30 le 12) | le cockpit lit le 12 novembre et compare au 11 | `DashboardServiceImplTest.getDashboard_justAfterMidnightUtcPlusThree_readsTheNextBusinessDay` | ✅ auto |
| Aucune activité | jour vide | tous les compteurs à 0, canaux actifs listés à 0, panier moyen 0 | `DashboardServiceImplTest.getDashboard_dayNotOpenedYet_hasNoStatusNorStockGapAndReadsNoClosing` | ✅ auto |
| Journée clôturée | statut `CLOSED` | la réconciliation reste lisible, l'écart est affiché | 🔍 `DayClosingService.getClosing` (« une journée clôturée reste lisible », guide JR-04) | 🔍 code |
| Hors ligne | lecture seule, jamais mise en file | l'écran affiche l'état d'erreur et propose « Réessayer » | `dashboard-page.spec.ts` « propose de réessayer quand l’agrégat du cockpit échoue » | ✅ auto |
| Zones encore simulées | graphique 7 jours, répartition par gamme et canal, flux d'activité, meilleurs articles, vendeuses, objectif mensuel | restent des données de démonstration | ⏭ DSH-02, DSH-04 (graphique, répartition) ; flux, meilleurs articles et vendeuses : tickets de rapports | ⏭ hors périmètre |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/dashboard/service/impl/DashboardServiceImplTest.java` (7 tests), `.../feature/dashboard/controller/DashboardIT.java` (7 tests, Testcontainers PostgreSQL).
- Frontend : `features/dashboard/data-access/dashboard-service.spec.ts` (2), `features/dashboard/components/dashboard-kpis/dashboard-kpis.spec.ts` (8), `features/dashboard/pages/dashboard-page/dashboard-page.spec.ts` (6, dont 4 de DSH-03 adaptés), `features/demo-pages.a11y.spec.ts` (page `dashboard`).

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| 1. Se connecter en administrateur, journée ouverte, faire une vente au comptoir, ouvrir `/dashboard` | CA, nombre de ventes et canal Boutique ont augmenté après rechargement | 🧪 |
| 2. Marquer une livraison livrée (LIV-03) | « livrées / total » monte d'un cran, une vente du canal Livraison apparaît | 🧪 |
| 3. Faire une réception hors transfert (inventaire ou réception) | l'écart de stock devient non nul avec « À justifier » ; « Voir la réconciliation » ouvre `/day/close` | 🧪 |
| 4. Se connecter en vendeuse puis en livreur | aucun indicateur, message de réservation ; alertes de stock pour la vendeuse seulement | 🧪 |
| 5. Afficher la page en malgache | libellés traduits, pas de clé brute | 🧪 |

## Résultat de l'exécution

- Date : 11/11/2026.
- Environnement : CI GitHub sur la branche `feature/DSH-01-cockpit-journalier` : `ci.yml` vert (backend 987 tests, 0 échec, dont `DashboardServiceImplTest` 7 et `DashboardIT` 7 sur Testcontainers PostgreSQL ; frontend Vitest 882 tests sur 139 fichiers), `conventions.yml` vert (Spotless, Checkstyle, Prettier, ESLint), Qodana for JVM vert avec 0 annotation. Rien n'a été lancé en local.
- Anomalies ouvertes : aucune. Le test `DashboardIT` des livraisons passe une commande « en tournée » et non « livrée » : une commande livrée exige une vente liée (contrainte `ck_delivery_orders_delivered_has_sale`), le compteur « livrées » est donc couvert côté service par `DashboardServiceImplTest.getDashboard_openDay_composesTheFourIndicators`.
- Recette manuelle : 5 étapes 🧪 restantes.
