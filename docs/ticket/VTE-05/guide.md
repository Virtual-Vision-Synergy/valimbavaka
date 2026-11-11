# Guide d'intégration — VTE-05 · Choix du mode de règlement

Statut : ✅ écrit le 2026-11-11 · Ticket : [#66](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/66) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

À la caisse, la vendeuse choisit le mode de règlement (Espèces, MVola, Orange Money, Carte) ; il part avec la vente (`paymentMethod`), est enregistré dans `sales.payment_method` (non modifiable ensuite) et se retrouve sur la liste des ventes et sur la facture dérivée de la vente. **Tout cela était déjà livré par VTE-01 (backend) et VTE-02 (sélecteur du poste)** : ce ticket n'ajoute aucun code de production, il verrouille le comportement par des tests (back et front) et documente le contrat. Aucune migration, aucun endpoint, aucune permission nouvelle.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/sales` | `sales.create` | `SaleWDto.paymentMethod` (obligatoire) | `SaleRDto.paymentMethod` | 400 mode inconnu, 401, 403, 422 mode absent |
| GET | `/api/v1/sales/{id}` | `sales.read` | | `SaleRDto.paymentMethod` | 404 |
| GET | `/api/v1/sales` | `sales.read` | | `SaleSummaryRDto.paymentMethod` | |

Valeurs : `CASH`, `MVOLA`, `ORANGE_MONEY`, `CARD`, `ON_DELIVERY` (`feature/sale/domain/PaymentMethod.java`, qui porte aussi le libellé du journal).

### Services et ports Java réutilisables

- `SaleCommand.paymentMethod()` / `SaleHeader.paymentMethod()` → `Sale.paymentMethod` (`domain/Sale.java:61`, `updatable = false`). La facture (FAC-01) lit le mode sur la vente ; elle n'a pas à le redemander.
- Le libellé du mode est repris dans la description de l'écriture de journal (`SaleLedger.java:82`).

### Frontend

- Sélecteur : `features/pos/components/pos-ticket/pos-ticket.ts` (constante `PAYMENTS`, 4 modes, `selectPayment`) et `.html` (groupe de boutons radio nommé « Mode de paiement »).
- État : `PosCart.payment` (`pos-cart.ts:40`, Espèces par défaut, remis à Espèces au ticket suivant) ; `canCheckout` n'exige un reçu suffisant qu'en espèces (`pos-cart.ts:68`).
- Envoi : `toSaleWDto(..., payment, ...)` (`pos-mapping.ts:98`), appelé par `PosPage.checkout` (`pos-page.ts:88`) ; hors ligne, la vente en file garde son mode.
- Affichage : colonne « Règlement » de `/sales` et document de facture (`sales-mapping.ts:25` et `:44`, libellés `sales.payment.*`, fr et mg).

### Données

Table `sales`, colonne `payment_method` `NOT NULL`, contrainte `ck_sales_payment_method` sur les 5 valeurs (migration `2026_11_08-11-changelog.sql`, VTE-01). Rien de nouveau.

## Règles métier à respecter par les autres tickets

- Le mode est fixé à l'enregistrement et **ne change plus** ; une correction passe par l'annulation / l'avoir (FAC-04) puis une nouvelle vente.
- `ON_DELIVERY` est réservé à la vente créée par LIV-03 (espèces encaissées par le livreur) ; il n'est pas proposé à la caisse.
- Encaissé du jour (JR-03 / JR-04) : `sales.total` regroupé par `payment_method` sur `business_day_id`.
- Le montant reçu et la monnaie rendue ne sont pas stockés (VTE-01) ; ils ne servent qu'au calcul du rendu au poste.

## Fichiers concernés

- Tests ajoutés : `feature/sale/controller/SaleIT.java` (3 tests), `pos-ticket.spec.ts` (bloc « mode de règlement (VTE-05) »), `pos-page.spec.ts` (2 tests), `sales-mapping.spec.ts` (1 test paramétré).
- Code de production : aucun changement.

## Dépendances

- [VTE-01](../VTE-01/guide.md) (enregistrement), [VTE-02](../VTE-02/guide.md) (poste de caisse), [VTE-07](../VTE-07/guide.md) (colonne Règlement).

## Comment l'utiliser depuis un autre ticket

```java
var sale = saleService.record(new SaleCommand(channelId, sellerId, PaymentMethod.MVOLA, lines));
// sale.getPaymentMethod() : à reprendre tel quel sur la facture
```

## Points d'attention

- **Hypothèse (écart avec le ticket)** : le critère « enregistré avec la vente » est déjà vrai ; « et la facture » est satisfait par dérivation (la facture FAC-01 n'existe pas encore, l'aperçu de `/sales` lit le mode de la vente). FAC-01 devra reprendre `Sale.paymentMethod`.
- **Hypothèse** : « à la livraison » reste absent du sélecteur de la caisse, mais l'API `POST /api/v1/sales` l'accepte (LIV-03 passe par `SaleService.record`). Si Randy veut le refuser côté API pour la caisse, c'est un ticket à part (règle 422 dans `SalePolicy`).
- Aucun changement de libellé ni de traduction : `pos.ticket.payment.*` et `sales.payment.*` existent en fr et en mg.
