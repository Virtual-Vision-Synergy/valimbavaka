# Scénarios de test — SYS-06 · Audit : auteur des opérations en UUID

Statut : ✔ rempli depuis le code et les tests au 2026-10-23 — à valider par le traiteur · Ticket : [#112](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/112) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, Testcontainers PostgreSQL (jamais H2).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (pour créer une ressource authentifiée) |
| Données de départ | un administrateur, base migrée |
| Tickets à avoir déployés avant | SYS-05 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | `created_by` / `updated_by` en UUID | Quand les migrations s'appliquent | colonnes de type UUID sur les tables auditées | `SupplierRepositoryIT.save_newSupplier_isPersistedWithAuditFields` (écriture sur une table créée avec ces colonnes) ; type vérifié à la main (recette) | ☐ |
| 2 | Auteur renseigné automatiquement à partir de l'utilisateur connecté | Quand l'administrateur crée une ressource | `created_by` et `updated_by` = son UUID | _(à ajouter : aucun test d'intégration dédié repéré)_ ; indirectement `JournalServiceImplTest.record_authenticatedUser_savesTimestampedEntryInTheNameOfTheUser` (lit le même `AuditorAware<UUID>`) | ☐ |
| 2b | Pas de saisie manuelle | Quand un DTO d'entrée contient `createdBy` ou `id` | ignoré | `SupplierSecurityIT.createSupplier_massAssignmentOfIdAndStatus_isIgnored` | ☐ |
| 2c | Modification par un autre compte | Quand un autre compte modifie la ressource | `updated_by` change, `created_by` reste | _(à ajouter)_ | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Écriture hors requête authentifiée (démarrage) | auteur `NULL`, pas d'erreur | `AdminAccountInitializerIT.run_atStartupWithoutAdministrator_createsAdministratorRequiringPasswordChange` (création sans compte connecté) | ☐ |
| Jeton sans `sub` UUID valide | auteur vide | _(à ajouter : test unitaire de `SecurityAuditorAware`)_ | ☐ |
| Modification concurrente (version) | 409 `CONFLICT` | _(à ajouter)_ | ☐ |

## Cas limites et hors ligne

- Écriture rejouée depuis la file hors ligne (SYS-02) : l'auteur est le compte du jeton au moment du rejeu, pas celui de la saisie initiale (même compte, car la file est rattachée à son propriétaire).
- Un seul bean `AuditorAware<UUID>` doit exister.

## Tests existants (repérés dans le code)

- Pas de classe de test dédiée à `BaseEntity` ni à `SecurityAuditorAware`. Couverture indirecte : `SupplierRepositoryIT`, `AdminAccountInitializerIT`, `JournalServiceImplTest`, `UserServiceImplTest` (mock de `AuditorAware<UUID>`).
- Frontend : sans objet.

## À faire à la main (recette QA)

- Se connecter en administrateur, créer un fournisseur, puis en base : `SELECT created_by, updated_by FROM suppliers` donne l'UUID de l'administrateur (`SELECT id FROM users WHERE email = …`).
- `\d suppliers` et `\d users` : `created_by` et `updated_by` sont de type `uuid`.
- Modifier la ressource avec un second compte administrateur : `updated_by` change, `created_by` non.
- Les anciennes lignes migrées ont `created_by` à `NULL`.

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
