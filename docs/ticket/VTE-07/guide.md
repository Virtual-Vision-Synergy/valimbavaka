# Guide d'intégration — VTE-07 · Consultation des ventes du jour

Statut : ✅ écrit le 2026-11-08 · Ticket : [#68](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/68) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

`GET /api/v1/sales` liste, page par page, les ventes d'un **jour ouvré** (aujourd'hui à Madagascar par défaut), avec pour chacune la référence, l'heure, le canal, la vendeuse, les articles, le règlement, la promo (`discountAmount`) et le total. Une **vendeuse ne lit que les ventes qui lui sont créditées** (le serveur impose son id) ; l'administrateur lit tout et filtre par vendeuse et par canal. L'écran `/sales` est branché sur l'API (filtres jour et canal, pagination serveur) et donne accès à la facture de chaque vente (aperçu construit depuis la vente en attendant FAC-01). Côté Java, `SaleQueryService` est le port de lecture de la feature `sale`, séparé de `SaleService` (écriture, VTE-01).

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/sales?date=&salesChannelId=&sellerId=&page=&size=&sortBy=&sortDirection=` | `sales.read` (admin, vendeuse) | paramètres d'URL (aucun corps) | 200 `PageResponse<SaleSummaryRDto>` | 400 (`date` mal formée), 401, 403 (livreur), 422 (`size` > 100, `page` < 1) |

- `date` : `yyyy-MM-dd`, jour ouvré de la vente (`sales.business_date`) ; absent = aujourd'hui en `Indian/Antananarivo`.
- `salesChannelId`, `sellerId` : facultatifs (absent = tous). **Pour une vendeuse active connectée, `sellerId` est remplacé par son propre id** (`SaleQueryServiceImpl.java:70`, même règle que le livreur, `DeliveryOrderServiceImpl.ownDriverId`).
- Tri en liste blanche : `createdAt` (défaut, décroissant), `number`, `total` ; tout autre champ retombe sur `createdAt` décroissant. `search` n'est pas appliqué.
- Tous les statuts sont listés (`VALIDATED`, `CANCELLED`, `REVERSED`) : rien n'est caché.
- `SaleSummaryRDto` (`feature/sale/dto/SaleSummaryRDto.java`) : `id`, `number`, `businessDate`, `createdAt` (heure de la vente), `salesChannelId`, `salesChannelLabel` (`null` si le canal a été désactivé depuis), `sellerId` et `sellerName` (« Miora Rakoto » ; `null` pour une vente créditée à personne, port LIV-03), `paymentMethod`, `status`, `subtotal`, `discountAmount`, `total`, `lines` (`SaleSummaryLineRDto` : `articleId`, `articleName` (`null` si la fiche est introuvable), `quantity`, `lineTotal`). Le prix unitaire et le coût d'achat ne sont pas exposés ici (le détail reste `GET /api/v1/sales/{id}`, VTE-01).

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.sale.service.SaleQueryService` | `PageResponse<SaleSummaryRDto> getSales(PageQuery query, SaleFilter filter)` | Ventes d'un jour paginées, filtre « mes ventes » appliqué pour une vendeuse (voir ci-dessus). |

