# Guide d'intégration — PRO-03 · Statut et utilisation d'une promotion

Statut : ✅ rédigé le 2026-11-11 sur le code livré, tests à lancer (CI) · Ticket : [#60](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/60) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le **statut** d'une promotion (`SCHEDULED`, `ACTIVE`, `ENDED`) reste déduit des dates sur le jour d'Antananarivo (PRO-01, PRO-04) : il n'est jamais stocké, donc toujours à jour. PRO-03 rend le **compteur** `usage_count` exact : il augmente d'une unité à chaque vente enregistrée qui porte la promotion et diminue d'une unité quand cette vente est annulée. Le plafond `usage_limit` se déclenche donc réellement, et reste sûr si deux ventes arrivent en même temps. Aucun endpoint, aucune permission, aucune migration : la fiche (`PromotionRDto.usageCount`, `status`) et le front l'affichaient déjà.

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint. `GET /api/v1/promotions` et `GET /api/v1/promotions/{id}` (`promotions.read`) renvoient `usageCount` désormais alimenté. `POST /api/v1/sales` (`sales.create`) peut répondre **409 `CONFLICT`** quand le plafond de la promotion retenue est atteint par une autre vente au même instant (voir Points d'attention).

### Services et ports Java réutilisables

- `PromotionService.recordUsage(UUID promotionId)` (`PromotionServiceImpl.java:133`) : compte un usage dans la transaction de l'appelant ; 409 si le plafond est atteint, 404 si la promotion n'existe pas. Appelé par `SaleLedger.post` (`SaleLedger.java:54`) quand `sale.getPromotionId()` n'est pas nul.
- `PromotionService.releaseUsage(UUID promotionId)` (`PromotionServiceImpl.java:145`) : rend un usage, jamais sous 0. Appelé par `SaleLedger.unpost` (`SaleLedger.java:77`), donc par `SaleService.cancel` (LIV-05).
- `PromotionRepository.incrementUsage` (`PromotionRepository.java:91`) : un seul `UPDATE ... SET usage_count = usage_count + 1 WHERE id = ? AND (usage_limit IS NULL OR usage_count < usage_limit)` ; renvoie le nombre de lignes changées. C'est ce qui garantit le plafond en concurrence. `decrementUsage` (l.96) fait l'inverse avec `usage_count > 0`.
- Choix d'architecture : le compteur est piloté **directement** par la vente (`SaleLedger`) et non par un écouteur de `SaleRecordedEvent` / `SaleCancelledEvent`, parce que ces événements ne portent que `saleId` et `number`, et qu'un écouteur côté promotion ferait dépendre `promotion` de `sale` (la vente dépend déjà de la promotion). Le compteur reste dans la transaction de la vente : tout ou rien.

### Frontend

Aucun changement : `features/promotions/components/promotion-detail` affiche déjà « utilisations / plafond » (`usageCount`, `usageLimit`) et le statut.

### Données

- Aucune migration : `promotions.usage_count` (PRO-01) est utilisé tel quel ; `sales.promotion_id` et son index viennent de PRO-02.
- Journal : aucune ligne nouvelle ; la vente garde ses lignes `CREATE` / `CANCEL` (`SaleLedger`).

## Règles métier à respecter par les autres tickets

- **Un usage = une vente validée portant la promotion** (`sales.promotion_id`), quel que soit le nombre de lignes ou le montant de la remise. Une vente sans remise ne porte pas de promotion (`Sale.record` l'oublie) et ne compte pas.
- **Annulation** : le compteur est rendu une seule fois, car `Sale.cancel()` n'est possible qu'une fois ; le compteur ne descend jamais sous 0.
- **Plafond** : `usageCount < usageLimit` pour qu'une promotion s'applique (déjà dans `findAutomaticActive`) ; l'incrément atomique referme la fenêtre entre le calcul et l'enregistrement.
- **Statut** : seule la période décide (`PromotionPeriod.statusOn`) ; une promotion dont le plafond est atteint reste `ACTIVE` tant que la période court (décision PRO-03 : le statut ne dépend que des dates, comme dit le critère ; l'affichage « plafond atteint » se déduit de `usageCount >= usageLimit`).
- **VTE-04 (code promo)** : quand le code sera appliqué, la vente devra porter `promotion_id` ; `SaleLedger.post` comptera alors l'usage sans autre changement.

## Fichiers concernés

- Backend, promotion : `feature/promotion/repository/PromotionRepository.java`, `service/PromotionService.java`, `service/impl/PromotionServiceImpl.java`, `dto/PromotionRDto.java` (commentaire).
- Backend, vente : `feature/sale/service/impl/SaleLedger.java`.
- Tests : voir le [scénario](scenario-test.md) (`SaleUsageIT`, `PromotionServiceImplTest`, `SaleLedgerTest`).

## Dépendances

**Ce ticket s'appuie sur** :
- [PRO-01](../PRO-01/description.md) · [guide](../PRO-01/guide.md) : référentiel, `usage_count`, `usage_limit`
- [PRO-02](../PRO-02/description.md) · [guide](../PRO-02/guide.md) : `sales.promotion_id`, sélection de la promotion, `SaleCancelledEvent` laissé à PRO-03
- [PRO-04](../PRO-04/description.md) · [guide](../PRO-04/guide.md) : statut sur le jour d'Antananarivo

**Ces tickets s'appuient sur lui** : [VTE-04](../VTE-04/description.md) (code promo, usage compté par le même chemin).

## Comment l'utiliser depuis un autre ticket

```java
// Toute vente qui porte une promotion passe par SaleLedger.post : rien d'autre à appeler.
// Hors vente (cas exceptionnel) : promotionService.recordUsage(promotionId);
```

## Points d'attention

- **Hypothèse (non validée par le PO)** : si le plafond est atteint par une autre vente entre le calcul de la remise et l'enregistrement, la vente est refusée en 409 (message « plafond d'utilisation … la vente doit être recalculée ») plutôt que d'être enregistrée avec une remise non comptée ; la caissière relance et le serveur recalcule sans la promotion. Ce cas n'arrive que sur la dernière unité du plafond, avec deux ventes simultanées. Une vente rejouée hors ligne est recalculée au rejeu : elle ne reçoit simplement plus la remise si le plafond est atteint.
- **Compteur et ventes existantes** : les ventes enregistrées avant PRO-03 portent `promotion_id` mais n'ont pas incrémenté le compteur (PRO-02 ne le faisait pas). Aucun rattrapage n'est fait : seul le futur compte. Un rattrapage (`UPDATE promotions SET usage_count = (SELECT count(*) FROM sales ...)`) pourrait être décidé par le PO si des ventes promotionnelles existent déjà en production.
- **Annulation** : `SaleService.cancel` n'est exposé que via l'annulation d'une livraison (LIV-05) ; une vente de livraison n'a jamais de remise, donc en pratique la restitution n'est exercée qu'à partir d'un futur écran d'annulation de vente comptoir. Elle est testée directement (`SaleUsageIT.cancel_saleWithAPromotion_givesTheUseBack`).
- **UPDATE en masse** : `incrementUsage` / `decrementUsage` ne passent pas par le `@Version` de l'entité et ne vident pas le contexte de persistance (pour ne pas détacher la vente en cours d'écriture) ; ne pas relire `usageCount` sur une entité `Promotion` chargée avant l'appel dans la même transaction.
- **Tests d'intégration partagés** : `SaleUsageIT` termine les promotions automatiques avant et après chaque test (`StockApiTestSupport.endAutomaticPromotions`).
