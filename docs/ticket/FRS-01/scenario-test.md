# Scénarios de test — FRS-01 · Fiche fournisseur complète

Statut : ✅ exécuté le 2026-10-23 — reste la recette manuelle (🧪) · Ticket : [#17](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/17) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (accès complet), `SELLER` (refusé) |
| Données de départ | un administrateur ; fournisseurs d'exemple « Huiles Ravo » (`LOCAL`) et « Provence Essences » (`FOREIGN`) |
| Tickets à avoir déployés avant | JNL-01, AUTH-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Tous les champs sont visibles | Quand l'administrateur ouvre une fiche | nom, type, contact, téléphone, courriel, adresse, notes, état, dates | `SupplierRepositoryIT.save_newSupplier_isPersistedWithAuditFields` ; `SupplierRDto` expose les 11 champs ; formulaire `supplier-form-page.html` avec les 7 champs saisissables | ✅ auto · 🔍 code |
| 1b | idem | Fournisseur inexistant | 404 | `SupplierSecurityIT.getSupplierById_unknownId_returns404` ; `SupplierServiceImplTest.getSupplierById_unknownId_throwsNotFound` | ✅ auto |
| 1c | Bloc « articles fournis » | Quand la fiche s'affiche | bloc présent, vide | section `articles` du formulaire ; contenu apporté par ART-01 | ⏭ hors périmètre (ART-01) |
| 2 | Tous les champs sont modifiables | Quand il modifie les 7 champs | tous changés, ligne `UPDATE` | `SupplierSecurityIT.updateSupplier_byAdmin_changesEveryField` ; `SupplierServiceImplTest.updateSupplier_existingSupplier_appliesAllFieldsAndTracesInJournal` | ✅ auto |
| 2b | Création | Fournisseur valide | 201 + `Location`, ligne `CREATE` | `SupplierSecurityIT.createSupplier_byAdmin_returns201AndTracesInJournal` ; `SupplierServiceImplTest.createSupplier_newName_savesNormalizedActiveSupplierAndTracesInJournal` | ✅ auto |
| 2c | Désactivation au lieu de suppression | Il désactive « Huiles Ravo » | `active = false`, ligne `UPDATE` | `SupplierSecurityIT.setActive_byAdmin_deactivatesSupplier` ; `SupplierServiceImplTest.setActive_activeSupplierDeactivated_deactivatesAndTracesInJournal` ; `supplier-service.spec.ts` « désactive un fournisseur via PATCH /status, sans suppression » | ✅ auto |
| 2d | idem (idempotent) | État déjà demandé | aucune trace | `SupplierServiceImplTest.setActive_stateAlreadyRequested_leavesNoTrace` | ✅ auto |
| 3 | Recherche et filtres | `search=Rasoa` (nom du contact) | trouvé même si le nom du fournisseur ne correspond pas | `SupplierRepositoryIT.search_byContactName_findsSupplierWhoseNameDoesNotMatch` | ✅ auto |
| 3b | idem | `type=LOCAL&active=true` | seuls les correspondants | `SupplierRepositoryIT.search_withTypeAndStatusFilters_returnsOnlyMatchingSuppliers` ; `supplier-service.spec.ts` « envoie la pagination de la liste » | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Liste sans jeton | — | 401 | `SupplierSecurityIT.getSuppliers_withoutToken_returns401` | ✅ auto |
| Liste avec un autre rôle | jeton vendeuse | 403 | `SupplierSecurityIT.getSuppliers_withNonAdminRole_returns403` | ✅ auto |
| Création avec un autre rôle | jeton vendeuse | 403 | `SupplierSecurityIT.createSupplier_withNonAdminRole_returns403` | ✅ auto |
| Nom déjà pris, autre casse | « HUILES RAVO » | 409 | `SupplierSecurityIT.createSupplier_nameAlreadyUsedWithOtherCase_returns409` ; `SupplierRepositoryIT.existsByNameIgnoreCase_sameNameOtherCase_returnsTrue` ; `SupplierServiceImplTest.createSupplier_nameAlreadyUsed_throwsConflictWithoutSaving` ; index `uq_suppliers_name` vérifié en base | ✅ auto · ✅ base |
| Nom pris par un autre à la modification | — | 409 | `SupplierServiceImplTest.updateSupplier_nameUsedByAnotherSupplier_throwsConflict` | ✅ auto |
| Nom et type manquants | `{}` | 422 | `SupplierSecurityIT.createSupplier_missingNameAndType_returns422` | ✅ auto |
| Type inconnu | `"type":"IMPORT"` | 400 (énumération illisible) ; refus de la base `ck_suppliers_type` | désérialisation de `SupplierType` ; contrainte vérifiée en base | 🔍 code · ✅ base |
| Mass assignment | `{"id":"…","active":false,…}` | ignorés | `SupplierSecurityIT.createSupplier_massAssignmentOfIdAndStatus_isIgnored` | ✅ auto |
| Suppression | `DELETE /api/v1/suppliers/{id}` | non exposée | `SupplierSecurityIT.deleteSupplier_isNotExposed` | ✅ auto |
| Courriel invalide, champ trop long | `email=abc` ; nom de 151 caractères | 422 | `@Email`, `@Size` de `SupplierWDto` | 🔍 code |

## Cas limites

| Cas | Exemple d'entrée | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Espaces autour du nom | `"  Huiles Ravo  "` | enregistré « Huiles Ravo », doublon détecté | `Supplier` applique `strip` ; le service compare le nom nettoyé. La base seule accepterait « ␣Huiles Ravo␣ » (index sur `lower(name)` sans `trim`) | 🔍 code · ✅ base |
| Limites de longueur | nom 150, téléphone 50, courriel 255, adresse 500, notes 2000 | acceptés ; +1 caractère → 422 | `SupplierWDto` + colonnes de la table | 🔍 code · ✅ base |
| Champs facultatifs vides | seulement nom + type | accepté, autres champs `null` | colonnes nullables | ✅ base |
| Deux administrateurs modifient la même fiche | A et B ouvrent la fiche (version 3), A enregistre, puis B | B reçoit 409 « The supplier was modified in the meantime… » et recharge | `SupplierServiceImplTest.updateSupplier_staleVersion_throwsConflictAndChangesNothing` ; `SupplierSecurityIT.updateSupplier_staleVersion_returns409AndKeepsTheSheet` ; `supplier-form-page.spec.ts` « should send back the version read so that a concurrent change is refused », « should show the server message when the sheet was modified in the meantime » | ✅ auto (corrigé le 2026-10-23) |
| Deux créations simultanées du même nom | course entre 2 requêtes | le second reçoit 409 `CONFLICT` | l'index `uq_suppliers_name` refuse le second ; `GlobalExceptionHandler` traduit le SQLSTATE `23505` en 409 : `GlobalExceptionHandlerTest.handleDataIntegrity_uniqueViolation_returns409` | ✅ auto (corrigé le 2026-10-23) |
| Pagination | `size=101` | 422 | `PageQuery` (`@MaxPageSize`) | 🔍 code |
| Tri hors liste blanche | `sortBy=email` | ignoré, retour au tri par défaut `createdAt` décroissant (pas d'erreur) | `PageQuery.toPageable` ; `SORT_FIELDS` = `createdAt`, `name`, `type` | 🔍 code |
| Écriture hors ligne | réseau coupé | non prise en charge (`HttpClient` direct) | `supplier-service.ts` | ⏭ hors périmètre (SYS-07) |

## Tests existants

- Backend : `feature/supplier/controller/SupplierSecurityIT.java`, `feature/supplier/repository/SupplierRepositoryIT.java`, `feature/supplier/service/impl/SupplierServiceImplTest.java`
- Frontend : `features/supplier/data-access/supplier-service.spec.ts` (aucun test des pages liste et formulaire)

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Créer un fournisseur local et un étranger, modifier chaque champ | valeurs visibles dans la liste et la fiche | 🧪 recette |
| Désactiver puis réactiver | badge mis à jour, deux lignes dans le Journal | 🧪 recette |
| Se connecter en vendeuse | pas de menu Fournisseurs, URL refusée | 🧪 recette |
| Ouvrir une fiche | bloc « articles fournis » présent et vide | 🧪 recette |

## Résultat de l'exécution

- **Date** : 2026-10-23.
- **Backend** : CI GitHub `mvn verify` vert sur un code identique à `develop`. **Frontend** : `ng test` local, 102 tests verts sur 102. **Base** : migration `2026_10_21-01` rejouée ; unicité insensible à la casse et contrainte de type vérifiées.
- **Anomalies** :
  1. ✅ Corrigé le 2026-10-23 (branche `fix/FRS-01-conflits-409`) : verrou optimiste et doublon en base renvoyés en 409 ; `SupplierRDto` expose `version`, que le formulaire renvoie, et une version périmée est refusée.
  2. ✅ La réponse « 409 version » documentée sur le `PUT` est désormais réellement produite.
  3. ⚠ Messages serveur et libellés du journal en anglais (« Supplier created: … ») contrairement aux autres modules.
  4. ⚠ Aucun test des pages fournisseur.
- **Recette manuelle** : 4 étapes 🧪.
