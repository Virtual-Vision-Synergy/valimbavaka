# Scénarios de test — JNL-02 · Interdiction de suppression des opérations validées

Statut : ✔ rempli depuis les tests existants au 2026-10-23 — à valider par le traiteur · Ticket : [#15](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/15) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | sans objet (socle technique, aucun endpoint propre) |
| Données de départ | base migrée (contrainte `ck_journal_entries_action` étendue) |
| Tickets à avoir déployés avant | JNL-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Seules l'annulation à chaud ou la contre-passation sont possibles | Étant donné une opération `VALIDATED` · Quand on demande l'annulation | autorisée | `OperationStatusTest.ensureCanCancel_validatedOperation_passes` | ☐ |
| 1b | idem | Quand on demande la contre-passation | autorisée | `OperationStatusTest.ensureCanReverse_validatedOperation_passes` | ☐ |
| 1c | idem | Étant donné une opération déjà `CANCELLED` ou `REVERSED` · Quand on demande l'annulation | `ConflictException` (409) | `OperationStatusTest.ensureCanCancel_alreadyClosedOperation_throwsConflict` | ☐ |
| 1d | idem | Quand on demande la contre-passation d'une opération déjà close | `ConflictException` (409) | `OperationStatusTest.ensureCanReverse_alreadyClosedOperation_throwsConflict` | ☐ |
| 1e | Le journal trace ces deux issues | Quand une ligne `CANCEL` ou `REVERSE` est insérée | la base l'accepte | `JournalAppendOnlyIT.save_cancellationOrReversal_isAcceptedByTheDatabase` | ☐ |
| 2 | Aucune suppression définitive n'est exposée dans le socle | Quand on analyse le code | aucun `@DeleteMapping` hors `feature.user` | `ArchitectureTest.delete_endpoints_are_limited_to_the_user_reference` | ☐ |
| 2b | idem | Quand un repository de feature métier hérite de `JpaRepository` ou `CrudRepository` | le build échoue | `ArchitectureTest.operation_features_have_no_deleting_repository` | ☐ |
| 2c | idem | Quand une méthode `delete*` ou `remove*` est ajoutée à un `OperationRepository` | le build échoue | `ArchitectureTest.operation_repositories_declare_no_delete` | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Suppression directe d'une ligne de journal | refusée par la base | `JournalAppendOnlyIT.delete_existingEntry_isRejectedByTheDatabase` | ☐ |
| Annulation d'une opération inexistante | 404 `NOT_FOUND` | _(à ajouter dans le premier ticket qui implémente `ReversibleOperationService`)_ | ☐ |
| Annulation sans motif | 422 `VALIDATION_FAILED` | _(à ajouter dans le premier ticket d'opération)_ | ☐ |
| Annulation par un rôle sans droit | 403 `FORBIDDEN` | _(à ajouter dans le premier ticket d'opération)_ | ☐ |
| `DELETE /api/v1/users/{id}` | exception volontaire aux règles | `ArchitectureTest.delete_endpoints_are_limited_to_the_user_reference` (exception codée) ; _(à clarifier avec AUTH-03 / AUTH-04)_ | ☐ |

## Cas limites et hors ligne

- Annulation rejouée depuis la file hors ligne avec la même `Idempotency-Key` : une seule annulation, une seule ligne `CANCEL` dans le journal (voir SYS-02).
- Contre-passation d'une opération déjà annulée : refusée (409) ; annulation d'une opération contre-passée : refusée (409).

## Tests existants (repérés dans le code)

- Backend : `shared/operation/OperationStatusTest.java`, `ArchitectureTest.java`, `feature/journal/repository/JournalAppendOnlyIT.java`
- Frontend : aucun (ticket purement backend)

## À faire à la main (recette QA)

- Ajouter volontairement un `@DeleteMapping` ou une méthode `deleteXxx` dans une copie locale et vérifier que `ArchitectureTest` échoue.
- Une fois un premier ticket d'opération livré (vente, transfert, production) : vérifier annulation, contre-passation, impossibilité de supprimer, et les deux lignes de journal.

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
