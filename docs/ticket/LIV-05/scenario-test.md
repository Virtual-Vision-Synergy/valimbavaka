# Scénarios de test — LIV-05 · Annulation d'une action récente

Statut : ✅ tests automatiques verts en CI (commit `a5862862`) ; recette 🧪 à passer · Ticket : [#50](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/50) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

> **Décision PO du 07/11/2026** (LIV-01) : une commande livrée est une vente (canal livraison) avec sa facture. Défaire « livrée » annule donc aussi la vente. Une livraison en échec n'a ni vente ni facture : défaire l'échec annule seulement le retour de stock (LIV-04).
>
> **Hypothèses prises (session de nuit, sans validation)** : délai « limité » = **5 minutes** après la dernière modification de la commande ; seules les actions « livrée » et « échec » sont annulables ; une action annulée ne peut pas être annulée une seconde fois (pas de « rétablir »).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôles utilisés | livreur « Tojo Nirina » (`deliveries.read`, `deliveries.deliver`, `deliveries.fail`, `deliveries.undo`) ; administrateur (`admin.test@valimbavaka.mg`, `deliveries.undo`) ; vendeuse (sans `deliveries.undo`) ; côté front, livreur « Hanta Rasolofoson » (`buildUser('DELIVERY_DRIVER')`) |
| Données de départ | article fini actif à 8 500 Ar ; stock posé dans `DELIVERY` par un mouvement `RECEIPT` ; journée du jour ouverte (JR-01) ; commande « Rakoto Be » du jour confiée à Tojo ; puis « livrée » (LIV-03) ou « en échec » (LIV-04) par l'API |
| Tickets à avoir déployés avant | LIV-01 (commandes), LIV-02 (`/round`, historique du jour), LIV-03 (livrée = vente), LIV-04 (échec = retour de stock), LIV-06 (historique lu dans le journal), JNL-01 (journal), VTE-01 (ventes), TRF-03 (transferts), SYS-02 / SYS-07 (`Idempotency-Key`, file hors ligne) |
| Migration | `2026_11_11-01-changelog.sql` : colonne `delivery_orders.previous_status` (CHECK `TO_PREPARE` / `ON_ROUND`) et permission `deliveries.undo` (administrateur, livreur) |

## Scénarios issus des critères d'acceptation

### Critère 1 — L'annulation restaure le statut précédent pendant un délai limité

`POST /api/v1/deliveries/{id}/undo` (sans corps). Le serveur vérifie que la commande est `DELIVERED` ou `FAILED`, que sa dernière modification date de moins de 5 minutes, annule ses effets (vente annulée et articles remis dans Livraison ; ou articles ramenés de l'Entrepôt vers Livraison), remet le statut d'avant et trace l'annulation, dans une seule transaction.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Livrée annulée | Tojo livre sa commande (2 savons, 5 en Livraison → 3), puis `POST …/undo` | 200 ; `status` `TO_PREPARE`, `saleId` absent ; la vente passe `CANCELLED` ; Livraison 3 → 5 | `DeliveryUndoIT.undo_deliveredOrder_restoresTheStatusCancelsTheSaleAndPutsTheStockBack` | ✅ auto |
| 1b · Retour au statut d'avant | commande `ON_ROUND` livrée puis annulée | `ON_ROUND` (pas `TO_PREPARE`) | `DeliveryUndoIT.undo_orderDeliveredFromARound_restoresOnRound` | ✅ auto |
| 1c · Échec annulé | échec « client absent » (3 savons en Livraison, 2 dans la commande : Livraison 3 → 1, Entrepôt +2), puis `undo` | 200 ; `TO_PREPARE`, `failureReason` et `returnTransferId` absents ; Livraison 1 → 3, Entrepôt 2 → 0 (transfert inverse) | `DeliveryUndoIT.undo_failedOrder_restoresTheStatusAndSendsTheArticlesBackToDelivery` | ✅ auto |
| 1d · Journal | livrée puis annulée | ligne `CANCEL` · `DELIVERY_ORDER` « Annulation de l'action sur la commande LIV-… : de « livrée » à « à préparer » » sans nom de client ; ligne `CANCEL` · `SALE` « Annulation de la vente VTE-… : annulation de la livraison LIV-… » | `DeliveryUndoIT.undo_deliveredOrder_isTracedInTheJournalWithoutCustomerData` | ✅ auto |
| 1e · Relivrer après annulation | livrée, annulée, livrée à nouveau | 200 ; nouvelle vente (autre `saleId`), la première reste `CANCELLED` ; Livraison 2 → 1 | `DeliveryUndoIT.undo_thenDeliveredAgain_createsASecondSaleAndKeepsTheFirstCancelled` | ✅ auto |
| 1f · Délai dépassé | livrée il y a 10 minutes (`updated_at` reculé) | 409 `CONFLICT` ; commande toujours `DELIVERED`, vente intacte, stock inchangé | `DeliveryUndoIT.undo_delayOver_returns409AndKeepsTheDelivery` | ✅ auto |
| 1g · Borne du délai | modifiée il y a 4 min 59 s, 5 min 00 s, 5 min 01 s | acceptée, acceptée, refusée (409) | `DeliveryOrderTest.undo_deliveredOrderWithinTheWindow_restoresThePreviousStatusAndForgetsTheSale`, `DeliveryOrderTest.undo_exactlyAtTheEndOfTheWindow_isStillAccepted`, `DeliveryOrderTest.undo_afterTheWindow_throwsConflictAndKeepsTheDelivery` | ✅ auto |
| 1h · Domaine : le statut d'avant est retenu | commande `ON_ROUND` puis `deliver` | `previousStatus` = `ON_ROUND` | `DeliveryOrderTest.moveTo_toDelivered_remembersThePreviousStatus` | ✅ auto |
| 1i · Domaine : échec annulé | commande `ON_ROUND`, `fail(ABSENT, « Portail fermé », retour)` puis `undo` à +1 min | `ON_ROUND`, motif, commentaire et retour effacés | `DeliveryOrderTest.undo_failedOrderWithinTheWindow_restoresThePreviousStatusAndForgetsTheFailure` | ✅ auto |
| 1j · Statuts annulables | les 5 statuts | seuls `DELIVERED` et `FAILED` | `DeliveryStatusTest.isUndoable_everyStatus_onlyDeliveredAndFailedCanBeUndone` | ✅ auto |
| 1k · Service : livrée | commande livrée il y a 0 min, horloge fixe +3 min | `SaleService.cancel(venteId, « annulation de la livraison LIV-0042 »)`, statut `TO_PREPARE`, journal `CANCEL` | `DeliveryUndoServiceImplTest.undoLastAction_deliveredOrder_cancelsTheSaleRestoresTheStatusAndTracesIt` | ✅ auto |
| 1l · Service : échec | commande en échec | `DeliveryReturn.undoReturn(commande)`, aucune annulation de vente | `DeliveryUndoServiceImplTest.undoLastAction_failedOrder_cancelsTheReturnAndRestoresTheStatus` | ✅ auto |
| 1m · Retour inverse | commande de 2 lignes (2 savons, 0,5 huile) | transfert `MANUAL` de `WAREHOUSE` vers `DELIVERY`, mêmes lignes, note « Annulation du retour de la livraison LIV-0042 » | `DeliveryReturnTest.undoReturn_orderWithTwoLines_sendsEveryArticleBackFromTheWarehouseToDelivery` | ✅ auto |
| 1n · Vente : annulation | vente validée du jour ouvert | statut `CANCELLED`, `SaleLedger.unpost` appelé avec le motif nettoyé | `SaleServiceImplTest.cancel_saleOfTheOpenDay_cancelsItAndPutsTheStockBack`, `SaleTest.cancel_validatedSale_becomesCancelled` | ✅ auto |
| 1o · Vente : stock et événement | vente de 2 lignes (3 savons, 1 huile) | mouvements `RETURN` positifs (3 et 1) dans l'emplacement du canal, référençant la vente ; journal `CANCEL` ; `SaleCancelledEvent` publié | `SaleLedgerTest.unpost_twoLines_putsTheStockBackInTheLocationTracesAndPublishes` | ✅ auto |
| 1p · Écran : bouton « Annuler » | historique du jour (`/round/history`) : une livrée il y a 2 min, un échec il y a 1 min, une livrée il y a 30 min | « Annuler l'action sur … » pour les deux premiers seulement | `driver-history-page.spec.ts` (« propose d'annuler une livraison ou un échec de moins de 5 minutes, pas les plus anciens ») | ✅ auto |
| 1q · Écran : annulation | clic sur « Annuler » de Rakoto Be | `undoLastAction('order-1')`, toast « Rakoto Be : action annulée », tournée relue | `driver-history-page.spec.ts` (« annule la dernière action, annonce le résultat et relit la tournée ») | ✅ auto |
| 1r · Service front | `undoLastAction('order-1')` | `OutboxService.submit('POST', '/api/v1/deliveries/order-1/undo')` | `delivery-service.spec.ts` (« annule la dernière action par la file hors ligne… ») | ✅ auto |
| 1s · Mapping front | commande `DELIVERED` modifiée à 07:00 | `changedAt` repris ; annulable jusqu'à 07:05:00 inclus, pas pour `TO_PREPARE` / `ON_ROUND` | `delivery-mapping.spec.ts` (« reporte la dernière modification serveur… », « autorise l'annulation d'une livraison ou d'un échec de moins de 5 minutes ») | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Appel sans jeton | `POST …/{id}/undo` sans `Authorization` | 401 `UNAUTHENTICATED` | `DeliveryUndoIT.undo_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton vendeuse | 403 `FORBIDDEN` | `DeliveryUndoIT.undo_sellerRole_returns403` | ✅ auto |
| Administrateur | commande livrée d'un livreur quelconque | 200, `TO_PREPARE` | `DeliveryUndoIT.undo_administratorOnAnyOrder_returns200`, `DeliveryUndoServiceImplTest.undoLastAction_administrator_undoesAnOrderOfAnyDriver` | ✅ auto |
| Commande d'un autre livreur | Ando annule la livraison de Tojo | 404 `NOT_FOUND` ; commande toujours `DELIVERED`, vente et stock intacts | `DeliveryUndoIT.undo_orderOfAnotherDriver_returns404AndKeepsTheDelivery`, `DeliveryUndoServiceImplTest.undoLastAction_orderOfAnotherDriver_throwsNotFoundAndCancelsNothing` | ✅ auto |
| Commande inconnue | id aléatoire | 404 `NOT_FOUND` | `DeliveryUndoIT.undo_unknownOrder_returns404`, `DeliveryUndoServiceImplTest.undoLastAction_unknownOrder_throwsNotFound` | ✅ auto |
| Champs interdits (mass assignment) | corps `{"status":"FAILED","role":"ADMIN","id":…}` | ignorés : 200, la commande visée repasse `TO_PREPARE` | `DeliveryUndoIT.undo_forbiddenFieldsInTheBody_areIgnored` | ✅ auto |
| Rien à annuler (à préparer) | commande `TO_PREPARE` jamais livrée | 409 `CONFLICT` « Aucune action récente à annuler… » | `DeliveryUndoIT.undo_orderNotDeliveredNorFailed_returns409`, `DeliveryUndoServiceImplTest.undoLastAction_orderToPrepare_throwsConflict`, `DeliveryOrderTest.undo_orderNeverDeliveredNorFailed_throwsConflict` | ✅ auto |
| Doublon (déjà annulée) | second `undo` | 409 `CONFLICT` ; Livraison reste à 1, rien n'est rendu deux fois | `DeliveryUndoIT.undo_alreadyUndone_returns409`, `DeliveryOrderTest.undo_alreadyUndone_throwsConflict` | ✅ auto |
| Délai dépassé avant tout effet | commande livrée il y a 10 min | 409 avant d'annuler la vente ; rien au journal | `DeliveryUndoServiceImplTest.undoLastAction_delayOver_throwsConflictBeforeTouchingTheSaleOrTheStock` | ✅ auto |
| Journée de la vente clôturée | vente d'une autre journée que la journée ouverte | 409 `CONFLICT`, vente toujours `VALIDATED`, aucun mouvement | `SaleServiceImplTest.cancel_saleOfAClosedDay_throwsConflictAndKeepsTheSale` | ✅ auto |
| Vente déjà annulée | `Sale.cancel()` deux fois | 409 `CONFLICT` | `SaleServiceImplTest.cancel_alreadyCancelledSale_throwsConflictAndMovesNoStock`, `SaleTest.cancel_alreadyCancelledSale_throwsConflict` | ✅ auto |
| Vente inconnue | id aléatoire | 404 `NOT_FOUND` | `SaleServiceImplTest.cancel_unknownSale_throwsNotFound` | ✅ auto |
| Vente non annulable : la commande ne bouge pas | `SaleService.cancel` refuse (journée clôturée) | 409 ; commande toujours `DELIVERED` avec sa vente ; rien au journal | `DeliveryUndoServiceImplTest.undoLastAction_saleCannotBeCancelled_keepsTheOrderDelivered` | ✅ auto |
| Articles de l'échec repartis de l'Entrepôt | 2 savons sortis de l'Entrepôt (inventaire) après l'échec | 409 `CONFLICT` (stock négatif refusé par STK-01) ; commande toujours `FAILED`, Livraison inchangée | `DeliveryUndoIT.undo_failureWhoseArticlesLeftTheWarehouse_returns409AndKeepsTheFailure`, `DeliveryReturnTest.undoReturn_warehouseLacksTheArticles_propagatesTheConflict` | ✅ auto |
| Refus affiché au livreur | le serveur répond 409 à l'annulation | toast « Rakoto Be : annulation refusée. Le délai d'annulation… » (ton `danger`) puis la tournée est relue | `driver-history-page.spec.ts` (« annonce une annulation refusée par le serveur (délai dépassé) et relit la tournée ») | ✅ auto |
| Accessibilité de l'historique | page avec un bouton « Annuler » | aucune violation axe (WCAG 2.2 AA, hors contraste) | `driver-history-page.spec.ts` (« n'a aucune violation d'accessibilité détectable ») | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Rejeu hors ligne (même `Idempotency-Key`) | même annulation envoyée deux fois avec la même clé | 1er : 200 ; 2e : 200 avec la même réponse ; stock rendu une seule fois (Livraison 1) | `DeliveryUndoIT.undo_replayedWithSameIdempotencyKey_undoesOnce` | ✅ auto |
| Hors ligne à l'annulation | réseau coupé au moment du clic | l'annulation part dans la file (`queued`) et s'applique au retour du réseau, après la livraison (ordre strict, SYS-10) ; si les 5 minutes sont passées entre-temps, 409 et toast « annulation refusée » | couvert par SYS-07 ; branchement vérifié par `delivery-service.spec.ts` (1r) ; recette R4 | ✅ auto |
| Commandes livrées avant LIV-05 | ligne déjà `DELIVERED` sans `previous_status` | 409 « Aucune action récente à annuler » (pas annulable) | `DeliveryOrderTest.undo_orderNeverDeliveredNorFailed_throwsConflict` (même garde) | ✅ auto |
| Journée changée | livrée à 23:58, annulée à 00:02 (journée du jour pas ouverte) | 409 : la vente ne peut plus être annulée | recette R5 | ☐ |
| Concurrence | deux téléphones annulent en même temps | une seule transaction aboutit (verrou optimiste `version`, puis 409) | non automatisé (recette R6) | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/delivery/controller/DeliveryUndoIT.java` (16), `…/delivery/service/impl/DeliveryUndoServiceImplTest.java` (8), `…/delivery/service/impl/DeliveryReturnTest.java` (+2), `…/delivery/domain/DeliveryOrderTest.java` (+7), `…/delivery/domain/DeliveryStatusTest.java` (+5 cas paramétrés), `…/sale/service/impl/SaleServiceImplTest.java` (+4), `…/sale/service/impl/SaleLedgerTest.java` (+1), `…/sale/domain/SaleTest.java` (+2).
- Frontend : `valimbavaka-ui/src/app/features/deliveries/pages/driver-history-page/driver-history-page.spec.ts` (4, nouveau), `…/data-access/delivery-service.spec.ts` (+1), `…/data-access/delivery-mapping.spec.ts` (+2).

