# Guide d'intégration — VTE-02 · Poste de caisse avec calcul automatique

Statut : ✅ écrit le 2026-11-08 · Ticket : [#63](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/63) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le poste de caisse `/pos` recalcule le ticket **à chaque ajout, retrait ou montant reçu** : total de chaque ligne,
sous-total, remise, net à payer et rendu monnaie, sans appel serveur (le poste marche hors connexion, SYS-07). Le
calcul reprend **exactement la règle du serveur** (`Sale.record` / `SaleLine.of`, VTE-01 : prix × quantité arrondi
HALF_UP au centime, somme des lignes, net = sous-total − remise), en centimes entiers. Le serveur recalcule tout à
l'encaissement et **fait foi** : si son net diffère du net affiché (prix modifié depuis l'ouverture du poste), la
caissière est avertie. Ticket **frontend seul** : aucun endpoint, aucune permission, aucune migration.

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint. Le poste utilise ceux de VTE-01 : `GET /api/v1/sales/form` et `POST /api/v1/sales`
(`sales.create`), qui ne reçoit **jamais** de prix, de remise ni de total (voir le [guide VTE-01](../VTE-01/guide.md)).

### Services et ports Java réutilisables

- Aucun. La règle de référence reste `feature/sale/domain/SaleLine.java:42-44` et `Sale.java` (`Sale.record`).

### Frontend (`valimbavaka-ui/src/app/features/pos/`)

- Route : `/pos` (inchangée).
- `data-access/pos-pricing.ts` (nouveau, fonctions pures) :
  - `lineTotal(unitPrice, quantity)` (l.34) : total d'une ligne, HALF_UP à 2 décimales, quantité à 3 décimales au plus ;
  - `lineDiscount(gross, rate)` (l.43) : remise d'une ligne en %, arrondie à l'ariary, **bornée au total de la ligne** ;
  - `ticketTotals(lines)` (l.51) : `{ subtotal, discount, total }` additionnés en centimes ;
  - `changeDue(received, total)` (l.62) : rendu (négatif = montant qui manque).
- `data-access/pos-cart.ts` (`PosCart`, fourni par la page) : signaux calculés `priced` (l.46 : lignes avec `gross`,
  `rate`, `discount`), `subtotal`, `discount`, `total`, `change` (l.58-62), `canCheckout` ; l'annonce `aria-live`
  donne le net à payer après chaque ajout ou retrait (`pos.ticket.added`, `decreased`, `removed` : paramètre `total`).
- `pages/pos-page/pos-page.ts` : `checkout()` garde le net affiché (l.109) ; `recorded()` (l.150) compare au `total`
  de `SaleRDto` → toast `pos.toast.totalAdjusted` (avertissement) si différent, sinon `pos.toast.paid` ; aucune
  comparaison pour une vente mise en file hors connexion (pas de réponse).
- Composant `components/pos-ticket` : inchangé, il affiche déjà `cart.subtotal()`, `cart.discount()`, `cart.total()`,
  `cart.change()`.
- Traductions (fr, mg) : `pos.ticket.added|decreased|removed` (+ `{{total}}`), `pos.toast.totalAdjusted`.

### Données

- Aucune table ni migration. Aucune écriture au journal en plus de celle de la vente (VTE-01).

## Règles métier à respecter par les autres tickets

- **Un seul calcul au poste** : tout montant affiché du ticket passe par `pos-pricing.ts` (via `PosCart`). Ne pas
  recalculer ailleurs avec des `number` flottants (0,1 × 3 ≠ 0,3).
- **Arrondis** : ligne = HALF_UP au centime (comme le serveur) ; remise de ligne arrondie à l'ariary ; remise ≤ total
  de la ligne, donc net ≥ 0 (contrainte `0 ≤ discount_amount ≤ subtotal` de `sales`).
- **Le serveur fait foi** : le poste n'envoie que les quantités ; un écart de net est signalé, jamais corrigé en
  silence. Si une règle change côté serveur (PRO-02, VTE-04), la reprendre dans `pos-pricing.ts` **et** dans le
  serveur, avec un test de chaque côté sur les mêmes exemples.
