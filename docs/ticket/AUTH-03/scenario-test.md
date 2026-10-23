# Scénarios de test — AUTH-03 · Création et modification d'un utilisateur

Statut : ✅ exécuté le 2026-10-23 — reste la recette manuelle (🧪) · Ticket : [#12](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/12) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · 🧪 recette = à faire à la main dans le navigateur. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (autorisé), `SELLER` (refusé) |
| Données de départ | un administrateur actif, les 3 rôles, les canaux de vente `STORE`, `FACEBOOK`, `PHONE`, `DELIVERY` |
| Tickets à avoir déployés avant | AUTH-01, JNL-01, SYS-05, JNL-02 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Le rôle choisi détermine la coquille | Quand l'administrateur crée une vendeuse puis qu'elle se connecte | menu et permissions de la vendeuse | `AuthSecurityIT.me_withSellerToken_returnsOnlyTheSellerPermissions` ; `menu.spec.ts` « should show sales and catalogue modules but no administration to a seller » | ✅ auto |
| 2 | Formulaire de saisie | Quand l'administrateur saisit email, mot de passe, prénom, nom, rôle | compte créé (201), ligne `CREATE` | `UserServiceImplTest.createUser_validRequest_recordsCreationInJournal` ; `JournalSecurityIT.createUser_byAdmin_leavesANominativeTimestampedLineInTheJournal` | ✅ auto |
| 2b | Formulaire de modification | Quand il modifie un compte | formulaire pré-rempli, mot de passe non modifiable, champs mis à jour, ligne `UPDATE` | `UserServiceImplTest.updateUser_administratorDemotedWhileAnotherIsActive_updatesAndRecordsInJournal` ; `user-form-page.ts` désactive le champ mot de passe en modification | ✅ auto · 🔍 code |
| 3 | Canal de vente et taux de commission | Quand le rôle est vendeuse avec `STORE` et `12.50` | canal et taux enregistrés | `UserServiceImplTest.createUser_sellerWithChannelAndCommission_savesSellerTerms` | ✅ auto |
| 3b | idem | Vendeuse sans commission | 422 | `UserServiceImplTest.createUser_sellerWithoutCommission_throwsBusinessException` | ✅ auto |
| 3c | idem | Administrateur avec commission | 422 | `UserServiceImplTest.createUser_administratorWithCommission_throwsBusinessException` | ✅ auto |
| 3d | idem | Canal inexistant ou inactif | 422 « Canal de vente introuvable » | `UserServiceImplTest.createUser_unknownSalesChannel_throwsBusinessException` | ✅ auto |
| 3e | idem (interface) | Quand le rôle choisi n'est pas vendeuse | canal et commission masqués et envoyés à `null` | `user-form-page.ts` (`isSeller`) | 🔍 code |
| 4 | Rôles proposés = Administrateur, Vendeuse, Livreur | Quand le formulaire charge les rôles | exactement trois | `AdminAccountInitializerIT.migrations_freshDatabase_seedExactlyTheThreeBusinessRoles` ; `user-service.spec.ts` « should return the business roles from the first page » ; contrainte `ck_roles_name` (insertion `USER` refusée) | ✅ auto · ✅ base |
| 5 | Création et modification réservées à l'admin, côté serveur | Quand une vendeuse appelle `POST` ou `PUT /api/v1/users` | 403 ; sans jeton 401 | `@HasPermission("users.create")` / `("users.update")`, rendu obligatoire par `ArchitectureTest.endpoints_require_permission` ; seul `GET` a un test IT (`AuthSecurityIT.getUsers_withNonAdminRole_returns403`) ; permissions `users.*` données à `ADMIN` seul en base | ✅ auto · ✅ base |
| 6 | Un utilisateur ne se supprime jamais, il se désactive | Quand on appelle `DELETE /api/v1/users/{id}` | 405, le compte existe toujours ; aucun bouton « Supprimer » dans la liste | `UserStatusIT.deleteUser_isNotExposed` ; `ArchitectureTest.no_delete_endpoints` ; `user-list-page.spec.ts` « should offer no way to delete an account, only to deactivate it » | ✅ auto (corrigé le 2026-10-23) |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Email déjà utilisé (création) | `vendeuse.test@valimbavaka.mg` existant | 409 `CONFLICT`, rien dans le journal | `UserServiceImplTest.createUser_emailAlreadyUsed_throwsConflict`, `createUser_emailAlreadyUsed_recordsNothingInJournal` | ✅ auto |
| Email déjà utilisé par un autre compte (modification) | renommer B avec `" Autre@Valimbavaka.mg "` | 409, rien dans le journal | `UserServiceImplTest.updateUser_emailUsedByAnotherUser_throwsConflictAndRecordsNothing` | ✅ auto |
| `id` ou `role` dans le corps | `{"id":"…","role":"ADMIN",…}` | ignorés ; le rôle passe uniquement par `roleId` | `ArchitectureTest.input_dtos_have_no_id_or_role` ; `UserStatusIT.setActive_massAssignmentOfRole_isIgnored` | ✅ auto |
| Données invalides | email `abc`, mot de passe de 14 caractères, taux `100.01` ou `12.345`, taux `-1` | 422 `VALIDATION_FAILED` avec la liste des champs | contraintes `@Email`, `@Size(min = 15)`, `@DecimalMin`, `@DecimalMax`, `@Digits` de `CreateUserWDto` / `UpdateUserWDto`, traduites en 422 par `GlobalExceptionHandler` | 🔍 code |
| Utilisateur introuvable | UUID au hasard | 404 `NOT_FOUND` | `UserServiceImplTest.getUserById_unknownId_throwsNotFound` | ✅ auto |
| Création rejouée (file hors ligne) | même `Idempotency-Key` | un seul compte | `IdempotencyIT.createUser_replayedWithSameKey_returnsSameResponseWithoutDuplicate` | ✅ auto |
| Pagination et recherche | `page=1&size=20&search=` | recherche vide non envoyée, tri en liste blanche | `user-service.spec.ts` « should request a page with the VVS pagination parameters and skip an empty search » | ✅ auto |

## Cas limites

| Cas | Exemple d'entrée | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Taux minimal | `0` | accepté | `@DecimalMin("0.00")` | 🔍 code |
| Taux maximal | `100` | accepté | `@DecimalMax("100.00")` ; colonne `NUMERIC(5,2)` | 🔍 code · ✅ base |
| Taux trop grand | `100.01` | 422 | `@DecimalMax` | 🔍 code |
| Trop de décimales | `12.345` | 422 | `@Digits(fraction = 2)` | 🔍 code |
| Taux négatif | `-1` | 422 | `@DecimalMin` | 🔍 code |
| Email avec majuscules et espaces | `  Rasoa@Valimbavaka.MG ` | enregistré `rasoa@valimbavaka.mg` ; doublon détecté même avec une autre casse | `User.normalizeEmail` (`strip` + minuscules) utilisé à la création et à la modification | 🔍 code |
| Passage vendeuse → livreur | rôle modifié | l'interface envoie canal et taux à `null` ; sinon 422 (règle `validateTerms` de l'entité) | `user-form-page.ts` + `User.validateTerms` | 🔍 code |
| Mot de passe de 14 caractères | `court-14-carac` | 422 ; 15 caractères acceptés (convention §9.2, sans MFA) | `@Size(min = 15)` de `CreateUserWDto` et `MIN_PASSWORD_LENGTH = 15` côté interface | 🔍 code (corrigé le 2026-10-23) |
| Mot de passe très long | 128 caractères | accepté ; 129 refusé | `@Size(max = 128)` | 🔍 code |
| Prénom ou nom de 101 caractères | — | 422 | `@Size(max = 100)` | 🔍 code |

