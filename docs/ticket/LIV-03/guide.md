# Guide d'intégration — LIV-03 · Marquage rapide d'une livraison

Statut : ✅ écrit le 2026-11-11 · Ticket : [#48](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/48) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Sur `/round`, le livreur touche « Livrée » d'un geste, sans saisie. Après le délai d'annulation de 5 s (LIV-02),
le marquage part vers `POST /api/v1/deliveries/{id}/deliver` par la file hors ligne (SYS-07). Le serveur, dans une
seule transaction, enregistre la commande comme **vente du canal Livraison** (décision PO du 07/11/2026) par le port
`SaleService.record` de VTE-01, passe la commande à `DELIVERED` avec l'id de cette vente, et trace l'opération au
journal. La facture de la vente vient de FAC-01 (écouteur de `SaleRecordedEvent`), pas de ce ticket.

## Contrat exposé

### Endpoints backend

| Méthode | Chemin | Permission | Corps | Réponse |
|---|---|---|---|---|
| `POST` | `/api/v1/deliveries/{id}/deliver` | `deliveries.deliver` (ADMIN, DELIVERY_DRIVER) | aucun (un corps envoyé est ignoré) | 200 `DeliveryOrderRDto` (`status` `DELIVERED`, `saleId` renseigné) |

Erreurs : 401 sans jeton, 403 mauvais rôle, 404 commande inconnue **ou d'un autre livreur**, 409 transition
interdite (déjà livrée, en échec, annulée), stock insuffisant en Livraison ou journée non ouverte (VTE-01), 422 si le
canal `DELIVERY` manque au référentiel. Rejouable avec `Idempotency-Key` (une seule vente).

Référence : `feature/delivery/controller/DeliveryCompletionController.java:37-44`.

### Services et ports Java réutilisables

- `DeliveryCompletionService.markDelivered(UUID)` (`service/DeliveryCompletionService.java:20`).
- `DeliveryOrder.deliver(UUID saleId)` (`domain/DeliveryOrder.java:115`) : seule façon de passer à `DELIVERED`.
- `DeliverySale.record(DeliveryOrder)` (`service/impl/DeliverySale.java:40`, interne au paquet) : construit le
  `SaleCommand` canal `DELIVERY`, règlement `ON_DELIVERY`, lignes aux prix figés de la commande.
- `DeliveryOrderRDto.saleId` (nullable) : lien commande → vente.

### Base de données

Migration `2026_11_08-66-changelog.sql` : colonne `delivery_orders.sale_id` (FK `sales`, unique), contrainte
`ck_delivery_orders_delivered_has_sale` (une commande `DELIVERED` a toujours sa vente) et permission
`deliveries.deliver`. Rollback fourni.

### Frontend (`valimbavaka-ui/src/app/features/deliveries/`)

- `data-access/delivery-service.ts:101` `markDelivered(id)` : `OutboxService.submit('POST', …/deliver)`, sans corps.
- `data-access/delivery-service.ts:124` `updateStatus(id, change)` : `DELIVERED` → `markDelivered`, `FAILED` →
  `reportFailure` (LIV-04). Plus aucune simulation.
- `data-access/delivery-models.ts:131` : `saleId` dans `DeliveryOrderRDto`.

## Règles métier à respecter par les autres tickets

- Une commande livrée **est** une vente : ne jamais créer une seconde vente pour la même commande
  (`uq_delivery_orders_sale`). Les rapports (RPT-*, DSH-*) comptent la vente, pas la commande, pour éviter les doublons.
- La vente est créditée à l'auteur de la commande **seulement s'il est vendeuse active** (`DeliverySale.java:52`) ;
  commande saisie par l'administrateur ou sans auteur → vente sans vendeuse, donc sans commission (COM-02).
- Prix et quantités sont ceux de la commande, même si le prix de l'article a changé ou s'il a été désactivé depuis.
- Le stock sort de l'emplacement **Livraison** (mouvement `SALE`, VTE-01) : les articles doivent y avoir été
  transférés (TRF-02) ; sinon 409 et rien ne change.
- `TO_PREPARE → DELIVERED` est permis en un geste (table de transitions LIV-01).

## Fichiers concernés

- Back : `feature/delivery/controller/DeliveryCompletionController.java`,
  `service/DeliveryCompletionService.java`, `service/impl/DeliveryCompletionServiceImpl.java`,
  `service/impl/DeliverySale.java`, `domain/DeliveryOrder.java`, `dto/DeliveryOrderRDto.java`,
  `db/changelog/changes/2026_11_08-66-changelog.sql`.
- Front : `data-access/delivery-service.ts`, `data-access/delivery-models.ts`,
  `pages/driver-round-page/driver-round-page.ts`, `public/i18n/fr.json`, `mg.json`.
- Tests : `DeliveryCompletionIT`, `DeliveryCompletionServiceImplTest`, `DeliverySaleTest`, `DeliveryOrderTest`,
  `delivery-service.spec.ts`, `driver-round-page.spec.ts` ; `DeliveryTrackingIT` (LIV-06) passe désormais par
  l'endpoint pour mettre une commande à `DELIVERED`.

## Dépendances

LIV-01/LIV-02 (commandes, tournée), VTE-01 (`SaleService.record`, journée ouverte), JNL-01 (journal), SYS-07 (file
hors ligne). LIV-04 partage `updateStatus` côté front.

## Comment l'utiliser depuis un autre ticket

- Retrouver la vente d'une commande : `DeliveryOrderRDto.saleId`, puis `GET /api/v1/sales/{id}` (VTE-01).
- Annulation (LIV-05) : défaire « livrée » demande d'annuler aussi la vente liée (et sa facture FAC-*) ; vider
  `sale_id` en même temps que le statut, sinon la contrainte `ck_delivery_orders_delivered_has_sale` refuse.
- Historique (LIV-07) : la ligne de journal `UPDATE` · `DELIVERY_ORDER` « Commande LIV-… livrée : vente VTE-…, n
  article(s), … Ar à la livraison » est écrite par `DeliveryCompletionServiceImpl.describe`.

## Points d'attention

- Tests qui vident `sales` : détacher d'abord les commandes livrées (`update delivery_orders set status =
  'CANCELLED', sale_id = null where sale_id is not null`), comme `BusinessDayIT.forgetToday`.
- Un test ne doit plus forcer `status = 'DELIVERED'` en SQL : passer par l'endpoint avec une journée ouverte et du
  stock en Livraison (voir `DeliveryTrackingIT.setStatus`).
- Concurrence (deux téléphones) : protégée par la version optimiste de la commande et l'unicité de `sale_id`, non
  automatisée (recette).
