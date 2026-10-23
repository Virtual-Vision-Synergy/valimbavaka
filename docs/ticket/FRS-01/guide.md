# Guide d'intégration — FRS-01 · Fiche fournisseur complète

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ⚠ pré-rempli depuis le code au 2026-10-23 — à vérifier et compléter · Ticket : [#17](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/17) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

_(2 à 3 phrases : ce que la fonctionnalité fait et ce qu'elle offre aux autres.)_

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie |
|---|---|---|---|---|
| GET | `/api/v1/suppliers` | suppliers.read | `PageQuery + filtres` | `PageResponse<SupplierRDto>` |
| GET | `/api/v1/suppliers/{id}` | suppliers.read | `-` | `SupplierRDto` |

_Relevé dans les contrôleurs ; compléter erreurs possibles, filtres et exemples de corps._

### Services et ports Java réutilisables

- _(à compléter : interface de service, port `security`, événement publié, etc.)_

### Frontend

- Routes : _(à compléter)_
- Services / stores (`data-access`) : _(à compléter)_
- Composants réutilisables : _(à compléter)_

### Données

- Tables et migrations Liquibase : _(à compléter)_
- Écriture dans le journal (action, champs) : _(à compléter)_

## Règles métier à respecter par les autres tickets

- _(à compléter : statuts, états interdits, calculs, arrondis, droits par rôle)_


## Fichiers concernés

_Fichiers existants repérés dans les PR liées ; ajouter ceux modifiés ensuite._

**Backend**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/controller/SupplierController.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/domain/Supplier.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/domain/SupplierDetails.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/domain/SupplierType.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/dto/SupplierRDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/dto/SupplierStatusWDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/dto/SupplierWDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/mapper/SupplierMapper.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/repository/SupplierRepository.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/service/SupplierService.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier/service/impl/SupplierServiceImpl.java`

**Frontend**
- `valimbavaka-ui/src/app/features/supplier/data-access/supplier-models.ts`
- `valimbavaka-ui/src/app/features/supplier/data-access/supplier-service.ts`
- `valimbavaka-ui/src/app/features/supplier/pages/supplier-form-page/supplier-form-page.css`
- `valimbavaka-ui/src/app/features/supplier/pages/supplier-form-page/supplier-form-page.html`
- `valimbavaka-ui/src/app/features/supplier/pages/supplier-form-page/supplier-form-page.ts`
- `valimbavaka-ui/src/app/features/supplier/pages/supplier-list-page/supplier-list-page.css`
- `valimbavaka-ui/src/app/features/supplier/pages/supplier-list-page/supplier-list-page.html`
- `valimbavaka-ui/src/app/features/supplier/pages/supplier-list-page/supplier-list-page.ts`
- `valimbavaka-ui/src/app/features/supplier/supplier.routes.ts`
- `valimbavaka-ui/src/app/features/suppliers/components/supplier-detail/supplier-detail.css`
- `valimbavaka-ui/src/app/features/suppliers/components/supplier-detail/supplier-detail.html`
- `valimbavaka-ui/src/app/features/suppliers/components/supplier-detail/supplier-detail.ts`
- `valimbavaka-ui/src/app/features/suppliers/data-access/supplier-fixtures.ts`
- `valimbavaka-ui/src/app/features/suppliers/data-access/supplier-models.ts`
- `valimbavaka-ui/src/app/features/suppliers/data-access/supplier-service.ts`
- `valimbavaka-ui/src/app/features/suppliers/data-access/supplier-tones.ts`
- `valimbavaka-ui/src/app/features/suppliers/pages/supplier-list-page/supplier-list-page.css`
- `valimbavaka-ui/src/app/features/suppliers/pages/supplier-list-page/supplier-list-page.html`
- `valimbavaka-ui/src/app/features/suppliers/pages/supplier-list-page/supplier-list-page.ts`
- `valimbavaka-ui/src/app/features/suppliers/suppliers.routes.ts`

**Tests**
- `valimbavaka-ui/src/app/features/supplier/data-access/supplier-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md)

**Ces tickets s'appuient sur lui** :
- [FRS-02](../FRS-02/description.md) · [guide](../FRS-02/guide.md)
- [FRS-03](../FRS-03/description.md) · [guide](../FRS-03/guide.md)
- [FRS-04](../FRS-04/description.md) · [guide](../FRS-04/guide.md)
- [ART-01](../ART-01/description.md) · [guide](../ART-01/guide.md)
- [STK-05](../STK-05/description.md) · [guide](../STK-05/guide.md)
- [STK-06](../STK-06/description.md) · [guide](../STK-06/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
