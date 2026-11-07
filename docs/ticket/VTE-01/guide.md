# Guide d'intégration — VTE-01 · Enregistrement d'une vente multicanal

Statut : ✅ écrit le 2026-11-08 · Ticket : [#62](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/62) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Ce ticket pose le **domaine « vente »** (`feature/sale`) : une vente est enregistrée dans la **journée ouverte** (JR-01), sur un **canal** du référentiel, **créditée à une vendeuse**, avec un **mode de règlement** et des **lignes à prix figés** ; le stock sort de l'emplacement du canal par des mouvements `SALE`, la vente est tracée au journal et un événement `SaleRecordedEvent` est publié. Une vente ne se supprime jamais (statut `VALIDATED` → `CANCELLED` / `REVERSED`, JNL-02). Le port Java `SaleService.record(SaleCommand)` permet à un autre module (LIV-03) de créer une vente. Le poste de caisse (`/pos`) est branché sur l'API.

## Contrat exposé

### Endpoints backend (`/api/v1/sales`)

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/sales/form` | `sales.create` (admin, vendeuse) | — | 200 `SaleFormRDto` | 401, 403 |
| POST | `/api/v1/sales` | `sales.create` (admin, vendeuse) | `SaleWDto` | 201 `SaleRDto` + `Location: /api/v1/sales/{id}` | 401, 403, 404 (article inconnu), 409 (journée non ouverte, stock insuffisant), 422 (voir règles) |
| GET | `/api/v1/sales/{id}` | `sales.read` (admin, vendeuse) | — | 200 `SaleRDto` | 401, 403, 404 |

- `SaleWDto` : `salesChannelId` (UUID de `sales_channels`), `sellerId` (compte vendeuse actif), `paymentMethod` (`CASH`, `MVOLA`, `ORANGE_MONEY`, `CARD`, `ON_DELIVERY` ; valeur inconnue → 400), `lines` (1 à 100 `SaleLineWDto` : `articleId`, `quantity` > 0, 9 chiffres et 3 décimales). **Aucun prix, numéro, statut, total ni id** : envoyés, ils sont ignorés.
- `SaleRDto` : `id`, `number` (`VTE-0042`), `businessDayId`, `businessDate`, `salesChannelId`, `stockLocationId`, `sellerId`, `paymentMethod`, `status` (`OperationStatus` : `VALIDATED`, `CANCELLED`, `REVERSED`), `subtotal`, `discountAmount` (0 pour l'instant), `total`, `lines` (`SaleLineRDto` : `articleId`, `quantity`, `unitPrice`, `lineTotal` ; le coût d'achat n'est **pas** exposé), `createdAt` (heure de la vente), `createdBy` (compte qui a saisi, distinct de la vendeuse créditée).
- `SaleFormRDto` : `nextNumber` (numéro probable, indicatif), `channels` (`SaleChannelOptionRDto` : `id`, `code`, `label`, `stockLocationId`), `sellers` (`SaleSellerRDto` : `id`, `firstName`, `lastName`, `salesChannelId`, 100 au plus, par prénom), `articles` (`SaleArticleRDto` : `id`, `code`, `barcode`, `name`, `rangeId`, `price`, `stock` = objet `{ idEmplacement: quantité }` pour les emplacements des canaux, emplacement absent = 0).
- Écriture rejouable : avec `Idempotency-Key` (file hors ligne SYS-07 / SYS-12), un rejeu renvoie la même vente.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.sale.service.SaleService` (`SaleService.java:24`) | `SaleRDto record(SaleCommand command)` | **Port des autres modules** (LIV-03 : commande livrée → vente canal Livraison). Journée ouverte exigée, contrôles, prix figés, stock, journal, événement, tout ou rien. À appeler dans une transaction. |
| idem (`:27`) | `SaleRDto createSale(SaleWDto request)` | Saisie au poste : `record` avec les prix courants des fiches. |
| idem (`:30`, `:33`) | `getSaleById(UUID)`, `getForm()` | Lecture d'une vente (404) ; formulaire. |

- `SaleCommand(salesChannelId, sellerId, paymentMethod, lines)` et `SaleLineCommand(articleId, quantity, @Nullable unitPrice)` (`feature/sale/domain/`) : `unitPrice` `null` = prix courant de l'article ; renseigné = **prix convenu** (ex. prix de la commande LIV-01), 2 décimales, ≥ 0.
- **Événement** `feature.sale.domain.SaleRecordedEvent(saleId, number)`, publié dans la transaction de la vente (`SaleLedger.java:37`). FAC-01 (facture) et COM-02 (commission) s'y abonnent sans que la vente dépende d'eux (pas de cycle ArchUnit).
- Implémentation : `service/impl/SaleServiceImpl.java:53` (`record`), `SalePolicy.java:38` (règles, prix `priceOf` l.65), `SaleChannels.java:35` (canal → emplacement), `SaleLedger.java:37` (mouvements, journal, événement), `SaleFormAssembler.java` (formulaire), `domain/Sale.java:83` (`Sale.record` : totaux).

### Frontend (`features/pos`)

- Route `/pos` (inchangée, garde `sales.read`). La page `pages/pos-page` encaisse pour de vrai : F12 / bouton « Encaisser » → `PosService.record` ; toast « Vente VTE-0042 encaissée · … », « Saisie enregistrée hors connexion », ou « Vente refusée : <message du serveur> » ; un second envoi est bloqué pendant l'envoi (`submitting`).
- `data-access/pos-service.ts` : `context()` = `GET /api/v1/sales/form` + gammes (`ReferentialService.listProductRanges`) → `PosContextRDto` ; `record(body)` → `OutboxService.submit('POST', '/api/v1/sales', body)` (`SubmitResult<SaleRDto>`).
- `data-access/pos-mapping.ts` : `toPosContext` (catalogue avec le stock de la Boutique, vendeuses, `channelIds` : `SHOP` → canal `STORE`, `FACEBOOK`, `PHONE`, vendeuse connectée proposée d'office), `toSaleWDto` (sans prix).
- `data-access/pos-models.ts` : types serveur `SaleFormRDto`, `SaleChannelOptionRDto`, `SaleSellerRDto`, `SaleArticleRDto`, `SaleWDto`, `SaleLineWDto`, `SaleRDto`, `SaleLineRDto`, `SaleStatus` ; `PosContextRDto` gagne `sellers`, `defaultSellerId`, `channelIds` ; `PosSeller`.
- `PosCart.seller` (signal) : vendeuse créditée, conservée d'un ticket à l'autre ; `canCheckout` l'exige. `components/pos-ticket` : liste « Vendeuse » (`<select id="pos-seller">`, input `sellers`).
- Traductions : `pos.ticket.sellerLabel`, `pos.ticket.sellerPlaceholder`, `pos.toast.sellerRequired`, `pos.toast.failed` (fr, mg).

### Données

- Migration `2026_11_08-11-changelog.sql` (4 changesets, rollback fourni) :
  - séquence `sale_number_seq` (numéros `VTE-0001`…, trous possibles, jamais réutilisés) ;
  - table `sales` : `number` unique, `business_day_id` (→ `business_days`), `business_date`, `sales_channel_id` (→ `sales_channels`), `stock_location_id` (→ `stock_locations`), `seller_id` (→ `users`), `payment_method` (CHECK 5 valeurs), `status` (CHECK `VALIDATED`, `CANCELLED`, `REVERSED`), `subtotal`, `discount_amount` (0 ≤ remise ≤ sous-total), `total` (CHECK `total = subtotal - discount_amount`), montants `NUMERIC(19,2)`, audit et `version` ; index `business_date`, `(seller_id, business_date)`, `sales_channel_id` ;
  - table `sale_lines` (`@ElementCollection`, ordre `line_number`) : `article_id` (→ `articles`, unique par vente), `quantity NUMERIC(12,3)` > 0, `unit_price`, `unit_cost` (coût d'achat figé, nullable), `line_total` ;
  - permission `sales.create` donnée à `ADMIN` et `SELLER` (`sales.read` existait, AUTH-01).
- Mouvements : un `SALE` par ligne, quantité négative, emplacement du canal, `reference_id` = id de la vente.
- Journal : ressource `SALE`, action `CREATE`, libellé `Vente VTE-0042 · Boutique · 3 article(s) · 64000.00 Ar · Espèces`.

## Règles métier à respecter par les autres tickets

- **Journée** : toute vente exige la journée du jour ouverte (`BusinessDayService.requireOpenDay()`, sinon 409) ; la vente garde `businessDayId` et `businessDate` → filtrer les ventes du jour sur `business_date` (VTE-07, JR-03, JR-04, RPT-*, DSH-*).
- **Canal → emplacement** (`SaleChannels`) : l'emplacement actif portant le même code que le canal (`STORE` → Boutique, `DELIVERY` → Livraison), sinon la **Boutique** (`FACEBOOK`, `PHONE`). Ne pas recalculer : lire `sales.stock_location_id`.
- **Vendeuse** : compte actif de rôle `SELLER` (`UserService.isActiveSeller`, sinon 422) ; l'administrateur peut saisir pour une vendeuse, la vendeuse peut choisir une collègue (caisse partagée). Commission (COM-02) : `CommissionRateResolver.resolve(sale.sellerId, sale.salesChannelId, null)` (COM-01), base `REVENUE` = `total`, base `MARGIN` = `total − Σ quantity × unit_cost`.
- **Articles vendables** : produit fini (`FINISHED`), actif, avec un prix (sinon 422) ; 1 à 100 lignes, chaque article une fois (422).
- **Prix figés** : `unit_price` est lu sur la fiche au moment de la vente (ou le prix convenu du port) ; `line_total = unit_price × quantity` arrondi HALF_UP à 2 décimales ; `subtotal = Σ line_total` ; `total = subtotal − discount_amount`. Une modification de prix ultérieure ne change jamais une vente.
- **Promotions** (PRO-02, VTE-04) : `discount_amount` vaut 0 ; elles le renseigneront (et ajouteront la référence de la promotion par migration) sans toucher aux lignes. Contrat PRO-01 non appliqué ici.
- **Jamais de suppression** : `SaleRepository` étend `OperationRepository`. Annulation à chaud / avoir : implémenter `ReversibleOperationService<SaleRDto>` (`cancel` : statut `CANCELLED`, mouvements `RETURN` positifs vers `stock_location_id`, journal `CANCEL` ; `reverse` : avoir FAC-04, statut `REVERSED`, journal `REVERSE`).
- **Facture (FAC-01)** : écouter `SaleRecordedEvent` avec un `@EventListener` **synchrone** (même transaction : pas de vente sans facture) ; pour un effet non critique (tableau de bord), `@TransactionalEventListener(AFTER_COMMIT)`.
- **LIV-03 (décision PO du 07/11/2026)** : une commande livrée devient une vente canal `DELIVERY`, règlement `ON_DELIVERY` (ou celui de la commande), créée par `saleService.record(...)` dans la transaction du marquage « livrée » ; garder `sale.id()` sur la livraison ; la facture suit par l'événement.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale/`)
- `controller/SaleController.java`
- `service/SaleService.java` ; `service/impl/SaleServiceImpl.java`, `SalePolicy.java`, `SaleChannels.java`, `SaleLedger.java`, `SaleFormAssembler.java`, `CheckedSale.java`
- `domain/Sale.java` (entité), `SaleLine.java` (`@Embeddable`), `PaymentMethod.java`, `SaleCommand.java`, `SaleLineCommand.java`, `SaleHeader.java`, `PricedSaleLine.java`, `SaleRecordedEvent.java`
- `repository/SaleRepository.java` ; `mapper/SaleMapper.java`
- `dto/SaleWDto.java`, `SaleLineWDto.java`, `SaleRDto.java`, `SaleLineRDto.java`, `SaleFormRDto.java`, `SaleChannelOptionRDto.java`, `SaleSellerRDto.java`, `SaleArticleRDto.java`

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-11-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/features/pos/`)
- `data-access/pos-service.ts`, `pos-mapping.ts` (nouveau), `pos-models.ts`, `pos-cart.ts`, `pos-fixtures.ts`
- `components/pos-ticket/pos-ticket.ts`, `.html` ; `pages/pos-page/pos-page.ts`, `.html`
- `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/sale/controller/SaleIT.java`, `feature/sale/domain/SaleTest.java`, `feature/sale/service/impl/SalePolicyTest.java`, `SaleLedgerTest.java`, `SaleServiceImplTest.java` ; `feature/day/controller/BusinessDayIT.java` (nettoyage des ventes avant les journées)
- Frontend : `features/pos/data-access/pos-mapping.spec.ts`, `pos-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [JR-01](../JR-01/guide.md) : `BusinessDayService.requireOpenDay()`
- [STK-01](../STK-01/guide.md) : `StockMovementService.recordAll` (`SALE`), `StockService.getQuantities`
- [ART-01](../ART-01/guide.md) : `ArticleService.getArticleById`, `getArticles` (finis actifs)
- [AUTH-03](../AUTH-03/guide.md) : `UserService.isActiveSeller`, `getActiveSellers`
- [SYS-05](../SYS-05/guide.md) : canaux et emplacements (`getSalesChannels`, `getStockLocations`)
- [JNL-01](../JNL-01/guide.md) : `JournalService.record` ; [JNL-02](../JNL-02/guide.md) : `OperationRepository`, `OperationStatus`
- [SYS-07](../SYS-07/guide.md) / [SYS-12](../SYS-12/guide.md) : file hors ligne et `Idempotency-Key`

**Ces tickets s'appuient sur lui** :
- [VTE-02](../VTE-02/guide.md) à [VTE-06](../VTE-06/guide.md) · caisse, raccourcis, code promo, règlement (`paymentMethod` déjà enregistré), ticket en attente
- [VTE-07](../VTE-07/guide.md), [VTE-08](../VTE-08/guide.md), [VTE-09](../VTE-09/guide.md) · ventes du jour, par canal, détail
- [FAC-01](../FAC-01/guide.md), [FAC-02](../FAC-02/guide.md), [FAC-04](../FAC-04/guide.md) · facture par `SaleRecordedEvent`, avoir par `reverse`
- [COM-02](../COM-02/guide.md) · commission ; [LIV-03](../LIV-03/guide.md) · vente canal livraison
- [JR-03](../JR-03/guide.md), [JR-04](../JR-04/guide.md) · suivi et réconciliation (composante « vendu » = mouvements `SALE`) ; [RPT-01](../RPT-01/guide.md), [DSH-02](../DSH-02/guide.md), [DSH-04](../DSH-04/guide.md)

## Comment l'utiliser depuis un autre ticket

LIV-03, dans la méthode `@Transactional` qui marque la livraison « livrée » :

```java
var lines = order.lines().stream()
        .map(line -> new SaleLineCommand(line.articleId(), line.quantity(), line.unitPrice()))
        .toList();
var sale = saleService.record(new SaleCommand(deliveryChannelId, order.sellerId(), PaymentMethod.ON_DELIVERY, lines));
delivery.markDelivered(sale.id()); // 409 si la journée n'est pas ouverte ou si le stock Livraison manque
```

Côté interface :

```ts
this.posService.record({ salesChannelId, sellerId, paymentMethod: 'CASH', lines: [{ articleId, quantity: 2 }] });
```

## Points d'attention

- **Hypothèse (canal → emplacement)** : Facebook et Téléphone sortent le stock de la Boutique ; Livraison de l'emplacement Livraison. Un nouveau canal sans emplacement homonyme vendra depuis la Boutique.
- **Hypothèse (vendeuse)** : n'importe quelle vendeuse active peut être créditée, y compris par une collègue (caisse partagée) ; `createdBy` garde qui a saisi. Si le métier veut qu'une vendeuse ne vende que pour elle-même, l'ajouter dans `SalePolicy`.
- **Hypothèse (lecture)** : `GET /api/v1/sales/{id}` est ouvert à `sales.read` (vendeuse comprise) pour toute vente ; VTE-07 / VTE-09 devront filtrer « mes ventes » côté liste.
- **Hypothèse (règlement)** : les 5 modes de la maquette (`CASH`, `MVOLA`, `ORANGE_MONEY`, `CARD`, `ON_DELIVERY`) ; VTE-05 n'a plus qu'à présenter le choix (déjà enregistré). Montant reçu et monnaie rendue ne sont pas stockés.
- **Non livré** : promotion (PRO-02 / VTE-04 : `discount_amount` à 0, la promotion de la maquette n'est plus affichée), commission (COM-02 : pas de colonne, COM-01 non mergé), facture (FAC-01), annulation / avoir (`ReversibleOperationService` non implémenté, LIV-05 / FAC-04), tickets en attente (VTE-06, toujours maquette).
- **Poste** : le numéro affiché avant encaissement est `nextNumber` (indicatif, format `VTE-42`) ; le vrai numéro (`VTE-0042`) est celui du toast. Le stock du catalogue n'est pas relu après chaque vente : le serveur refuse (409) si une autre caisse a vendu le dernier article. Formulaire limité à 100 vendeuses et 2 000 articles.
- **Tests d'intégration** : `sales` référence `business_days` (FK) ; tout test qui vide `business_days` doit d'abord vider `sale_lines` et `sales` (fait dans `BusinessDayIT`). `SaleIT` ouvre la journée dans son `@BeforeEach` (201 ou 409 si déjà ouverte).
- PR empilée sur JR-01 (`feature/JR-01-repartition-matinale-stock`) : à merger après elle.