- **PRO-02 / VTE-04 (promotions, code promo)** : la remise de ligne est déjà branchée : `PosCart.start(…, promotion, …)`
  reçoit une `PosPromotionRDto` (`rate`, `articleIds`) et `lineDiscount` l'applique aux articles ciblés ;
  `appliedPromotion` alimente le bandeau « … appliquée automatiquement ». `toPosContext` passe aujourd'hui
  `promotion: null` (VTE-01) : PRO-02 la remplit depuis l'API et le serveur doit renseigner `discount_amount` avec
  le même résultat, sinon le toast d'écart apparaîtra à chaque vente remisée. Les types `AMOUNT` et `BUY_X` de PRO-01
  demanderont d'étendre `lineDiscount` (ou une remise de ticket ajoutée dans `ticketTotals`).
- **VTE-05 (règlement)** : `canCheckout` exige en espèces un reçu ≥ net (`change() >= 0`) ; les autres modes n'en
  exigent pas. Le montant reçu et le rendu ne sont pas envoyés au serveur.
- **VTE-06 (mise en attente)** : `PosCart.clear()` remet lignes, reçu et règlement à zéro et garde la vendeuse ; un
  ticket repris passe par `PosCart.start(articles, promotion, openTicket, received)` et ses totaux se recalculent seuls.
- **VTE-03 (raccourcis)** : `+` / `−` passent par `PosCart.add` / `decrement`, donc par le même calcul.

## Fichiers concernés

**Créés** : `features/pos/data-access/pos-pricing.ts`, `pos-pricing.spec.ts`, `pos-cart.spec.ts` ;
`features/pos/components/pos-ticket/pos-ticket.spec.ts` ; `features/pos/pages/pos-page/pos-page.spec.ts`.

**Modifiés** : `features/pos/data-access/pos-cart.ts`, `features/pos/pages/pos-page/pos-page.ts`,
`public/i18n/fr.json`, `public/i18n/mg.json`.

Backend : aucun fichier (`feature/sale` posé par VTE-01).

## Dépendances

**Ce ticket s'appuie sur** :
- [VTE-01](../VTE-01/description.md) · [guide](../VTE-01/guide.md) : `PosService`, `SaleRDto.total`, règle d'arrondi de `SaleLine.of`

**Ces tickets s'appuient sur lui** :
- [PRO-02](../PRO-02/description.md) · [guide](../PRO-02/guide.md)
- [VTE-03](../VTE-03/description.md) · [guide](../VTE-03/guide.md)
- [VTE-04](../VTE-04/description.md) · [guide](../VTE-04/guide.md)
- [VTE-05](../VTE-05/description.md) · [guide](../VTE-05/guide.md)
- [VTE-06](../VTE-06/description.md) · [guide](../VTE-06/guide.md)
- [FAC-01](../FAC-01/description.md) · [guide](../FAC-01/guide.md)
- [FAC-02](../FAC-02/description.md) · [guide](../FAC-02/guide.md)

## Comment l'utiliser depuis un autre ticket

```ts
const cart = inject(PosCart); // fourni par PosPage
cart.add(article);            // sous-total, remise, net et rendu se recalculent
const net = cart.total();     // même valeur que SaleRDto.total si les prix n'ont pas changé
```

## Points d'attention

- **Hypothèse (périmètre)** : le critère « mise à jour en temps réel » est rempli au poste, sans aller-retour serveur
  (hors connexion possible, réponse immédiate) ; pas d'endpoint de devis. Si le métier veut un net garanti avant
  l'encaissement (promotions complexes), un `POST /api/v1/sales/quote` pourra être ajouté par PRO-02.
- **Hypothèse (écart de prix)** : un prix modifié pendant que le poste est ouvert n'est connu qu'à l'encaissement ; la
  vente est enregistrée au prix du serveur et la caissière est avertie (« Vérifiez le rendu »). Recharger `/pos`
  reprend les nouveaux prix. Le catalogue n'est pas relu automatiquement (la vendeuse sélectionnée serait perdue).
- **Remise de ligne** : arrondie à l'ariary, comme la maquette ; à confirmer avec PRO-02 quand le serveur la calculera.
- Montants affichés sans décimale (`AriaryPipe`) : une ligne à 0,83 Ar s'affiche « 1 Ar » mais le calcul garde les
  centimes.
- Base : `feature/VTE-01-enregistrement-vente-multicanal` (PR empilée, à merger après VTE-01).
