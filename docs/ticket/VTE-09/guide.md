# Guide d'intégration — VTE-09 · Détail d'une vente

Statut : ✅ rédigé · Ticket : [#70](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/70) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Un seul appel, `GET /api/v1/sales/{id}/detail`, rend une vente avec les noms de son canal, de sa vendeuse et de ses articles, le prix unitaire de chaque ligne, la promotion à l'origine de la remise, sa facture (numéro, statut) et sa commission avec la règle appliquée (origine du taux, nom de la règle du canal, base, taux, montant, statut). L'écran `/sales` l'affiche dans un panneau « Détail de la vente » au-dessus du panneau facture. Une vendeuse ne lit que ses ventes et sa commission (404 sinon) ; l'administrateur lit tout.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/sales/{saleId}/detail` | `sales.read` (admin, vendeuse) | identifiant de la vente, aucun corps | 200 `SaleDetailRDto` | 401, 403 (livreur), 404 (vente inconnue ou d'une autre vendeuse) |

- `SaleDetailRDto` (`feature/saledetail/dto/SaleDetailRDto.java`) : `id`, `number`, `businessDate`, `createdAt`, `salesChannelId`, `salesChannelLabel` (`null` si le canal est désactivé), `sellerId` / `sellerName` (`null` pour une vente créditée à personne), `paymentMethod`, `status`, `subtotal`, `discountAmount`, `total`, `promotion` (`id`, `name`, `code` ; `null` sans remise), `lines` (`articleId`, `articleCode`, `articleName` nuls si la fiche est introuvable, `quantity`, `unitPrice`, `lineTotal`), `invoice` (`id`, `number`, `issuedAt`, `status` ; `null` sans facture), `commission` (`id`, `revenue`, `baseAmount`, `base`, `rate`, `rateSource`, `ruleId`, `ruleName`, `amount`, `status` ; `null` sans commission).
- Le coût d'achat n'est jamais exposé.

### Services et ports Java réutilisables

- `feature.saledetail.service.SaleDetailService.getDetail(UUID)` : agrégat en lecture seule, modèle des features `articleoverview` et `dayoverview`.
- Ajout à `feature.invoice.service.InvoiceService` : `Optional<InvoiceRDto> findInvoiceBySaleId(UUID)` (vide sans facture, 404 pour la facture d'une collègue comme `getInvoiceBySaleId`). Sert à ne pas lever d'exception dans la transaction de lecture.
- Réutilisés tels quels : `SaleService.getSaleById`, `CommissionService.findBySource(SALE, id)` (prévu par COM-02 pour ce ticket), `CommissionRuleService.getRuleById`, `PromotionService.getPromotionById`, `ArticleService.getArticlesByIds` (un seul appel pour toutes les lignes), `ReferentialService.getSalesChannels`, `UserService.getUserById`.

### Frontend

- Pas de nouvelle route : le détail vit dans `/sales` (`?sale=VTE-nnnn` ouvre la vente de la page courante).
- `features/sales/data-access/sales-service.ts` : `detail(saleId)` ; types `SaleDetailRDto`, `SaleDetailLineRDto`, `SaleDetailPromotionRDto`, `SaleDetailInvoiceRDto`, `SaleDetailCommissionRDto` dans `sales-models.ts`.
- `features/sales/components/sale-detail-panel` (`vvs-sale-detail-panel`, entrée `saleId`) : réutilisable par un autre écran. `pages/sales-page` l'affiche à côté de `vvs-invoice-panel`, dans un bloc `.side`.
- Traductions `sales.detail.*` (fr, mg) ; l'origine du taux et la base réutilisent `commissionList.rateSource.*` et `commissionList.base.*`.

### Données

- Aucune migration, aucune permission nouvelle (`sales.read` existe depuis AUTH-01).
- Aucune écriture au journal (consultation).

## Règles métier à respecter par les autres tickets

- **Droits** : l'accès se décide dans `SaleDetailServiceImpl.requireReadable` : une vendeuse active ne lit que `sales.seller_id = elle` ; une vente créditée à personne n'est lisible que par l'administrateur. Réponse 404 et non 403, pour ne pas révéler l'existence d'une vente. À noter : `GET /api/v1/sales/{id}` (VTE-01) ne filtre pas par vendeuse, le détail est le point d'entrée à utiliser pour un écran vendeuse.
- **Commission** : une vente a au plus une commission (`uq_commissions_source`) ; `rateSource` dit le niveau gagnant (`SALES_CHANNEL`, `SELLER`, `NONE`), `ruleName` n'est renseigné que pour `SALES_CHANNEL`. La règle affichée est celle **appliquée au calcul** (`commissions.commission_rule_id`), pas la règle actuelle du canal : modifier ou désactiver la règle ensuite ne change pas le détail (le recalcul est COM-05). Le nom de la règle, lui, est lu à la consultation : un renommage s'y reflète.
- Une commission `CANCELLED` (vente annulée, LIV-05) est rendue avec son statut, pas masquée.

## Fichiers concernés

- Backend, créés : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/saledetail/{controller/SaleDetailController,service/SaleDetailService,service/impl/SaleDetailServiceImpl,dto/SaleDetail*RDto}.java` ; modifiés : `feature/invoice/service/InvoiceService.java`, `feature/invoice/service/impl/InvoiceServiceImpl.java` (`findInvoiceBySaleId`).
- Frontend : `valimbavaka-ui/src/app/features/sales/components/sale-detail-panel/` (ts, html, css, spec), `pages/sales-page/sales-page.{ts,html,css}`, `data-access/sales-service.ts`, `sales-models.ts`, `public/i18n/{fr,mg}.json`.
- Tests : voir le [scénario](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** : [VTE-01](../VTE-01/guide.md) (vente), [VTE-07](../VTE-07/guide.md) (liste et règle « mes ventes »), [FAC-01](../FAC-01/guide.md) (facture), [COM-02](../COM-02/guide.md) (commission, `findBySource`), [COM-04](../COM-04/guide.md) (une vendeuse ne voit que sa commission).

**Ces tickets s'appuient sur lui** : aucun.

## Comment l'utiliser depuis un autre ticket

```html
<vvs-sale-detail-panel [saleId]="sale.id" />
```

```java
SaleDetailRDto detail = saleDetailService.getDetail(saleId); // 404 si la vente n'est pas lisible par l'appelant
```

## Points d'attention

- **Choix d'architecture** : une feature d'agrégation `saledetail` (comme `articleoverview`, `dayoverview`). La vente ne peut pas dépendre de `commission` ni de `invoice` (ArchUnit : ces features dépendent déjà de `sale`) ; faire composer l'écran à partir de trois endpoints aurait laissé la règle « une vendeuse ne lit que sa vente » à reconstituer côté client, et `GET /api/v1/sales/{id}` ne la porte pas.
- Hypothèse : le détail s'ouvre dans la page `/sales` (panneau), pas dans une page dédiée : le ticket demande de « consulter le détail » sans écran précis, et le panneau facture y existe déjà.
- Hypothèse : la facture n'est résumée (numéro, statut) ; le document, le PDF, l'envoi et l'avoir restent dans `vvs-invoice-panel` (FAC-02 à FAC-04).
- Le nom du canal n'est lu que parmi les canaux actifs (même limite que VTE-07) : canal désactivé, libellé `null`.
- Dette connue, non traitée ici : `GET /api/v1/sales/{id}` (VTE-01) laisse une vendeuse lire la vente d'une collègue. À fermer dans un ticket `fix/` (filtrer comme `SaleDetailServiceImpl.requireReadable`).
