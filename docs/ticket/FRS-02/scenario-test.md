# Scénarios de test — FRS-02 · Création / modification d'un fournisseur

Statut : ✅ exécuté le 2026-10-26 — reste la recette manuelle (🧪) · Ticket : [#18](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/18) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Point de départ : FRS-01 a livré l'API (`POST`, `PUT`, `PATCH /status`) et le formulaire. FRS-02 vérifie que **le formulaire enregistre correctement** et corrige ce qui ne l'était pas : une modification sans changement écrivait une ligne `UPDATE` dans le journal, un nom fait d'espaces partait au serveur, un nom trop long affichait « Le nom est obligatoire ». Les tests de FRS-01 restent valables et ne sont pas recopiés ici, sauf ceux qui portent directement sur le critère.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (crée et modifie), `DELIVERY_DRIVER` (refusé) |
| Données de départ | un administrateur ; fiche « Huiles Ravo » (`LOCAL`, contact « Rasoa », version 3) |
| Tickets à avoir déployés avant | FRS-01, JNL-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Le formulaire enregistre correctement les modifications | Étant donné la fiche « Huiles Ravo » ouverte, quand l'administrateur ouvre `/suppliers/:id/edit` | les champs sont pré-remplis avec la fiche lue (nom, type, contact ; champs vides pour les valeurs `null`) | `supplier-form-page.spec.ts` « should fill the form with the sheet read from the server » | ✅ auto |
| 1b | idem | Quand il change les 7 champs (« Import Océan », `FOREIGN`, « Mr Lee », `+33 1 00 00 00 00`, `lee@ocean.com`, « Marseille », « Livraison le mardi ») et enregistre | `PUT` avec les 7 valeurs et `version: 3`, puis retour à `/suppliers` | « should save every edited field with PUT then go back to the list » | ✅ auto |
| 1c | idem, côté serveur | `PUT` avec les 7 champs | 200, les 7 champs renvoyés modifiés | `SupplierSecurityIT.updateSupplier_byAdmin_changesEveryField` ; `SupplierServiceImplTest.updateSupplier_existingSupplier_appliesAllFieldsAndTracesInJournal` | ✅ auto |
| 1d | idem, traçabilité | `PUT` avec la version lue et `notes` changées | 200 et une ligne `UPDATE` sur ce fournisseur, en plus de la ligne `CREATE` | `SupplierSecurityIT.updateSupplier_withVersionRead_savesAndTracesUpdateInJournal` | ✅ auto |
| 1e | idem, rien de changé | Il enregistre sans rien modifier (ou en ajoutant seulement des espaces : `"  Huiles Ravo "`, contact `" "`) | 200, fiche inchangée, **aucune** nouvelle ligne de journal (règle JNL-01) | `SupplierSecurityIT.updateSupplier_withoutChange_leavesNoNewJournalLine` ; `SupplierServiceImplTest.updateSupplier_sameValuesWithOtherSpacing_leavesNoTrace` | ✅ auto (corrigé) |
| 1f | Création | Formulaire vide, nom `"  Coopérative Itasy "`, type `FOREIGN` | `POST` sans version (`version: null`), puis retour à la liste ; le serveur enregistre « Coopérative Itasy » | « should create with POST, without version, then go back to the list » ; `SupplierServiceImplTest.createSupplier_newName_savesNormalizedActiveSupplierAndTracesInJournal` | ✅ auto |
| 1g | Création tracée | `POST` valide | 201 + `Location`, ligne `CREATE` | `SupplierSecurityIT.createSupplier_byAdmin_returns201AndTracesInJournal` | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Modification sans jeton | `PUT /api/v1/suppliers/{id}` sans `Authorization` | 401 `UNAUTHENTICATED` | `SupplierSecurityIT.updateSupplier_withoutToken_returns401` | ✅ auto |
| Modification avec un autre rôle | jeton livreur | 403 `FORBIDDEN`, fiche inchangée | `SupplierSecurityIT.updateSupplier_withNonAdminRole_returns403AndKeepsTheSheet` | ✅ auto |
| Création avec un autre rôle | jeton livreur | 403 `FORBIDDEN` | `SupplierSecurityIT.createSupplier_withNonAdminRole_returns403` | ✅ auto |
| Mass assignment à la modification | `{"id":"<autre uuid>","active":false,…}` | `id` et `active` ignorés : même identifiant, fiche toujours active | `SupplierSecurityIT.updateSupplier_massAssignmentOfIdAndStatus_isIgnored` | ✅ auto |
| Mass assignment à la création | idem sur `POST` | ignorés | `SupplierSecurityIT.createSupplier_massAssignmentOfIdAndStatus_isIgnored` | ✅ auto |
| Données invalides à la modification | nom de 151 caractères, `email: "abc"` | 422 `VALIDATION_FAILED`, fiche inchangée | `SupplierSecurityIT.updateSupplier_tooLongNameAndInvalidEmail_returns422AndKeepsTheSheet` | ✅ auto |
| Données invalides à la création | `{"email":"not-an-email"}` (ni nom ni type) | 422 `VALIDATION_FAILED` | `SupplierSecurityIT.createSupplier_missingNameAndType_returns422` | ✅ auto |
| Fiche inconnue | `PUT` sur un UUID aléatoire | 404 `NOT_FOUND` | `SupplierSecurityIT.updateSupplier_unknownId_returns404` | ✅ auto |
| Fiche inconnue dans le formulaire | `/suppliers/<uuid inconnu>/edit` | message du serveur affiché (« Fournisseur introuvable : … ») | « should show the server message » (bloc « when the sheet does not exist ») | ✅ auto |
| Nom d'un autre fournisseur, autre casse | renommer en « HUILES RAVO » alors que « Huiles Ravo » existe | 409 `CONFLICT` | `SupplierSecurityIT.updateSupplier_nameOfAnotherSupplierWithOtherCase_returns409` ; `SupplierServiceImplTest.updateSupplier_nameUsedByAnotherSupplier_throwsConflict` | ✅ auto |
| Nom déjà pris à la création, dans le formulaire | « HUILES RAVO » | 409, message du serveur affiché, pas de navigation | « should show the server message when the name is already used » ; `SupplierSecurityIT.createSupplier_nameAlreadyUsedWithOtherCase_returns409` | ✅ auto |
| Journal | création / modification réelle | `CREATE` / `UPDATE`, ressource `SUPPLIER` | lignes 1d et 1g | ✅ auto |

## Cas limites

| Cas | Exemple d'entrée | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Nom vide | `""` en création | rien n'est envoyé, message « Le nom est obligatoire » | « should send nothing when the name is empty » | ✅ auto |
| Nom fait d'espaces | `"   "` | rien n'est envoyé, message « Le nom est obligatoire » (avant : envoyé puis 422 en anglais) | « should send nothing and ask for a name when the name is only spaces » | ✅ auto (corrigé) |
| Nom à la borne | 150 caractères | envoyé | « should accept a name of exactly 150 characters » | ✅ auto |
| Nom borne + 1 | 151 caractères | rien n'est envoyé, message « 150 caractères maximum » (avant : « Le nom est obligatoire ») ; côté serveur 422 | « should send nothing and show the length message when the name exceeds 150 characters » ; `updateSupplier_tooLongNameAndInvalidEmail_returns422AndKeepsTheSheet` | ✅ auto (corrigé) |
| Courriel invalide | `abc` | rien n'est envoyé, message « L'email n'est pas valide » | « should send nothing when the email is invalid » | ✅ auto |
| Autres longueurs, borne + 1 | contact 151, téléphone 51, courriel 256, adresse 501, notes 2001 | rien n'est envoyé, message « N caractères maximum » sous le champ (avant : blocage sans message) ; côté serveur 422 | « should send nothing and explain the limit when %s exceeds %i characters » (5 cas) | ✅ auto (corrigé) |
| Autres longueurs, borne | contact 150, téléphone 50, adresse 500, notes 2000, courriel 254 | envoyés. Le courriel est plafonné à 254 par Angular (RFC 5321) alors que le serveur accepte 255 : écart sans conséquence pratique | « should accept every field at its maximum length (email 254, the Angular cap) » | ✅ auto |
| Messages du serveur en français | renommer en « HUILES RAVO » ; modification tracée | `detail` = « Un fournisseur avec ce nom existe déjà. » ; journal « Modification du fournisseur … » (avant : anglais) | `SupplierSecurityIT.updateSupplier_nameOfAnotherSupplierWithOtherCase_returns409` ; `SupplierSecurityIT.updateSupplier_withVersionRead_savesAndTracesUpdateInJournal` ; libellés vérifiés dans `SupplierServiceImplTest` | ✅ auto (corrigé) |
| Champs facultatifs vidés | contact `""`, courriel `""` | enregistrés `null` (pas de chaîne vide en base) | `SupplierServiceImplTest.createSupplier_newName_savesNormalizedActiveSupplierAndTracesInJournal` (contact `" "` → `null`) | ✅ auto |
| Espaces autour, casse du courriel | `"  Huiles Ravo  "`, `Contact@Itasy.MG` | « Huiles Ravo », `contact@itasy.mg` | `createSupplier_newName_savesNormalizedActiveSupplierAndTracesInJournal` | ✅ auto |
| Double clic sur « Enregistrer » | deux clics avant la réponse | une seule requête ; bouton désactivé pendant l'envoi, réactivé ensuite | « should disable the save button while saving so that a double click sends one request » | ✅ auto |
| Deux administrateurs sur la même fiche | A et B ouvrent la version 3, A enregistre, puis B | B reçoit 409 « Le fournisseur a été modifié entre-temps… », reste sur le formulaire avec sa saisie | « should send back the version read so that a concurrent change is refused », « should show the server message when the sheet was modified in the meantime » ; `SupplierSecurityIT.updateSupplier_staleVersion_returns409AndKeepsTheSheet` | ✅ auto |
| Session expirée pendant la saisie | jeton expiré au clic | l'intercepteur d'authentification rafraîchit le jeton puis rejoue (AUTH-02) | intercepteur commun, non spécifique au fournisseur | ⏭ hors périmètre (AUTH-02) |
| Saisie hors ligne | réseau coupé au clic | non pris en charge : erreur affichée, rien en file | `supplier-service.ts` appelle `HttpClient` directement | ⏭ hors périmètre ([SYS-07](../SYS-07/description.md)) |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/supplier/controller/SupplierSecurityIT.java` (7 tests ajoutés pour le `PUT`), `…/service/impl/SupplierServiceImplTest.java` (1 test ajouté), `…/repository/SupplierRepositoryIT.java` (inchangé).
- Frontend : `valimbavaka-ui/src/app/features/supplier/pages/supplier-form-page/supplier-form-page.spec.ts` (19 tests, dont 17 nouveaux), `…/data-access/supplier-service.spec.ts` (inchangé).

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Créer « Coopérative Itasy » (local), puis la modifier : changer chaque champ, enregistrer | retour à la liste, valeurs à jour dans la liste et à la réouverture de la fiche | 🧪 recette |
| Rouvrir la fiche et enregistrer sans rien changer | Journal : aucune nouvelle ligne `UPDATE` | 🧪 recette |
| Ouvrir la même fiche dans deux onglets, enregistrer dans le premier puis dans le second | le second affiche le message de conflit et garde la saisie | 🧪 recette |
| Saisir 51 caractères dans le téléphone | message « 50 caractères maximum » sous le champ | 🧪 recette |
| Saisir un nom de 151 caractères, puis seulement des espaces | messages « 150 caractères maximum » puis « obligatoire », bouton sans effet | 🧪 recette |

## Résultat de l'exécution

- **Date** : 2026-10-26.
- **Backend** : `mvn -B -ntp verify` en local (JDK 21, Testcontainers PostgreSQL) : `BUILD SUCCESS`, 110 tests unitaires et 96 tests d'intégration verts (dont 21 dans `SupplierSecurityIT`, 9 dans `SupplierServiceImplTest`), ArchUnit compris, aucun avertissement de compilation (relancé après les corrections 4 et 5).
- **Frontend** : `npx ng test --watch=false` : 132 tests verts sur 132 (32 fichiers), relancé après les corrections 4 et 5 ; `ng lint` sans erreur.
- **Anomalies** :
  1. ✅ Corrigé (moyenne) : enregistrer une fiche sans changement écrivait une ligne `UPDATE` dans le journal, contraire à la règle JNL-01 « ne pas tracer si rien n'a changé ». `Supplier.update` indique désormais si la fiche a changé.
  2. ✅ Corrigé (faible) : un nom fait d'espaces passait la validation du formulaire et revenait en 422 avec un message anglais.
  3. ✅ Corrigé (faible) : un nom de plus de 150 caractères affichait « Le nom est obligatoire ».
  4. ✅ Corrigé (faible) : messages serveur, messages de validation et libellés du journal du module fournisseur en anglais, affichés tels quels dans l'interface française (relevé dans FRS-01, anomalie 3). Passés en français comme les autres modules ; les lignes de journal déjà écrites restent en anglais (append-only).
  5. ✅ Corrigé (faible) : le formulaire n'affichait de message que pour le nom et le courriel ; chaque champ a maintenant son message (« N caractères maximum »).
  6. ℹ Écart accepté : courriel limité à 254 caractères dans le formulaire (Angular) contre 255 côté serveur.
- **Recette manuelle** : 5 étapes 🧪.
