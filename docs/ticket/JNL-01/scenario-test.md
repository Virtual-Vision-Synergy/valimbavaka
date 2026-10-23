# Scénarios de test — JNL-01 · Journal append-only des opérations

Statut : ✅ exécuté le 2026-10-23 — reste la recette manuelle (🧪) · Ticket : [#14](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/14) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · 🧪 recette = à faire à la main. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (lecture), `SELLER` (refusé) |
| Données de départ | un administrateur ; au moins une opération métier faite (ex. création d'un utilisateur) |
| Tickets à avoir déployés avant | AUTH-01, SYS-06 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Chaque opération génère une ligne horodatée et nominative | Quand l'administrateur crée un utilisateur | ligne `CREATE` à son nom, horodatée, avec la ressource | `JournalSecurityIT.createUser_byAdmin_leavesANominativeTimestampedLineInTheJournal` | ✅ auto |
| 1b | idem (service) | Quand un utilisateur authentifié appelle `record` | ligne horodatée à son nom | `JournalServiceImplTest.record_authenticatedUser_savesTimestampedEntryInTheNameOfTheUser` | ✅ auto |
| 1c | idem (sans compte) | Quand `record` est appelé hors requête authentifiée | auteur « Système », `actor_id` vide | `JournalServiceImplTest.record_noAuthenticatedUser_attributesTheEntryToTheSystem` | ✅ auto |
| 1d | idem (compte disparu) | Quand le compte auteur n'existe plus | identifiant conservé, nom « Compte inconnu » | `JournalServiceImplTest.record_authorAccountNoLongerExists_keepsTheIdAndFlagsTheNameAsUnknown` | ✅ auto |
| 1e | idem (modules livrés) | Quand on crée, modifie, active ou désactive un utilisateur ou un fournisseur | une ligne par opération | `UserServiceImplTest.createUser_validRequest_recordsCreationInJournal`, `setActive_deactivateActiveSeller_deactivatesAndRecordsInJournal`, `updateUser_administratorDemotedWhileAnotherIsActive_updatesAndRecordsInJournal` ; `SupplierServiceImplTest.createSupplier_newName_savesNormalizedActiveSupplierAndTracesInJournal`, `updateSupplier_existingSupplier_appliesAllFieldsAndTracesInJournal` | ✅ auto |
| 2 | Brique commune | Quand un module appelle `JournalService.record` | ligne écrite dans la transaction de l'appelant (`@Transactional`, propagation par défaut) | `JournalServiceImpl.record` | 🔍 code |
| 3 | Append-only : `UPDATE` | Quand on modifie une ligne | refus de la base | `JournalAppendOnlyIT.update_existingEntry_isRejectedByTheDatabase` ; en base : « journal_entries est append-only : UPDATE interdit » | ✅ auto · ✅ base |
| 3b | Append-only : `DELETE` | Quand on supprime une ligne | refus | `JournalAppendOnlyIT.delete_existingEntry_isRejectedByTheDatabase` ; en base : « DELETE interdit » | ✅ auto · ✅ base |
| 3c | Append-only : `TRUNCATE` | Quand on vide la table | refus | `JournalAppendOnlyIT.truncate_journal_isRejectedByTheDatabase` ; en base : « TRUNCATE interdit » | ✅ auto · ✅ base |
| 3d | Insertion | Quand on insère une ligne valide | enregistrée avec son identifiant | `JournalAppendOnlyIT.save_newEntry_isPersistedWithItsGeneratedId` | ✅ auto |
| 4 | Consultation par l'administrateur | Quand il ouvre la page Journal | qui a fait quoi, action traduite | `journal-list-page.spec.ts` « should show who did what, with a translated action » | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Lecture sans jeton | `GET /api/v1/journal` | 401 | `JournalSecurityIT.getEntries_withoutToken_returns401` | ✅ auto |
| Lecture avec un autre rôle | jeton vendeuse | 403 | `JournalSecurityIT.getEntries_withNonAdminRole_returns403` | ✅ auto |
| Verbes d'écriture sur `/api/v1/journal` | `POST`, `PUT`, `PATCH`, `DELETE` | non exposés | `JournalSecurityIT.journal_writeVerbs_areNotExposed` | ✅ auto |
| Modification ou suppression depuis l'interface | — | aucun bouton | `journal-list-page.spec.ts` « should offer no way to edit or delete an entry » | ✅ auto |
| Journal vide | base neuve | message dédié | `journal-list-page.spec.ts` « should tell the administrator when the journal is empty » | ✅ auto |
| Erreur de lecture | serveur en 500 | erreur affichée | `journal-list-page.spec.ts` « should display the error when the journal cannot be read » | ✅ auto |
| Échec d'écriture du journal | contrainte violée pendant `record` | l'opération métier est annulée (même transaction) ; réponse 500 `INTERNAL_ERROR` | `record` rejoint la transaction de l'appelant ; `GlobalExceptionHandler` traduit l'exception en 500 | 🔍 code |
| Action inconnue | `PURGE` | refus de la base (`ck_journal_entries_action`) | insertion refusée en base | ✅ base |

## Cas limites

| Cas | Exemple | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Libellé de 500 caractères | `description = 'a' × 500` | accepté | colonne `VARCHAR(500)` | ✅ base |
| Libellé de 501 caractères | `'a' × 501` | doit être tronqué ou refusé proprement | refus de la base « value too long » ; `record` ne tronque pas, l'opération métier échoue en 500 | ❌ écart |
| Nom d'auteur long | prénom + nom de 200 caractères | accepté (`actor_name` ≤ 255 ; prénom et nom ≤ 100 chacun) | insertion en base | ✅ base |
| Nom de l'auteur modifié après coup | renommer l'administrateur | les anciennes lignes gardent l'ancien nom | `actor_name` figé à l'écriture | 🔍 code |
| Pagination | `page=1&size=100` ; `size=101` | 100 lignes ; 422 au-delà | `PageQuery` ; `journal-list-page.spec.ts` « should request the next page » | ✅ auto |
| Recherche | `search=Rakoto` (auteur), `search=fournisseur` (libellé), `search=SUPPLIER` (type) | lignes correspondantes, insensible à la casse | `journal-list-page.spec.ts` « should search from the first page with the typed term » ; requête `search` du repository | ✅ auto · 🔍 code |
| Tri | sans `sortBy` | `occurredAt` décroissant | `SORT_FIELDS` + défaut du service | 🔍 code |
| Rejeu hors ligne | même `Idempotency-Key` | une seule ligne | `IdempotencyIT.createUser_replayedWithSameKey_returnsSameResponseWithoutDuplicate` | ✅ auto |

## Tests existants

- Backend : `feature/journal/controller/JournalSecurityIT.java`, `feature/journal/repository/JournalAppendOnlyIT.java`, `feature/journal/service/impl/JournalServiceImplTest.java`
- Frontend : `features/journal/data-access/journal-service.spec.ts`, `features/journal/pages/journal-list-page/journal-list-page.spec.ts`

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Créer puis désactiver un utilisateur, ouvrir le Journal | deux lignes nominatives et horodatées | 🧪 recette |
| Rechercher par nom d'auteur puis par un mot du libellé | lignes filtrées | 🧪 recette |
| Se connecter en vendeuse et ouvrir `/journal` | pas de menu Journal, retour à l'accueil | 🧪 recette |

## Résultat de l'exécution

- **Date** : 2026-10-23.
- **Backend** : CI GitHub `mvn verify` vert sur un code identique à `develop`. **Frontend** : `ng test` local, 102 tests verts sur 102. **Base** : migrations `2026_10_19-02` et `2026_10_20-02` rejouées sur PostgreSQL 16 ; triggers `UPDATE`, `DELETE`, `TRUNCATE` et contrainte d'action vérifiés à la main.
- **Anomalies** :
  1. ❌ Gravité moyenne : un libellé de plus de 500 caractères fait échouer toute l'opération métier en 500. Tronquer `description` à 500 dans `JournalEntry.of` (avec « … ») et ajouter `record_descriptionLongerThan500_isTruncated`.
  2. ⚠ Le critère « chaque opération génère une ligne » repose sur l'appel de `record` par chaque service : aucune règle ArchUnit ne le vérifie.
  3. ⚠ Pas de filtre par ressource, auteur ou période sur `GET /api/v1/journal` (nécessaire pour ART-06 et LIV-07).
- **Recette manuelle** : 3 étapes 🧪.
