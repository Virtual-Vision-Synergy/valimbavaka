# Scénarios de test — LIV-04 · Signalement d'un échec de livraison

Statut : ☐ tests écrits, à lancer (CI ou local) ; recette 🧪 à passer · Ticket : [#49](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/49) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

> **Décision PO du 07/11/2026** (LIV-01) : une commande livrée devient une vente (canal livraison) avec sa facture, au moment où la livraison est marquée livrée. Une livraison **en échec ne crée ni vente ni facture** : seule « livrée » (LIV-03) le fait. LIV-04 remet seulement les articles en stock.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôles utilisés | livreur « Tojo Nirina » (`deliveries.read`, `deliveries.fail`) ; administrateur (`admin.test@valimbavaka.mg`, `deliveries.fail`) ; vendeuse (sans `deliveries.fail`) ; côté front, livreur « Hanta Rasolofoson » (`buildUser('DELIVERY_DRIVER')`) |
| Données de départ | article fini actif à 8 500 Ar (`StockApiTestSupport.createArticle`) ; stock posé dans l'emplacement `DELIVERY` (Livraison) par un mouvement `RECEIPT` (`StockApiTestSupport.move`), à la place des transferts du matin (JR-01) ; commande « Rakoto Be » du jour (`DeliveryApiTestSupport.createdOrderId`) |
| Tickets à avoir déployés avant | LIV-01 (commandes, statuts, `DeliveryOrder.moveTo`), LIV-02 (écran `/round` et feuille « Échec »), TRF-03 (retour de stock `RET-…`), STK-01 (mouvements), JNL-01 (journal), SYS-02 / SYS-07 (`Idempotency-Key`, file hors ligne) |
| Migration | `2026_11_08-65-changelog.sql` : colonnes `failure_reason`, `failure_note`, `return_transfer_id` sur `delivery_orders`, contrainte « une commande `FAILED` a un motif et un retour », permission `deliveries.fail` (administrateur, livreur) |

## Scénarios issus des critères d'acceptation

### Critère 1 — Le statut Échec exige un motif et remet les articles en stock retour

`POST /api/v1/deliveries/{id}/failure` avec `{"reason": "ABSENT" | "ADDRESS_NOT_FOUND" | "REFUSED" | "OTHER", "note": "…"}` (commentaire facultatif, 280 caractères). Le serveur passe la commande à `FAILED` et enregistre un **retour de stock** TRF-03 (`RET-…`, origine et statut `RETURN`) de l'emplacement `DELIVERY` vers `WAREHOUSE`, pour chaque ligne de la commande, dans la même transaction.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Échec signalé par le livreur | Tojo, sa commande du jour (2 savons) ; 5 savons en Livraison ; `{"reason":"ABSENT","note":"  Portail fermé, personne  "}` | 200 ; `status` `FAILED`, `failureReason` `ABSENT`, `failureNote` « Portail fermé, personne » (espaces retirés), `returnTransferId` renseigné ; Livraison 5 → 3, Entrepôt +2 ; le bon `RET-…` a l'origine et le statut `RETURN` | `DeliveryFailureIT.reportFailure_driverOwnOrder_returns200FailedAndMovesTheArticlesBackToTheWarehouse` | ☐ |
| 1b · Aucune vente | même échec, 1 savon | les seuls mouvements de l'article sont `RECEIPT` puis deux `TRANSFER` (sortie Livraison, entrée Entrepôt) : ni `SALE` ni `DELIVERY` | `DeliveryFailureIT.reportFailure_failedOrder_createsNoSaleMovementOnlyTheReturnTransfer` | ☐ |
| 1c · Motif obligatoire | `{"note":"Pas de motif"}` | 422 `VALIDATION_FAILED` « Le motif de l'échec est obligatoire » ; commande toujours `TO_PREPARE` | `DeliveryFailureIT.reportFailure_withoutReason_returns422AndKeepsTheOrder` | ☐ |
| 1d · Domaine | commande `ON_ROUND`, `fail(REFUSED, « Colis abîmé », retour)` | statut `FAILED`, motif, commentaire et retour enregistrés | `DeliveryOrderTest.fail_orderOnRound_movesToFailedWithItsReasonCommentAndStockReturn` | ☐ |
| 1e · Service | commande du livreur, commentaire « ␣␣Portail fermé␣␣ » | commande `FAILED` avec retour, commentaire nettoyé ; journal `UPDATE` · `DELIVERY_ORDER` « Commande LIV-0042 en échec : client absent, 1 article(s) remis en stock » | `DeliveryFailureServiceImplTest.reportFailure_ownOrder_failsTheOrderReturnsTheArticlesAndTracesIt` | ☐ |
| 1f · Commentaire vide | `note` = « ␣␣␣ » | commentaire `null` | `DeliveryFailureServiceImplTest.reportFailure_blankComment_storesNoComment` | ☐ |
| 1g · Retour de stock | commande de 2 lignes (2 savons, 0,5 huile) | `TransferService.record` avec origine `RETURN`, de `DELIVERY` vers `WAREHOUSE`, mêmes articles et quantités, note « Retour de la livraison en échec LIV-0042 » | `DeliveryReturnTest.record_orderWithTwoLines_returnsEveryArticleFromDeliveryToTheWarehouse` | ☐ |
| 1h · Journal | échec avec commentaire « Portail fermé, personne » | ligne `UPDATE` · `DELIVERY_ORDER` qui se termine par « en échec : client absent, 1 article(s) remis en stock », **sans** le commentaire (il peut nommer le client) ; le retour a sa propre ligne `CREATE` · `TRANSFER` (TRF-03) | `DeliveryFailureIT.reportFailure_validFailure_isTracedInTheJournalWithoutTheDriverComment` | ☐ |
| 1i · Écran : envoi après le délai d'annulation | `/round`, arrêt « Rasoa Vola » ; « Échec », motif « Colis refusé », commentaire « ␣␣Le client a changé d'avis␣␣ », « Enregistrer l'échec » | l'arrêt quitte « Prochain arrêt » (Hery Andry passe devant) ; rien n'est envoyé pendant 5 s ; puis `updateStatus('order-2', { status: 'FAILED', failureReason: 'REFUSED', note: 'Le client a changé d'avis' })` | `driver-round-page.spec.ts` (« envoie l'échec avec son motif et son commentaire une fois le délai d'annulation passé ») | ☐ |
| 1j · Service front | `updateStatus('order-1', FAILED, ABSENT, « Portail fermé »)` | `OutboxService.submit('POST', '/api/v1/deliveries/order-1/failure', { reason: 'ABSENT', note: 'Portail fermé' })` | `delivery-service.spec.ts` (« envoie l'échec et son motif par la file hors ligne… ») | ☐ |
| 1k · « Livrée » reste simulée | `updateStatus(…, DELIVERED)` | aucun appel à la file (LIV-03) | `delivery-service.spec.ts` (« n'appelle pas l'API d'échec pour une livraison marquée livrée (LIV-03) ») | ☐ |
| 1l · Carte du tableau | commande API `FAILED`, motif `REFUSED` | carte « Échecs » avec le motif « Colis refusé » | `delivery-mapping.spec.ts` (« montre le motif d'une commande en échec sur sa carte ») | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Appel sans jeton | `POST …/{id}/failure` sans `Authorization` | 401 `UNAUTHENTICATED` | `DeliveryFailureIT.reportFailure_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton vendeuse | 403 `FORBIDDEN` | `DeliveryFailureIT.reportFailure_sellerRole_returns403` | ☐ |
| Administrateur | commande d'un livreur quelconque, `{"reason":"ADDRESS_NOT_FOUND"}` | 200, `failureReason` `ADDRESS_NOT_FOUND`, pas de commentaire | `DeliveryFailureIT.reportFailure_administratorOnAnyOrder_returns200`, `DeliveryFailureServiceImplTest.reportFailure_administrator_failsAnOrderOfAnyDriver` | ☐ |
| Commande d'un autre livreur | Ando signale l'échec d'une commande de Tojo | 404 `NOT_FOUND` (on ne révèle pas qu'elle existe) ; commande `TO_PREPARE`, stock inchangé, rien au journal | `DeliveryFailureIT.reportFailure_orderOfAnotherDriver_returns404AndKeepsTheOrder`, `DeliveryFailureServiceImplTest.reportFailure_orderOfAnotherDriver_throwsNotFoundAndWritesNothing` | ☐ |
| Commande inconnue | id aléatoire | 404 `NOT_FOUND` | `DeliveryFailureIT.reportFailure_unknownOrder_returns404`, `DeliveryFailureServiceImplTest.reportFailure_unknownOrder_throwsNotFound` | ☐ |
| Champs interdits (mass assignment) | `{"reason":"REFUSED","id":…,"status":"DELIVERED","returnTransferId":…,"driverId":…}` | ignorés : 200, la commande visée par l'URL passe `FAILED`, son livreur ne change pas, le retour est celui créé par le serveur | `DeliveryFailureIT.reportFailure_forbiddenFieldsInTheBody_areIgnored` | ☐ |
| Commentaire trop long | 281 caractères | 422 `VALIDATION_FAILED` | `DeliveryFailureIT.reportFailure_commentOver280Characters_returns422` | ☐ |
| Motif inconnu | `{"reason":"RAIN"}` | 400 `MALFORMED_REQUEST` | `DeliveryFailureIT.reportFailure_unknownReason_returns400` | ☐ |
| Déjà en échec (doublon) | second signalement, motif `REFUSED` | 409 `CONFLICT` « La livraison ne peut pas passer de « en échec » à « en échec ». » ; articles rendus une seule fois (Livraison 4 → 2, Entrepôt 2) | `DeliveryFailureIT.reportFailure_orderAlreadyFailed_returns409AndReturnsTheArticlesOnlyOnce` | ☐ |
| Déjà livrée | commande `DELIVERED` | 409 `CONFLICT` avant tout retour de stock, rien au journal | `DeliveryFailureServiceImplTest.reportFailure_deliveredOrder_throwsConflictBeforeAnyStockReturn`, `DeliveryOrderTest.fail_deliveredOrder_throwsConflictAndRecordsNoReason` | ☐ |
| Articles absents de la Livraison | aucun stock en `DELIVERY` (pas de transfert du matin) | 409 `CONFLICT` (stock négatif refusé par STK-01) ; commande `TO_PREPARE`, aucun mouvement | `DeliveryFailureIT.reportFailure_articlesNotInTheDeliveryLocation_returns409AndChangesNothing`, `DeliveryFailureServiceImplTest.reportFailure_stockReturnRefused_leavesTheOrderUnchangedAndTracesNothing` | ☐ |
| Emplacement Livraison désactivé | `requireActiveStockLocationId("DELIVERY")` refuse | 422 `VALIDATION_FAILED`, aucun retour | `DeliveryReturnTest.record_deliveryLocationInactive_throwsBusinessExceptionAndRecordsNoReturn` | ☐ |
| Refus affiché au livreur | le serveur répond 409 à l'échec envoyé | toast « Rasoa Vola : saisie refusée. La livraison ne peut pas passer… » (ton `danger`) puis la tournée est relue | `driver-round-page.spec.ts` (« annonce un échec refusé par le serveur et relit la tournée ») | ☐ |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Rejeu hors ligne (même `Idempotency-Key`) | même échec envoyé deux fois avec la même clé | 1er : 200 ; 2e : 200 avec `Idempotency-Replayed: true` et la même réponse ; un seul retour (Livraison 3 → 2, Entrepôt 1) | `DeliveryFailureIT.reportFailure_replayedWithSameIdempotencyKey_failsTheOrderOnce` | ☐ |
| Hors ligne à la saisie | réseau coupé au moment de l'envoi | la saisie part dans la file (`OutboxService.submit`, `queued`), le bandeau « Hors ligne : vos saisies (1)… » l'annonce ; envoi au retour du réseau | couvert par SYS-07 (`outbox-service.spec.ts`) ; branchement vérifié par `delivery-service.spec.ts` (1j) | ☐ |
| Annulation dans les 5 s | « Échec » puis « Annuler » | rien n'est envoyé (comportement LIV-02 inchangé) | recette R3 | ☐ |
| Quantité décimale | 0,5 huile | rendue telle quelle (3 décimales) | `DeliveryReturnTest.record_orderWithTwoLines_returnsEveryArticleFromDeliveryToTheWarehouse` | ☐ |
| Concurrence | deux échecs simultanés sur la même commande | une seule transaction aboutit (verrou optimiste `version`, puis 409 par `ensureCanMoveTo`) | non automatisé (recette R4) | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/delivery/controller/DeliveryFailureIT.java` (15), `…/service/impl/DeliveryFailureServiceImplTest.java` (7), `…/service/impl/DeliveryReturnTest.java` (2), `…/domain/DeliveryOrderTest.java` (+2).
- Frontend : `valimbavaka-ui/src/app/features/deliveries/pages/driver-round-page/driver-round-page.spec.ts` (+2), `…/data-access/delivery-service.spec.ts` (+2), `…/data-access/delivery-mapping.spec.ts` (+1).

## Recette manuelle (🧪)

| # | Étape | Attendu | OK |
|---|---|---|---|
| R1 | Administrateur : transfert de 3 savons Entrepôt → Livraison (`/transfers` ou API), puis commande du jour de 2 savons pour Tojo | commande à l'arrêt n de la tournée de Tojo | ☐ |
| R2 | Tojo sur `/round` : « Échec », motif « Client absent », commentaire, « Enregistrer l'échec », attendre 5 s | l'arrêt passe dans l'historique (`/round/history`) en échec ; après rechargement il ne revient pas ; journal : « Commande LIV-… en échec : client absent… » et « Retour de stock RET-… · Livraison → Entrepôt » ; stock Livraison −2, Entrepôt +2 | ☐ |
| R3 | Même geste puis « Annuler » avant 5 s | rien n'est envoyé, l'arrêt reste à faire | ☐ |
| R4 | Deux téléphones du même livreur signalent l'échec de la même commande | un seul retour `RET-…` ; le second reçoit le toast « saisie refusée » | ☐ |
| R5 | Commande sans stock en Livraison | toast « saisie refusée » avec le message de stock insuffisant ; la commande reste à faire | ☐ |

## Résultat de l'exécution

- Date : à lancer
- Environnement : à lancer (CI ou local)
- Anomalies ouvertes : aucune connue