## Recette manuelle (🧪)

| # | Étape | Attendu | OK |
|---|---|---|---|
| R1 | Journée ouverte, 3 savons en Livraison, commande de 2 savons pour Tojo ; Tojo sur `/round` : « Livrée », attendre 5 s | l'arrêt passe dans `/round/history` en « Livrée » | ☐ |
| R2 | Dans `/round/history`, « Annuler » avant 5 minutes | toast « action annulée » ; l'arrêt n'est plus dans l'historique et revient dans « Prochain arrêt » ; la vente VTE-… est annulée dans `/sales` ; stock Livraison de retour à 3 | ☐ |
| R3 | Même parcours avec « Échec » (« Client absent »), puis « Annuler » | l'arrêt revient à faire ; stock Livraison 3, Entrepôt inchangé par rapport au départ ; journal : « Annulation de l'action… de « en échec » à « à préparer » » | ☐ |
| R4 | Couper le réseau, « Annuler », rétablir | bandeau « Hors ligne : vos saisies (1)… » puis annulation appliquée ; si plus de 5 minutes : toast « annulation refusée » | ☐ |
| R5 | Livrer à 23:55, attendre minuit, tenter l'annulation | 409 « Journée … n'est pas ouverte » ou « sa journée est clôturée » (toast « annulation refusée ») | ☐ |
| R6 | Deux téléphones du même livreur annulent la même livraison | une seule annulation ; le second reçoit « annulation refusée » | ☐ |
| R7 | Vérifier que le bouton « Annuler » disparaît de l'historique après 5 minutes (sans recharger) | bouton retiré au plus tard 30 s après le délai | ☐ |

## Résultat de l'exécution

- Date : 11/11/2026, CI GitHub Actions sur `feature/LIV-05-annulation-action-recente` (commit `a5862862`, workflows CI, Convention VVS et Qodana verts ; Qodana for JVM : 0 annotation)
- Environnement : CI (Testcontainers PostgreSQL, Vitest). Backend : 676 tests, 0 échec, dont `DeliveryUndoIT` (16), `DeliveryUndoServiceImplTest` (8), `DeliveryOrderTest` (16), `SaleServiceImplTest` (6). Frontend : 111 fichiers de tests, 0 échec, dont `driver-history-page.spec.ts` (4)
- Non automatisés, restent ☐ : journée changée (R5), concurrence (R6) et la recette R1 à R7
- Anomalies ouvertes : aucune connue
