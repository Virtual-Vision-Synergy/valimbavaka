# Scénarios de test — ART-01 · Fiche article complète

Statut : ☐ tests écrits le 2026-10-26, **à lancer** (CI ou `mvn verify` / `ng test`) · Ticket : [#21](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/21) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique exécuté et vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main · ☐ = écrit, pas encore exécuté. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Point de départ : aucun module article n'existait côté serveur (le dossier `features/articles` est une maquette sur données de démonstration). ART-01 livre la table, l'API `/api/v1/articles` et une fiche réelle (`features/article`). Le lien avec [FRS-01](../FRS-01/guide.md) : l'article référence son fournisseur par `supplierId` (UUID), ce qui remplit le bloc « articles fournis » de la fiche fournisseur.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (lit et écrit), `SELLER` (lit seulement, `articles.read`), `DELIVERY_DRIVER` (refusé) |
| Données de départ | un administrateur ; gammes Zezika / Botanika / Solika (SYS-05) ; fournisseur « Huiles Ravo » actif (FRS-01) ; article d'exemple « Savon noir 100 g », `FINISHED`, gamme Botanika, coût `3120.50`, prix `8500`, seuil `40` |
| Tickets à avoir déployés avant | FRS-01, JNL-01, SYS-05 (migration `2026_10_26-01` appliquée) |

## Scénarios issus des critères d'acceptation

### Critère 1 — Tous les champs sont visibles et modifiables sur la fiche

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Tous les champs visibles | Quand l'administrateur ouvre `/articles/:id/edit` | code (lecture seule), nom, type, unité, gamme, prix, coût, seuil, code-barres, fournisseur pré-remplis avec la fiche lue | `article-sheet-page.spec.ts` « should show the code and fill every field with the sheet read from the server » | ☐ |
| 1b | Lecture par l'API | `GET /api/v1/articles/{id}` | 200 avec les 15 champs de `ArticleRDto`, `version` à 0 | `controller\ArticleSecurityIT.getArticleById_existingArticle_returnsTheWholeSheet` | ☐ |
| 1c | Fiche inconnue | `GET` d'un identifiant inexistant | 404 `NOT_FOUND` ; l'écran affiche « Article introuvable » | `controller\ArticleSecurityIT.getArticleById_unknownId_returns404` ; `ArticleServiceImplTest.getArticleById_unknownId_throwsNotFound` ; `article-sheet-page.spec.ts` « when the sheet does not exist » | ☐ |
| 1d | Tous les champs modifiables | `PUT` avec nom, code-barres, type `SEMI_FINISHED`, unité `KILOGRAM`, coût `4100.25`, seuil `12.5`, fournisseur | 200, tous les champs changés (gamme et prix remis à `null`) | `controller\ArticleUpdateIT.updateArticle_byAdmin_changesEveryField` ; `ArticleServiceImplTest.updateArticle_changedFields_appliesThemAndTracesInJournal` | ☐ |
| 1e | Enregistrement depuis l'écran | Il change le nom en « Savon noir 120 g » et le seuil en `25`, puis enregistre | `PUT` avec les valeurs saisies et `version: 3`, retour à `/articles` | `article-sheet-page.spec.ts` « should save with PUT, sending the version read, then go back to the list » | ☐ |
| 1f | Modification tracée | `PUT` avec la version lue et le seuil changé | 200 et une ligne `UPDATE` (ressource `ARTICLE`) en plus de la ligne `CREATE` | `controller\ArticleUpdateIT.updateArticle_withVersionRead_savesAndTracesUpdateInJournal` | ☐ |
| 1g | Rien de changé | `PUT` avec `"  <nom> "` (espaces autour) et les autres valeurs identiques | 200 et **aucune** nouvelle ligne de journal | `controller\ArticleUpdateIT.updateArticle_withoutChange_leavesNoNewJournalLine` ; `ArticleServiceImplTest.updateArticle_sameValuesWithOtherSpacing_leavesNoTrace` ; `ArticleTest.update_sameValuesWithOtherScale_returnsFalse` | ☐ |
| 1h | Changement réel | Prix `8500` → `9000.50` | `update` renvoie vrai, le code ne change jamais | `ArticleTest.update_changedPrice_returnsTrueAndKeepsTheCode` | ☐ |
| 1i | Création | `POST` d'un produit fini valide | 201 + `Location`, code `ART-nnnn` attribué par le serveur, `active = true`, ligne `CREATE` | `controller\ArticleCreationIT.createArticle_finishedProductByAdmin_returns201WithCodeAndTracesInJournal` ; `ArticleServiceImplTest.createArticle_validFinishedProduct_savesWithServerCodeAndTracesInJournal` ; `ArticleTest.create_finishedProduct_keepsRangePriceAndIsActive` | ☐ |
| 1j | Création depuis l'écran | Nom `"  Gommage café 200 g "`, gamme Botanika, prix `12500`, seuil 0 | `POST` sans version (`version: null`), retour à `/articles` | `article-sheet-page.spec.ts` « should create with POST, without version, then go back to the list » | ☐ |
| 1k | Code jamais saisi | Corps `{"code":"HACK-1",…}` | le code envoyé est ignoré, le serveur attribue `ART-nnnn` | `controller\ArticleCreationIT.createArticle_massAssignmentOfIdCodeAndStatus_isIgnored` ; `controller\ArticleUpdateIT.updateArticle_massAssignmentOfIdCodeAndStatus_isIgnored` | ☐ |
| 1l | Codes jamais réutilisés | Deux appels successifs à la séquence | numéros croissants ; deux articles de même nom ont deux codes différents | `ArticleRepositoryIT.nextCodeNumber_calledTwice_returnsIncreasingNumbers` ; `controller\ArticleCreationIT.createArticle_sameNameTwice_isAcceptedWithTwoDifferentCodes` | ☐ |
| 1m | Désactivation (jamais de suppression) | `PATCH /status {"active":false}` puis `false` puis `true` | `active` suit la demande ; 2 lignes `UPDATE` seulement (la 2e demande identique ne trace rien) | `controller\ArticleUpdateIT.setActive_byAdmin_deactivatesThenReactivatesAndTracesOnlyRealChanges` ; `ArticleServiceImplTest.setActive_activeArticleDeactivated_deactivatesAndTracesInJournal` ; `ArticleServiceImplTest.setActive_stateAlreadyRequested_leavesNoTrace` ; `ArticleTest.deactivateThenActivate_togglesTheState` ; `article-service.spec.ts` « désactive une fiche via PATCH /status, sans suppression » | ☐ |

### Critère 2 — Le type d'article (MP/SF/Fini) est correctement enregistré

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 2 | Matière première et semi-fini | `POST` avec `RAW_MATERIAL` puis `SEMI_FINISHED` (unité `KILOGRAM`, seuil 5) | 201, le type renvoyé est celui envoyé, ni gamme ni prix | `controller\ArticleCreationIT.createArticle_rawMaterialAndSemiFinished_areRecordedWithoutRangeNorPrice` | ☐ |
| 2b | Produit fini | `POST` avec `FINISHED`, gamme Botanika, prix `8500` | 201, type `FINISHED`, gamme et prix conservés | `controller\ArticleCreationIT.createArticle_finishedProductByAdmin_returns201WithCodeAndTracesInJournal` | ☐ |
| 2c | Gamme et prix ignorés hors produit fini | `RAW_MATERIAL` envoyé avec `rangeId` et `price: 15000`, coût `12000` | 201, gamme et prix remis à `null`, coût conservé | `controller\ArticleCreationIT.createArticle_rawMaterialWithRangeAndPrice_dropsThem` ; `ArticleTest.create_rawMaterial_dropsRangeAndPrice` | ☐ |
| 2d | Changement de type | Un produit fini passe en `SEMI_FINISHED` | type changé, gamme et prix effacés | `ArticleTest.update_finishedProductBecomesSemiFinished_dropsRangeAndPrice` | ☐ |
| 2e | Type enregistré par nom | Colonne `type` | valeur `RAW_MATERIAL` / `SEMI_FINISHED` / `FINISHED` (pas d'ordinal), refus d'une autre valeur par `ck_articles_type` | `controller\ArticleCreationIT.createArticle_rawMaterialAndSemiFinished_areRecordedWithoutRangeNorPrice` (relecture) ; contrainte à vérifier en base | ☐ · ☐ base |
| 2f | Type inconnu | `"type":"PACKAGING"` | 400 `MALFORMED_REQUEST` | `controller\ArticleCreationIT.createArticle_unknownTypeOrUnit_returns400` | ☐ |
| 2g | Écran : gamme et prix masqués | Il choisit « Matière première » | champs gamme et prix absents, envoyés à `null` | `article-sheet-page.spec.ts` « should hide the range and the price and send them as null for a raw material » ; « should create a raw material without range or price » | ☐ |
| 2h | Écran : produit fini incomplet | Produit fini sans gamme ni prix | rien n'est envoyé, messages sous les champs | « should send nothing when a finished product has no range or no price » | ☐ |

### Lien avec FRS-01 — articles fournis

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 3 | Article lié à un fournisseur | `POST` avec `supplierId` d'un fournisseur actif | 201, `supplierId` renvoyé ; `GET /articles?supplierId=…` renvoie cet article seul | `controller\ArticleCreationIT.createArticle_withActiveSupplier_linksTheArticleToIt` | ☐ |
| 3b | Fournisseur inconnu ou désactivé | `supplierId` aléatoire, ou fournisseur désactivé par `PATCH` | 422 `VALIDATION_FAILED` | `controller\ArticleCreationIT.createArticle_unknownRangeOrSupplier_returns422` ; `controller\ArticleCreationIT.createArticle_deactivatedSupplier_returns422` ; `ArticleReferenceCheckerTest.check_unknownOrInactiveSupplier_throwsBusinessException` | ☐ |
| 3c | Fournisseur désactivé depuis, article conservé | Une fiche garde son fournisseur devenu inactif | la fiche reste modifiable (le fournisseur n'est contrôlé que s'il change) | `ArticleReferenceCheckerTest.check_supplierDeactivatedButKeptByTheSheet_passesWithoutAskingTheSupplier` | ☐ |
| 3d | Filtre côté base | Recherche par fournisseur | seuls les articles de ce fournisseur | `ArticleRepositoryIT.search_bySupplier_returnsOnlyTheArticlesOfThatSupplier` | ☐ |
| 3e | Bloc « articles fournis » de la fiche fournisseur | Il ouvre `/suppliers/:id/edit` d'un fournisseur ayant des articles | la liste (code + nom) apparaît, lue avec `supplierId` ; sans article : « Aucun article fourni par ce fournisseur. » | `supplier-form-page.spec.ts` « should list the articles of this supplier, read with its identifier » ; « should say that no article is supplied when the list is empty » | ☐ |
| 3f | Contrôles croisés | Gamme ou fournisseur actifs | `isActiveProductRange` vrai pour une gamme semée, faux pour un identifiant inconnu ; `isActiveSupplier` vrai/faux | `ReferentialServiceImplIT.isActiveProductRange_seededRangeIsActiveAndUnknownIdIsNot` ; `SupplierServiceImplTest.isActiveSupplier_activeSupplier_returnsTrueAndUnknownOrInactiveReturnsFalse` | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Liste sans jeton | `GET /api/v1/articles` | 401 `UNAUTHENTICATED` | `controller\ArticleSecurityIT.getArticles_withoutToken_returns401` | ☐ |
| Création sans jeton | `POST` | 401 | `controller\ArticleSecurityIT.createArticle_withoutToken_returns401` | ☐ |
| Modification sans jeton | `PUT` | 401 | `controller\ArticleUpdateIT.updateArticle_withoutToken_returns401` | ☐ |
| Lecture par un livreur | jeton livreur | 403 `FORBIDDEN` | `controller\ArticleSecurityIT.getArticles_withDeliveryDriverRole_returns403` | ☐ |
| Lecture par une vendeuse | jeton vendeuse (`articles.read`) | 200 | `controller\ArticleSecurityIT.getArticles_withSellerRole_returns200` | ☐ |
| Création par une vendeuse | jeton vendeuse | 403 | `controller\ArticleSecurityIT.createArticle_withSellerRole_returns403` | ☐ |
| Modification par une vendeuse | jeton vendeuse | 403, fiche inchangée | `controller\ArticleUpdateIT.updateArticle_withSellerRole_returns403AndKeepsTheSheet` | ☐ |
| Désactivation par une vendeuse | jeton vendeuse | 403 | `controller\ArticleUpdateIT.setActive_withSellerRole_returns403` | ☐ |
| Mass assignment | `{"id":"…","code":"HACK-1","active":false,…}` | `id`, `code`, `active` ignorés | `controller\ArticleCreationIT.createArticle_massAssignmentOfIdCodeAndStatus_isIgnored` ; `controller\ArticleUpdateIT.updateArticle_massAssignmentOfIdCodeAndStatus_isIgnored` | ☐ |
| Corps vide | `{}` | 422 `VALIDATION_FAILED` | `controller\ArticleCreationIT.createArticle_emptyBody_returns422` | ☐ |
| Valeurs invalides | nom de 121 caractères, nom d'espaces, seuil `-1`, coût `-0.01`, coût `10.123`, code-barres `12345` ou `abcdefghijklm` | 422 | `controller\ArticleCreationIT.createArticle_invalidValues_returns422` ; `controller\ArticleUpdateIT.updateArticle_invalidData_returns422AndKeepsTheSheet` | ☐ |
| Produit fini sans gamme / sans prix | `FINISHED` sans `rangeId`, puis sans `price` | 422 « La gamme est obligatoire… » / « Le prix est obligatoire… » | `controller\ArticleCreationIT.createArticle_finishedProductWithoutRangeOrPrice_returns422` ; `ArticleReferenceCheckerTest.check_finishedProductWithoutRange_throwsBusinessException` ; `ArticleReferenceCheckerTest.check_finishedProductWithoutPrice_throwsBusinessException` | ☐ |
| Gamme inconnue | `rangeId` aléatoire | 422 | `controller\ArticleCreationIT.createArticle_unknownRangeOrSupplier_returns422` ; `ArticleReferenceCheckerTest.check_unknownOrInactiveRange_throwsBusinessException` | ☐ |
| Type ou unité inconnus | `"type":"PACKAGING"` | 400 `MALFORMED_REQUEST` | `controller\ArticleCreationIT.createArticle_unknownTypeOrUnit_returns400` | ☐ |
| Article inconnu | `PUT`, `PATCH` d'un identifiant aléatoire | 404 `NOT_FOUND` | `controller\ArticleUpdateIT.updateArticle_unknownId_returns404` ; `controller\ArticleUpdateIT.setActive_unknownId_returns404` ; `ArticleServiceImplTest.updateArticle_unknownId_throwsNotFound` | ☐ |
| Code-barres déjà pris | même `3760123456789` sur 2 articles | 409 `CONFLICT` « Un article avec ce code-barres existe déjà. » | `controller\ArticleCreationIT.createArticle_barcodeAlreadyUsed_returns409` ; `controller\ArticleUpdateIT.updateArticle_barcodeOfAnotherArticle_returns409` ; `ArticleServiceImplTest.createArticle_barcodeAlreadyUsed_throwsConflictWithoutSaving` ; `ArticleServiceImplTest.updateArticle_barcodeOfAnotherArticle_throwsConflict` ; `ArticleRepositoryIT.existsByBarcode_usedBarcode_returnsTrueAndIgnoresTheArticleItself` | ☐ |
| Version périmée | `PUT` avec `"version":42` sur une fiche en version 0 | 409, fiche inchangée | `controller\ArticleUpdateIT.updateArticle_staleVersion_returns409AndKeepsTheSheet` ; `ArticleServiceImplTest.updateArticle_staleVersion_throwsConflictAndChangesNothing` ; `article-sheet-page.spec.ts` « should show the server message when the sheet was modified in the meantime » | ☐ |
| Référence refusée : rien n'est enregistré | contrôle croisé en échec | aucune écriture, aucune ligne de journal | `ArticleServiceImplTest.createArticle_referenceRefused_savesNothing` | ☐ |
| Désactivation sans champ | `PATCH {}` | 422 | `controller\ArticleUpdateIT.setActive_withoutActiveField_returns422` | ☐ |
| Suppression | `DELETE /api/v1/articles/{id}` | 405, l'article existe toujours | `controller\ArticleSecurityIT.deleteArticle_isNotExposed` | ☐ |
| Auteur d'audit | création par l'administrateur | `createdBy` et `updatedBy` = identifiant du compte | `controller\ArticleSecurityIT.createArticle_byAdmin_fillsTheAuditAuthorWithHisAccountId` | ☐ |
| Journal | création, modification réelle, changement d'état | lignes `CREATE` / `UPDATE` ressource `ARTICLE` | voir 1f, 1i, 1m | ☐ |

## Cas limites

| Cas | Exemple d'entrée | Résultat attendu | Comment c'est vérifié | Statut |
|---|---|---|---|---|
| Nom au maximum / maximum + 1 | 120 puis 121 caractères | 120 accepté, 121 refusé (422 ; écran : « 120 caractères maximum ») | `controller\ArticleCreationIT.createArticle_boundaryValues_areAccepted` ; `controller\ArticleCreationIT.createArticle_invalidValues_returns422` ; `article-sheet-page.spec.ts` « should send nothing when the name exceeds 120 characters, and accept exactly 120 » | ☐ |
| Nom vide ou d'espaces | `"   "` | refusé (écran : « Ce champ est obligatoire ») | `article-sheet-page.spec.ts` « should send nothing and ask for a name when the name is only spaces » ; `controller\ArticleCreationIT.createArticle_invalidValues_returns422` | ☐ |
| Espaces autour du nom | `"  Savon noir 100 g "` | enregistré « Savon noir 100 g » | `ArticleServiceImplTest.createArticle_validFinishedProduct_savesWithServerCodeAndTracesInJournal` ; `ArticleTest.create_nameBarcodeWithSpaces_isStrippedAndBlankBarcodeBecomesNull` | ☐ |
| Même nom, deux articles | « Savon noir 100 g » deux fois | accepté, codes différents (le nom n'est pas unique, le code l'est) | `controller\ArticleCreationIT.createArticle_sameNameTwice_isAcceptedWithTwoDifferentCodes` ; `ArticleRepositoryIT.save_sameCodeTwice_isRefusedByTheDatabase` | ☐ |
| Coût et seuil à zéro | coût `0`, seuil `0` | acceptés | `controller\ArticleCreationIT.createArticle_boundaryValues_areAccepted` ; `article-sheet-page.spec.ts` « should refuse a negative cost, price or threshold and accept zero » | ☐ |
| Valeurs négatives | coût `-0.01`, prix `-1`, seuil `-5` | refusés (écran : « La valeur ne peut pas être négative ») | `controller\ArticleCreationIT.createArticle_invalidValues_returns422` ; « should refuse a negative cost, price or threshold and accept zero » | ☐ |
| Décimales exactes | prix `12500.50`, seuil `12.5` | relus à l'identique (pas d'arrondi binaire) ; au-delà de 2 décimales sur le coût : 422 | `ArticleRepositoryIT.save_newArticle_isPersistedWithAuditFieldsAndExactDecimals` ; `controller\ArticleUpdateIT.updateArticle_byAdmin_changesEveryField` | ☐ |
| Code-barres | 13 chiffres / 12 chiffres / lettres / vide | 13 chiffres accepté ; 12 chiffres et lettres refusés ; vide = absent | `article-sheet-page.spec.ts` « should send nothing when the barcode is %s » (3 cas) et « should send an empty barcode as null » ; `controller\ArticleCreationIT.createArticle_invalidValues_returns422` | ☐ |
| Plusieurs articles sans code-barres | deux articles sans code-barres | acceptés (l'unicité ne vise que les valeurs renseignées) ; un code-barres en double est refusé | `ArticleRepositoryIT.save_sameBarcodeTwice_isRefusedButSeveralWithoutBarcodeAreAccepted` | ☐ |
| Son propre code-barres | `PUT` qui renvoie le code-barres déjà porté par la fiche | accepté | `controller\ArticleUpdateIT.updateArticle_ownBarcodeUnchanged_isAccepted` | ☐ |
| Recherche | `search` sur le nom, le code (`art-0031`) ou le code-barres, quelle que soit la casse | l'article est trouvé | `ArticleRepositoryIT.search_byNameCodeOrBarcode_findsTheArticleWhateverTheCase` ; `controller\ArticleSecurityIT.getArticles_byTypeAndSearch_returnsOnlyMatchingArticles` | ☐ |
| Filtres combinés | type `FINISHED` + gamme Botanika + `active=true` | seuls les articles correspondants, total exact | `ArticleRepositoryIT.search_withTypeRangeAndStatusFilters_returnsOnlyMatchingArticles` ; `article-service.spec.ts` « envoie la recherche, le type, la gamme, le fournisseur et le statut choisis » | ☐ |
| Critères vides | `search=""`, pas de type | non envoyés | `article-service.spec.ts` « envoie la pagination de la liste, sans critère vide » | ☐ |
| Liste vide | `supplierId` sans article | `items` vide, `totalCount` 0 | `ArticleRepositoryIT.search_bySupplier_returnsOnlyTheArticlesOfThatSupplier` (second article d'un autre fournisseur absent) | ☐ |
| Pagination | `size=100` / `size=101` | 200 / 422 | `controller\ArticleSecurityIT.getArticles_unknownTypeOrSizeAboveLimit_isRefused` | ☐ |
| Tri hors liste blanche | `sortBy=supplierId; drop table articles` | ignoré, tri par défaut `createdAt` décroissant, pas d'erreur | `controller\ArticleSecurityIT.getArticles_sortOutsideTheWhiteList_fallsBackToDefaultSort` | ☐ |
| Double clic sur Enregistrer | deux clics rapides | une seule requête, bouton désactivé pendant l'envoi | `article-sheet-page.spec.ts` « should disable the save button while saving so that a double click sends one request » | ☐ |
| Deux administrateurs modifient la même fiche | A et B ouvrent la version 3, A enregistre, puis B | B reçoit 409 et la saisie reste à l'écran | voir « Version périmée » | ☐ |
| Message du serveur affiché | code-barres déjà pris | message du `ProblemDetail` sous le formulaire, saisie conservée | `article-sheet-page.spec.ts` « should show the server message when the barcode is already used » | ☐ |
| Fournisseur proposé | fournisseur lié devenu inactif | seuls les fournisseurs actifs et celui déjà lié sont proposés | `article-sheet-page.spec.ts` « should only offer active suppliers, plus the one already linked » | ☐ |
| Séquence concurrente | deux créations simultanées | deux codes distincts (séquence PostgreSQL, trous possibles) | `ArticleRepositoryIT.nextCodeNumber_calledTwice_returnsIncreasingNumbers` | ☐ |
| Écriture hors ligne | réseau coupé | non prise en charge (`HttpClient` direct) | `article-service.ts` | ⏭ hors périmètre (SYS-07) |
| Stock total, statut de stock, lots, promotions | fiche 360° | non fournis par cette API | maquette `features/articles` | ⏭ hors périmètre (STK-01, ART-06) |
| Liste, filtres et import/export | liste réelle des articles | la page liste reste la maquette | `features/articles/pages/article-list-page` | ⏭ hors périmètre (ART-05, ART-07, ART-08) |
| Migration | `2026_10_26-01` | table, séquence, contraintes `ck_articles_*`, index `uq_articles_barcode` partiel, permissions `articles.create` / `articles.update` pour `ADMIN` seul, rollback propre | à rejouer sur une base neuve | ☐ base |

## Tests existants

- Backend : `feature/article/domain/ArticleTest.java`, `feature/article/service/impl/ArticleServiceImplTest.java`, `feature/article/service/impl/ArticleReferenceCheckerTest.java`, `feature/article/repository/ArticleRepositoryIT.java`,  complétés dans `feature/supplier/service/impl/SupplierServiceImplTest.java` (`isActiveSupplier`) et `feature/referential/service/impl/ReferentialServiceImplIT.java` (`isActiveProductRange`).
- Frontend : `features/article/data-access/article-service.spec.ts`, `features/article/pages/article-sheet-page/article-sheet-page.spec.ts` ; complété dans `features/supplier/pages/supplier-form-page/supplier-form-page.spec.ts` (bloc « articles fournis »).

## Recette manuelle (🧪)

| Étape | Attendu | Statut |
|---|---|---|
| Ouvrir `/articles/sheet/new`, créer une matière première « Beurre de karité brut » (kg, seuil 5, fournisseur « Huiles Ravo ») | retour à la liste ; l'article apparaît dans le bloc « articles fournis » de « Huiles Ravo » | 🧪 recette |
| Créer un produit fini « Savon noir 100 g » (gamme Botanika, prix 8500) puis rouvrir sa fiche par `/articles/<id>/edit` | code `ART-nnnn` affiché, tous les champs identiques | 🧪 recette |
| Changer le type du produit fini en « Semi-fini » | gamme et prix disparaissent de l'écran ; après enregistrement ils sont vides | 🧪 recette |
| Deux onglets sur la même fiche, enregistrer dans les deux | le second affiche « modifié entre-temps » et garde la saisie | 🧪 recette |
| Journal | lignes `CREATE` et `UPDATE` de l'article, aucune ligne si rien n'a changé | 🧪 recette |
| Se connecter en vendeuse | lecture de la liste possible, URL `/articles/<id>/edit` refusée | 🧪 recette |

## Résultat de l'exécution

- **Date** : tests écrits le 2026-10-26 ; **non exécutés** par l'auteur (consigne : ne pas lancer `mvn verify` ni `ng test`).
- **À lancer** : `cd valimbavaka-core && mvn -B -ntp verify` (Docker requis pour Testcontainers ; inclut `ArchitectureTest`) puis `cd valimbavaka-ui && npx ng test --watch=false`. Aucun statut ✅ tant que la CI n'est pas verte.
- **Points à surveiller au premier passage** : `ArchitectureTest` (nouvelle feature `article` : pas de cycle, repository utilisé par le service seulement) ; liaison de paramètres `null` de type UUID dans la requête `ArticleRepository.search` (même schéma que l'énumération et le booléen de `SupplierRepository.search`) ; migration `2026_10_26-01` avec `ddl-auto=validate`.
- **Anomalies / écarts connus** :
  1. Moyenne — le coût d'achat est visible de toute personne ayant `articles.read`, donc de la vendeuse ; à trancher avec le métier (masquer `cost` pour `SELLER`).
  2. Faible — `ArticleRepository.search` a 6 paramètres (limite de 4 de la convention) : liaison de requête JPQL sans logique ; exception à justifier dans la PR.
  3. Faible — la clé de la fiche réelle est `/articles/sheet/new` et `/articles/:id/edit` tant que la maquette garde `/articles/new` ; [ART-02](../ART-02/description.md) remplacera la maquette.
  4. Faible — le type `PACKAGING` de la maquette n'existe pas dans le contrat du ticket (MP / SF / Fini) ; à confirmer avec le métier avant [ART-04](../ART-04/description.md).
- **Recette manuelle** : 6 étapes 🧪.