## Tests existants

- Backend : `feature/user/service/impl/UserServiceImplTest.java`, `feature/user/controller/UserStatusIT.java`, `feature/user/config/AdminAccountInitializerIT.java`, `feature/auth/controller/AuthSecurityIT.java`, `feature/journal/controller/JournalSecurityIT.java`, `security/idempotency/IdempotencyIT.java`, `ArchitectureTest.java`
- Frontend : `features/user/data-access/user-service.spec.ts`, `features/user/pages/user-list-page/user-list-page.spec.ts` (pas de test pour `user-form-page`)

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Créer une vendeuse (canal + taux), un livreur, un administrateur | seul le formulaire vendeuse montre canal et commission | 🧪 recette |
| Se connecter avec chaque compte créé | menu du rôle | 🧪 recette |
| Modifier le rôle d'un compte | ligne `UPDATE` dans le journal | 🧪 recette |
| Créer avec un email déjà pris | message du serveur affiché | 🧪 recette |

## Résultat de l'exécution

- **Date** : 2026-10-23.
- **Backend** : CI GitHub `mvn verify` vert sur un code identique à `develop`. **Frontend** : `ng test` local, 102 tests verts sur 102. **Base** : migrations rejouées sur PostgreSQL 16 ; insertion d'un rôle `USER` refusée par `ck_roles_name`.
- **Anomalies** :
  1. ✅ Corrigé le 2026-10-23 (branche `fix/AUTH-04-garde-fous-administrateur`) : `DELETE /api/v1/users/{id}` et la permission `users.delete` sont retirés (migration `2026_10_23-01`), le bouton « Supprimer » aussi ; `ArchitectureTest.no_delete_endpoints` interdit tout `@DeleteMapping`.
  2. ✅ Corrigé le 2026-10-23 : mot de passe d'un nouveau compte à 15 caractères minimum, serveur et interface. Reste ouvert : refus des mots de passe courants (convention §9.2), gravité faible.
  3. ⚠ `updateUser` est maintenant couvert en unitaire (6 tests) et en intégration (2 tests) ; les contrôles 403 de `POST` et `PUT` ne sont encore prouvés que par la règle ArchUnit : ajouter `createUser_withSellerToken_returns403`.
- **Recette manuelle** : 4 étapes 🧪.
