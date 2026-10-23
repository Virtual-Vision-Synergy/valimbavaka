# Scénarios de test — JNL-01 · Journal append-only des opérations

Statut : ✔ rempli depuis les tests existants au 2026-10-23 — à valider par le traiteur · Ticket : [#14](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/14) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (lecture), `SELLER` (refusé) |
| Données de départ | un administrateur ; au moins une opération métier faite (ex. création d'un utilisateur) |
| Tickets à avoir déployés avant | AUTH-01, SYS-06 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Chaque opération génère une ligne horodatée et nominative | Quand l'administrateur crée un utilisateur | une ligne `CREATE` avec son nom, l'horodatage et la ressource | `JournalSecurityIT.createUser_byAdmin_leavesANominativeTimestampedLineInTheJournal` | ☐ |
| 1b | idem (service) | Quand un utilisateur authentifié appelle `record` | ligne horodatée à son nom | `JournalServiceImplTest.record_authenticatedUser_savesTimestampedEntryInTheNameOfTheUser` | ☐ |
| 1c | idem (sans compte) | Quand `record` est appelé hors requête authentifiée | auteur « Système », `actorId` vide | `JournalServiceImplTest.record_noAuthenticatedUser_attributesTheEntryToTheSystem` | ☐ |
| 1d | idem (compte disparu) | Quand le compte auteur n'existe plus | identifiant conservé, nom signalé comme inconnu | `JournalServiceImplTest.record_authorAccountNoLongerExists_keepsTheIdAndFlagsTheNameAsUnknown` | ☐ |
| 2 | Brique commune appelable par tous les modules | Quand un module appelle `JournalService.record` | ligne écrite dans la transaction de l'appelant | `JournalServiceImplTest` ; utilisations dans `UserServiceImplTest`, `SupplierServiceImplTest` | ☐ |
| 3 | Append-only (titre du ticket) | Quand on tente un `UPDATE` sur une ligne | la base refuse | `JournalAppendOnlyIT.update_existingEntry_isRejectedByTheDatabase` | ☐ |
| 3b | idem | Quand on tente un `DELETE` | la base refuse | `JournalAppendOnlyIT.delete_existingEntry_isRejectedByTheDatabase` | ☐ |
| 3c | idem | Quand on tente un `TRUNCATE` | la base refuse | `JournalAppendOnlyIT.truncate_journal_isRejectedByTheDatabase` | ☐ |
| 3d | idem | Quand on insère une ligne | enregistrée avec son identifiant généré | `JournalAppendOnlyIT.save_newEntry_isPersistedWithItsGeneratedId` ; actions `CANCEL` / `REVERSE` acceptées : `save_cancellationOrReversal_isAcceptedByTheDatabase` | ☐ |
| 4 | L'administrateur consulte le journal | Quand il ouvre la page Journal | qui a fait quoi, action traduite | `journal-list-page.spec.ts` « should show who did what, with a translated action » | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Lecture sans jeton | 401 `UNAUTHENTICATED` | `JournalSecurityIT.getEntries_withoutToken_returns401` | ☐ |
| Lecture avec un autre rôle que l'administrateur | 403 `FORBIDDEN` | `JournalSecurityIT.getEntries_withNonAdminRole_returns403` | ☐ |
| Verbes d'écriture sur `/api/v1/journal` | non exposés | `JournalSecurityIT.journal_writeVerbs_areNotExposed` | ☐ |
| Aucune action de modification ou suppression dans l'interface | boutons absents | `journal-list-page.spec.ts` « should offer no way to edit or delete an entry » | ☐ |
| Journal vide | message dédié | `journal-list-page.spec.ts` « should tell the administrator when the journal is empty » | ☐ |
| Erreur de lecture | erreur affichée | `journal-list-page.spec.ts` « should display the error when the journal cannot be read » | ☐ |
| Échec d'écriture du journal | l'opération métier est annulée (même transaction) | _(à ajouter : aucun test dédié repéré)_ | ☐ |

## Cas limites et hors ligne

- Pagination : `page` base 1, `size` ≤ 100 ; recherche vide, recherche avec un terme présent dans le libellé, dans l'auteur, dans le type de ressource ; tri par défaut `occurredAt` décroissant. Tests de l'interface : `journal-list-page.spec.ts` « should search from the first page with the typed term », « should request the next page » ; `journal-service.spec.ts`.
- Libellé de plus de 500 caractères : à tester (la colonne est limitée à 500).
- Écritures rejouées hors ligne : la ligne de journal ne doit exister qu'une fois (voir SYS-02, `IdempotencyIT`).

## Tests existants (repérés dans le code)

- Backend : `feature/journal/controller/JournalSecurityIT.java`, `feature/journal/repository/JournalAppendOnlyIT.java`, `feature/journal/service/impl/JournalServiceImplTest.java`
- Frontend : `features/journal/data-access/journal-service.spec.ts`, `features/journal/pages/journal-list-page/journal-list-page.spec.ts`

## À faire à la main (recette QA)

- Créer puis désactiver un utilisateur, ouvrir le Journal : deux lignes nominatives et horodatées.
- Rechercher par le nom de l'auteur, puis par un mot du libellé.
- Se connecter en vendeuse : le menu n'offre pas le Journal et l'URL `/journal` renvoie à l'accueil.
- Directement en base (environnement de test) : un `UPDATE` ou `DELETE` sur `journal_entries` doit lever « journal_entries est append-only ».

## Résultat de l'exécution

- Date : _(à compléter)_
- Environnement : _(à compléter)_
- Anomalies ouvertes : _(à compléter)_
