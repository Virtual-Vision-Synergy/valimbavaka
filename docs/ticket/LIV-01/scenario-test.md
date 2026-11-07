# Scénarios de test — LIV-01 · Création d'une commande à livrer

Statut : ☐ tests écrits, à exécuter · Ticket : [#46](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/46) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

> **Décision PO du 07/11/2026** (question « une commande livrée devient-elle une vente avec facture ? ») : oui. Une commande livrée devient une vente (canal livraison) avec sa facture, au moment où la livraison est marquée livrée ([LIV-03](../LIV-03/description.md)). LIV-01 crée seulement la commande à livrer ; le contrat que LIV-03 suivra est décrit dans le [guide](guide.md#contrat-pour-liv-03--commande-livrée--vente--facture).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôles utilisés | administrateur (`admin.test@valimbavaka.mg`) ; vendeuse (`deliveries.create`, `deliveries.read`) ; livreur « Tojo Nirina » (`deliveries.read` seulement) |
| Données de départ | article fini actif « Article-… » créé par `POST /api/v1/articles`, prix 8 500 Ar (`StockApiTestSupport.createArticle`) ; un livreur actif créé par test (`DeliveryApiTestSupport.createDriver`) ; jour de tournée = demain à Antananarivo |
| Tickets à avoir déployés avant | ART-01 (articles), AUTH-03 (utilisateurs et rôles), JNL-01 (journal), JNL-02 (`OperationRepository`), SYS-07 (file hors ligne et `Idempotency-Key`). STK-01 n'est pas utilisé : la création ne bouge pas le stock (voir guide) |
| Migration | `2026_11_08-64-changelog.sql` : séquence `delivery_order_number_seq`, tables `delivery_orders` et `delivery_order_lines`, permission `deliveries.create` (administrateur, vendeuse), `deliveries.read` donnée en plus à la vendeuse |

## Scénarios issus des critères d'acceptation

### Critère 1 — La commande créée est ajoutée à la tournée du livreur assigné

La tournée d'un livreur est l'ensemble de ses commandes d'un jour (`driverId` + `scheduledDate`), dans l'ordre de `stopNumber`.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Création par la vendeuse | vendeuse ; client « Rakoto Be », 034 12 345 67, « Lot II J 45, rue Pasteur », Ivandry ; livreur Tojo ; demain ; 2 articles à 8 500 Ar | 201, `Location`, numéro `LIV-0001`…, statut `TO_PREPARE`, arrêt 1, prix unitaire 8 500, montant 17 000, `createdBy` = la vendeuse ; `GET /api/v1/deliveries?driverId=…&date=…&sortBy=stopNumber` la renvoie | `DeliveryOrderIT.createOrder_bySeller_returns201ToPrepareAndAddsItToTheRoundOfTheDriver` | ☐ |
| 1b · Ajout en fin de tournée | 2ᵉ commande pour le même livreur et le même jour, puis une 3ᵉ le surlendemain | arrêt 2 ; arrêt 1 pour l'autre jour (autre tournée) | `DeliveryOrderIT.createOrder_secondOrderOfTheSameRound_isAppendedAtTheNextStop` | ☐ |
| 1c · Service : verrou puis position | création pour Tojo le 2026-11-09, séquence 42, prochain arrêt 3 | verrou `delivery-round:<driverId>:2026-11-09` pris **avant** la lecture du prochain arrêt ; enregistrée `LIV-0042`, arrêt 3, `TO_PREPARE` | `DeliveryOrderServiceImplTest.createOrder_validRequest_appendsTheOrderToTheRoundOfItsDriverAndTracesIt`, `DeliveryOrderServiceImplTest.createOrder_validRequest_locksTheRoundBeforeReadingTheNextStop` | ☐ |
| 1d · Entité | 2 × 8 500 et 0,333 × 1 000 | statut `TO_PREPARE`, montant 17 333,00 (ligne 2 arrondie à 333,00) ; lignes en lecture seule | `DeliveryOrderTest.create_twoLines_startsToPrepareWithTheAmountToCollect`, `DeliveryOrderTest.getLines_afterCreation_isReadOnly` | ☐ |
| 1e · Prix lus sur l'article | ligne « Savon noir » ×2, client « &nbsp;&nbsp;Rakoto Be », quartier « &nbsp;Ivandry&nbsp; », note « &nbsp;&nbsp; » | prix unitaire 8 500 copié de la fiche ; espaces retirés ; note vide → `null` | `DeliveryOrderPolicyTest.check_validRequest_stripsTheCustomerAndPricesTheLinesFromTheArticle` | ☐ |
| 1f · Écran de création | vendeuse : `/deliveries/new`, « 2,5 » savons, livreur Tojo N. | `POST` avec `quantity` 2.5, `district` `null`, jour = aujourd'hui ; toast « Commande LIV-0042 ajoutée à la tournée (arrêt 3). », retour à `/deliveries` | `delivery-order-page.spec.ts` (« crée la commande, annonce son arrêt… ») | ☐ |
| 1g · Listes de l'écran | ouverture de `/deliveries/new` | livreurs actifs (« Tojo N. »), seulement les produits finis actifs (`type=FINISHED&active=true`), jour par défaut = aujourd'hui à Antananarivo ; montant estimé 17 000 Ar pour 2 savons | `delivery-order-page.spec.ts` (« propose les livreurs actifs… », « affiche le montant estimé… ») | ☐ |
| 1h · Tableau du jour | `GET /api/v1/deliveries?date=<aujourd'hui>&size=100&sortBy=stopNumber&sortDirection=ASC` + `GET /api/v1/deliveries/drivers` | une tournée par livreur ayant des commandes (« Tojo N. »), une carte par commande à son arrêt, commandes annulées écartées | `delivery-service.spec.ts` (« lit les commandes du jour… »), `delivery-mapping.spec.ts` (« fait une carte par commande… », « écarte les commandes annulées… ») | ☐ |
| 1i · Statuts et transitions pour LIV-02 à LIV-05 | table de transitions | autorisées : `TO_PREPARE` → `ON_ROUND` / `DELIVERED` / `FAILED` / `CANCELLED`, `ON_ROUND` → `DELIVERED` / `FAILED` ; `DELIVERED`, `FAILED`, `CANCELLED` finaux ; refus 409 « La livraison ne peut pas passer de « livrée » à « en échec ». » | `DeliveryStatusTest.canMoveTo_allowedTransition_returnsTrue`, `DeliveryStatusTest.canMoveTo_forbiddenTransition_returnsFalse`, `DeliveryStatusTest.isFinal_finalStatus_leavesToNoOtherStatus`, `DeliveryStatusTest.ensureCanMoveTo_deliveredToFailed_throwsConflictWithFrenchLabels`, `DeliveryOrderTest.moveTo_allowedTransition_changesTheStatus`, `DeliveryOrderTest.moveTo_fromFinalStatus_throwsConflictAndKeepsTheStatus` | ☐ |

## Sécurité et erreurs

Endpoints : `POST /api/v1/deliveries`, `GET /api/v1/deliveries`, `GET /api/v1/deliveries/{id}`, `GET /api/v1/deliveries/drivers`.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | création valide sans `Authorization` | 401 `UNAUTHENTICATED` | `DeliveryOrderIT.createOrder_withoutToken_returns401` | ☐ |
| Mauvais rôle (création) | jeton livreur | 403 `FORBIDDEN` | `DeliveryOrderIT.createOrder_withDeliveryDriverRole_returns403` | ☐ |
| Mauvais rôle (livreurs) | jeton livreur sur `/drivers` | 403 | `DeliveryOrderIT.getDrivers_withDeliveryDriverRole_returns403` | ☐ |
| Mass assignment | `"id"` forcé, `"number":"LIV-9999"`, `"status":"DELIVERED"`, `"stopNumber":99`, `"amount":1`, ligne `"unitPrice":1` | ignorés : autre id, numéro du serveur, `TO_PREPARE`, arrêt 1, prix 8 500, montant 8 500 | `DeliveryOrderIT.createOrder_massAssignmentOfServerFields_isIgnored` | ☐ |
| Livreur inactif ou pas livreur | `driverId` = compte administrateur | 422 `VALIDATION_FAILED` | `DeliveryOrderIT.createOrder_assignedToAnAdministrator_returns422`, `DeliveryOrderPolicyTest.check_driverInactiveOrNotADriver_throwsBusinessException` | ☐ |
| Jour passé | hier | 422 « La date de livraison ne peut pas être dans le passé. » | `DeliveryOrderIT.createOrder_yesterday_returns422`, `DeliveryOrderPolicyTest.check_yesterday_throwsBusinessException` | ☐ |
| Champ obligatoire vide | nom du client « &nbsp;&nbsp;&nbsp; » | 422 | `DeliveryOrderIT.createOrder_blankCustomerName_returns422` | ☐ |
| Téléphone invalide | « appelez-moi » | 422 | `DeliveryOrderIT.createOrder_invalidPhone_returns422` | ☐ |
| Article inconnu | `articleId` aléatoire | 404 `NOT_FOUND`, aucune commande écrite pour ce livreur | `DeliveryOrderIT.createOrder_unknownArticle_returns404AndWritesNothing`, `DeliveryOrderPolicyTest.check_unknownArticle_propagatesNotFound` | ☐ |
| Article non vendable | matière première ; produit fini désactivé | 422 « L'article ART-0031 n'est pas un produit fini actif… » | `DeliveryOrderPolicyTest.check_rawMaterial_throwsBusinessException`, `DeliveryOrderPolicyTest.check_inactiveFinishedProduct_throwsBusinessException` | ☐ |
| Commande inconnue | `GET /api/v1/deliveries/<uuid aléatoire>` | 404 `NOT_FOUND` | `DeliveryOrderIT.getOrder_unknownId_returns404`, `DeliveryOrderServiceImplTest.getOrder_unknownId_throwsNotFound` | ☐ |
| Cloisonnement livreur (lecture) | Tojo lit sa commande puis celle d'un autre livreur | 200 puis 404 | `DeliveryOrderIT.getOrder_driverReadingHisOwnOrAnotherDriversOrder_returns200Or404`, `DeliveryOrderServiceImplTest.getOrder_driverReadingAnotherDriversOrder_throwsNotFound`, `DeliveryOrderServiceImplTest.getOrder_sellerReadingAnyOrder_returnsIt` | ☐ |
| Cloisonnement livreur (liste) | Tojo demande `?driverId=<autre livreur>` | seulement ses propres commandes | `DeliveryOrderIT.getOrders_driverAskingForAnotherRound_onlyGetsHisOwnOrders`, `DeliveryOrderServiceImplTest.getOrders_driverAskingForAnotherDriver_isLimitedToHisOwnOrders` | ☐ |
| Filtres administrateur / vendeuse | vendeuse, `?status=TO_PREPARE` ; administrateur, livreur + jour + statut, tri `stopNumber` | 200 ; filtres transmis tels quels, tri en liste blanche | `DeliveryOrderIT.getOrders_withSellerRoleAndStatusFilter_returns200`, `DeliveryOrderServiceImplTest.getOrders_administratorWithFilters_passesThemUnchanged` | ☐ |
| Liste des livreurs | vendeuse, `/drivers?size=100` | contient le livreur créé, sans email | `DeliveryOrderIT.getDrivers_bySeller_listsTheActiveDriversWithoutEmail`, `UserServiceImplTest.isActiveDriver_activeDeliveryDriver_returnsTrue` | ☐ |
| Journal | commande de 1,5 article à 8 500 | une ligne `CREATE` / `DELIVERY_ORDER` : « Création de la commande à livrer LIV-… : 1 article(s), 12750.00 Ar, tournée du … (arrêt 1) » (sans nom ni téléphone du client) | `DeliveryOrderIT.createOrder_validOrder_isTracedInTheJournal`, `DeliveryOrderServiceImplTest.createOrder_validRequest_appendsTheOrderToTheRoundOfItsDriverAndTracesIt` | ☐ |
| Refus métier avant écriture | règle violée | ni `save` ni ligne de journal | `DeliveryOrderServiceImplTest.createOrder_rejectedByThePolicy_writesNothing` | ☐ |
| Permissions de la vendeuse | `GET /api/v1/auth/me` vendeuse | contient `deliveries.create` (et `deliveries.read`), pas `journal.read` ni `users.read` | `AuthSecurityIT.me_withSellerToken_returnsOnlyTheSellerPermissions` (modifié) ; front : `menu.spec.ts` (le menu « Livraisons » apparaît pour la vendeuse) | ☐ |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Aujourd'hui à Antananarivo | 2026-11-08 22:30 UTC = 2026-11-09 01:30 à Antananarivo, livraison le 2026-11-09 | acceptée (jour métier d'Antananarivo, pas UTC) | `DeliveryOrderPolicyTest.check_todayInAntananarivoWhileStillYesterdayInUtc_isAccepted` ; front : `delivery-mapping.spec.ts` (« donne le jour d'Antananarivo… ») | ☐ |
| Aucune ligne | `"lines":[]` | 422 | `DeliveryOrderIT.createOrder_noLine_returns422` | ☐ |
| Nombre de lignes | 101 lignes (maximum 100) | 422 | `DeliveryOrderIT.createOrder_101Lines_returns422` | ☐ |
| Quantité nulle | `0` | 422 | `DeliveryOrderIT.createOrder_zeroQuantity_returns422` | ☐ |
| Décimales | `1.5` accepté (montant 12 750,00) ; saisie « 2,5 » à l'écran envoyée `2.5` | 201 | `DeliveryOrderIT.createOrder_validOrder_isTracedInTheJournal`, `delivery-order-page.spec.ts` (« crée la commande… ») | ☐ |
| Doublon d'article | Savon 1 puis Savon 2 | 422 « Un article ne peut figurer qu'une fois dans une commande. » | `DeliveryOrderIT.createOrder_sameArticleTwice_returns422`, `DeliveryOrderPolicyTest.check_sameArticleTwice_throwsBusinessException` | ☐ |
| `size` = 101 | `GET /api/v1/deliveries?size=101` | 422 | `DeliveryOrderIT.getOrders_sizeAbove100_returns422` | ☐ |
| Deux vendeuses en même temps | 4 créations simultanées pour le même livreur et le même jour | 4 arrêts distincts 1, 2, 3, 4 (verrou transactionnel par tournée) | `DeliveryRoundIT.createOrder_fourOrdersOfTheSameRoundAtTheSameTime_getDistinctStops` | ☐ |
| Rejeu hors ligne | même `Idempotency-Key` envoyée deux fois | 2ᵉ réponse identique (`Idempotency-Replayed: true`), une seule commande | `DeliveryRoundIT.createOrder_replayedWithSameIdempotencyKey_createsOneOrder` | ☐ |
| Saisie mise en file | réseau coupé | toast « Saisie enregistrée hors connexion… » (`common.queuedSaved`) ; service : `POST /api/v1/deliveries` via `OutboxService.submit` | `delivery-order-page.spec.ts` (« annonce une commande mise en file… »), `delivery-service.spec.ts` (« envoie la commande par la file hors ligne… ») | ☐ |
| Formulaire invalide | livreur vide, téléphone « appelez-moi », jour 2020-01-01 | rien n'est envoyé, messages sous les champs | `delivery-order-page.spec.ts` (« refuse un formulaire sans livreur… ») | ☐ |
| Refus serveur affiché | 422 « Le livreur choisi est inconnu, désactivé… » | message dans l'alerte, saisie conservée, on reste sur la page | `delivery-order-page.spec.ts` (« affiche le refus du serveur… ») | ☐ |
| Livreur désactivé depuis | commande d'un livreur qui n'est plus actif | la tournée reste affichée sous « — » | `delivery-mapping.spec.ts` (« écarte les commandes annulées et garde la tournée… ») | ☐ |
| Nom du livreur sans nom de famille | « Lova » + nom vide | « Lova » (pas d'initiale) | `delivery-mapping.spec.ts` (« nomme le livreur… ») | ☐ |
| Accessibilité du tableau | `/deliveries` branché sur l'API (pages vides) | aucune violation axe | `demo-pages.a11y.spec.ts` (« deliveries ») | ☐ |
| Prix modifié après la commande | prix de l'article changé après la création | le montant à encaisser ne change pas (prix copié dans la ligne) | — (structure : `DeliveryOrderLine.unitPrice` non modifiable) | 🔍 code |
| Stock disponible | commande d'un article sans stock | acceptée : la création ne réserve ni ne bouge le stock (voir guide, Points d'attention) | — | ⏭ hors périmètre (LIV-03 / LIV-04, JR-01) |

## Tests existants

**Backend** (`valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/`)
- `delivery/domain/DeliveryStatusTest.java`, `delivery/domain/DeliveryOrderTest.java`
- `delivery/service/impl/DeliveryOrderPolicyTest.java`, `delivery/service/impl/DeliveryOrderServiceImplTest.java`
- `delivery/controller/DeliveryOrderIT.java`, `delivery/controller/DeliveryRoundIT.java` (socle : `delivery/controller/DeliveryApiTestSupport.java`, qui étend `stock/controller/StockApiTestSupport.java`)
- `user/service/impl/UserServiceImplTest.java` (ajout : `isActiveDriver_activeDeliveryDriver_returnsTrue`)
- `auth/controller/AuthSecurityIT.java` (modifié : la vendeuse a désormais `deliveries.create` et `deliveries.read`)
- `ArchitectureTest` (existant) couvre la nouvelle feature : contrôleur mince, `@HasPermission`, DTO `record`, `DeliveryOrderRepository` sans `delete`.

**Frontend** (`valimbavaka-ui/src/app/`)
- `features/deliveries/data-access/delivery-mapping.spec.ts`, `features/deliveries/data-access/delivery-service.spec.ts`
- `features/deliveries/pages/delivery-order-page/delivery-order-page.spec.ts`
- `core/layout/menu.spec.ts` (modifié), `features/demo-pages.a11y.spec.ts` (existant, page `deliveries`)

## Recette manuelle (🧪)

| Cas | Exemple | Résultat attendu | Statut |
|---|---|---|---|
| Parcours vendeuse | vendeuse, `/deliveries` → « Nouvelle commande » → client, adresse, livreur Tojo, aujourd'hui, 2 savons | toast « Commande LIV-… ajoutée à la tournée (arrêt n). », la carte apparaît dans la colonne « À préparer » avec « Arrêt n/x », filtre « Tojo N. » | 🧪 |
| Ordre de la tournée | 3 commandes pour Tojo aujourd'hui | arrêts 1, 2, 3 dans l'ordre de saisie | 🧪 |
| Commande pour demain | jour de livraison = demain | acceptée ; elle n'apparaît pas sur le tableau du jour (le tableau montre aujourd'hui) | 🧪 |
| Livreur | se connecter en livreur | arrive sur `/round` (maquette LIV-02) ; `/deliveries/new` le renvoie vers l'accueil | 🧪 |
| Hors ligne | couper le réseau, créer une commande, rétablir | message de mise en file, puis une seule commande créée | 🧪 |
| Mobile | écran ≤ 640 px | les champs s'empilent, bouton « Retirer » accessible | 🧪 |

## Résultat de l'exécution

- Résultat de l'exécution : à lancer (CI ou local).
- Anomalies ouvertes : aucune connue.
- Étapes 🧪 restantes : 6.
