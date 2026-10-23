# Scénarios de test — FRS-01 · Fiche fournisseur complète

Statut : ✔ rempli depuis les tests existants au 2026-10-23 — à valider par le traiteur · Ticket : [#17](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/17) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (accès complet), `SELLER` (refusé) |
| Données de départ | un administrateur ; un ou deux fournisseurs |
| Tickets à avoir déployés avant | JNL-01, AUTH-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Tous les champs sont visibles | Quand l'administrateur ouvre une fiche | nom, type, contact, téléphone, courriel, adresse, notes, état | `SupplierServiceImplTest.createSupplier_newName_savesNormalizedActiveSupplierAndTracesInJournal` ; `SupplierRepositoryIT.save_newSupplier_isPersistedWithAuditFields` | ☐ |
| 1b | idem | Quand il demande un fournisseur inexistant | 404 | `SupplierSecurityIT.getSupplierById_unknownId_returns404` ; `SupplierServiceImplTest.getSupplierById_unknownId_throwsNotFound` | ☐ |
| 2 | Tous les champs sont modifiables | Quand il modifie tous les champs | tous changés, trace dans le journal | `SupplierSecurityIT.updateSupplier_byAdmin_changesEveryField` ; `SupplierServiceImplTest.updateSupplier_existingSupplier_appliesAllFieldsAndTracesInJournal` | ☐ |
| 2b | Création | Quand il crée un fournisseur valide | 201, ligne de journal `CREATE` | `SupplierSecurityIT.createSupplier_byAdmin_returns201AndTracesInJournal` | ☐ |
| 2c | Désactivation (pas de suppression) | Quand il désactive un fournisseur | `active = false`, trace | `SupplierSecurityIT.setActive_byAdmin_deactivatesSupplier` ; `SupplierServiceImplTest.setActive_activeSupplierDeactivated_deactivatesAndTracesInJournal` | ☐ |
| 2d | Désactivation idempotente | Quand l'état est déjà celui demandé | aucune trace | `SupplierServiceImplTest.setActive_stateAlreadyRequested_leavesNoTrace` | ☐ |
| 3 | Recherche et filtres (liste) | Quand il cherche par nom de contact | le fournisseur est trouvé même si le nom ne correspond pas | `SupplierRepositoryIT.search_byContactName_findsSupplierWhoseNameDoesNotMatch` | ☐ |
| 3b | idem | Quand il filtre par type et par état | seuls les fournisseurs correspondants | `SupplierRepositoryIT.search_withTypeAndStatusFilters_returnsOnlyMatchingSuppliers` | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Liste sans jeton | 401 | `SupplierSecurityIT.getSuppliers_withoutToken_returns401` | ☐ |
| Liste avec un autre rôle | 403 | `SupplierSecurityIT.getSuppliers_withNonAdminRole_returns403` | ☐ |
| Création avec un autre rôle | 403 | `SupplierSecurityIT.createSupplier_withNonAdminRole_returns403` | ☐ |
| Nom déjà pris (casse différente) | 409 `CONFLICT` | `SupplierSecurityIT.createSupplier_nameAlreadyUsedWithOtherCase_returns409` ; `SupplierRepositoryIT.existsByNameIgnoreCase_sameNameOtherCase_returnsTrue` ; `SupplierServiceImplTest.createSupplier_nameAlreadyUsed_throwsConflictWithoutSaving` | ☐ |
| Nom pris par un autre fournisseur à la modification | 409 | `SupplierServiceImplTest.updateSupplier_nameUsedByAnotherSupplier_throwsConflict` | ☐ |
| Nom et type manquants | 422 `VALIDATION_FAILED` | `SupplierSecurityIT.createSupplier_missingNameAndType_returns422` | ☐ |
| Mass assignment de `id` et du statut | ignoré | `SupplierSecurityIT.createSupplier_massAssignmentOfIdAndStatus_isIgnored` | ☐ |
| Suppression d'un fournisseur | non exposée | `SupplierSecurityIT.deleteSupplier_isNotExposed` | ☐ |
| Courriel invalide, champs trop longs | 422 | _(à ajouter : aucun test dédié repéré)_ | ☐ |
| Modification concurrente (version) | 409 | _(à ajouter)_ | ☐ |

## Cas limites et hors ligne

- Nom avec espaces autour : nettoyé avant comparaison et enregistrement.
- Pagination `page` base 1, `size` ≤ 100, tri hors liste blanche refusé.
- Écriture hors ligne : non prise en charge pour l'instant (voir SYS-07).

## Tests existants (repérés dans le code)

- Backend : `SupplierSecurityIT`, `SupplierRepositoryIT`, `SupplierServiceImplTest` (`feature/supplier/...`)
- Frontend : `features/supplier/data-access/supplier-service.spec.ts` (pagination envoyée, désactivation par `PATCH /status` sans suppression). Aucun test de page.

## À faire à la main (recette QA)

- Se connecter en administrateur, créer un fournisseur local puis un étranger, modifier chaque champ, vérifier l'affichage dans la liste et la fiche.
- Désactiver puis réactiver : badge mis à jour, deux lignes dans le Journal.
- Se connecter en vendeuse : le menu n'offre pas Fournisseurs et l'URL est refusée.
- Vérifier que le bloc « articles fournis » est présent et vide (attente d'ART-01).

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
