# Guide d'intégration — ART-07 · Import d'articles en masse

Statut : ✅ écrit le 2026-10-29 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#27](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/27) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur importe des articles depuis un fichier CSV (UTF-8, séparateur `;`, 1 000 lignes au maximum) depuis le bouton « Importer » de la liste `/articles`. L'import est partiel : chaque ligne valide est créée, chaque ligne qui se heurte à un article existant (même code ou même code-barres) est rejetée et listée en conflit sans rien modifier, et l'administrateur peut ensuite choisir d'écraser ces articles. Chaque création ou remplacement est tracé dans le journal.

## Contrat exposé

### Endpoint backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/articles/import` | `articles.import` (admin seul) | multipart : `file` (CSV) ; paramètres `onConflict` = `SKIP` (défaut) ou `OVERWRITE`, `onlyRows` = numéros de ligne séparés par des virgules (facultatif) | 200 `ArticleImportRDto` | 400 (mode inconnu, partie `file` absente), 401, 403, 422 (fichier vide, illisible, non UTF-8, > 1 000 lignes, colonne inconnue ou obligatoire absente) |

Il n'y a ni 404 ni 409 : un conflit est une ligne `CONFLICT` du rapport, la réponse reste 200.

- **Colonnes acceptées** (`ArticleCsvParser.COLUMNS`) : `code`, `name`, `barcode`, `type`, `rangeId`, `unit`, `cost`, `price`, `threshold`, `supplierId`. Obligatoires dans l'en-tête : `name`, `type`, `unit`, `threshold`. Majuscules/minuscules libres dans l'en-tête et pour `type` / `unit`. Toute autre colonne (`id`, `active`, `role`…) fait refuser **tout le fichier** en 422 : jamais appliquée en silence.
- **Valeurs** : décimaux avec `.` ou `,` ; `rangeId` et `supplierId` sont des **UUID** ; les règles de la fiche (ART-01) s'appliquent ligne par ligne (Bean Validation de `ArticleWDto`, gamme et prix obligatoires pour un produit fini, fournisseur actif, code-barres à 13 chiffres).
- **Code** : colonne vide → `ART-nnnn` de la séquence ; renseignée → doit respecter `ART-` + 4 à 9 chiffres ; un code libre est gardé et la séquence `article_code_seq` est avancée au moins jusqu'à lui (`ArticleRepository.advanceCodeSequence`), pour que les articles créés ensuite ne le heurtent pas.
- **Rapport** `ArticleImportRDto` : `created`, `updated`, `unchanged`, `conflicts`, `invalid`, `rows[]`. Chaque `ArticleImportRowRDto` donne `row` (numéro après l'en-tête, dès 1), `status` (`CREATED`, `UPDATED`, `UNCHANGED`, `CONFLICT`, `INVALID`), `code` (article créé, remplacé ou en conflit) et `message` (motif).
- **Décision de conflit** (`ArticleImportRowProcessor.write`) : la ligne vise l'article trouvé par son code, sinon par son code-barres. Si le code désigne un article et le code-barres un autre, ou si le code est inconnu mais le code-barres pris, c'est un conflit **même en `OVERWRITE`**. Un code ou un code-barres répété dans le fichier : la première ligne gagne, les suivantes sont `CONFLICT` « Doublon dans le fichier » (`ArticleImportServiceImpl.findRepeat`).
- **`SKIP`** : l'article existant n'est pas touché. **`OVERWRITE`** : remplacement complet comme un `PUT` (un champ facultatif vide efface la valeur) ; `UNCHANGED` si rien ne change, sans ligne de journal. L'état actif/inactif n'est jamais modifié par l'import.
- **Transactions** : une transaction par ligne (`ArticleImportRowProcessor`, `ArticleImportServiceImpl.importArticles` en `NOT_SUPPORTED`), c'est ce qui rend l'import partiel. Une erreur d'accès aux données sur une ligne devient un `CONFLICT` « écriture simultanée » et l'import continue.
- **Journal** : ressource `ARTICLE`, `CREATE` « Création de l'article ART-0031 · Savon noir 100 g (import) » ou `UPDATE` « Modification de l'article … (import) ». Une ligne par article, pas de ligne récapitulative.

### Services Java réutilisables

- `feature.article.service.ArticleImportService.importArticles(MultipartFile, ImportConflictMode, Set<Integer> onlyRows)` : `onlyRows` vide = toutes les lignes. Le format CSV et le rapport sont propres à l'article ; un autre import (fournisseurs…) doit avoir son propre service.
- `ArticleCsvParser`, `CsvTokenizer` et `ArticleImportRowProcessor` sont privés au paquet `service.impl`. `CsvTokenizer` est un lecteur RFC 4180 avec `;` : à déplacer dans `shared/` si un autre ticket doit lire du CSV.

### Frontend

- `features/article/data-access/article-service.ts` : `importFile(file, mode, onlyRows?)` (multipart en `HttpClient`, **hors file hors ligne** : un fichier n'est pas mis en attente). Types `ArticleImportRDto`, `ArticleImportRowRDto`, `ImportConflictMode`, `ImportRowStatus` dans `article-models.ts`.
- `features/articles/pages/article-list-page` : bouton « Importer » sous `*vvsHasPermission="'articles.import'"` (c'était un `demoAction()`), champ fichier caché, rapport affiché au-dessus de la liste, liste rechargée après chaque import. Le fichier choisi est gardé en mémoire pour le rejeu : « Écraser » renvoie le même fichier avec `OVERWRITE` et `onlyRows` = les seules lignes en conflit.
- `features/articles/components/article-import-report/*` : compteurs, tableau des lignes `CONFLICT` / `INVALID` avec leur motif, bouton « Écraser les N article(s) en conflit ».
- Traductions : `articles.importReport.*` dans `public/i18n/fr.json` et `mg.json` (malgache à relire). `core/auth/auth-testing.ts` donne `articles.import` à `ADMIN`.

### Données

- Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_29-01-changelog.sql`, déclarée dans `db.changelog-master.yaml` : permission `articles.import`, donnée à `ADMIN` seulement (rollback fourni). Aucune table nouvelle.

## Règles métier à respecter par les autres tickets

- Un article importé est un article ordinaire : mêmes règles qu'ART-01 (type, gamme et prix pour le produit fini, fournisseur actif, code-barres unique), actif à la création. Les écrans de sélection continuent de filtrer sur `active = true`.
- Le code est stable et jamais réutilisé, qu'il vienne de la séquence ou du fichier. Référencer un article par son UUID.
- L'import ne désactive jamais un article et ne supprime rien.
- L'import est réservé à `articles.import` (administrateur) et exige d'être en ligne.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article/`)
- `controller/ArticleController.java` (action `importArticles`, ligne 117)
- `domain/ImportConflictMode.java`, `domain/ImportRowStatus.java`
- `dto/ArticleImportRDto.java`, `dto/ArticleImportRowRDto.java`
- `service/ArticleImportService.java`, `service/impl/ArticleImportServiceImpl.java`, `ArticleImportRowProcessor.java`, `ArticleCsvParser.java`, `ArticleCsvRecord.java`, `CsvTokenizer.java`
- `repository/ArticleRepository.java` (`findByCode`, `findByBarcode`, `advanceCodeSequence`)

**Migration** : `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_29-01-changelog.sql`.

**Frontend** (`valimbavaka-ui/src/app/features/`) : `article/data-access/article-models.ts`, `article/data-access/article-service.ts`, `articles/pages/article-list-page/article-list-page.{ts,html}`, `articles/components/article-import-report/*`.

**Tests** : voir « Tests existants » du [scénario](scenario-test.md).

## Dépendances

**Ce ticket s'appuie sur** : [ART-01](../ART-01/guide.md) (fiche article, séquence de code, contrôles croisés), [JNL-01](../JNL-01/guide.md) (journal).

**Ces tickets s'appuient sur lui** : aucun.

## Comment l'utiliser depuis un autre ticket

```ts
// Envoyer un fichier, puis rejouer seulement les conflits en écrasant
this.articleService.importFile(file, 'SKIP').subscribe((report) => {
  const conflicts = report.rows.filter((r) => r.status === 'CONFLICT').map((r) => r.row);
  this.articleService.importFile(file, 'OVERWRITE', conflicts).subscribe();
});
```

Ne jamais rejouer le fichier entier en `OVERWRITE` : une ligne sans code ni code-barres ne peut pas retrouver son article et serait créée une seconde fois.

## Points d'attention

- **`rangeId` et `supplierId` sont des UUID** dans le fichier : l'administrateur ne peut pas écrire « Botanika » ou le nom d'un fournisseur. À décider avec le porteur de projet (recherche par libellé dans `ReferentialService` et `SupplierService`, qui n'ont pas cette recherche).
- Pas de fichier modèle téléchargeable ni d'export au même format : voir [ART-08](../ART-08/description.md) pour l'export, qui devrait produire ce format pour permettre l'aller-retour.
- Limite de taille : Spring accepte 1 Mo par fichier par défaut, largement assez pour 1 000 lignes ; la limite de lignes (1 000) est dans `ArticleCsvParser.MAX_ROWS`.
- Le numéro de ligne est celui de l'enregistrement après l'en-tête, pas celui de la ligne physique du fichier (différent si un champ entre guillemets contient un retour à la ligne). Les lignes vides sont ignorées sans être comptées.
- `advanceCodeSequence` lit puis écrit la séquence sans verrou : deux imports simultanés avec des codes de fichier pourraient la faire reculer ; la contrainte `uq_articles_code` empêche tout doublon (la ligne concernée devient un conflit).
- Les traductions malgaches de `articles.importReport.*` sont à faire relire.
- Les tests n'ont pas été exécutés : voir le scénario.
