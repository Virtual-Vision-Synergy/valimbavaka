# Guide d'intégration — STK-06 · Réception d'une livraison fournisseur (entrée en stock)

Statut : ✅ rédigé — tests à lancer (voir « Résultat de l'exécution » du scénario) · Ticket : [#113](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/113) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur enregistre la livraison d'un fournisseur actif : articles, quantités et coût unitaire d'achat,
vers l'emplacement choisi (l'entrepôt par défaut). Le serveur numérote la réception (`REC-000001`), fait entrer le
stock par un mouvement `RECEIPT` par ligne (STK-01) et la trace au journal. La réception peut reprendre un bon de
réapprovisionnement de STK-05, qui passe alors à `RECEIVED`. Elle ne se supprime jamais : une erreur se corrige par
une contre-passation qui enregistre les mouvements inverses (JNL-02).

## Contrat exposé

### Endpoints backend (`/api/v1/stocks/receipts`, administrateur seul)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| `POST` | `/api/v1/stocks/receipts` | `stocks.receive` | `SupplierReceiptWDto` | 201 + `Location`, `SupplierReceiptRDto` | 401, 403, 404 (fournisseur, article ou bon inconnu), 409 (bon déjà reçu), 422 |
| `GET` | `/api/v1/stocks/receipts?page&size&sortBy&sortDirection` | `stocks.receive` | `PageQuery` ; tri `createdAt` (défaut, `DESC`) ou `number` | `PageResponse<SupplierReceiptRDto>` (lignes incluses) | 401, 403, 422 |
| `GET` | `/api/v1/stocks/receipts/{id}` | `stocks.receive` | — | `SupplierReceiptRDto` | 401, 403, 404 |
| `POST` | `/api/v1/stocks/receipts/{id}/reverse` | `stocks.receive` | `SupplierReceiptReversalWDto` `{ reason }` (obligatoire, ≤ 500) | 200, `SupplierReceiptRDto` `REVERSED` | 401, 403, 404, 409 (déjà contre-passée, ou marchandise sortie de l'emplacement), 422 |

- `SupplierReceiptWDto` (`feature/stock/dto/SupplierReceiptWDto.java`) : `supplierId` (obligatoire), `locationId`
  (facultatif : absent = entrepôt), `restockOrderId` (facultatif), `lines` (1 à 100, sans `null`), `note` (≤ 500).
  Ni `id`, ni `number`, ni `status` : envoyés, ils sont ignorés (mass assignment testé).
- `SupplierReceiptLineWDto` : `articleId`, `quantity` (> 0, 9 chiffres et 3 décimales), `unitCost` (≥ 0, 12 chiffres et
  2 décimales, en ariary).
- `SupplierReceiptRDto` : `id`, `number`, `status` (`VALIDATED` | `REVERSED`, `OperationStatus`), `supplierId`,
  `supplierName`, `locationId`, `restockOrderId`, `note`, `createdAt`, `totalCost`, `lines`.
- `SupplierReceiptLineRDto` : `articleId`, `articleCode`, `articleName`, `quantity`, `unitCost`, `lineCost`
  (quantité × coût, arrondi au centime, `HALF_UP`).
- Controller : `feature/stock/controller/SupplierReceiptController.java`. Aucun `PUT`, `PATCH` ni `DELETE`.

### Services et ports Java réutilisables

- `SupplierReceiptService` (`feature/stock/service/SupplierReceiptService.java`) : `receive`, `getReceipts`,
  `getReceiptById`, `reverse(UUID, String reason)` ; implémenté dans `service/impl/SupplierReceiptServiceImpl.java:62`,
  `:81`, `:87`, `:93`. Les règles avant écriture sont dans `SupplierReceiptPolicy.check` (`SupplierReceiptPolicy.java:41`).
- Ajout à STK-05 : `RestockOrderService.markReceived(UUID restockOrderId, String receiptNumber)`
  (`RestockOrderServiceImpl.java:103`) → `PENDING` à `RECEIVED`, journal `UPDATE`, renvoie la référence `BR-…` ;
  domaine `RestockOrder.markReceived()` (`domain/RestockOrder.java:46`), 409 si déjà reçu.
- Domaine : `SupplierReceipt.create(number, SupplierReceiptCommand)` et `reverse()` (`domain/SupplierReceipt.java:68`,
  `:86`) ; lignes `@Embeddable` `SupplierReceiptLine` ; repository `SupplierReceiptRepository`
  (`OperationRepository`, `nextNumber()`, `findAll(Pageable)`).
- Modification de STK-01 : `StockMovementType.RECEIPT` accepte désormais les deux signes (négatif = contre-passation
  d'une réception) ; le test `StockMovementTypeTest.accepts_receiptWithNegativeQuantity_returnsTrueForTheReversalOfAReceipt`
  remplace l'ancien `…_returnsFalse`.

### Frontend (`features/stocks`)

- Route `/stocks/receipt` (garde `permissionGuard('stocks.receive')`, `stocks.routes.ts`), page
  `pages/supplier-receipt-page` ; le bouton « Réception fournisseur » de la page des stocks y mène (visible avec
  `stocks.receive`, `stock-page.ts:71`), à la place de l'ancienne action de démonstration.
- `StockService.recordReceipt(body)` (`data-access/stock-service.ts:97`) : `POST` par la file hors ligne
  (`OutboxService`, `Idempotency-Key`), `SubmitResult<SupplierReceiptRDto>`.
- `StockService.pendingRestockOrders()` (`stock-service.ts:85`) : les 100 bons les plus récents, filtrés sur `PENDING`.
- Types `SupplierReceiptWDto`, `SupplierReceiptLineWDto`, `SupplierReceiptRDto`, `SupplierReceiptLineRDto` dans
  `data-access/stock-models.ts`.
- La page (`supplier-receipt-page.ts:135`, `useOrder`) préremplit, à la reprise d'un bon, une ligne par article du bon
  (quantité commandée, coût d'achat actuel de l'article) et le fournisseur s'il est unique dans le bon.
- Traductions (fr, mg) : bloc `stocks.receipt.*` ; `auth-testing.ts` donne `stocks.receive` à `ADMIN`.

### Données

- Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-22-changelog.sql` :
  - séquence `supplier_receipt_number_seq` ;
  - table `supplier_receipts` (`number` unique, `status` CHECK `VALIDATED`/`REVERSED`, FK fournisseur, emplacement et
    bon ; index unique partiel `uq_supplier_receipts_restock_order` : un bon n'est repris qu'une fois ; audit, `version`) ;
  - table `supplier_receipt_lines` (PK `(supplier_receipt_id, line_number)`, unique `(supplier_receipt_id, article_id)`,
    `quantity > 0`, `unit_cost >= 0`, code et nom de l'article copiés) ;
  - permission `stocks.receive` donnée à `ADMIN`. Chaque changeset a son rollback.
- Journal (ressource `SUPPLIER_RECEIPT`) :
  - `CREATE` « Réception REC-000012 du fournisseur Huiles Ravo (saisie libre | bon BR-000007) : 2 article(s), 14.5 unité(s) » ;
  - `REVERSE` « Contre-passation de la réception REC-000012 : <motif> » ;
  - plus une ligne `CREATE` `STOCK_MOVEMENT` par mouvement (STK-01) et, en cas de reprise, `UPDATE` `RESTOCK_ORDER`.

## Règles métier à respecter par les autres tickets

- **Entrée en stock** : un mouvement `RECEIPT` positif par ligne vers l'emplacement de la réception, `referenceId` =
  id de la réception, dans la même transaction que la réception et son journal (tout ou rien).
- **Fournisseur** : doit exister (404) et être actif (422). Son nom est copié dans la réception.
- **Emplacement** : actif (422) ; absent = `WAREHOUSE` (`ReferentialService.requireActiveStockLocationId`).
- **Articles** : connus (404), une seule fois par réception (422). Un article désactivé est accepté (comme STK-01).
  Code et nom sont copiés : renommer l'article ne change pas la réception.
- **Reprise d'un bon** : un bon `PENDING` passe à `RECEIVED` ; un bon déjà reçu → 409, rien n'est enregistré.
- **Contre-passation** : seule correction possible. Mouvements `RECEIPT` négatifs (même emplacement, mêmes quantités),
  statut `REVERSED`, journal `REVERSE` avec le motif. 409 si déjà contre-passée, ou si le stock de l'emplacement ne
  couvre plus la quantité reçue (contrôle de solde de STK-01) : il faut d'abord ramener la marchandise.
- **Droits** : `stocks.receive` (ADMIN) pour tout ; la vendeuse et le livreur reçoivent 403.

## Fichiers concernés

- Back (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/stock/`) :
  - `controller/SupplierReceiptController.java`
  - `domain/{SupplierReceipt,SupplierReceiptLine,SupplierReceiptCommand,SupplierReceiptLineCommand}.java`
  - `domain/RestockOrder.java` (`markReceived`), `domain/StockMovementType.java` (signe de `RECEIPT`)
  - `dto/{SupplierReceiptWDto,SupplierReceiptLineWDto,SupplierReceiptReversalWDto,SupplierReceiptRDto,SupplierReceiptLineRDto}.java`
  - `mapper/SupplierReceiptMapper.java`, `repository/SupplierReceiptRepository.java`
  - `service/SupplierReceiptService.java`, `service/impl/{SupplierReceiptServiceImpl,SupplierReceiptPolicy}.java`
  - `service/RestockOrderService.java`, `service/impl/RestockOrderServiceImpl.java` (`markReceived`)
- Migration : `db/changelog/changes/2026_11_08-22-changelog.sql` (déclarée dans `db.changelog-master.yaml`)
- Front (`valimbavaka-ui/src/app/`) :
  - `features/stocks/pages/supplier-receipt-page/*` (nouveau)
  - `features/stocks/data-access/{stock-models,stock-service}.ts`, `features/stocks/stocks.routes.ts`
  - `features/stocks/pages/stock-page/stock-page.{ts,html}`
  - `core/auth/auth-testing.ts`, `public/i18n/{fr,mg}.json`
- Tests : voir [scénario de test](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** :
- [FRS-01](../FRS-01/description.md) · [guide](../FRS-01/guide.md) : `SupplierService.getSupplierById`, fournisseurs actifs.
- [STK-01](../STK-01/description.md) · [guide](../STK-01/guide.md) : `StockMovementService.recordAll`, contrôle de solde.
- [STK-05](../STK-05/description.md) · [guide](../STK-05/guide.md) : bon de réapprovisionnement repris.
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md) : `JournalService.record`.
- [JNL-02](../JNL-02/description.md) · [guide](../JNL-02/guide.md) : `OperationRepository`, `OperationStatus`, contre-passation.
- [SYS-05](../SYS-05/description.md) · [guide](../SYS-05/guide.md) : emplacements, rôle `ADMIN`.

**Ces tickets s'appuient sur lui** : aucun pour l'instant (un futur calcul du coût moyen pondéré pourrait lire les
réceptions).

## Comment l'utiliser depuis un autre ticket

```java
supplierReceiptService.reverse(receiptId, "Livraison saisie deux fois"); // 409 si déjà contre-passée ou stock sorti
```

```ts
inject(StockService).recordReceipt({ supplierId, locationId: null, restockOrderId: null, lines, note: null });
```

## Points d'attention

- **Hypothèse : une réception reprend au plus un bon, et le clôt** (`RECEIVED`), même si les quantités livrées
  diffèrent (écart accepté, la réception fait foi). Réception partielle en plusieurs livraisons pour un même bon : non
  prévue (index unique `uq_supplier_receipts_restock_order`) ; à reprendre si le client en a besoin.
- **Hypothèse : le bon peut être multi-fournisseurs** (STK-05 en fait un par génération) ; la réception porte un seul
  fournisseur choisi par l'administrateur, prérempli seulement si le bon n'a qu'un fournisseur. Aucun contrôle de
  cohérence fournisseur / lignes du bon.
- **Hypothèse : contre-passer une réception ne rouvre pas le bon** : il reste `RECEIVED`. Pour recommander, générer
  un nouveau bon ou saisir une réception libre.
- **Hypothèse : pas d'annulation à chaud** : le service n'implémente pas `ReversibleOperationService` (pas de
  `cancel`), car le stock entre dès la validation ; seule la contre-passation existe, avec la même signature
  `reverse(UUID, String)`.
- **Signe de `RECEIPT` élargi** (STK-01) : un `RECEIPT` négatif est accepté par `StockMovementService`. Seul
  `SupplierReceiptServiceImpl.reverse` l'utilise ; les autres features ne doivent pas s'en servir pour sortir du stock.
- **Le coût de la réception ne met pas à jour le coût de l'article** (`articles.cost`) ni ne calcule de coût moyen :
  à décider avec la valorisation (STK-03).
- **Pas d'écran de liste ni de bouton de contre-passation** : l'API existe (`GET /receipts`, `POST /{id}/reverse`),
  l'écran se limite à la saisie. À ajouter si la recette le demande.
- L'écran ne propose que les 100 articles actifs triés par nom (`StockService.quantities()`) et les 100 bons les plus
  récents encore `PENDING` : au-delà, passer par l'API.
- Tests écrits, à lancer en CI : voir le scénario avant de s'appuyer sur ce contrat.
