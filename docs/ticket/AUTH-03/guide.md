# Guide d'intégration — AUTH-03 · Création et modification d'un utilisateur

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ⚠ pré-rempli depuis le code au 2026-10-23 — à vérifier et compléter · Ticket : [#12](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/12) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

_(2 à 3 phrases : ce que la fonctionnalité fait et ce qu'elle offre aux autres.)_

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie |
|---|---|---|---|---|
| GET | `/api/v1/users` | users.read | `PageQuery` | `PageResponse<UserRDto>` |
| GET | `/api/v1/users/{id}` | users.read | `-` | `UserRDto` |
| POST | `/api/v1/users` | users.create | `CreateUserWDto` | `UserRDto` |
| PUT | `/api/v1/users/{id}` | users.update | `UpdateUserWDto` | `UserRDto` |
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
- `valimbavaka-ui/src/app/features/user/data-access/role-service.ts`
- `valimbavaka-ui/src/app/features/user/data-access/sales-channel-service.ts`
- `valimbavaka-ui/src/app/features/user/data-access/user-models.ts`
- `valimbavaka-ui/src/app/features/user/data-access/user-service.ts`
- `valimbavaka-ui/src/app/features/user/pages/user-form-page/user-form-page.css`
- `valimbavaka-ui/src/app/features/user/pages/user-form-page/user-form-page.html`
- `valimbavaka-ui/src/app/features/user/pages/user-form-page/user-form-page.ts`
- `valimbavaka-ui/src/app/features/user/pages/user-list-page/user-list-page.css`
- `valimbavaka-ui/src/app/features/user/pages/user-list-page/user-list-page.html`
- `valimbavaka-ui/src/app/features/user/pages/user-list-page/user-list-page.ts`
- `valimbavaka-ui/src/app/features/user/user.routes.ts`

**Tests**
- `valimbavaka-ui/src/app/features/user/data-access/user-service.spec.ts`
- `valimbavaka-ui/src/app/features/user/pages/user-list-page/user-list-page.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [AUTH-01](../AUTH-01/description.md) · [guide](../AUTH-01/guide.md)
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md)
- [SYS-05](../SYS-05/description.md) · [guide](../SYS-05/guide.md)
- [JNL-02](../JNL-02/description.md) · [guide](../JNL-02/guide.md)

**Ces tickets s'appuient sur lui** :
- [AUTH-04](../AUTH-04/description.md) · [guide](../AUTH-04/guide.md)
- [LIV-01](../LIV-01/description.md) · [guide](../LIV-01/guide.md)
- [VTE-01](../VTE-01/description.md) · [guide](../VTE-01/guide.md)
- [COM-01](../COM-01/description.md) · [guide](../COM-01/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
