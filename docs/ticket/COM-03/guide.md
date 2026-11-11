# Guide d'intégration — COM-03 · Validation des commissions

Statut : ✅ rédigé · Ticket : [#87](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/87) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur voit, pour chaque vendeuse et sur une période (le mois dans l'écran), ce qu'il reste à valider et ce qui l'est déjà, puis valide en une fois toutes les commissions calculées de la vendeuse sur la période. Une commission validée est définitive. Les commissions annulées (`CANCELLED`) ne sont ni comptées ni validées.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/commissions/sellers?from&to&page&size` | `commissions.read-all` (existante, `ADMIN`) | journées `from` et `to` (incluses), obligatoires ; page base 1, size ≤ 100 | `PageResponse<SellerCommissionRDto>` (vendeuses par nom : `status` `TO_VALIDATE` / `VALIDATED`, nombre et total à valider, nombre et total validés) | 401, 403, 422 |
| POST | `/api/v1/commissions/validation` | `commissions.validate` (nouvelle, `ADMIN` seulement) | `CommissionValidationWDto` : `sellerId`, `from`, `to` | `CommissionValidationRDto` : vendeuse, période, `validatedCount`, `validatedAmount` | 401, 403, 404 (aucune commission non annulée pour la vendeuse sur la période), 409 (tout est déjà validé), 422 (champ absent, `from` après `to`) |

### Services et ports Java réutilisables

- `CommissionValidationService` (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/commission/service/CommissionValidationService.java`) : `getSellerSummaries(query, from, to)` et `validate(request)` (une transaction, une ligne de journal par commission).
- `Commission.validate(Instant)` n'accepte que `CALCULATED` ; `Commission.cancel()` accepte aussi `VALIDATED` (voir ci-dessous).
- `CommissionRepository` : `searchSellers`, `totalsBySellerAndStatus`, `findBySellerIdAndStatusAndBusinessDateBetween`.

### Frontend (`valimbavaka-ui/src/app/features/commissions/`)

- Route inchangée : `/commissions/calculated` (garde `commissions.read-all`), page `commission-list-page` : tableau « Commissions par vendeuse » (pastille À valider / Validée, totaux, bouton « Valider » réservé à `commissions.validate`, confirmation par `vvs-confirm-dialog`), puis le détail des commissions du mois.
- `data-access/commission-service.ts` : `sellers(query, from, to)` et `validate(request)` ; types `SellerCommissionRDto`, `CommissionValidationWDto`, `CommissionValidationRDto` dans `commission-models.ts`.

### Données

- Migration `2026_11_11-60-changelog.sql` (rollback fourni) : colonne `commissions.validated_at`, contrainte `ck_commissions_status` étendue à `VALIDATED`, contrainte `ck_commissions_validated_at`, permission `commissions.validate` donnée à `ADMIN`.
- Journal : `UPDATE` sur `COMMISSION` pour chaque commission validée (« Commission VTE-… · vendeuse validée : montant Ar »).

## Règles métier à respecter par les autres tickets

- Statuts : `CALCULATED` → `VALIDATED` (définitif, pas de dé-validation) ; `CANCELLED` exclu de tous les totaux (COM-04, COM-05 doivent faire pareil).
- Statut d'une vendeuse : `TO_VALIDATE` tant qu'au moins une commission `CALCULATED` existe sur la période, sinon `VALIDATED` ; une vendeuse dont toutes les commissions sont annulées n'apparaît pas.
- Une nouvelle vente après une validation crée une commission `CALCULATED` : la vendeuse repasse « À valider », la validation suivante ne prend que les nouvelles.
- Vente annulée après validation (avoir FAC-04 ou annulation) : hypothèse prudente, la commission passe à `CANCELLED` (n'est plus comptée) et garde `validated_at` ; le journal le dit (« déjà validée : versement à corriger »). Aucun recalcul du versement n'est fait ici : c'est une correction à faire par l'administrateur (COM-05).
- Une vendeuse ne valide jamais ses propres commissions (403).

## Fichiers concernés

**Créés** : `CommissionValidationService` / `CommissionValidationServiceImpl`, `CommissionValidationWDto`, `CommissionValidationRDto`, `SellerCommissionRDto`, `SellerCommissionStatus`, projections `SellerKey` et `SellerStatusTotal`, migration `2026_11_11-60`, `CommissionValidationIT`. **Modifiés** : `Commission`, `CommissionStatus`, `CommissionRepository`, `CommissionController`, `CommissionServiceImpl.cancelBySource`, `commission-list-page`, `commission-service.ts`, `commission-models.ts`, `fr.json`, `mg.json`, `auth-testing.ts`.

## Dépendances

**Ce ticket s'appuie sur** :
- [COM-02](../COM-02/description.md) · [guide](../COM-02/guide.md)
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md)

**Ces tickets s'appuient sur lui** :
- _(rien)_ ; COM-04 et COM-05 liront le statut `VALIDATED`.

## Comment l'utiliser depuis un autre ticket

```text
GET  /api/v1/commissions/sellers?from=2026-11-01&to=2026-11-30&size=100
POST /api/v1/commissions/validation  { sellerId, from, to }
```

## Points d'attention

- Hypothèse : la validation se fait par vendeuse et par période (mois dans l'écran), pas commission par commission ; le critère demande le statut par vendeuse.
- Hypothèse : une commission validée dont la vente est annulée ensuite passe à `CANCELLED` (la description ne tranche pas) ; l'alternative « rester validée » aurait gonflé les totaux d'une vente annulée.
- L'écran charge au plus 100 vendeuses du mois (maximum de l'API), sans pagination.
- La page n'est pas dans `demo-pages.a11y.spec.ts` ; le bouton porte un libellé nommé par vendeuse, à vérifier en recette.
