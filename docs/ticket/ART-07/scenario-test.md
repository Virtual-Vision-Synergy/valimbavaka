# Scénarios de test — ART-07 · Import d'articles en masse

Statut : ✅ tests automatiques verts en CI le 2026-10-29 (recette 🧪 restante) · Ticket : [#27](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/27) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (`articles.import`), `SELLER` et `DELIVERY_DRIVER` pour les refus |
| Données de départ | Un article existant `ART-0007 · Savon noir 100 g` de code-barres `3760001230014` ; une gamme du référentiel (SYS-05) ; un fournisseur actif |
| Fichier type | `code;name;barcode;type;unit;threshold` puis `;Huile de ricin;;RAW_MATERIAL;LITER;2,5` (séparateur `;`, UTF-8, virgule ou point décimal) |
| Tickets à avoir déployés avant | ART-01 (fiche article, séquence de code), JNL-01 (journal) |
| Migration | `2026_10_29-02-changelog.sql` : permission `articles.import` donnée à `ADMIN` |

## Scénarios issus des critères d'acceptation

### Critère 1 — Import d'un fichier CSV (1 000 lignes au maximum)

| # | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 | Fichier de 2 lignes valides (`Huile de ricin`, `RAW_MATERIAL`, `2,5`) | 200, `created = 2`, deux codes `ART-nnnn`, seuil lu `2.5` | `ArticleImportIT.importArticles_validFile_createsArticlesAndTracesEachOneInJournal` | ✅ auto |
| 1b | Virgule décimale et types en minuscules (`raw_material`, `0,125`) | acceptés | `ArticleImportIT.importArticles_decimalCommaAndLowerCaseEnums_areAccepted`, `ArticleImportRowProcessorTest.process_decimalWithComma_isAccepted` | ✅ auto |
| 1c | Lecture du fichier : BOM, CRLF, lignes vides, champ entre guillemets contenant `;` | lu correctement | `ArticleCsvParserTest.parse_validFile_returnsRecordsByColumnName`, `parse_withBomCrLfAndBlankLines_readsFileCorrectly`, `parse_quotedFieldWithSeparatorAndQuotes_keepsFieldWhole`, `parse_headerInOtherCase_isAccepted` | ✅ auto |
| 1d | Exactement 1 000 lignes | accepté | `ArticleCsvParserTest.parse_exactlyMaxRows_isAccepted` | ✅ auto |
| 1e | 1 001 lignes | 422, rien n'est créé | `ArticleCsvParserTest.parse_oneRowOverMax_throwsBusinessException`, `ArticleImportIT.importArticles_moreThan1000Rows_returns422` | ✅ auto |
| 1f | Colonne `active` (ou `id`) dans l'en-tête | 422, rien n'est créé (mass assignment) | `ArticleCsvParserTest.parse_unknownColumnActive_throwsBusinessException`, `ArticleImportIT.importArticles_unknownColumnActive_returns422AndCreatesNothing` | ✅ auto |

### Critère 2 — Code du serveur ou du fichier

| # | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 2 | Colonne `code` vide | code `ART-0031` pris dans la séquence | `ArticleImportRowProcessorTest.process_newRowWithoutCode_createsWithGeneratedCodeAndJournals` | ✅ auto |
| 2b | Colonne `code` = `ART-0500`, libre | créé avec ce code, séquence avancée à 500 | `ArticleImportRowProcessorTest.process_newRowWithFileCode_createsWithThatCodeAndAdvancesSequence` | ✅ auto |
| 2c | Après l'import de `ART-nnnn` (n + 50), création d'un article par l'API | son code est supérieur : pas de collision | `ArticleImportIT.importArticles_codeFromFile_isKeptAndNextGeneratedCodeDoesNotCollide` | ✅ auto |
| 2d | Code `ART-12` (format invalide) | ligne `INVALID` « Code invalide : « ART-12 » (format attendu ART-0001). » | `ArticleImportRowProcessorTest.process_badCodeFormat_returnsInvalid` | ✅ auto |

### Critère 3 — Une ligne qui se heurte à un article existant est rejetée et signalée

| # | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 3 | Mode `SKIP`, code-barres `3760001230014` déjà pris par `ART-0007` | ligne `CONFLICT`, `code = ART-0007`, rien écrit, rien au journal | `ArticleImportRowProcessorTest.process_existingBarcodeInSkipMode_returnsConflictAndWritesNothing`, `ArticleImportIT.importArticles_existingBarcodeInSkipMode_keepsExistingArticle` | ✅ auto |
| 3b | Mode `SKIP`, code `ART-0007` déjà pris | ligne `CONFLICT` | `ArticleImportRowProcessorTest.process_existingCodeInSkipMode_returnsConflict` | ✅ auto |
| 3c | Même code-barres sur deux lignes du fichier | la 2e est `CONFLICT` « Doublon dans le fichier … ligne 1 » | `ArticleImportServiceImplTest.importArticles_barcodeRepeatedInFile_rejectsSecondRowWithoutProcessingIt` | ✅ auto |
| 3d | Même code sur deux lignes du fichier | la 2e est `CONFLICT` | `ArticleImportServiceImplTest.importArticles_codeRepeatedInFile_rejectsSecondRow` | ✅ auto |
| 3e | Code `ART-0007` et code-barres de `ART-0008` sur la même ligne | `CONFLICT` même en mode `OVERWRITE` (ambigu) | `ArticleImportRowProcessorTest.process_codeAndBarcodeOfTwoArticles_returnsConflictEvenInOverwriteMode` | ✅ auto |
| 3f | Code inconnu `ART-0900` mais code-barres d'un autre article | `CONFLICT`, même en `OVERWRITE` | `ArticleImportRowProcessorTest.process_unknownCodeWithBarcodeOfAnotherArticle_returnsConflict` | ✅ auto |

### Critère 4 — Choisir d'écraser les articles en conflit

| # | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 4 | Conflit en `SKIP`, puis rejeu de la ligne 1 en `OVERWRITE` (`onlyRows=1`) | `updated = 1`, nom et seuil remplacés, ligne `UPDATE` au journal | `ArticleImportIT.importArticles_conflictReplayedInOverwriteMode_updatesArticleAndTracesIt`, `ArticleImportRowProcessorTest.process_existingBarcodeInOverwriteMode_updatesArticleAndJournals` | ✅ auto |
| 4b | `OVERWRITE` avec exactement les valeurs déjà enregistrées | `UNCHANGED`, aucune écriture, aucune ligne de journal | `ArticleImportRowProcessorTest.process_overwriteWithSameValues_returnsUnchangedWithoutJournal`, `ArticleImportIT.importArticles_overwriteWithIdenticalData_reportsUnchanged` | ✅ auto |
| 4c | `onlyRows=2` sur un fichier de 2 lignes | seule la ligne 2 est traitée, la 1 est ignorée | `ArticleImportServiceImplTest.importArticles_onlyRows_processesOnlyTheListedRows`, `ArticleImportIT.importArticles_onlyRows_leavesTheOtherRowsUntouched` | ✅ auto |
| 4d | Écran : après un import avec un conflit, rien n'est écrasé tant que l'on ne clique pas | aucune 2e requête `/import` | `article-list-page-import.spec.ts` « n'écrase rien tant que l'administrateur ne le demande pas » | ✅ auto |
| 4e | Écran : clic sur « Écraser les 1 article(s) en conflit » | requête `OVERWRITE` avec `onlyRows=2`, rapport mis à jour | `article-list-page-import.spec.ts` « rejoue uniquement les lignes en conflit quand l'administrateur choisit d'écraser » | ✅ auto |

### Critère 5 — Import partiel

| # | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 5 | 3 lignes : une valide, une en conflit (code-barres pris), une sans nom | 200, `created = 1`, `conflicts = 1`, `invalid = 1`, message « Le nom est obligatoire » | `ArticleImportIT.importArticles_partialFile_keepsValidRowsAndReportsTheOthers`, `ArticleImportServiceImplTest.importArticles_mixedRows_countsEachStatus` | ✅ auto |
| 5b | Nom vide `"   "` | `INVALID` « Le nom est obligatoire » | `ArticleImportRowProcessorTest.process_blankName_returnsInvalid` | ✅ auto |
| 5c | Type `EMBALLAGE` | `INVALID` « Valeur inconnue pour type : « EMBALLAGE ». » | `ArticleImportRowProcessorTest.process_unknownType_returnsInvalid` | ✅ auto |
| 5d | Prix `-1` et code-barres `123` | `INVALID` avec les deux motifs | `ArticleImportRowProcessorTest.process_negativePriceAndShortBarcode_returnsInvalidWithBothMessages` | ✅ auto |
| 5e | Seuil `dix` | `INVALID` « Nombre invalide pour threshold : « dix ». » | `ArticleImportRowProcessorTest.process_notANumber_returnsInvalid` | ✅ auto |
| 5f | Produit fini sans gamme | `INVALID` (règle d'ART-01), rien écrit | `ArticleImportRowProcessorTest.process_finishedWithoutRange_returnsInvalidFromReferenceChecker` | ✅ auto |
| 5g | Ligne avec 3 colonnes au lieu de 4 | `INVALID` « Nombre de colonnes incorrect : 4 attendues, 3 trouvées. » | `ArticleCsvParserTest.parse_rowWithWrongColumnCount_returnsRecordWithError`, `ArticleImportRowProcessorTest.process_recordWithReadError_returnsInvalid` | ✅ auto |
| 5h | La base refuse une ligne (écriture simultanée, unicité) | `CONFLICT`, les autres lignes continuent | `ArticleImportServiceImplTest.importArticles_databaseRefusesRow_reportsConflictAndGoesOn` | ✅ auto |
| 5i | Écran : fichier choisi puis rapport | compteurs, lignes rejetées avec motif, bouton d'écrasement | `article-list-page-import.spec.ts` « envoie le fichier choisi puis affiche le compte rendu avec les lignes rejetées » | ✅ auto |

### Critère 6 — Journal

| # | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 6 | Article créé par l'import | une ligne `CREATE` / `ARTICLE` « Création de l'article ART-nnnn · … (import) » | `ArticleImportIT.importArticles_validFile_createsArticlesAndTracesEachOneInJournal`, `ArticleImportRowProcessorTest.process_newRowWithoutCode_createsWithGeneratedCodeAndJournals` | ✅ auto |
| 6b | Article remplacé | une ligne `UPDATE` « Modification de l'article … (import) » | `ArticleImportIT.importArticles_conflictReplayedInOverwriteMode_updatesArticleAndTracesIt` | ✅ auto |

### Critère 7 — En ligne uniquement, administrateur seulement

| # | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 7 | Fichier envoyé par `HttpClient` en multipart, pas par la file hors ligne | `POST /api/v1/articles/import?onConflict=SKIP`, aucune `Idempotency-Key` | `article-service.spec.ts` « envoie le fichier d'import en multipart avec le mode de conflit, sans passer par la file » | ✅ auto |
| 7b | Rejeu des conflits | `?onConflict=OVERWRITE&onlyRows=2,5` | `article-service.spec.ts` « limite le rejeu en mode écrasement aux lignes en conflit demandées » | ✅ auto |
| 7c | Écran : bouton « Importer » | visible pour l'administrateur, absent pour la vendeuse | `article-list-page-import.spec.ts` « affiche le bouton d'import à l'administrateur », « n'affiche pas le bouton d'import à la vendeuse » | ✅ auto |

## Sécurité et erreurs

| Cas | Résultat attendu | Test | Statut |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `ArticleImportSecurityIT.importArticles_withoutToken_returns401` | ✅ auto |
| Vendeuse (`SELLER`) | 403 `FORBIDDEN` | `ArticleImportSecurityIT.importArticles_withSellerRole_returns403` | ✅ auto |
| Livreur (`DELIVERY_DRIVER`) | 403 `FORBIDDEN` | `ArticleImportSecurityIT.importArticles_withDeliveryDriverRole_returns403` | ✅ auto |
| Colonne `id`, `active` ou `role` dans le fichier | 422, refusée, jamais appliquée (mass assignment) | `ArticleImportIT.importArticles_unknownColumnActive_returns422AndCreatesNothing` | ✅ auto |
| Fichier vide | 422 `VALIDATION_FAILED` | `ArticleImportIT.importArticles_emptyFile_returns422`, `ArticleCsvParserTest.parse_emptyFile_throwsBusinessException` | ✅ auto |
| En-tête seul, sans article | 422 | `ArticleCsvParserTest.parse_headerOnly_throwsBusinessException` | ✅ auto |
| Colonne obligatoire absente (`threshold`) ou répétée | 422 | `ArticleCsvParserTest.parse_missingRequiredColumn_throwsBusinessException`, `parse_repeatedColumn_throwsBusinessException` | ✅ auto |
| Fichier non UTF-8 (ISO-8859-1) | 422 « Le fichier doit être encodé en UTF-8. » | `ArticleCsvParserTest.parse_notUtf8_throwsBusinessException` | ✅ auto |
| Guillemet non fermé | 422 | `ArticleCsvParserTest.parse_unclosedQuote_throwsBusinessException` | ✅ auto |
| Fichier illisible côté serveur | 422 « Le fichier n'a pas pu être lu. » | `ArticleImportServiceImplTest.importArticles_unreadableFile_throwsBusinessException` | ✅ auto |
| Fichier invalide : aucune écriture avant l'erreur | rien en base | `ArticleImportServiceImplTest.importArticles_invalidFile_throwsBeforeWritingAnything` | ✅ auto |
| `onConflict=REPLACE` (mode inconnu) | 400 `MALFORMED_REQUEST` | `ArticleImportIT.importArticles_unknownConflictMode_returns400` | ✅ auto |
| Partie `file` absente | 400 | `ArticleImportIT.importArticles_withoutFilePart_returns400` | ✅ auto |
| Échec côté écran (422) | message d'erreur, pas de rapport affiché | `article-list-page-import.spec.ts` « signale l'échec d'un fichier refusé sans afficher de compte rendu » | ✅ auto |
| 404 | ⏭ sans objet : l'endpoint ne vise aucune ressource par identifiant | — | ⏭ |
| 409 | ⏭ jamais renvoyé : un conflit est une ligne `CONFLICT` du rapport (200) | — | ⏭ |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Code-barres à 12 chiffres | `376000123001` | `INVALID` (13 chiffres exigés) | `ArticleImportRowProcessorTest.process_negativePriceAndShortBarcode_returnsInvalidWithBothMessages` (cas `123`) | ✅ auto |
| Espaces autour des valeurs | `"  Savon noir 100 g  "` | rognés, nom enregistré sans espaces | `ArticleImportRowProcessorTest.process_newRowWithoutCode_createsWithGeneratedCodeAndJournals` | ✅ auto |
| Majuscules / minuscules de l'en-tête et des types | `NAME;Type`, `raw_material` | acceptées | `ArticleCsvParserTest.parse_headerInOtherCase_isAccepted`, `ArticleImportIT.importArticles_decimalCommaAndLowerCaseEnums_areAccepted` | ✅ auto |
| Deux administrateurs importent le même fichier | même code-barres, en même temps | une ligne gagne, l'autre est `CONFLICT` (contrainte `uq_articles_barcode`) | `ArticleImportServiceImplTest.importArticles_databaseRefusesRow_reportsConflictAndGoesOn` | ✅ auto |
| Rejeu complet d'un fichier déjà importé en `OVERWRITE` | mêmes données | lignes `UNCHANGED` (sauf lignes sans code ni code-barres, qui seraient recréées : c'est pourquoi l'écran n'envoie que les lignes en conflit) | `ArticleImportIT.importArticles_overwriteWithIdenticalData_reportsUnchanged` | ✅ auto |
| Hors ligne | réseau coupé pendant l'envoi | échec affiché (le fichier n'est pas mis en file, `Idempotency-Key` sans objet), à renvoyer une fois en ligne | 🧪 recette | 🧪 |
| Fichier de plus de 1 Mo | — | refusé par Spring (limite multipart par défaut 1 Mo) ; un fichier de 1 000 lignes en fait environ 100 Ko | 🧪 recette | 🧪 |

