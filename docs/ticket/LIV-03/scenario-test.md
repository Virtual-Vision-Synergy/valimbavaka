# Scénarios de test — LIV-03 · Marquage rapide d'une livraison

Statut : ✅ tests automatiques exécutés (CI verte) ; recette 🧪 à passer · Ticket : [#48](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/48) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

> **Décision PO du 07/11/2026** (LIV-01) : une commande livrée devient une vente (canal livraison) avec sa facture, au moment où la livraison est marquée livrée. LIV-03 crée la vente par le port `SaleService.record` de VTE-01 ; la **facture** sera produite par FAC-01 (écouteur de `SaleRecordedEvent`, pas encore livré) : ce ticket ne la teste pas.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôles utilisés | livreur « Tojo Nirina » (`deliveries.read`, `deliveries.deliver`) ; administrateur (`admin.test@valimbavaka.mg`, `deliveries.deliver`) ; vendeuse « Miora Rakoto » (saisit la commande, sans `deliveries.deliver`) ; côté front, livreur « Hanta Rasolofoson » (`buildUser('DELIVERY_DRIVER')`) |
| Données de départ | journée du jour ouverte (`POST /api/v1/days`, JR-01 ; 201 ou 409 si déjà ouverte) ; article fini actif à 8 500 Ar (`StockApiTestSupport.createArticle`) ; stock posé dans l'emplacement `DELIVERY` (Livraison) par un mouvement `RECEIPT` (`StockApiTestSupport.move`), à la place des transferts du matin ; commande « Rakoto Be » du jour (`DeliveryApiTestSupport.createdOrderId`) |
| Tickets à avoir déployés avant | LIV-01 (commandes, `DeliveryOrder.moveTo`), LIV-02 (écran `/round`, bouton « Livrée », annulation 5 s), VTE-01 (port `SaleService.record`, PR #212), JR-01 (journée ouverte), STK-01 (mouvements), JNL-01 (journal), SYS-07 / SYS-12 (file hors ligne, `Idempotency-Key`) |
| Migration | `2026_11_08-66-changelog.sql` : colonne `delivery_orders.sale_id` (→ `sales`, unique), contrainte « une commande `DELIVERED` a sa vente », permission `deliveries.deliver` (administrateur, livreur) |

## Scénarios issus des critères d'acceptation

### Critère 1 — Le statut passe à Livrée sans saisie complémentaire

`POST /api/v1/deliveries/{id}/deliver`, **sans corps**. Le serveur enregistre la vente canal `DELIVERY` aux prix de la commande, puis passe la commande à `DELIVERED` avec `saleId`, dans la même transaction (tout ou rien).

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Livrée par le livreur | Tojo, sa commande du jour (2 savons à 8 500 Ar, saisie par l'administrateur) ; 5 savons en Livraison | 200 ; `status` `DELIVERED`, `saleId` renseigné ; la vente `VTE-…` est sur le canal `DELIVERY`, sort de l'emplacement Livraison, règlement `ON_DELIVERY`, statut `VALIDATED`, total 17 000 Ar, **sans vendeuse** ; Livraison 5 → 3, un seul mouvement `SALE` | `DeliveryCompletionIT.markDelivered_driverOwnOrder_returns200DeliveredWithADeliverySale` | ✅ auto |
| 1b · Vendeuse créditée | commande saisie par la vendeuse Miora | la vente porte `seller_id` = Miora (commission COM-02) | `DeliveryCompletionIT.markDelivered_orderEnteredBySeller_creditsTheSaleToTheSeller`, `DeliverySaleTest.record_orderEnteredBySeller_recordsADeliverySaleCreditedToHerAtTheOrderPrices` | ✅ auto |
| 1c · Commande de l'administrateur | auteur actif mais pas vendeuse | vente sans vendeuse (`sellerId` `null`) | `DeliverySaleTest.record_orderEnteredByAdministrator_recordsASaleWithoutSeller` | ✅ auto |
| 1d · Commande sans auteur | `createdBy` `null` | vente sans vendeuse, `UserService.isActiveSeller` non appelé | `DeliverySaleTest.record_orderWithoutAuthor_recordsASaleWithoutSeller` | ✅ auto |
| 1e · Prix figé de la commande | prix de l'article passé de 8 500 à 9 900 Ar après la commande | ligne de vente à 8 500 Ar, total 17 000 Ar | `DeliveryCompletionIT.markDelivered_priceChangedSinceTheOrder_sellsAtTheOrderPrice` | ✅ auto |
| 1f · Article désactivé depuis | article désactivé après la commande | 200 `DELIVERED` (prix convenu accepté par `SalePolicy`) | `DeliveryCompletionIT.markDelivered_articleDeactivatedSinceTheOrder_stillDeliversAtTheOrderPrice` | ✅ auto |
| 1g · Lignes de la vente | commande de 2 lignes (2 savons à 8 500, 0,5 huile à 12 000) | `SaleCommand` canal Livraison, `ON_DELIVERY`, mêmes articles, quantités et prix unitaires | `DeliverySaleTest.record_orderEnteredBySeller_recordsADeliverySaleCreditedToHerAtTheOrderPrices` | ✅ auto |
| 1h · Domaine | commande `ON_ROUND`, `deliver(vente)` ; commande `TO_PREPARE` | statut `DELIVERED`, `saleId` gardé ; `TO_PREPARE → DELIVERED` en un geste | `DeliveryOrderTest.deliver_orderOnRound_movesToDeliveredWithItsSale`, `DeliveryOrderTest.deliver_orderToPrepare_movesToDeliveredInOneStep` | ✅ auto |
| 1i · Service | commande du livreur, vente `VTE-0007` de 17 000 Ar | commande `DELIVERED` avec la vente ; journal `UPDATE` · `DELIVERY_ORDER` « Commande LIV-0042 livrée : vente VTE-0007, 1 article(s), 17000.00 Ar à la livraison » | `DeliveryCompletionServiceImplTest.markDelivered_ownOrder_recordsTheSaleDeliversTheOrderAndTracesIt`, `DeliveryCompletionServiceImplTest.markDelivered_orderOnRound_deliversItToo` | ✅ auto |
| 1j · Journal | commande d'un savon livrée | ligne `UPDATE` · `DELIVERY_ORDER` « Commande LIV-… livrée : vente VTE-…, 1 article(s), 8500.00 Ar à la livraison », **sans** le nom du client ; la vente a sa propre ligne `CREATE` · `SALE` (VTE-01) | `DeliveryCompletionIT.markDelivered_validDelivery_isTracedInTheJournal` | ✅ auto |
| 1k · Écran : un geste, envoi après 5 s | `/round`, arrêt « Rasoa Vola » ; bouton « Livrée » | l'arrêt quitte « Prochain arrêt » (Hery Andry passe devant), avancement 2/3 ; rien n'est envoyé pendant 5 s ; puis `updateStatus('order-2', { status: 'DELIVERED', failureReason: null, note: null })` | `driver-round-page.spec.ts` (« marque l'arrêt livré d'un geste et ne l'envoie qu'après le délai d'annulation ») | ✅ auto |
| 1l · Écran : annulation | « Livrée » puis « Annuler » avant 5 s | rien n'est envoyé, « Rasoa Vola » redevient le prochain arrêt | `driver-round-page.spec.ts` (« n'envoie rien si le livreur annule « Livrée » pendant le délai ») | ✅ auto |
| 1m · Service front | `updateStatus('order-1', DELIVERED)` | `OutboxService.submit('POST', '/api/v1/deliveries/order-1/deliver')`, sans corps ; renvoie l'id | `delivery-service.spec.ts` (« marque la livraison livrée par la file hors ligne, sans corps… ») | ✅ auto |
| 1n · Échec non concerné | `updateStatus(…, FAILED)` | un seul appel, vers `…/failure` (LIV-04), jamais `…/deliver` | `delivery-service.spec.ts` (« n'appelle pas l'API « livrée » pour un échec (LIV-04) ») | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Appel sans jeton | `POST …/{id}/deliver` sans `Authorization` | 401 `UNAUTHENTICATED` | `DeliveryCompletionIT.markDelivered_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton vendeuse | 403 `FORBIDDEN` | `DeliveryCompletionIT.markDelivered_sellerRole_returns403` | ✅ auto |
| Administrateur | commande d'un livreur quelconque | 200 `DELIVERED` | `DeliveryCompletionIT.markDelivered_administratorOnAnyOrder_returns200`, `DeliveryCompletionServiceImplTest.markDelivered_administrator_deliversAnOrderOfAnyDriver` | ✅ auto |
| Commande d'un autre livreur | Ando marque livrée une commande de Tojo | 404 `NOT_FOUND` (on ne révèle pas qu'elle existe) ; commande `TO_PREPARE`, aucune vente, rien au journal | `DeliveryCompletionIT.markDelivered_orderOfAnotherDriver_returns404AndKeepsTheOrder`, `DeliveryCompletionServiceImplTest.markDelivered_orderOfAnotherDriver_throwsNotFoundAndRecordsNoSale` | ✅ auto |
| Commande inconnue | id aléatoire | 404 `NOT_FOUND` | `DeliveryCompletionIT.markDelivered_unknownOrder_returns404`, `DeliveryCompletionServiceImplTest.markDelivered_unknownOrder_throwsNotFound` | ✅ auto |
| Champs interdits (mass assignment) | corps `{"id":…,"status":"FAILED","saleId":…,"driverId":…,"amount":1}` | ignorés (l'endpoint ne lit aucun corps) : 200, la commande de l'URL passe `DELIVERED`, livreur et montant inchangés, une seule vente | `DeliveryCompletionIT.markDelivered_forbiddenFieldsInTheBody_areIgnored` | ✅ auto |
| Déjà livrée (doublon) | second « livrée » sans clé d'idempotence | 409 `CONFLICT` « La livraison ne peut pas passer de « livrée » à « livrée ». » ; une seule vente (Livraison 4 → 2, un mouvement `SALE`) | `DeliveryCompletionIT.markDelivered_orderAlreadyDelivered_returns409AndSellsOnlyOnce`, `DeliveryCompletionServiceImplTest.markDelivered_alreadyDelivered_throwsConflictBeforeAnySale`, `DeliveryOrderTest.deliver_alreadyDeliveredOrder_throwsConflictAndKeepsTheFirstSale` | ✅ auto |
| Articles absents de la Livraison | 1 savon en Livraison pour une commande de 2 | 409 `CONFLICT` (stock insuffisant, VTE-01) ; commande `TO_PREPARE`, `sale_id` vide, aucun mouvement `SALE` | `DeliveryCompletionIT.markDelivered_articlesNotInTheDeliveryLocation_returns409AndChangesNothing`, `DeliveryCompletionServiceImplTest.markDelivered_saleRefused_leavesTheOrderUnchangedAndTracesNothing` | ✅ auto |
| Journée non ouverte | aucune journée du jour (JR-01) | 409 `CONFLICT` « La journée du … » ; commande `TO_PREPARE`, stock inchangé | `DeliveryCompletionIT.markDelivered_dayNotOpened_returns409AndKeepsTheOrder` | ✅ auto |
| Canal Livraison absent du référentiel | `findSalesChannelId("DELIVERY")` vide | 422 `VALIDATION_FAILED` « Le canal de vente Livraison est introuvable. », aucune vente | `DeliverySaleTest.record_deliveryChannelMissing_throwsBusinessExceptionAndRecordsNoSale` | ✅ auto |
| Vente refusée | `SaleService.record` lève 409 | l'erreur remonte telle quelle | `DeliverySaleTest.record_saleRefused_propagatesTheConflict` | ✅ auto |
| Refus affiché au livreur | le serveur répond 409 (journée non ouverte) au « livrée » envoyé | toast « Rasoa Vola : saisie refusée. La journée du 09/11/2026 n'est pas ouverte. » (ton `danger`) puis la tournée est relue | `driver-round-page.spec.ts` (« annonce un « Livrée » refusé par le serveur et relit la tournée ») | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Rejeu hors ligne (même `Idempotency-Key`) | même « livrée » envoyé deux fois avec la même clé | 1er : 200 ; 2e : 200 avec `Idempotency-Replayed: true` et la même réponse ; une seule vente (Livraison 3 → 2) | `DeliveryCompletionIT.markDelivered_replayedWithSameIdempotencyKey_sellsOnce` | ✅ auto |
| Hors ligne à la saisie | réseau coupé au moment de l'envoi | la saisie part dans la file (`OutboxService.submit`, `queued`), le bandeau « Hors ligne : vos saisies (1)… » l'annonce ; envoi au retour du réseau | couvert par SYS-07 (`outbox-service.spec.ts`) ; branchement vérifié par 1m | ✅ auto |
| Quantité décimale | 0,5 huile | vendue telle quelle (3 décimales) | `DeliverySaleTest.record_orderEnteredBySeller_recordsADeliverySaleCreditedToHerAtTheOrderPrices` | ✅ auto |
| Commande `TO_PREPARE` (jamais partie en tournée) | « livrée » directement | autorisé (`TO_PREPARE → DELIVERED`, table LIV-01) | `DeliveryOrderTest.deliver_orderToPrepare_movesToDeliveredInOneStep` | ✅ auto |
| Concurrence | deux « livrée » simultanés sur la même commande (deux téléphones) | une seule transaction aboutit : verrou optimiste `version` de la commande (409) et `uq_delivery_orders_sale` ; la vente de la transaction perdante est annulée avec elle | non automatisé (recette R4) | 🧪 recette (R4) |
| Tests qui vident les ventes | `BusinessDayIT`, `MorningTransferIT`, `SaleIT` vident `sales` | les commandes livrées sont d'abord détachées (`sale_id` vidé, statut `CANCELLED`) pour respecter la clé étrangère | `BusinessDayIT.forgetToday`, `MorningTransferIT.forgetToday`, `SaleIT.createSale_dayNotOpened_returns409` | ✅ auto |
| Statut `DELIVERED` dans les tests de LIV-06 | `DeliveryTrackingIT` met des commandes à `DELIVERED` | passage par `POST …/deliver` (journée ouverte, stock en Livraison) au lieu d'un `update` SQL que `ck_delivery_orders_delivered_has_sale` refuse | `DeliveryTrackingIT.getOrders_filteredByEachStatus_returnsOnlyTheOrderInThatStatus`, `DeliveryTrackingIT.getOrders_pastDayFilteredByStatus_returnsOnlyTheOrdersOfThatDay` | ✅ auto |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/delivery/controller/DeliveryCompletionIT.java` (15), `…/service/impl/DeliveryCompletionServiceImplTest.java` (7), `…/service/impl/DeliverySaleTest.java` (5), `…/domain/DeliveryOrderTest.java` (+3).
- Frontend : `valimbavaka-ui/src/app/features/deliveries/pages/driver-round-page/driver-round-page.spec.ts` (+3), `…/data-access/delivery-service.spec.ts` (+2).

## Recette manuelle (🧪)

| # | Étape | Attendu | OK |
|---|---|---|---|
| R1 | Administrateur : ouvrir la journée avec 3 savons Entrepôt → Livraison (`/day`), puis la vendeuse saisit une commande du jour de 2 savons pour Tojo | commande à l'arrêt n de la tournée de Tojo | ☐ |
| R2 | Tojo sur `/round` : « Livrée », attendre 5 s | l'arrêt passe dans l'historique ; après rechargement il reste livré ; journal : « Commande LIV-… livrée : vente VTE-… » et « Vente VTE-… · Livraison · … · À la livraison » ; stock Livraison −2 ; la vente est créditée à la vendeuse | ☐ |
| R3 | « Livrée » puis « Annuler » avant 5 s | rien n'est envoyé, l'arrêt reste à faire | ☐ |
| R4 | Deux téléphones du même livreur marquent la même commande livrée | une seule vente ; le second reçoit le toast « saisie refusée » | ☐ |
| R5 | Commande sans stock en Livraison, ou journée non ouverte | toast « saisie refusée » avec le message du serveur ; la commande reste à faire | ☐ |
| R6 | (après FAC-01) livraison marquée livrée | la facture de la vente existe | ☐ |

## Résultat de l'exécution

- Date : 2026-11-11, commit `5d6be4c` de `feature/LIV-03-marquage-rapide-livraison` (PR #216)
- Environnement : CI GitHub Actions — `ci.yml` vert (Backend Spring Boot avec Testcontainers PostgreSQL, Frontend Vitest + build), `conventions.yml` vert (Spotless, Checkstyle, Prettier, ESLint, gitleaks, PR et migrations), Qodana for JVM vert sans annotation
- Recette manuelle 🧪 : à passer par Njara après le merge
- Anomalies ouvertes : aucune connue
