# Scénarios de test — JR-04 · Réconciliation automatique de clôture

Statut : ☐ tests automatiques écrits, pas encore exécutés (CI à lancer), 🧪 recette à passer · Ticket : [#91](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/91) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (permission `days.close`, nouvelle) ; `SELLER` et `DELIVERY_DRIVER` pour le refus |
| Données de départ | Journée du jour ouverte (JR-01) ; un article fini reçu à l'entrepôt (200 pièces) ; une vendeuse (canal Boutique, 5 %) ; transfert du matin de 30 pièces vers la boutique, vente de 5 pièces, sortie shooting de 2 pièces rendue puis une de 1 pièce non rendue |
| Tickets à avoir déployés avant | JR-01 (journée, transferts du matin), JR-02, JR-03 (`BusinessDayService.findToday()`), VTE-01 (vente), LIV-03 / LIV-04 (livraison et retour), SHO-01 / SHO-02 (shooting), SRV-02 (consommation salon), TRF-02 / TRF-03 (transferts), STK-01 (mouvements), JNL-01 |

La réconciliation est calculée **à partir des mouvements de stock** de la journée d'Antananarivo (`[00:00, 24:00[`) : rien n'est stocké. Pour chaque article et chacun des trois emplacements (Boutique `STORE`, Livraison `DELIVERY`, Salon `TRADE_SHOW`) : report à l'ouverture, six composantes, théorique, stock du système et écart (voir le guide).

## Scénarios issus des critères d'acceptation

Critère unique : « Chaque article affiche les 6 composantes et l'écart théorique/compté. » Il est découpé en scénarios.

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Les 6 composantes : boutique | Transfert du matin de 30, vente de 5, shooting de 2 rendu (retour 2) et shooting de 1 non rendu | `transferred` 30, `returned` 2, `sold` 5, `shooting` 3, `delivered` 0, `salon` 0, `theoretical` 24, `systemStock` 24, `gap` 0 | `DayClosingIT.getClosing_storeActivity_showsTransferredSoldReturnedAndShooting` | ☐ |
| 2 | Les 6 composantes : livraison | 10 transférés à la livraison, une vente de 4 sur l'emplacement Livraison (commande livrée, LIV-03) et un mouvement `DELIVERY` de 1 | `delivered` 5, `sold` 0, `theoretical` 5 | `DayClosingIT.getClosing_saleInTheDeliveryLocationAndDeliveryMovement_areDelivered` | ☐ |
| 3 | Les 6 composantes : salon | 6 transférés au salon, consommation de 3 (mouvement `TRADE_SHOW`, SRV-02) | `salon` 3, `theoretical` 3 | `DayClosingIT.getClosing_salonConsumption_showsTheSalonQuantity` | ☐ |
| 4 | Les 6 composantes : calcul | Report 4, transféré 30, retour 1, vendu 19, livré 2, salon 3, shooting 1 | théorique 10 (report + transféré + retour − vendu − livré − salon − shooting) | `ClosingTallyTest.theoretical_sixComponents_addsTheEntriesAndRemovesTheExits`, `theoretical_openingStock_isCarriedIntoTheTotal` | ☐ |
| 4 bis | Classement des mouvements | Une vente sur l'emplacement Livraison | comptée « livré », pas « vendu » | `ClosingTallyTest.add_saleInTheDeliveryLocation_countsAsDelivered` | ☐ |
| 5 | Écart théorique / compté | Un ajustement d'inventaire de −2 sur la boutique après un transfert de 10 | `theoretical` 10, `systemStock` 8, `gap` −2 | `DayClosingIT.getClosing_inventoryAdjustment_showsAGapBetweenTheoreticalAndStock` | ☐ |
| 5 bis | Écart (calcul) | Stock 8 pour un théorique de 10 ; stock égal au théorique | écart −2 ; écart 0 | `ClosingTallyTest.gap_movementOutsideTheSixComponents_showsInTheGap`, `gap_stockMatchingTheTheoretical_isZero` | ☐ |
| 5 ter | Écart à l'écran | La boutique compte 5 au lieu de 6 pour l'huile | écart « −1 » recalculé à la saisie | `day-close-page.spec.ts` « should compute the gap again when the administrator changes the count » | ☐ |
| 6 | Écran : 6 composantes | Quand l'administrateur ouvre `/day/close` | colonnes Transféré, Retour, Vendu, Livré, Salon, Shooting, Théorique, Compté, Écart ; ligne « Savon noir » : 4 / 30 / 1 / 19 / 0 / 0 / 2 / 14 / 12 / −2 | `day-close-page.spec.ts` « should show the six components, the theoretical, the count and the gap of each article » | ☐ |
| 6 bis | Écran : emplacements | Réponse avec Boutique, Livraison, Salon | les trois emplacements sont proposés | `day-close-page.spec.ts` « should list the three locations of the closing » | ☐ |
| 6 ter | Écran : lecture | Appel `GET /api/v1/days/closing` | lecture seule ; l'entrepôt est ignoré ; comptage proposé = stock du système, sans motif | `day-service.spec.ts` « lit la réconciliation sur GET /api/v1/days/closing et la range par colonne (JR-04) » | ☐ |
| 6 quater | Écran : théorique | Report 4, 30, 1, 19, 2, 3, 1 | 10 | `day-service.spec.ts` « calcule le théorique : report + transféré + retour − vendu − livré − salon − shooting » | ☐ |
| 7 | Les emplacements | Lecture de la clôture | trois blocs dans l'ordre `STORE`, `DELIVERY`, `TRADE_SHOW`, jamais l'entrepôt | `DayClosingIT.getClosing_openDay_listsStoreDeliveryAndSalonButNotTheWarehouse`, `DayClosingServiceImplTest.getClosing_openDay_returnsTheThreeLocationsInDisplayOrderWithoutTheWarehouse` | ☐ |
| 8 | Report de la veille | 7 pièces laissées à la boutique il y a 2 jours puis 3 transférées ce matin | `opening` 7, `transferred` 3 (le mouvement ancien n'est pas du jour), `theoretical` 10, `gap` 0 | `DayClosingIT.getClosing_stockLeftByAnEarlierDay_isTheOpeningAndNotAMovementOfToday`, `DayClosingServiceImplTest.getClosing_articleHeldWithoutMovementToday_showsItsOpeningStock` | ☐ |
| 9 | Retour de stock vers l'entrepôt | 30 transférés puis 4 renvoyés à l'entrepôt | `transferred` 26 (solde net des transferts) | `DayClosingIT.getClosing_transferBackToTheWarehouse_lowersTheTransferredQuantity`, `ClosingTallyTest.add_transferLeavingTheLocation_lowersTheTransferredQuantity` | ☐ |
| 10 | Articles sans activité | Article présent seulement à l'entrepôt ; transferts qui s'annulent | aucune ligne | `DayClosingIT.getClosing_articleOnlyInTheWarehouse_hasNoLineInAnyLocation`, `DayClosingServiceImplTest.getClosing_transfersCancellingOut_leavesNoLine`, `ClosingTallyTest.isEmpty_nothingHeldNorMoved_isTrue`, `isEmpty_anyQuantity_isFalse` | ☐ |
| 11 | Tri | « shampoing », « Baume », « Huile » | ordre alphabétique sans tenir compte de la casse : Baume, Huile, shampoing | `DayClosingServiceImplTest.getClosing_severalArticles_sortsTheLinesByName` | ☐ |
| 12 | Calcul complet côté service | Report 4, transféré 30, retour 1, vendu 19, shooting 2, stock 12 | théorique 14, écart −2 | `DayClosingServiceImplTest.getClosing_movementsOfTheDay_buildsTheSixComponentsTheTheoreticalAndTheGap` | ☐ |
| 12 bis | Livraison côté service | Transfert 8 et vente 6 sur l'emplacement Livraison | `delivered` 6, aucune ligne à la boutique | `DayClosingServiceImplTest.getClosing_saleInTheDeliveryLocation_isReportedAsDelivered` | ☐ |
| 13 | Jour d'Antananarivo | Journée du 11/11/2026 | mouvements lus de `2026-11-10T21:00Z` à `2026-11-11T21:00Z` (minuit local) | `DayClosingServiceImplTest.getClosing_dayOfMadagascar_readsTheLedgerFromMidnightToMidnightLocalTime` | ☐ |
| 14 | Journée close | Journée au statut `CLOSED` | la réconciliation reste lisible, `status` = `CLOSED` | `DayClosingServiceImplTest.getClosing_closedDay_stillReturnsTheReconciliation` | ☐ |
| 15 | Lecture des mouvements (stock) | Mouvements de plusieurs types, périodes, instants | somme par type, bornes `[from, to[`, soldes avant un instant, soldes nuls omis | `StockMovementRepositoryIT.totalsByType_movementsOfSeveralTypes_sumsEachTypeSeparately`, `totalsByType_periodEndingBeforeTheMovement_leavesItOut`, `balancesBefore_movementsOnBothSidesOfTheInstant_countsOnlyTheEarlierOnes`, `balancesBefore_movementsCancellingOut_omitsTheArticle` ; `StockDayLedgerServiceImplTest.getMovementTotals_locationsGiven_mapsTheRepositoryRows`, `getMovementTotals_noLocation_returnsEmptyWithoutReading`, `getBalancesBefore_locationsGiven_mapsTheRepositoryRows`, `getBalancesBefore_noLocation_returnsEmptyWithoutReading` | ☐ |
| 16 | Menu et garde | Entrée « Clôture de journée » | visible et accessible avec `days.close` (administrateur), plus avec `sales.read` | `menu.spec.ts` « should show every module to the administrator » et « …no administration to a seller » (assertions `menu.item.dayClose` ajoutées) ; garde `permissionGuard('days.close')` dans `day.routes.ts` | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `DayClosingIT.getClosing_withoutToken_returns401` | ☐ |
| Appel avec un mauvais rôle (vendeuse, livreur) | 403 `FORBIDDEN` | `DayClosingIT.getClosing_sellerAndDeliveryDriver_return403` | ☐ |
| Journée du jour non ouverte | 409 `CONFLICT`, détail « La journée du 11/11/2026 n'est pas ouverte. » ; l'écran affiche une alerte au lieu du tableau | `DayClosingIT.getClosing_dayNotOpened_returns409`, `DayClosingServiceImplTest.getClosing_dayNotOpened_throwsConflictWithoutReadingTheStock`, `day-close-page.spec.ts` « should tell the administrator that the day is not opened (409) » | ☐ |
| Champ interdit dans le corps (`role`, `id`) | sans objet : lecture seule, aucun corps | — | — |
| Paramètre `date` | ignoré : on réconcilie toujours aujourd'hui | `DayClosingIT.getClosing_dateParameter_isIgnoredAndTodayIsReconciled` | ☐ |
| Données invalides (422), ressource inconnue (404) | sans objet : aucune entrée, aucun identifiant | — | — |
| Accessibilité de l'écran | aucune violation axe (WCAG 2 A / AA) | `day-close-page.spec.ts` « should have no detectable accessibility violation » (remplace l'entrée « day close » de `demo-pages.a11y.spec.ts`, la page n'étant plus alimentée par des données de démonstration) | ☐ |
| Journal | sans objet : lecture seule, rien n'est écrit | — | — |

## Cas limites et hors ligne

- **Journée non ouverte** : 409 (contrairement au suivi JR-03, qui reste lisible) : il n'y a rien à réconcilier avant la répartition du matin. Une journée **close** reste lisible.
- **Jour = jour d'Antananarivo**, jamais la date UTC : plage `[00:00, 24:00[` locale (13), mêmes bornes que JR-03.
- **Mouvements hors des six composantes** (réception fournisseur, production, inventaire) : ignorés des composantes, donc visibles dans l'écart (5).
- **Vente annulée** : le stock revient par un mouvement `RETURN` (VTE) ; elle compte donc dans « Retour » (comme la réintégration d'un shooting).
- **Quantités décimales** : trois décimales côté serveur, affichées telles quelles.
- **Vide** : aucun article actif sur un emplacement : bloc avec `lines` vide, l'écran affiche « Aucun écart dans cet emplacement ».
- **Hors ligne** : lecture seule, rien n'est mis en file (`Idempotency-Key` sans objet) ; l'écran affiche l'état d'erreur avec « Réessayer ».
- **Reste en démonstration** (JR-05, JR-06) : la caisse (espèces attendues / comptées), la liste de contrôle (livraisons soldées, tickets, shootings) et le verrouillage ; la page garde `underConstruction`.
- **Base partagée entre classes de test** : `DayClosingIT` travaille sur ses propres articles (lignes exactes quoi qu'ait enregistré une autre classe), ouvre la journée si besoin (201 ou 409 accepté), appelle `endAutomaticPromotions()` avant la vente, crée sa vendeuse avec le prénom « Aaron » (le formulaire de vente ne liste que les 100 premières par prénom) ; le test « journée non ouverte » supprime la journée puis la rouvre dans un `finally`. Aucun test ne clôture la journée.

## Tests existants

- Backend : `feature/dayclosing/controller/DayClosingIT.java`, `feature/dayclosing/service/impl/DayClosingServiceImplTest.java`, `ClosingTallyTest.java`, `feature/stock/service/impl/StockDayLedgerServiceImplTest.java`, `feature/stock/repository/StockMovementRepositoryIT.java` (4 tests ajoutés).
- Frontend : `features/day/pages/day-close-page/day-close-page.spec.ts`, `features/day/data-access/day-service.spec.ts` (2 tests ajoutés), `features/demo-pages.a11y.spec.ts` (entrée « day close » retirée), `core/layout/menu.spec.ts` (assertions ajoutées).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Ouvrir la journée, transférer des articles, vendre au comptoir, livrer une commande, enregistrer une prestation avec consommation et une sortie shooting ; ouvrir `/day/close` | chaque article affiche ses six composantes par emplacement, le théorique et l'écart nul ; la formule affichée est « Théorique = Report + Transféré + Retour − Vendu − Livré − Salon − Shooting » | 🧪 recette |
| R2 | Corriger le comptage d'un article (saisir moins que le théorique) | l'écart passe en rouge et le motif devient obligatoire (JR-05 : à enregistrer) | 🧪 recette |
| R3 | Ouvrir `/day/close` avant l'ouverture de la journée | alerte « la journée du jour n'est pas ouverte » | 🧪 recette |

## Résultat de l'exécution

- Date : à lancer (CI ou local).
- Environnement : aucun test exécuté au moment de l'écriture (`mvn` indisponible dans l'environnement de rédaction) ; la CI de la branche `feature/JR-04-reconciliation-automatique-cloture` est la référence.
- Anomalies ouvertes : aucune connue ; 3 étapes 🧪 restantes.
