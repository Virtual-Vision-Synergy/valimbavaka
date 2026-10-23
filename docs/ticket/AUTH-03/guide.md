# Guide d'intégration — AUTH-03 · Création et modification d'un utilisateur

Statut : ✅ complet au 2026-10-23 — vérifié sur le code, les tests (CI + exécution locale) et la base · Ticket : [#12](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/12) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur crée et modifie les comptes (identité, rôle, et pour une vendeuse son canal de vente et son taux de commission). Chaque écriture est tracée dans le journal. Les autres modules lisent les utilisateurs par leur identifiant (UUID) et ne dépendent jamais de l'entité `User`.

## Contrat exposé

### Endpoints backend (`/api/v1/users`, `/api/v1/roles`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/users` | `users.read` | `PageQuery` (`page`, `size`, `search`, `sortBy`, `sortDirection`) | 200 `PageResponse<UserRDto>` | 401, 403 |
| GET | `/api/v1/users/{id}` | `users.read` | - | 200 `UserRDto` | 404 `NOT_FOUND` |
| POST | `/api/v1/users` | `users.create` | `CreateUserWDto` | 201 `UserRDto` + `Location` | 409 `CONFLICT` (email), 422 `VALIDATION_FAILED` |
| PUT | `/api/v1/users/{id}` | `users.update` | `UpdateUserWDto` | 200 `UserRDto` | 404, 409, 422 (données invalides ou garde-fous AUTH-04) |
| GET | `/api/v1/roles` | `roles.read` | `PageQuery` | 200 `PageResponse<RoleRDto>` | 401, 403 |

Tri autorisé sur `/users` : `createdAt` (défaut, décroissant), `email`, `firstName`, `lastName`. L'endpoint `PATCH /{id}/status` est décrit dans [AUTH-04](../AUTH-04/guide.md).

**`CreateUserWDto`** : `email` (obligatoire, email valide, 255 max), `password` (15 à 128 caractères, convention §9.2), `firstName`, `lastName` (obligatoires, 100 max), `roleId` (UUID, obligatoire), `salesChannelId` (UUID, facultatif), `commissionRate` (0 à 100, 2 décimales, facultatif).
**`UpdateUserWDto`** : mêmes champs sans `password`, plus `active` (obligatoire). Le mot de passe ne se modifie pas ici.
**`UserRDto`** : `id`, `email`, `firstName`, `lastName`, `role` (`RoleRDto`), `salesChannelId`, `commissionRate`, `active`, `passwordChangeRequired`, `createdAt`, `updatedAt`. Jamais de hash de mot de passe.

### Règles métier portées par le serveur

- Rôles proposés : exactement `ADMIN` (Administrateur), `SELLER` (Vendeuse), `DELIVERY_DRIVER` (Livreur), voir [SYS-05](../SYS-05/guide.md).
- Email normalisé (espaces retirés, minuscules) et unique : 409 s'il est déjà pris, y compris à la modification d'un autre compte.
- Une **vendeuse** doit avoir à la fois un canal de vente et un taux de commission ; les deux sont refusés pour un autre rôle (`BusinessException`, 422). Le canal doit exister et être actif (`ReferentialService.isActiveSalesChannel`).
- Le mot de passe est encodé (Argon2id) ; le DTO d'entrée ne contient ni `id` ni champ de rôle autre que `roleId` (règle ArchUnit `input_dtos_have_no_id_or_role`).
- Création et modification : une ligne `CREATE` / `UPDATE` dans le journal (« Création de l'utilisateur … », « Modification de l'utilisateur … »), dans la même transaction. Rien n'est tracé si l'email est déjà utilisé.
- Création initiale de l'administrateur (SYS-05) : voir [SYS-05](../SYS-05/guide.md).

### Services et ports Java

- `feature.user.service.UserService` : `getUsers`, `getUserById`, `createUser`, `updateUser`, `setActive`, `createInitialAdministratorIfMissing`.
- `feature.user.service.RoleService` : lecture des rôles.
- `feature.user.service.impl.AuthenticatedUserLoaderImpl` implémente le port `security.AuthenticatedUserLoader`.
- Autre feature qui a besoin d'un utilisateur : stocker son UUID (pas d'association JPA entre features) et lire l'auteur courant via `SecurityAuditorAware` ou `JwtSubject`.

### Frontend (`features/user`)

- Routes lazy (`user.routes.ts`, monté sur `/users`, permission `users.read`) : liste `''`, création `new`, modification `:id/edit`.
- `data-access/user-service.ts` : `list`, `getById`, `create`, `update`, `setActive`, `delete` ; `role-service.ts` : `listAll` ; `sales-channel-service.ts` : `listAll`.
- `pages/user-form-page` : formulaire réactif (email, mot de passe à la création, prénom, nom, rôle, actif) ; canal de vente et taux de commission n'apparaissent et ne sont envoyés que si le rôle choisi est `SELLER`, sinon `null`.
- `pages/user-list-page` : recherche, pagination serveur, voir [AUTH-04](../AUTH-04/guide.md) pour le statut.
- Types : `UserRDto`, `CreateUserWDto`, `UpdateUserWDto`, `UserStatusWDto` dans `data-access/user-models.ts`.

