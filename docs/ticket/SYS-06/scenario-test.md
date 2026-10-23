# Scénarios de test — SYS-06 · Audit : auteur des opérations en UUID

Statut : ✅ exécuté le 2026-10-23 — reste la recette manuelle (🧪) · Ticket : [#112](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/112) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · 🧪 recette = à faire à la main. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (pour créer une ressource authentifiée) |
| Données de départ | un administrateur, base migrée |
| Tickets à avoir déployés avant | SYS-05 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | `created_by` / `updated_by` en UUID | Migrations appliquées | type `uuid` sur `users`, `roles`, `permissions`, `sales_channels`, `product_ranges`, `stock_locations`, `suppliers`, `refresh_tokens` | lecture de `information_schema.columns` | ✅ base |
| 1b | idem (tables restantes) | Toutes les tables avec `created_by` | aucune en `bigint` | la table technique `dev` (changeset `1789471390262-2`) a encore `created_by` / `updated_by` en `bigint` et `created_at` sans fuseau | ❌ écart |
| 1c | Persistance des champs d'audit | Enregistrement d'un fournisseur | `created_at`, `updated_at` renseignés ; `version` à 0 | `SupplierRepositoryIT.save_newSupplier_isPersistedWithAuditFields` | ✅ auto |
| 2 | Auteur renseigné depuis l'utilisateur connecté | L'administrateur crée une ressource | `created_by` = `updated_by` = UUID du `sub` du jeton | `SecurityAuditorAware.getCurrentAuditor` → `JwtSubject.userIdOf` ; `BaseEntity` `@CreatedBy` / `@LastModifiedBy` ; aucun test d'intégration ne relit `created_by` | 🔍 code |
| 2b | idem (même source que le journal) | Le journal écrit son auteur | même `AuditorAware<UUID>` | `JournalServiceImplTest.record_authenticatedUser_savesTimestampedEntryInTheNameOfTheUser` | ✅ auto |
| 2c | Pas de saisie manuelle | Corps avec `id`, `createdBy` | ignorés | `ArchitectureTest.input_dtos_have_no_id_or_role` ; `SupplierSecurityIT.createSupplier_massAssignmentOfIdAndStatus_isIgnored` ; `BaseEntity` n'a aucun setter | ✅ auto · 🔍 code |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Écriture hors requête authentifiée | création de l'administrateur au démarrage | auteur `NULL`, pas d'erreur | `AdminAccountInitializerIT.run_atStartupWithoutAdministrator_createsAdministratorFromEnvironmentVariables` | ✅ auto |
| Jeton sans `sub` | jeton sans claim `sub` | auteur vide (`Optional.empty`) | `JwtSubject.userIdOf` | 🔍 code |
| `sub` qui n'est pas un UUID | `sub = "abc"` | `IllegalArgumentException` (500) ; impossible en pratique car le jeton est signé par le serveur avec l'UUID du compte | `JwtSubject.userIdOf` (`UUID::fromString`) | 🔍 code |
| Conflit de version | deux mises à jour concurrentes | 409 attendu | `ObjectOptimisticLockingFailureException` n'est pas traduite : 500 | ❌ écart |

## Cas limites

| Cas | Exemple | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Modification par un second compte | A crée, B modifie | `updated_by` = B, `created_by` reste A (`updatable = false`) | annotations de `BaseEntity` | 🔍 code |
| Mise à jour sans changement | même valeurs | `updated_by` et `version` inchangés (pas de flush) | comportement JPA ; `setActive` ne modifie rien si l'état est identique | 🔍 code |
| Anciennes lignes migrées | lignes antérieures à `2026_10_15-01-4` | `created_by` à `NULL` | `USING NULL` dans la migration | 🔍 code |
| Écriture SQL directe | `idempotency_keys`, `journal_entries` | pas d'audit JPA ; le journal a son propre auteur | tables sans `created_by` relevées en base | ✅ base |
| Rejeu hors ligne | saisie de A rejouée | auteur = A (la file est rattachée à son compte) | SYS-02 | ✅ auto |
| Dates en UTC | `created_at` | `timestamp with time zone` | lecture en base | ✅ base |

## Tests existants

- Aucune classe dédiée à `BaseEntity` ou `SecurityAuditorAware`. Couverture indirecte : `SupplierRepositoryIT`, `AdminAccountInitializerIT`, `JournalServiceImplTest`, `UserServiceImplTest` (mock de `AuditorAware<UUID>`), `ArchitectureTest`.
- Frontend : sans objet.

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Créer un fournisseur en administrateur puis `SELECT created_by, updated_by FROM suppliers` | UUID de l'administrateur | 🧪 recette |
| Modifier avec un second administrateur | `updated_by` change, `created_by` non | 🧪 recette |

## Résultat de l'exécution

- **Date** : 2026-10-23.
- **Backend** : CI GitHub `mvn verify` vert sur un code identique à `develop`. **Base** : types de toutes les colonnes d'audit relus après les 10 migrations.
- **Anomalies** :
  1. ❌ Gravité faible : la table technique `dev` (issue du premier changeset `2026_09_15-01`, sans entité Java) garde `created_by` / `updated_by` en `bigint`. Ajouter un changeset qui la supprime (avec `revinfo` et `revchanges` si Envers n'est pas utilisé) avant la mise en production.
  2. ❌ Gravité moyenne : conflit de version renvoyé en 500 (voir FRS-01).
  3. ⚠ Aucun test ne vérifie que `created_by` contient bien l'UUID du compte connecté : ajouter `save_byAuthenticatedAdmin_fillsCreatedByWithHisId` dans `SupplierSecurityIT`.
- **Recette manuelle** : 2 étapes 🧪.
