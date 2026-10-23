# Guide d'intégration — SYS-05 · Rôles métier, référentiels et compte administrateur initial

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ⚠ pré-rempli depuis le code au 2026-10-23 — à vérifier et compléter · Ticket : [#111](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/111) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

_(2 à 3 phrases : ce que la fonctionnalité fait et ce qu'elle offre aux autres.)_

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie |
|---|---|---|---|---|
| GET | `/api/v1/sales-channels` | referentials.read | `-` | `SalesChannelRDto[]` |
| GET | `/api/v1/product-ranges` | referentials.read | `-` | `ProductRangeRDto[]` |
| GET | `/api/v1/stock-locations` | referentials.read | `-` | `StockLocationRDto[]` |
| GET | `/api/v1/roles` | roles.read | `-` | `RoleRDto[]` |

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
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/controller/ReferentialController.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/domain/ProductRange.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/domain/ReferentialEntity.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/domain/SalesChannel.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/domain/StockLocation.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/dto/ProductRangeRDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/dto/SalesChannelRDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/dto/StockLocationRDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/mapper/ReferentialMapper.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/repository/ProductRangeRepository.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/repository/SalesChannelRepository.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/repository/StockLocationRepository.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/service/ReferentialService.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/referential/service/impl/ReferentialServiceImpl.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/config/AdminAccountInitializer.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/config/AdminAccountProperties.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/controller/RoleController.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/controller/UserController.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/domain/Permission.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/domain/Role.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/domain/RoleName.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/domain/SellerTerms.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/domain/User.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/domain/UserProfile.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/dto/CreateUserWDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/dto/RoleRDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/dto/UpdateUserWDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/dto/UserRDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/dto/UserStatusWDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/exception/AdminAccountConfigurationException.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/mapper/RoleMapper.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/mapper/UserMapper.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/repository/RoleRepository.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/repository/UserRepository.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/service/RoleService.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/service/UserService.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/service/impl/AuthenticatedUserLoaderImpl.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/service/impl/RoleServiceImpl.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/service/impl/UserServiceImpl.java`

**Frontend**
- `valimbavaka-ui/src/app/features/referential/data-access/referential-models.ts`
- `valimbavaka-ui/src/app/features/referential/data-access/referential-service.ts`

**Tests**
- `valimbavaka-ui/src/app/features/referential/data-access/referential-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- _(rien)_

**Ces tickets s'appuient sur lui** :
- [AUTH-01](../AUTH-01/description.md) · [guide](../AUTH-01/guide.md)
- [AUTH-03](../AUTH-03/description.md) · [guide](../AUTH-03/guide.md)
- [ART-01](../ART-01/description.md) · [guide](../ART-01/guide.md)
- [ART-03](../ART-03/description.md) · [guide](../ART-03/guide.md)
- [ART-05](../ART-05/description.md) · [guide](../ART-05/guide.md)
- [STK-01](../STK-01/description.md) · [guide](../STK-01/guide.md)
- [STK-03](../STK-03/description.md) · [guide](../STK-03/guide.md)
- [TRF-03](../TRF-03/description.md) · [guide](../TRF-03/guide.md)
- [PRO-01](../PRO-01/description.md) · [guide](../PRO-01/guide.md)
- [VTE-01](../VTE-01/description.md) · [guide](../VTE-01/guide.md)
- [VTE-08](../VTE-08/description.md) · [guide](../VTE-08/guide.md)
- [INV-01](../INV-01/description.md) · [guide](../INV-01/guide.md)
- [COM-01](../COM-01/description.md) · [guide](../COM-01/guide.md)
- [COM-02](../COM-02/description.md) · [guide](../COM-02/guide.md)
- [DSH-04](../DSH-04/description.md) · [guide](../DSH-04/guide.md)
- [RPT-01](../RPT-01/description.md) · [guide](../RPT-01/guide.md)
- [RPT-04](../RPT-04/description.md) · [guide](../RPT-04/guide.md)
- [SYS-06](../SYS-06/description.md) · [guide](../SYS-06/guide.md)
- [STK-06](../STK-06/description.md) · [guide](../STK-06/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