### Données

- Tables `users` (colonnes `is_active`, `is_password_change_required`, `sales_channel_id`, `commission_rate NUMERIC(5,2)` avec `CHECK` 0 à 100, `failed_attempts`, `locked_until`) et `roles` ; migrations `2026_10_07-01`, `2026_10_15-01` (rôles, canaux, audit), `2026_10_16-02` (canal et commission).
- Permissions `users.read`, `users.create`, `users.update`, `roles.read` (migration `2026_10_19-01`), toutes réservées à `ADMIN` ; `users.delete` a été retirée par `2026_10_23-01`.

## Règles métier à respecter par les autres tickets

- Pour lier une donnée à un utilisateur (vendeuse d'une vente, livreur d'une livraison) : stocker son UUID ; filtrer sur `active = true` et sur le rôle voulu dans les listes de sélection opérationnelles (voir [AUTH-04](../AUTH-04/guide.md)).
- Le taux de commission d'une vendeuse vit sur le compte (`users.commission_rate`) ; la question client « commission propre à l'utilisateur, au canal ou au service salon ? » reste ouverte et concerne [COM-01](../COM-01/description.md) et [SRV-01](../SRV-01/description.md).
- Un utilisateur ne se supprime jamais en pratique : il se désactive ([AUTH-04](../AUTH-04/guide.md), JNL-02).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user/`)
- `controller/UserController.java`, `controller/RoleController.java`
- `domain/User.java`, `Role.java`, `RoleName.java`, `Permission.java`, `SellerTerms.java`, `UserProfile.java`
- `dto/CreateUserWDto.java`, `UpdateUserWDto.java`, `UserRDto.java`, `UserStatusWDto.java`, `RoleRDto.java`
- `mapper/UserMapper.java`, `RoleMapper.java` ; `repository/UserRepository.java`, `RoleRepository.java`
- `service/UserService.java`, `RoleService.java`, `service/impl/UserServiceImpl.java`, `RoleServiceImpl.java`, `AuthenticatedUserLoaderImpl.java`

**Migrations Liquibase** (`valimbavaka-core/src/main/resources/db/changelog/changes/`)
- `2026_10_07-01-changelog.sql`, `2026_10_15-01-changelog.sql`, `2026_10_16-02-changelog.sql`, `2026_10_19-01-changelog.sql`

**Frontend** (`valimbavaka-ui/src/app/features/user/`)
- `user.routes.ts`, `data-access/{user-service,role-service,sales-channel-service,user-models}.ts`
- `pages/user-form-page/*`, `pages/user-list-page/*`

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/user/service/impl/UserServiceImplTest.java`, `feature/user/controller/UserStatusIT.java`, `security/idempotency/IdempotencyIT.java` (création rejouée)
- `valimbavaka-ui/src/app/features/user/data-access/user-service.spec.ts`, `pages/user-list-page/user-list-page.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [AUTH-01](../AUTH-01/guide.md) (droits `@HasPermission`), [JNL-01](../JNL-01/guide.md) (`JournalService.record`), [SYS-05](../SYS-05/guide.md) (rôles et canaux), [JNL-02](../JNL-02/guide.md) (pas de suppression)

**Ces tickets s'appuient sur lui** :
- [AUTH-04](../AUTH-04/guide.md), [LIV-01](../LIV-01/description.md) (livreurs), [VTE-01](../VTE-01/description.md) (vendeuses), [COM-01](../COM-01/description.md) (taux de commission)

## Comment l'utiliser depuis un autre ticket

```java
// Autre feature : on ne référence l'utilisateur que par son UUID
@Column(name = "seller_id", nullable = false)
private UUID sellerId;
```

```ts
// Frontend : lister les vendeuses actives pour un sélecteur
this.http.get<PageResponse<UserRDto>>('/api/v1/users', { params: { search: '', size: 100 } });
// puis filtrer côté appelant sur role.name === 'SELLER' && active
```

Aujourd'hui `GET /api/v1/users` n'a ni filtre `role` ni filtre `active` : un ticket qui a besoin d'une liste de sélection opérationnelle doit ajouter ce filtre côté serveur (voir les points d'attention de [AUTH-04](../AUTH-04/guide.md)).

## Points d'attention

- `PUT /api/v1/users/{id}` applique les garde-fous de `setActive` sur `active` et sur le retrait du rôle administrateur (voir [AUTH-04](../AUTH-04/guide.md)).
- Aucun compte ne se supprime : il n'existe aucun `DELETE` (règle ArchUnit `no_delete_endpoints`).
- Question client en attente (voir le ticket) : propriétaire de la commission, et connexion par email ou identifiant.
- Aucun endpoint de changement de mot de passe ni de réinitialisation : le mot de passe n'est défini qu'à la création.
