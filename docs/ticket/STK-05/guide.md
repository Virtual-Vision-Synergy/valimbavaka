# Guide d'intégration — STK-05 · Génération d'un bon de réapprovisionnement

Statut : ✅ rédigé (tests à exécuter) · Ticket : [#34](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/34) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur génère un bon de réapprovisionnement à partir des articles sous leur seuil (alertes STK-04), en
option limité aux articles qu'il a cochés. Le serveur enregistre le bon (`BR-000001`, statut `PENDING`) avec une ligne
par article : stock, seuil, quantité à commander, fournisseur, délai et livraison prévue (FRS-04), copiés au moment de
la génération. Il le trace au journal et renvoie la référence, que l'écran confirme. STK-06 (réception) reprend ce bon.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| `POST` | `/api/v1/stocks/restock-orders` | `stocks.restock` (ADMIN) | `RestockOrderWDto` `{ articleIds?: UUID[] }` (≤ 500, sans `null`) ; absent ou vide = toutes les alertes | 201 + `Location`, `RestockOrderRDto` | 401, 403, 409 (aucun article demandé sous le seuil), 422 |
| `GET` | `/api/v1/stocks/restock-orders?page&size&sortBy&sortDirection` | `stocks.restock` | `PageQuery` ; tri `createdAt` (défaut, `DESC`) ou `reference` | `PageResponse<RestockOrderRDto>` (lignes incluses) | 401, 403, 422 |
| `GET` | `/api/v1/stocks/restock-orders/{id}` | `stocks.restock` | — | `RestockOrderRDto` | 401, 403, 404 |

- `RestockOrderRDto` (`feature/stock/dto/RestockOrderRDto.java`) : `id`, `reference`, `status` (`PENDING` | `RECEIVED`),
  `createdAt`, `lines`.
- `RestockOrderLineRDto` (`feature/stock/dto/RestockOrderLineRDto.java`) : `lineNumber` (dès 1), `articleId`,
  `articleCode`, `articleName`, `unit`, `stockQuantity`, `threshold`, `quantity` (à commander), `supplierId`,
  `supplierName`, `leadTimeDays`, `expectedDeliveryDate` (les quatre derniers `null` pour la fabrication interne).
- Controller : `feature/stock/controller/RestockOrderController.java`. Aucun `PUT`, `PATCH` ni `DELETE`.

### Services et ports Java réutilisables

- `RestockOrderService` (`feature/stock/service/RestockOrderService.java`) : `generate(RestockOrderWDto)`,
  `getRestockOrders(PageQuery)`, `getRestockOrderById(UUID)` ; implémenté dans `RestockOrderServiceImpl.java:64`, `:86`, `:94`.
- `StockService.getAllAlerts()` (`feature/stock/service/StockService.java:28`, `StockServiceImpl.java:96`) : toutes
  les alertes de seuil, non paginées, dans l'ordre de `getAlerts`. `getAlerts` s'appuie désormais dessus (même calcul).
- Domaine : `RestockOrder.create(long number)` (`domain/RestockOrder.java:33`), `RestockOrderLine.of(orderId, lineNumber,
  RestockOrderLineCommand)`, `RestockOrderStatus`.
- Repositories (sans suppression, `OperationRepository`) : `RestockOrderRepository` (`nextNumber()`, `findAll(Pageable)`)
  et `RestockOrderLineRepository` (`saveAll`, `findLinesOf(Collection<UUID>)`).

### Frontend (`features/stocks`)

- Route : aucune nouvelle ; tout se passe sur la page des stocks.
- `StockService.generateRestockOrder(articleIds)` (`data-access/stock-service.ts:63`) : `POST` via la file hors ligne
  (`OutboxService`, en-tête `Idempotency-Key`), renvoie `SubmitResult<RestockOrderRDto>`.
- Types `RestockOrderWDto`, `RestockOrderRDto`, `RestockOrderLineRDto` dans `data-access/stock-models.ts`.
- `vvs-restock-draft` (`components/restock-draft/`) : entrées `alerts: StockAlertRDto[]` et `busy` ; sorties
  `validated` et `exported`. Il regroupe par fournisseur et affiche la quantité suggérée et la livraison prévue.
- `StockPage` (`pages/stock-page/stock-page.ts`) : bouton « Bon de réapprovisionnement » visible avec `stocks.restock`
  seulement (`canRestock`, `:114`) ; il coche toutes les alertes du serveur. Le brouillon montre les alertes cochées
  (`selectedAlerts`, `:117`) ; « Valider le bon » appelle le serveur (`validateRestock`, `:142`) et affiche
  « Bon de réapprovisionnement BR-… généré · N article(s) », ou le message d'erreur (ton danger), ou « Saisie
  enregistrée hors connexion ».
- Traductions (fr, mg) : `stocks.restock.subtitle` (modifiée), `stocks.restock.expectedDelivery`, `generated`, `failed`
  (nouvelles) ; `orderBefore` et `validated` supprimées.

### Données

- Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-20-changelog.sql` :
  - séquence `restock_order_number_seq` ;
  - table `restock_orders` (`reference` unique, `status` CHECK `PENDING`/`RECEIVED`, colonnes d'audit, `version`) ;
  - table `restock_order_lines` (FK vers le bon, l'article et le fournisseur ; uniques `(restock_order_id, line_number)` et
    `(restock_order_id, article_id)` ; `quantity > 0`, `stock_quantity >= 0`) ;
  - permission `stocks.restock` donnée à `ADMIN`. Chaque changeset a son rollback.
- Journal : `CREATE` / `RESTOCK_ORDER` / id du bon, « Génération du bon de réapprovisionnement BR-000007 : 2 article(s) »,
  dans la même transaction que le bon.

## Règles métier à respecter par les autres tickets

- **Contenu** : une ligne par alerte de seuil du moment (article actif, seuil > 0, stock total < seuil). Les articles
  demandés qui ne sont plus sous le seuil (ou inconnus) sont ignorés sans erreur ; s'il ne reste rien → 409, aucun
  bon ni numéro consommé.
- **Quantité à commander** = quantité suggérée de STK-04 (manque arrondi à la dizaine supérieure, minimum 10).
- **Ordre des lignes** = ordre des alertes (couverture croissante, puis délai décroissant, puis code), numérotées dès 1.
- **Copie figée** : code, nom, unité, fournisseur, délai et date de livraison prévue sont copiés ; modifier ensuite
  l'article ou le fournisseur ne change pas le bon. Une ligne n'est jamais modifiée (`@Immutable`).
- **Référence** `BR-` + 6 chiffres, donnée par la séquence ; une génération annulée laisse un trou dans la numérotation.
- **Un bon par génération**, tous fournisseurs confondus : le regroupement par fournisseur est un affichage.
- **Droits** : `stocks.restock` (ADMIN) pour générer et lire ; la vendeuse voit les alertes (`stocks.read`) mais pas les bons.

### Contrat pour STK-06 (réception d'une livraison fournisseur)

- Lire le bon à reprendre avec `RestockOrderService.getRestockOrderById(id)` (404 si inconnu) ou la liste
  `GET /api/v1/stocks/restock-orders`. Préremplir la réception avec `lines[].articleId`, `quantity`, `supplierId`.
- Le statut `RECEIVED` existe déjà (enum et CHECK en base) : STK-06 ajoute sur `RestockOrder` la méthode qui fait
  passer `PENDING` → `RECEIVED` (409 si déjà reçu) et trace un `UPDATE`. Aucune migration nécessaire pour ce statut.
- Les mouvements d'entrée peuvent porter l'id du bon dans `StockMovementCommand.referenceId`.
- Réception partielle, écart de quantité ou plusieurs livraisons pour un même bon : non prévus ici, à décider dans STK-06.

## Fichiers concernés

- Back :
  - `feature/stock/controller/RestockOrderController.java`
  - `feature/stock/domain/{RestockOrder,RestockOrderLine,RestockOrderLineCommand,RestockOrderStatus}.java`
  - `feature/stock/dto/{RestockOrderWDto,RestockOrderRDto,RestockOrderLineRDto}.java`
  - `feature/stock/mapper/RestockOrderMapper.java`
  - `feature/stock/repository/{RestockOrderRepository,RestockOrderLineRepository}.java`
  - `feature/stock/service/RestockOrderService.java`, `service/impl/RestockOrderServiceImpl.java`
  - `feature/stock/service/StockService.java`, `service/impl/StockServiceImpl.java` (`getAllAlerts`)
- Migration : `db/changelog/changes/2026_11_08-20-changelog.sql` (déclarée dans `db.changelog-master.yaml`)
- Front :
  - `features/stocks/components/restock-draft/*`
  - `features/stocks/data-access/{stock-models,stock-service,stock-levels}.ts` (`proposedQuantity` et `COVERAGE_DAYS` retirés)
  - `features/stocks/pages/stock-page/stock-page.{ts,html}`
  - `core/auth/auth-testing.ts` (`stocks.restock` pour ADMIN), `public/i18n/{fr,mg}.json`
- Tests : voir [scénario de test](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** :
- [FRS-01](../FRS-01/description.md) · [guide](../FRS-01/guide.md) : fournisseur de l'article.
- [FRS-04](../FRS-04/description.md) · [guide](../FRS-04/guide.md) : délai et date de livraison prévue des alertes.
- [STK-04](../STK-04/description.md) · [guide](../STK-04/guide.md) : alertes de seuil et quantité suggérée.
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md) : journal.

**Ces tickets s'appuient sur lui** :
- [STK-06](../STK-06/description.md) · [guide](../STK-06/guide.md) : réception à partir d'un bon.

## Comment l'utiliser depuis un autre ticket

```java
RestockOrderRDto order = restockOrderService.getRestockOrderById(restockOrderId); // 404 si inconnu
order.lines().forEach(line -> /* articleId, quantity, supplierId */ ...);
```

```ts
inject(StockService).generateRestockOrder([]); // [] = toutes les alertes ; Observable<SubmitResult<RestockOrderRDto>>
```

## Points d'attention

- **Hypothèse : un seul bon par génération**, tous fournisseurs confondus (le brouillon de la maquette le regroupait
  déjà par fournisseur à l'affichage). Si le client veut un bon par fournisseur, il faudra découper à la génération.
- **Hypothèse : quantité = quantité suggérée de STK-04**, non modifiable dans cet écran. L'ancien calcul de la maquette
  (30 jours de ventes, `proposedQuantity`) a été retiré : les ventes n'existent pas encore côté serveur.
- **Hypothèse : permission dédiée `stocks.restock`** réservée à l'administrateur (user story), distincte de `stocks.read`.
- L'écran ne connaît que les 100 alertes les plus urgentes (`StockService.alerts()`, STK-04) : au-delà, « Bon de
  réapprovisionnement » n'en coche que 100. Un appel API sans `articleIds` prend bien toutes les alertes.
- Le bouton « Exporter PDF » du brouillon reste une action de démonstration ; l'export du bon est hors périmètre.
- Aucun écran ne liste encore les bons générés : la liste existe côté API pour STK-06.
- `StockRowRDto` garde ses champs provisoires `supplierName`, `leadTimeDays`, `dailySales` (`stock-mapping.ts`,
  `PENDING_RESTOCK_FIELDS`) : ils ne servent plus au bon et pourront être retirés avec les fixtures.
- `getRestockOrders` charge les lignes des bons de la page en une requête (`findLinesOf`), pas de N+1.
- Le back n'a pas été compilé localement (Maven indisponible) : la CI fait foi. Les tests n'ont pas encore été exécutés.
