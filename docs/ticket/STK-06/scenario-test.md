# Scénarios de test — STK-06 · Réception d'une livraison fournisseur (entrée en stock)

Statut : ✅ tests automatiques verts (CI) · recette 🧪 à passer · Ticket : [#113](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/113) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (nouvelle permission `stocks.receive`) ; `SELLER` et `DELIVERY_DRIVER` pour les 403 |
| Données de départ | Fournisseur actif « Huiles Ravo » (`LOCAL`) et un fournisseur désactivé ; « Savon noir 100 g » (`ART-0001`, pièce, seuil 40, coût 3 120,5 Ar) sans stock ; « Huile de coco » (`ART-0002`) ; un bon `BR-000007` en attente (`PENDING`) généré par STK-05 |
| Tickets à avoir déployés avant | FRS-01, STK-01, JNL-01, JNL-02, SYS-05, STK-05 |

## Scénarios issus des critères d'acceptation

### Critère 1 — La réception génère un mouvement d'entrée en stock à l'emplacement choisi (entrepôt par défaut), tracé au journal

Règle : un mouvement `RECEIPT` positif par ligne, vers l'emplacement de la réception, avec `referenceId` = id de la
réception ; sans `locationId`, l'entrepôt (`WAREHOUSE`). Le journal reçoit une ligne `CREATE` `SUPPLIER_RECEIPT` et une
ligne `CREATE` `STOCK_MOVEMENT` par mouvement (STK-01). Numéro `REC-000001`, `REC-000002`… donné par le serveur.

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Saisie libre sans emplacement | `POST /api/v1/stocks/receipts` `{"supplierId":…,"lines":[{"articleId":<savon>,"quantity":12,"unitCost":3120.5}]}` | 201, `Location`, `number` `REC-dddddd`, `status` `VALIDATED`, `locationId` = entrepôt, `lineCost` et `totalCost` 37 446 ; stock entrepôt 12 ; 1 mouvement `RECEIPT` ; 1 ligne `CREATE` au journal | `SupplierReceiptIT.receive_freeEntryWithoutLocation_returns201AndPutsTheGoodsInTheWarehouse` | ✅ auto |
| 1b | Emplacement choisi | `locationId` = boutique, quantité 2,5, coût 0 | stock boutique 2,5, entrepôt 0 | `SupplierReceiptIT.receive_chosenLocation_putsTheGoodsThere` | ✅ auto |
| 1c | Mouvements et libellé du journal | 2 lignes : savon 12 × 3 000, huile 2,5 × 8 400, numéro 12 | `recordAll` reçoit 2 `RECEIPT` (+12, +2,5) vers l'entrepôt avec l'id de la réception ; total 57 000 ; journal « Réception REC-000012 du fournisseur Huiles Ravo (saisie libre) : 2 article(s), 14.5 unité(s) » | `SupplierReceiptServiceImplTest.receive_freeEntry_recordsOneReceiptMovementPerLineAndTracesIt` | ✅ auto |
| 1d | Contrôles avant écriture | sans emplacement ; note « ␣␣Livraison de lundi␣ » | entrepôt, nom du fournisseur, code et nom de l'article copiés, note nettoyée | `SupplierReceiptPolicyTest.check_withoutLocation_receivesInTheWarehouseWithTheCopiedArticleAndSupplier` | ✅ auto |
| 1e | Écran : entrepôt par défaut | ouverture de « Réception fournisseur » | emplacement « Entrepôt » présélectionné ; seuls les fournisseurs actifs proposés | `supplier-receipt-page.spec.ts` › « reçoit par défaut à l’entrepôt et ne propose que les fournisseurs actifs » | ✅ auto |
| 1f | Écran : envoi | fournisseur, savon, quantité « 12,5 », coût 3000 | `POST` avec `quantity` 12.5, `restockOrderId` null ; message « Réception REC-000012 enregistrée : le stock est à jour. » ; retour aux stocks | `supplier-receipt-page.spec.ts` › « envoie la réception, annonce son numéro et revient aux stocks » | ✅ auto |

### Critère 2 — Une réception peut reprendre un bon de réapprovisionnement (STK-05) ou être saisie librement

Règle (hypothèse, voir le guide) : une réception reprend au plus un bon, le bon passe de `PENDING` à `RECEIVED` dans
la même transaction (journal `UPDATE` `RESTOCK_ORDER`) ; un bon déjà reçu est refusé (409). Les lignes restent celles
saisies : quantités et articles peuvent différer du bon (écart de livraison accepté).

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 2 | Reprise d'un bon | `restockOrderId` = bon généré pour le savon (40) | 201, `restockOrderId` renvoyé ; `GET /restock-orders/{id}` → `RECEIVED` ; stock 40 | `SupplierReceiptIT.receive_fromRestockOrder_marksItReceivedAndRefusesASecondReceipt` | ✅ auto |
| 2b | Bon déjà reçu | seconde réception du même bon | 409 `CONFLICT`, aucun stock en plus | `SupplierReceiptIT.receive_fromRestockOrder_marksItReceivedAndRefusesASecondReceipt` | ✅ auto |
| 2c | Journal de la reprise | bon `BR-000007`, réception 13 | journal « … (bon BR-000007) : … » | `SupplierReceiptServiceImplTest.receive_fromRestockOrder_marksTheOrderReceivedAndNamesItInTheJournal` | ✅ auto |
| 2d | Bon déjà reçu (unitaire) | `markReceived` lève 409 | rien d'enregistré : ni réception, ni mouvement, ni journal | `SupplierReceiptServiceImplTest.receive_restockOrderAlreadyReceived_throwsConflictAndSavesNothing` | ✅ auto |
| 2e | Passage `PENDING` → `RECEIVED` | bon `BR-000007`, réception `REC-000004` | statut `RECEIVED`, journal `UPDATE` « Bon de réapprovisionnement BR-000007 réceptionné (réception REC-000004) » | `RestockOrderServiceImplTest.markReceived_pendingOrder_becomesReceivedAndTracesAnUpdate` | ✅ auto |
| 2f | Bon déjà `RECEIVED` | `BR-000008` déjà reçu | 409 « Le bon de réapprovisionnement BR-000008 est déjà réceptionné. », pas de journal | `RestockOrderServiceImplTest.markReceived_orderAlreadyReceived_throwsConflictAndTracesNothing` | ✅ auto |
| 2g | Bon inconnu | `restockOrderId` aléatoire | 404, aucun mouvement | `SupplierReceiptIT.receive_unknownRestockOrder_returns404AndMovesNothing` | ✅ auto |
| 2h | Écran : reprise | choix de `BR-000007` (savon 40, huile 20, fournisseur Huiles Ravo) | lignes préremplies (40 et 20), coût actuel de l'article (3120.5, vide si l'article n'a pas de coût), fournisseur prérempli | `supplier-receipt-page.spec.ts` › « préremplit les lignes et le fournisseur en reprenant un bon de réapprovisionnement » | ✅ auto |
| 2i | Écran : seuls les bons en attente | liste avec un bon `RECEIVED` et un `PENDING` | seul `BR-000002` (`PENDING`) proposé | `stock-service.spec.ts` › « ne propose que les bons de réapprovisionnement encore en attente de livraison (STK-06) » | ✅ auto |

### Critère 3 — Une réception validée ne se supprime pas : une erreur se corrige par un mouvement inverse

Règle : aucun `DELETE` ni `PUT`. `POST /receipts/{id}/reverse` avec un motif enregistre un mouvement `RECEIPT`
**négatif** par ligne (type d'origine, signe contraire), passe la réception à `REVERSED` et trace `REVERSE` avec le motif.
Le contrôle de solde de STK-01 s'applique : si la marchandise a déjà quitté l'emplacement, 409 et rien ne change.

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 3 | Contre-passation | réception de 12 savons, motif « Livraison saisie deux fois » | 200, `status` `REVERSED` ; stock entrepôt 0 ; 2 mouvements `RECEIPT` (+12, −12) ; 1 ligne `REVERSE` | `SupplierReceiptIT.reverse_validatedReceipt_returnsReversedAndTakesTheGoodsBackOut` | ✅ auto |
| 3b | Mouvements inverses (unitaire) | 2 lignes (12 et 2,5), motif avec espaces | `RECEIPT` −12 et −2,5 référençant la réception ; journal « Contre-passation de la réception REC-000003 : Livraison saisie deux fois » | `SupplierReceiptServiceImplTest.reverse_validatedReceipt_recordsInverseMovementsAndTracesTheReason` | ✅ auto |
| 3c | Déjà contre-passée | second `reverse` | 409 « … statut REVERSED », aucun mouvement | `SupplierReceiptIT.reverse_validatedReceipt_returnsReversedAndTakesTheGoodsBackOut`, `SupplierReceiptServiceImplTest.reverse_alreadyReversedReceipt_throwsConflictAndMovesNothing` | ✅ auto |
| 3d | Marchandise déjà sortie | 12 reçus, 5 transférés hors de l'entrepôt, puis `reverse` | 409 « Stock insuffisant… » ; réception toujours `VALIDATED`, stock 7 | `SupplierReceiptIT.reverse_goodsAlreadyMovedOut_returns409AndKeepsTheReceiptValidated` | ✅ auto |
| 3e | Pas de suppression | `DELETE /receipts/{id}` | 4xx (méthode non exposée) ; règle ArchUnit `no_delete_endpoints` | `SupplierReceiptIT.deleteReceipt_isNotExposed` | ✅ auto |
| 3f | Signe du type `RECEIPT` | `RECEIPT` −1 | accepté (contre-passation) ; zéro toujours refusé | `StockMovementTypeTest.accepts_receiptWithNegativeQuantity_returnsTrueForTheReversalOfAReceipt` | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST /receipts` `{}` | 401 `UNAUTHENTICATED` | `SupplierReceiptIT.receive_withoutToken_returns401` | ✅ auto |
| Mauvais rôle (réception) | jeton vendeuse, corps valide | 403 `FORBIDDEN` | `SupplierReceiptIT.receive_withSellerRole_returns403` | ✅ auto |
| Mauvais rôle (contre-passation) | jeton livreur | 403 | `SupplierReceiptIT.reverse_withDeliveryDriverRole_returns403` | ✅ auto |
| Mass assignment | `{"id":…,"status":"REVERSED","number":"REC-999999",…}` | 201, `id` et `number` du serveur, `status` `VALIDATED` | `SupplierReceiptIT.receive_massAssignmentOfIdStatusAndNumber_isIgnored` | ✅ auto |
| Données invalides | `lines` vide ; quantité 0 ; coût −1 | 422 `VALIDATION_FAILED` | `SupplierReceiptIT.receive_invalidLines_returns422` | ✅ auto |
| Article en double | même article sur deux lignes | 422 « Un article ne peut figurer qu'une fois dans une réception. » | `SupplierReceiptIT.receive_sameArticleTwice_returns422`, `SupplierReceiptPolicyTest.check_sameArticleTwice_throwsBusinessException` | ✅ auto |
| Fournisseur désactivé | fournisseur passé inactif (`PATCH /suppliers/{id}/status`) | 422, aucun article lu | `SupplierReceiptIT.receive_deactivatedSupplier_returns422`, `SupplierReceiptPolicyTest.check_deactivatedSupplier_throwsBusinessExceptionBeforeReadingTheArticles` | ✅ auto |
| Emplacement inactif | `locationId` inconnu ou inactif | 422 | `SupplierReceiptPolicyTest.check_inactiveLocation_throwsBusinessException` | ✅ auto |
| Ressource inconnue | fournisseur aléatoire ; `GET /receipts/<uuid>` | 404 `NOT_FOUND` | `SupplierReceiptIT.receive_unknownSupplier_returns404`, `SupplierReceiptIT.getReceipts_afterReceipt_listsItFirstAndUnknownIdReturns404`, `SupplierReceiptServiceImplTest.getReceiptById_unknownId_throwsNotFound` | ✅ auto |
| Motif vide | `{"reason":"  "}` | 422 | `SupplierReceiptIT.reverse_blankReason_returns422` | ✅ auto |
| Écran : vendeuse | `SELLER` sur la page des stocks | bouton « Réception fournisseur » absent ; route gardée par `stocks.receive` | — (garde `permissionGuard('stocks.receive')`, `stocks.routes.ts`) | 🔍 code |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Pagination | `size=101` | 422 ; liste triée du plus récent au plus ancien | `SupplierReceiptIT.getReceipts_afterReceipt_listsItFirstAndUnknownIdReturns404` | ✅ auto |
| Bornes des lignes | 101 lignes | 422 (`@Size(max = 100)`) | — (contrainte lue dans `SupplierReceiptWDto`) | 🔍 code |
| Coût nul | `unitCost` 0 (don, échantillon) | accepté | `SupplierReceiptIT.receive_chosenLocation_putsTheGoodsThere` | ✅ auto |
| Écran : saisie invalide | quantité 0, coût −1 | rien n'est envoyé ; « Quantité supérieure à zéro… », « Coût positif ou nul… » | `supplier-receipt-page.spec.ts` › « refuse une quantité nulle ou un coût négatif sans rien envoyer » | ✅ auto |
| Écran : refus du serveur | 409 bon déjà reçu | message d'erreur affiché, on reste sur la page | `supplier-receipt-page.spec.ts` › « affiche le refus du serveur (bon déjà reçu) et reste sur la page » | ✅ auto |
| Hors ligne | réseau coupé à la validation | saisie en file (`Idempotency-Key`), « Saisie enregistrée hors connexion » ; le rejeu ne crée pas deux réceptions (SYS-07) | `stock-service.spec.ts` › « enregistre une réception fournisseur par la file hors ligne, avec une clé d’idempotence (STK-06) » (en-tête) ; rejeu : 🧪 | ✅ auto · 🧪 rejeu |
| Concurrence sur un bon | deux réceptions simultanées du même bon | une seule passe ; l'autre 409 (verrou optimiste du bon, index unique `uq_supplier_receipts_restock_order`) | — | 🧪 recette |
| Double clic | « Valider la réception » pendant l'envoi | bouton désactivé (`isSaving`) | — | 🧪 recette |

## Tests existants

- Back : `feature/stock/service/impl/SupplierReceiptServiceImplTest.java`, `SupplierReceiptPolicyTest.java` (nouveaux),
  `RestockOrderServiceImplTest.java` (+2 tests `markReceived_*`), `feature/stock/domain/StockMovementTypeTest.java`
  (test du signe de `RECEIPT` modifié) ; `feature/stock/controller/SupplierReceiptIT.java` (Testcontainers, nouveau).
- Front : `pages/supplier-receipt-page/supplier-receipt-page.spec.ts` (nouveau), `data-access/stock-service.spec.ts` (+2).

## Recette manuelle (🧪)

1. En administrateur, page Stocks → « Réception fournisseur » : choisir « Huiles Ravo », garder l'entrepôt, saisir
   12 savons à 3 000 Ar, valider. Message « Réception REC-00000N enregistrée » ; la page des stocks montre +12 à
   l'entrepôt ; le journal montre `CREATE` `SUPPLIER_RECEIPT` et `CREATE` `STOCK_MOVEMENT`.
2. Générer un bon (STK-05), puis « Réception fournisseur » → choisir le bon : lignes, quantités et coûts préremplis.
   Valider : `GET /api/v1/stocks/restock-orders/{id}` donne `RECEIVED` ; le bon n'est plus proposé.
3. `POST /api/v1/stocks/receipts/{id}/reverse` `{"reason":"Erreur de saisie"}` : statut `REVERSED`, stock revenu à
   l'état d'avant, journal `REVERSE` avec le motif. Un second appel répond 409.
4. Couper le réseau puis valider une réception : saisie en file ; au retour, une seule réception créée.
5. En vendeuse : pas de bouton « Réception fournisseur » ; `/stocks/receipt` refusé ; l'API répond 403.

## Résultat de l'exécution

- Date : 08/11/2026, CI GitHub (workflows `CI`, `Convention VVS` et `Qodana`, lancés à la main sur la branche) au
  commit `dff0f79` (branche à jour de `develop`).
- Back : `mvn -B -ntp verify` vert — 421 tests unitaires (dont `SupplierReceiptServiceImplTest` 6,
  `SupplierReceiptPolicyTest` 4, `RestockOrderServiceImplTest` 9, `StockMovementTypeTest` 14) et 438 tests
  d'intégration Testcontainers (dont `SupplierReceiptIT` 17) ; Spotless, Checkstyle et ArchUnit verts.
- Front : `ng test` vert — 71 fichiers (`supplier-receipt-page.spec.ts` 5, `stock-service.spec.ts` 7) ; Prettier et
  ESLint verts.
- Qodana : aucun problème dans les fichiers du ticket (les 2 signalés concernent `shared/excel/PdfTableWriter.java`,
  venu de `develop`).
- Anomalies ouvertes : aucune connue. Étapes 🧪 restantes : 5.
