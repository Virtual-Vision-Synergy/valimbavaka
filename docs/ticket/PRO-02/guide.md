# Guide d'intégration — PRO-02 · Application automatique d'une promotion en caisse

Statut : ☐ rédigé le 2026-11-11 sur le code livré, tests à exécuter (CI) · Ticket : [#59](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/59) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Une promotion **sans code**, active aujourd'hui (jour d'Antananarivo), sur le canal de la vente et dont le plafond n'est pas atteint s'applique d'elle-même. Le **serveur décide** : `SaleService.createSale` calcule la remise (`PromotionService.priceBasket`), l'enregistre dans `sales.discount_amount` et garde la promotion dans `sales.promotion_id`. Le formulaire de vente (`GET /api/v1/sales/form`) renvoie les promotions automatiques du jour et le poste `/pos` applique les mêmes règles à l'écran, hors connexion comprise ; un écart de net est signalé à la caissière (VTE-02). Aucun endpoint, aucune permission nouvelle.

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint ; deux réponses de VTE-01 s'enrichissent.

| Méthode | URL | Permission | Changement | Erreurs |
|---|---|---|---|---|
| `POST` | `/api/v1/sales` | `sales.create` | `SaleRDto.discountAmount` n'est plus toujours 0 ; nouveau champ `promotionId` (`null` sans remise). L'entrée `SaleWDto` ne change pas : ni remise, ni total, ni promotion acceptés (mass assignment ignoré) | inchangées (VTE-01) |
| `GET` | `/api/v1/sales/form` | `sales.create` | nouveau `promotions[]` : `SalePromotionRDto` (`id`, `name`, `kind`, `value`, `articleIds`, `channelIds`) | inchangées |

`SalePromotionRDto.articleIds` : les articles vendables du formulaire que la promotion atteint (catalogue entier, gamme ou articles ciblés), résolus par le serveur ; `channelIds` : les canaux où elle s'applique.

### Services et ports Java réutilisables

- `PromotionService.priceBasket(UUID salesChannelId, List<PromotionBasketLine> lines)` → `PromotionDiscount` (`PromotionServiceImpl.java:100`) : parmi les promotions automatiques actives du canal, retient celle qui donne la plus grande remise ; à égalité la première (fin la plus proche, puis nom) ; `PromotionDiscount.NONE` sinon. Réutilisable par VTE-04 (code promo) pour comparer.
- `PromotionService.getAutomaticPromotions()` (`PromotionServiceImpl.java:93`) : promotions automatiques actives du jour, plafond non atteint (`PromotionRepository.findAutomaticActive`, ligne 77).
- `PromotionPricing.discountOf(Promotion, List<PromotionBasketLine>)` (`feature/promotion/domain/PromotionPricing.java:25`) : la règle, fonction pure testée sur des exemples chiffrés. `Promotion.covers(articleId, rangeId)` et `Promotion.isOpenTo(channelId)` donnent l'éligibilité.
- Côté vente : `SaleDiscounts.of(SaleCommand, List<PricedSaleLine>)` (`feature/sale/service/impl/SaleDiscounts.java:35`) construit le panier (gamme de chaque article lue par `ArticleService.getArticlesByIds`), appelé par `SaleServiceImpl.record` (ligne 67) ; `Sale.record(number, header, lines, SaleDiscount)` (`Sale.java:97`) arrondit à 2 décimales, plafonne au sous-total et oublie la promotion si la remise est nulle. `Sale.record` sans remise est conservé.

### Frontend (`valimbavaka-ui/src/app/features/pos/`)

- `data-access/pos-pricing.ts` : `bestPromotion(promotions, channel, lines)` (l.107) renvoie `{ promotion, lineDiscounts, ticketDiscount }` ; `ticketTotals(lines, ticketDiscount = 0)` ajoute la remise de ticket (promotion `AMOUNT`). `lineDiscount` a disparu (remplacée par les règles ci-dessus).
- `data-access/pos-cart.ts` : `start(articles, promotions, openTicket, received)` reçoit **la liste** des promotions ; le signal `applied` (l.56) dépend du canal du ticket, `priced` donne la remise de chaque ligne et le taux (pourcentage seulement), `appliedPromotion` alimente le bandeau.
- `data-access/pos-mapping.ts` : `toPosPromotions` (l.69) traduit les identifiants de canal du serveur en canaux du poste (`SHOP`, `FACEBOOK`, `PHONE`) ; `PosContextRDto.promotion` devient `promotions`.
- `data-access/pos-models.ts` : `PosPromotionRDto` (`id`, `name`, `kind`, `value`, `articleIds`, `channels`, sans `code`), `SalePromotionRDto`, `SaleFormRDto.promotions`, `SaleRDto.promotionId`.
- `components/pos-ticket` : la ligne remisée affiche « −15 % · −3 825 » pour un pourcentage, « Offert · −8 500 » sinon (`pos.ticket.lineDiscountFree`, fr et mg) ; le bandeau n'affiche plus de pastille de code.

### Données

- Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_11-80-changelog.sql` : `sales.promotion_id UUID` nullable, clé étrangère vers `promotions`, index partiel `ix_sales_promotion` ; rollback prévu.
- Journal : aucune ligne nouvelle ; la vente garde sa ligne `CREATE` / `SALE` de VTE-01 (le montant tracé est le total net).

## Règles métier à respecter par les autres tickets

Reprises du guide PRO-01 (hypothèses fixées par PRO-01, appliquées ici) :
- **Automatique ou par code** : seules les promotions **sans code** sont appliquées par la caisse ; celles avec code attendent VTE-04.
- **Éligibilité** : jour d'Antananarivo entre `startsOn` et `endsOn` inclus, canal ∈ `channelIds`, `usageLimit` nul ou `usageCount < usageLimit`, et article visé (`ALL`, gamme ∈ `targetIds`, article ∈ `targetIds`).
- **Calcul** (par `PromotionPricing`, arrondi à l'ariary HALF_UP, jamais au-delà de ce dont la remise est retirée) :
  - `PERCENT` : `total de ligne × value / 100`, ligne par ligne, plafonné au total de la ligne ;
  - `AMOUNT` : `value` retiré **une fois** du total des lignes visées, plafonné à ce total ;
  - `BUY_X` : par ligne visée, `floor(quantité / value)` unités offertes au prix unitaire (3 achetés = 1 offert, soit `value` = 3 → « 3 pour 2 »).
- **Pas de cumul** : la plus avantageuse pour le client ; un code promo (VTE-04) devra la remplacer s'il donne plus.
- **Prix convenu** : une ligne à prix imposé (commande livrée, LIV-03) ne participe pas au panier ; une vente de livraison garde `discount_amount` à 0.
- **Le serveur fait foi** : le front et le serveur doivent donner les mêmes montants sur les mêmes exemples (voir `PromotionPricingTest` et `pos-pricing.spec.ts`, mêmes valeurs). Si la règle change, la changer des deux côtés.
- **Compteur** : PRO-02 n'incrémente pas `usage_count`. PRO-03 peut compter les ventes `VALIDATED` portant `promotion_id` (index prêt) ou incrémenter à l'enregistrement ; il doit aussi décider du sort du compteur à l'annulation d'une vente.

## Fichiers concernés

- Backend, promotion : `feature/promotion/domain/{PromotionPricing,PromotionBasketLine,PromotionDiscount,Promotion}.java`, `repository/PromotionRepository.java`, `service/PromotionService.java`, `service/impl/PromotionServiceImpl.java`.
- Backend, vente : `feature/sale/domain/{Sale,SaleDiscount}.java`, `dto/{SaleRDto,SaleFormRDto,SalePromotionRDto}.java`, `service/impl/{SaleDiscounts,SaleServiceImpl,SaleFormAssembler}.java`.
- Migration : `db/changelog/changes/2026_11_11-80-changelog.sql` (déclarée dans `db.changelog-master.yaml`).
- Frontend : `features/pos/data-access/{pos-pricing,pos-cart,pos-mapping,pos-models,pos-fixtures}.ts`, `components/pos-ticket/pos-ticket.html`, `public/i18n/{fr,mg}.json`.
- Tests : voir le [scénario](scenario-test.md) (`PromotionPricingTest`, `SalePromotionIT`, `SaleDiscountsTest`, `pos-pricing.spec.ts`, `pos-cart.spec.ts`…). `StockApiTestSupport.endAutomaticPromotions()` termine les promotions automatiques laissées par d'autres classes de test avant les tests de vente.

## Dépendances

**Ce ticket s'appuie sur** :
- [PRO-01](../PRO-01/description.md) · [guide](../PRO-01/guide.md) : référentiel et règles de calcul
- [PRO-04](../PRO-04/description.md) · [guide](../PRO-04/guide.md) : promotions lisibles, statut sur le jour d'Antananarivo
- [VTE-01](../VTE-01/description.md) · [guide](../VTE-01/guide.md) : `discount_amount`, `Sale.record`
- [VTE-02](../VTE-02/description.md) · [guide](../VTE-02/guide.md) : calcul au poste, toast d'écart de net

**Ces tickets s'appuient sur lui** :
- [PRO-03](../PRO-03/description.md) · [guide](../PRO-03/guide.md)
- [VTE-04](../VTE-04/description.md) · [guide](../VTE-04/guide.md)

## Comment l'utiliser depuis un autre ticket

```java
// VTE-04 : comparer le code saisi à la promotion automatique déjà retenue pour le panier
PromotionDiscount automatic = promotionService.priceBasket(salesChannelId, basket);
```

```ts
// Poste : promotions du formulaire, déjà traduites en canaux du poste
cart.start(ctx.articles, ctx.promotions, ctx.openTicket, ctx.openTicketReceived);
```

## Points d'attention

- **Périmètre réel** : VTE-01 enregistrait une remise nulle et VTE-02 ne connaissait qu'un taux de ligne fixé par la maquette ; PRO-02 ajoute le calcul serveur, `sales.promotion_id`, les trois types de remise et la sélection de la meilleure promotion. Pas d'endpoint de devis : le poste calcule seul, le serveur recalcule.
- **Jour de la promotion** : le jour calendaire d'Antananarivo à l'enregistrement (`PromotionServiceImpl.today()`), pas la `businessDate` de la journée ouverte ; une journée restée ouverte après minuit prend les promotions du jour calendaire.
- **Poste hors connexion** : la remise affichée vient du formulaire chargé à l'ouverture. Une vente rejouée plus tard est recalculée avec les promotions du jour du rejeu ; le toast d'écart (VTE-02) ne s'affiche pas pour une vente en file (pas de réponse immédiate).
- **Compteur et plafond** : `usage_count` n'est pas incrémenté (PRO-03) ; tant qu'il ne l'est pas, le plafond ne se déclenche jamais en pratique. Une annulation de vente n'a pas d'effet sur le compteur (décision à prendre par PRO-03).
- **Hypothèses non validées par le PO** : `BUY_X` = `floor(quantité / N)` unités offertes ; remise de ligne arrondie à l'ariary ligne par ligne ; `AMOUNT` appliqué une fois au panier sur les lignes visées ; les lignes à prix convenu sont exclues ; une vente du canal livraison (`DELIVERY`) n'a que des prix convenus, donc pas de remise.
- **Poste** : les canaux du poste sont `SHOP`, `FACEBOOK`, `PHONE` ; une promotion limitée à `DELIVERY` n'est jamais appliquée à l'écran. `PosPromotionRDto.code` a disparu : le bandeau n'affiche plus de pastille. La fixture `pos-fixtures.ts` garde « Octobre rose » pour les tests.
- **Tests d'intégration partagés** : la base de test est commune ; `SaleIT`, `SaleQueryIT` et `SalePromotionIT` terminent d'abord les promotions automatiques existantes (`StockApiTestSupport.endAutomaticPromotions`). Tout nouveau test qui enregistre une vente au comptoir doit faire de même.
- **Limites connues** : une promotion éligible mais sans effet (BUY_X avec une seule unité) n'est pas affichée dans le bandeau ; aucune explication « pourquoi pas de remise » n'est donnée à la caissière.