## Tests existants

**Backend** (`valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/article/`)
- `service/impl/ArticleCsvParserTest.java`, `ArticleImportRowProcessorTest.java`, `ArticleImportServiceImplTest.java`
- `controller/ArticleImportIT.java`, `ArticleImportSecurityIT.java` (socle `ArticleApiTestSupport.java`)

**Frontend** (`valimbavaka-ui/src/app/features/`)
- `article/data-access/article-service.spec.ts` (deux tests ajoutés)
- `articles/pages/article-list-page/article-list-page-import.spec.ts`

## Recette manuelle (🧪)

| Étape | Action | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Connecté en administrateur, ouvrir `/articles`, cliquer « Importer », choisir un CSV de 20 lignes préparé dans Excel (enregistré « CSV UTF-8 », séparateur `;`) | rapport avec 20 créés, liste rafraîchie | 🧪 |
| R2 | Réimporter le même fichier | 20 en conflit (lignes avec code-barres ou code), rien de modifié | 🧪 |
| R3 | Cliquer « Écraser les … en conflit » | rapport « remplacé(s) / inchangé(s) », journal avec les lignes `UPDATE` | 🧪 |
| R4 | Se connecter en vendeuse | pas de bouton « Importer » | 🧪 |
| R5 | Couper le réseau puis importer | message d'échec, aucun rapport | 🧪 |

## Résultat de l'exécution

- Date : 2026-10-29
- Environnement : CI GitHub de la PR #167, commit `ef7fe4e` (fusion de `develop` comprise) : `Backend (Spring Boot)` = `mvn verify`, BUILD SUCCESS, 197 tests d'intégration sans échec (tests unitaires compris dans la même exécution) ; `Frontend (Angular)` vert ; Spotless, Checkstyle, Prettier, ESLint, gitleaks, CodeQL et Qodana verts. Le détail par test n'a pas été relu : « ✅ auto » vaut pour un job vert avec le test présent dans le code.
- Anomalies ouvertes : aucune.
- Écarts à suivre : les colonnes `rangeId` et `supplierId` attendent des UUID (pas de recherche par libellé de gamme ou nom de fournisseur) ; les traductions malgaches de `articles.importReport.*` sont à faire relire.
- Étapes 🧪 restantes : 7 (R1 à R5, hors ligne, fichier > 1 Mo), pour Njara.
