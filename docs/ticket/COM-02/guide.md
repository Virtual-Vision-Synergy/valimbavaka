# Guide d'intégration — COM-02 · Calcul automatique de la commission

Statut : ✅ rédigé · Ticket : [#86](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/86) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Chaque vente créditée à une vendeuse (`SaleRecordedEvent`) et chaque prestation du salon qu'elle enregistre reçoit une commission calculée par le serveur, dans la transaction de l'opération : base (CA après remise, ou marge) × taux, arrondi à l'ariary. Le taux est le plus précis, sans cumul (décision PO du 07/11/2026) : service salon, sinon règle du canal (COM-01), sinon taux de la vendeuse. La commission d'une vente annulée (`SaleCancelledEvent`, LIV-05) passe à `CANCELLED`. L'administrateur les consulte dans Administration › Commissions calculées.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/commissions?page&size&sortBy&sortDirection&sellerId&from&to` | `commissions.read-all` (nouvelle, `ADMIN` seulement) | paramètres de requête, aucune écriture | `PageResponse<CommissionRDto>` | 401, 403, 422 (période inversée, taille > 100) |

### Services et ports Java réutilisables

- `CommissionService` (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/commission/service/CommissionService.java`) : `calculate(CommissionSource)` (idempotent, vide si le compte n'est pas une vendeuse active), `cancelBySource(type, id)`, `getCommissions(...)`, `findBySource(type, id)` (pour VTE-09).
- `SaleCommissionListener` (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/commission/service/impl/SaleCommissionListener.java:35` et `:40`) écoute `SaleCancelledEvent` et `SaleRecordedEvent` ; la prestation appelle `calculate` depuis `ServiceRecordServiceImpl`.
- La vente fournit le coût d'achat par `SaleService.getPurchaseCost(saleId)` (`Sale.purchaseCost()`).

### Frontend (`valimbavaka-ui/src/app/features/commissions/`)

- Route : `/commissions/calculated` (garde `commissions.read-all`), page `commission-list-page`.
- Service : `data-access/commission-service.ts`, types `CommissionRDto` dans `commission-models.ts`.

### Données

- Table `commissions` et permission `commissions.read-all` : migration `2026_11_08-83-changelog.sql` (rollback fourni). Une commission par opération (`uq_commissions_source`).
- Journal : `CREATE` sur `COMMISSION` au calcul (« Commission VTE-… · vendeuse · taux % de base Ar (CA ou marge) = montant Ar »), `CANCEL` quand la vente est annulée.

## Règles métier à respecter par les autres tickets

- Taux le plus précis, sans cumul : service salon > règle du canal > taux de la vendeuse ; aucun taux : commission 0 (`rateSource = NONE`).
- Base : `revenue` = total de la vente après remise (PRO-02) ; une règle « marge » applique le taux à CA − coût d'achat, jamais sous 0.
- Montant arrondi HALF_UP à l'ariary (`Commission.applyTerms`, `Commission.java:115`).
- Une vente sans vendeuse (commande de l'administrateur, LIV-03) n'a pas de commission.
- Statuts actuels : `CALCULATED`, `CANCELLED`. COM-03 y ajoutera la validation ; la contrainte `ck_commissions_status` devra alors être étendue par une nouvelle migration.
- Ni suppression ni modification manuelle : le recalcul est COM-05.

## Fichiers concernés

**Créés** : feature `commission` (`domain/Commission*`, `service/impl/CommissionServiceImpl`, `SaleCommissionListener`, `CommissionController`, `CommissionRDto`, `CommissionMapper`, `CommissionRepository`), migration `2026_11_08-83`, `commission-list-page`, `commission-service.ts`. **Modifiés** : `Sale.purchaseCost`, `SaleService.getPurchaseCost`, `ServiceRecordServiceImpl`.

## Dépendances

**Ce ticket s'appuie sur** :
- [COM-01](../COM-01/description.md) · [guide](../COM-01/guide.md)
- [SRV-02](../SRV-02/description.md) · [guide](../SRV-02/guide.md)
- [VTE-01](../VTE-01/description.md) · [guide](../VTE-01/guide.md)
- [SYS-05](../SYS-05/description.md) · [guide](../SYS-05/guide.md)

**Ces tickets s'appuient sur lui** :
- [VTE-09](../VTE-09/description.md) · [guide](../VTE-09/guide.md)
- [COM-03](../COM-03/description.md) · [guide](../COM-03/guide.md)
- [COM-04](../COM-04/description.md) · [guide](../COM-04/guide.md)
- [COM-05](../COM-05/description.md) · [guide](../COM-05/guide.md)

## Comment l'utiliser depuis un autre ticket

```java
commissionService.findBySource(CommissionSourceType.SALE, saleId); // VTE-09 : détail d'une vente
```

## Points d'attention

- Hypothèse : la commission d'une vente annulée est conservée (statut `CANCELLED`, montant gardé) plutôt que supprimée, pour la trace ; COM-03/COM-04 doivent exclure `CANCELLED` de leurs totaux.
- L'annulation d'une prestation n'existe pas encore : à brancher sur `cancelBySource(SERVICE_RECORD, id)` quand elle sera livrée.
- Le calcul et l'annulation sont synchrones : une erreur de commission annule la vente ou l'annulation ; c'est voulu (pas de vente sans commission).
- La page n'est pas dans `demo-pages.a11y.spec.ts` ; à vérifier en recette.
