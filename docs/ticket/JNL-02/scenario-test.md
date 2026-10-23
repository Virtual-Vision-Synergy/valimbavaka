# Scénarios de test — JNL-02 · Interdiction de suppression des opérations validées

Statut : ✅ exécuté le 2026-10-23 · Ticket : [#15](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/15) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé au premier ticket d'opération. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | sans objet (socle technique, aucun endpoint propre) |
| Données de départ | base migrée (contrainte `ck_journal_entries_action` étendue) |
| Tickets à avoir déployés avant | JNL-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Seules l'annulation à chaud et la contre-passation sont possibles | Opération `VALIDATED` · annulation demandée | autorisée | `OperationStatusTest.ensureCanCancel_validatedOperation_passes` | ✅ auto |
| 1b | idem | Opération `VALIDATED` · contre-passation demandée | autorisée | `OperationStatusTest.ensureCanReverse_validatedOperation_passes` | ✅ auto |
| 1c | idem | Opération `CANCELLED` ou `REVERSED` · annulation | `ConflictException` (409) | `OperationStatusTest.ensureCanCancel_alreadyClosedOperation_throwsConflict` | ✅ auto |
| 1d | idem | Opération close · contre-passation | `ConflictException` (409) | `OperationStatusTest.ensureCanReverse_alreadyClosedOperation_throwsConflict` | ✅ auto |
| 1e | Le journal trace ces deux issues | Insertion d'une ligne `CANCEL` ou `REVERSE` | acceptée ; `PURGE` refusée | `JournalAppendOnlyIT.save_cancellationOrReversal_isAcceptedByTheDatabase` ; insertions vérifiées en base | ✅ auto · ✅ base |
| 2 | Aucune suppression définitive dans le socle | Analyse du code | aucun `@DeleteMapping` dans toute l'application | `ArchitectureTest.no_delete_endpoints` | ✅ auto |
| 2b | idem | Un repository de feature métier hérite de `JpaRepository` ou `CrudRepository` | le build échoue | `ArchitectureTest.operation_features_have_no_deleting_repository` | ✅ auto |
| 2c | idem | Une méthode `delete*` ou `remove*` est ajoutée à un `OperationRepository` | le build échoue | `ArchitectureTest.operation_repositories_declare_no_delete` | ✅ auto |
| 2d | idem (utilisateurs) | `DELETE /api/v1/users/{id}` | 405 | `UserStatusIT.deleteUser_isNotExposed` | ✅ auto (corrigé le 2026-10-23) |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Suppression directe d'une ligne de journal | `DELETE FROM journal_entries` | refusée par la base | `JournalAppendOnlyIT.delete_existingEntry_isRejectedByTheDatabase` | ✅ auto |
| Conflit d'état traduit en HTTP | `ensureCanCancel` sur `CANCELLED` | 409 `CONFLICT` | `ConflictException extends BusinessException`, traduite par `GlobalExceptionHandler.handleBusiness` | 🔍 code |
| Annulation d'une opération inexistante | UUID au hasard | 404 | aucun service d'opération encore livré | ⏭ hors périmètre (premier ticket d'opération) |
| Annulation sans motif | `reason` vide | 422 | idem | ⏭ hors périmètre |
| Annulation par un rôle sans droit | jeton livreur | 403 | idem | ⏭ hors périmètre |

## Cas limites

| Cas | Exemple | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Contre-passer une opération annulée | `CANCELLED` → `reverse` | 409 | `OperationStatus.ensureCanReverse` | ✅ auto |
| Annuler une opération contre-passée | `REVERSED` → `cancel` | 409 | `OperationStatus.ensureCanCancel` | ✅ auto |
| Annulation rejouée hors ligne | même `Idempotency-Key` | une seule annulation, une seule ligne `CANCEL` | mécanisme SYS-02, `IdempotencyIT` | ✅ auto |
| Repository d'opération sans `delete` | `interface SaleRepository extends OperationRepository<Sale, UUID>` | seulement `save` et `findById` hérités | `OperationRepository extends Repository` | 🔍 code |
| Données de référence (utilisateur, fournisseur) | désactivation au lieu de suppression | autorisé, ce ne sont pas des opérations | `operation_features_have_no_deleting_repository` exclut référentiel et authentification | ✅ auto |

## Tests existants

- Backend : `shared/operation/OperationStatusTest.java`, `ArchitectureTest.java`, `feature/journal/repository/JournalAppendOnlyIT.java`
- Frontend : sans objet (ticket backend)

## Recette manuelle

Aucune étape d'interface : ticket purement technique. Contrôle conseillé lors de la revue de chaque ticket d'opération : la PR ne doit contenir ni `@DeleteMapping` ni repository `JpaRepository` dans la feature.

## Résultat de l'exécution

- **Date** : 2026-10-23.
- **Backend** : CI GitHub `mvn verify` (ArchUnit compris) vert sur un code identique à `develop`. **Base** : contrainte d'action vérifiée (`CANCEL` et `REVERSE` acceptées, `PURGE` refusée).
- **Anomalies** :
  1. ✅ Corrigé le 2026-10-23 (branche `fix/AUTH-04-garde-fous-administrateur`) : `DELETE /api/v1/users/{id}` retiré ; la règle ArchUnit `no_delete_endpoints` n'a plus d'exception.
  2. ⚠ Aucune implémentation de `ReversibleOperationService` n'existe : les cas 404, 422 et 403 seront testés par le premier ticket d'opération (VTE-01, TRF-03 ou PRD-01).
  3. ⚠ Le commentaire du ticket cite la migration `2026_10_20-01` ; le fichier réel est `2026_10_20-02`.