- `feature.sale.domain.SaleFilter(@Nullable LocalDate businessDate, @Nullable UUID salesChannelId, @Nullable UUID sellerId)` ; `forSeller(UUID)` restreint à une vendeuse.
- Implémentation : `service/impl/SaleQueryServiceImpl.java:54` (jour par défaut, filtre vendeuse, tri) ; `repository/SaleQueryRepository.java:34` (`searchIds` : page d'**ids**, puis `findWithLinesByIdIn` l.39 lit la page avec ses lignes en une requête, sans N+1 ni pagination en mémoire) ; `service/impl/SaleSummaryAssembler.java:46` (noms lus une fois par page : canaux actifs `ReferentialService.getSalesChannels`, vendeuses distinctes `UserService.getUserById`, articles `ArticleService.getArticlesByIds`).
- `SaleQueryRepository` étend `Repository` (lecture seule, ni `save` ni `delete`) : les tickets de lecture (VTE-08, RPT-*, DSH-*) peuvent y ajouter leurs requêtes d'agrégat sans toucher à `SaleRepository` (écriture).

### Frontend

- Route `/sales` (inchangée, garde `sales.read`, marqueur « En construction » conservé : livre des ventes, totaux par canal et factures émises manquent).
- `features/sales/data-access/sales-service.ts:21` : `list(query: SalesQuery)` → `GET /api/v1/sales` ; `date`, `salesChannelId`, `sellerId` envoyés seulement s'ils sont renseignés.
- `features/sales/data-access/sales-mapping.ts` : `toSaleRow` (ligne du tableau, heure locale `HH:MM`, articles « Savon coco ×2 · Huile de ricin ×1 »), `toInvoicePreview` (aperçu de facture : sans numéro, statut `PENDING`, `CREDITED` si la vente est contre-passée), `articlesLabel`, `localTime`, `localIsoDate`.
- `features/sales/data-access/sales-models.ts` : types serveur `SaleSummaryRDto`, `SaleSummaryLineRDto`, `SaleStatus`, `SalesQuery` ; `InvoiceRDto` a désormais `number` / `issuedAt` nullables et `channelLabel` (libellé serveur) au lieu du code de canal.
- `pages/sales-page` : filtres « Jour » (`input type="date"`, jour local) et canal (`vvs-filter-chips` alimenté par `ReferentialService.listSalesChannels`), retour en page 1 à chaque changement, tableau Vente / Heure / Canal / Vendeuse / Articles / Règlement / Promo / Total / Facture (bouton « Voir »), badge « Annulée » / « Contre-passée » à côté de la référence. `components/invoice-panel` prend la vente (`[sale]`) au lieu d'un numéro.
- Traductions ajoutées (fr, mg) : `sales.noSeller`, `sales.filters.date`, `sales.col.articles|promo|total`, `sales.saleStatus.*`, `sales.invoice.open|pendingNumber|previewNote` ; `sales.subtitle` ne garde que le nombre de ventes ; `sales.col.amount` retiré.

### Données

- Aucune migration, aucune permission nouvelle (`sales.read` existe depuis AUTH-01 pour `ADMIN` et `SELLER`).
- Aucune écriture au journal (consultation).

## Règles métier à respecter par les autres tickets

- **Jour** = `sales.business_date` (jour de la journée ouverte), jamais la date UTC de `created_at`.
- **Mes ventes** : une vendeuse active ne lit que `seller_id = elle` (ventes **créditées**, pas celles qu'elle a saisies pour une collègue) ; l'administrateur lit tout. Pour un autre écran vendeuse (VTE-09, COM-03), réutiliser `SaleQueryService` ou la même règle `auditorAware.getCurrentAuditor().filter(userService::isActiveSeller)`.
- **Vente sans vendeuse** (port LIV-03) : listée avec `sellerName` `null` (écran « Sans vendeuse »), jamais perdue.
- **Statuts** : les ventes annulées / contre-passées restent listées ; les totaux de CA (VTE-08, RPT-*, DSH-*) doivent, eux, ne compter que `VALIDATED`.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale/`)
- `controller/SaleQueryController.java` (nouveau, même préfixe `/api/v1/sales` que `SaleController`, sans le modifier)
- `service/SaleQueryService.java` ; `service/impl/SaleQueryServiceImpl.java`, `SaleSummaryAssembler.java`
- `repository/SaleQueryRepository.java`
- `domain/SaleFilter.java` ; `dto/SaleSummaryRDto.java`, `SaleSummaryLineRDto.java`

**Frontend** (`valimbavaka-ui/src/app/features/sales/`)
- `data-access/sales-service.ts`, `sales-models.ts`, `sales-mapping.ts` (nouveau) ; `sales-fixtures.ts` supprimé
- `pages/sales-page/sales-page.ts`, `.html`, `.css` ; `components/invoice-panel/invoice-panel.ts`, `.html` ; `components/invoice-document/invoice-document.html` ; `sales.routes.ts` (commentaire)
- `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/sale/controller/SaleQueryIT.java`, `feature/sale/service/impl/SaleQueryServiceImplTest.java`, `SaleSummaryAssemblerTest.java`
- Frontend : `features/sales/data-access/sales-service.spec.ts`, `sales-mapping.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [VTE-01](../VTE-01/guide.md) : entité `Sale`, `sales.business_date`, `seller_id` nullable, `discount_amount`
- [SYS-05](../SYS-05/guide.md) : `ReferentialService.getSalesChannels` ; [AUTH-03](../AUTH-03/guide.md) : `UserService.isActiveSeller`, `getUserById` ; [ART-01](../ART-01/guide.md) : `ArticleService.getArticlesByIds`

**Ces tickets s'appuient sur lui** :
- [VTE-08](../VTE-08/guide.md) (totaux par canal : `components/channel-summary` et les types `SalesDayRDto` / `ChannelTotalRDto` sont gardés, non affichés), [VTE-09](../VTE-09/guide.md) (détail avec commission), [FAC-01](../FAC-01/guide.md) (facture émise : remplacer `toInvoicePreview`)

## Comment l'utiliser depuis un autre ticket

```java
var page = saleQueryService.getSales(new PageQuery(1, 100, null, "createdAt", SortDirection.DESC),
        new SaleFilter(LocalDate.of(2026, 11, 8), null, null)); // vendeuse connectée : ses ventes seulement
```

```ts
this.salesService.list({ page: 1, size: 10, date: '2026-11-08', salesChannelId });
```

## Points d'attention

- **Hypothèse (mes ventes)** : « mes ventes » = ventes **créditées** à la vendeuse (`seller_id`), pas celles qu'elle a saisies pour une collègue (`created_by`). Règle prise sur le modèle des livraisons (le livreur ne voit que ses commandes, `DeliveryOrderServiceImpl.ownDriverId`) ; à faire valider par le PO.
- **Hypothèse (accès facture)** : FAC-01 n'existe pas encore ; « Voir » ouvre un **aperçu** construit depuis la vente (numéro « À émettre », statut « En attente », émetteur « By Valimbavaka Cosmetics », NIF à compléter). FAC-01 remplacera `toInvoicePreview` par la lecture de la facture émise. PDF, envoi, régénération et avoir restent des actions de démonstration.
- **Hypothèse (filtres)** : la description n'en impose aucun ; jour et canal à l'écran, vendeuse seulement par l'API (administrateur). Le sous-titre affiche le nombre de ventes du filtre, pas le CA du jour (agrégat : VTE-08).
- **Promo** : `discountAmount` vaut toujours 0 tant que PRO-02 / VTE-04 ne l'alimentent pas ; l'écran affiche « — ». Le libellé de la promotion n'existe pas encore en base.
- **Hors périmètre** : totaux par canal (VTE-08 ; les cartes `channel-summary` ne sont plus affichées), détail avec règle de commission (VTE-09), livre des ventes (bouton de démonstration).
- `?sale=VTE-nnnn` n'ouvre la facture que si la vente est dans la page affichée.
- **PR empilée** sur `feature/VTE-01-enregistrement-vente-multicanal` : à merger après elle.
