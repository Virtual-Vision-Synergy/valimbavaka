# Guide d'intégration — ART-01 · Fiche article complète

Statut : ✅ écrit le 2026-10-26 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#21](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/21) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le catalogue a maintenant une vraie fiche article côté serveur : code (attribué par le serveur), nom, code-barres, type (matière première, semi-fini, produit fini), gamme, unité, coût, prix de vente, seuil minimum, fournisseur et état actif. L'administrateur crée et modifie la fiche, la vendeuse la lit, rien ne se supprime (on désactive) et chaque écriture est tracée dans le journal. L'article référence son fournisseur par `supplierId` : c'est ce lien qui remplit le bloc « articles fournis » de la fiche [FRS-01](../FRS-01/guide.md).

## Contrat exposé

### Endpoints backend (`/api/v1/articles`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/articles` | `articles.read` (admin, vendeuse) | `PageQuery` + `type`, `rangeId`, `supplierId`, `active` | 200 `PageResponse<ArticleRDto>` | 400 (type inconnu), 401, 403, 422 (`size` > 100) |
| GET | `/api/v1/articles/{id}` | `articles.read` | — | 200 `ArticleRDto` | 401, 403, 404 |
| POST | `/api/v1/articles` | `articles.create` (admin) | `ArticleWDto` | 201 `ArticleRDto` + `Location` | 400, 401, 403, 409 (code-barres), 422 |
| PUT | `/api/v1/articles/{id}` | `articles.update` (admin) | `ArticleWDto` | 200 `ArticleRDto` | 400, 401, 403, 404, 409 (code-barres ou version périmée), 422 |
| PATCH | `/api/v1/articles/{id}/status` | `articles.update` | `ArticleStatusWDto` (`active`) | 200 `ArticleRDto` | 401, 403, 404, 422 |

Aucun `DELETE` (le test `ArticleSecurityIT.deleteArticle_isNotExposed` le vérifie).

- Tri autorisé : `createdAt` (défaut, décroissant), `code`, `name`, `type`, `price`. `search` porte sur le nom, le code (`ART-0031`) et le code-barres, sans tenir compte de la casse.
- `ArticleWDto` : `name` (obligatoire, ≤ 120), `barcode` (13 chiffres ou absent), `type` (`RAW_MATERIAL` / `SEMI_FINISHED` / `FINISHED`), `rangeId`, `unit` (`PIECE` / `KILOGRAM` / `LITER`), `cost` (≥ 0, 2 décimales), `price` (≥ 0, 2 décimales), `threshold` (obligatoire, ≥ 0, 3 décimales), `supplierId`, `version` (facultatif). **Ni `id`, ni `code`, ni `active`** : envoyés, ils sont ignorés.
- `ArticleRDto` : `id`, `code`, `name`, `barcode`, `type`, `rangeId`, `unit`, `cost`, `price`, `threshold`, `supplierId`, `active`, `createdAt`, `updatedAt`, `version`.
- `PUT` est un remplacement complet : un champ facultatif absent ou `null` est **effacé**. Toujours renvoyer tous les champs.
- Verrou optimiste : renvoyer `ArticleRDto.version` dans `ArticleWDto.version` ; une valeur périmée donne 409 « L'article a été modifié entre-temps : rechargez la fiche puis réessayez. ».
- `PATCH /status` est idempotent : aucune modification ni ligne de journal si l'état est déjà celui demandé.
- Journal : ressource `ARTICLE`, `CREATE` à la création, `UPDATE` à une modification réelle (aucune ligne si rien ne change) et au changement d'état. Libellé : `Création de l'article ART-0031 · Savon noir 100 g`.

### Services Java réutilisables

- `feature.article.service.ArticleService` : `getArticles(PageQuery, ArticleFilter)`, `getArticleById(UUID)`, `createArticle`, `updateArticle`, `setActive`. `ArticleFilter(type, rangeId, supplierId, active)` est un `record` du paquet `domain` ; un critère `null` veut dire « tous ».
- Contrôles croisés ajoutés par ce ticket aux services des autres features (même schéma que `ReferentialService.isActiveSalesChannel`) :
  - `SupplierService.isActiveSupplier(UUID)` ;
  - `ReferentialService.isActiveProductRange(UUID)`.
- Les autres features ne lisent jamais `ArticleRepository` : pas d'association JPA entre features, on référence un article par son `articleId` (UUID).

### Frontend

- Routes (sous `/articles`, gardées par `articles.read` puis par la permission d'écriture) : `/articles/new` (création, `articles.create`, depuis [ART-02](../ART-02/guide.md) ; c'était `/articles/sheet/new`) et `/articles/:id/edit` (fiche, `articles.update`). Les routes `''` et `:id` de la maquette sont inchangées.
- `features/article/data-access/article-service.ts` : `list(query)`, `getById`, `create`, `update`, `setActive`. Types dans `article-models.ts` (`ArticleRDto`, `ArticleWDto`, `ArticleListQuery`, `ArticleStatusWDto`, `ArticleType`, `ArticleUnit`). Attention : `features/articles/` (avec un « s ») contient la **maquette** sur données de démonstration, avec ses propres types ; ne pas les mélanger.
- `features/article/pages/article-sheet-page` : un seul formulaire pour créer (sans `id`) et modifier (`id` lié en `input()`). Gamme et prix n'apparaissent que pour un produit fini ; la version lue est renvoyée ; le message du `ProblemDetail` s'affiche en cas d'erreur sans perdre la saisie.
- Bloc « articles fournis » de `features/supplier/pages/supplier-form-page` : liste code + nom des articles d'un fournisseur (lien vers la fiche), lue avec `ArticleService.list({ supplierId })`.
- Traductions : namespace `article.*` dans `public/i18n/fr.json` et `mg.json` ; `supplier.articles.empty` reformulé.

### Données

- Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_26-01-changelog.sql` : séquence `article_code_seq`, table `articles` (`id` UUID, `code` unique, `name`, `barcode`, `type`, `range_id`, `unit`, `cost`, `price`, `threshold`, `supplier_id`, `is_active`, audit, `version`), contraintes `ck_articles_type`, `ck_articles_unit`, `ck_articles_cost`, `ck_articles_price`, `ck_articles_threshold`, clés étrangères vers `product_ranges` et `suppliers`, index unique partiel `uq_articles_barcode` (valeurs renseignées seulement), index sur `supplier_id` et `range_id`, permissions `articles.create` et `articles.update` données à `ADMIN`.
- `articles.read` (AUTH-01) reste donnée à `ADMIN` et `SELLER`.

## Règles métier à respecter par les autres tickets

- **Type d'article** : `RAW_MATERIAL` (MP), `SEMI_FINISHED` (SF), `FINISHED` (produit fini). C'est le seul champ qui décide de l'usage en production et en vente ([ART-04](../ART-04/description.md) : MP/SF entrants, produits finis sortants). Ne pas rajouter de drapeau parallèle.
- **Gamme et prix de vente ne concernent que le produit fini** : obligatoires pour lui (422 sinon), remis à `null` pour MP et SF, même si le client les envoie. Une vente, une promotion ou une commande ne doit proposer que des articles `FINISHED` et `active = true`.
- Un article désactivé reste lisible et garde son historique ; les écrans de sélection (vente, production, réapprovisionnement) doivent filtrer sur `active = true` (`GET /articles?active=true`).
- Le code `ART-nnnn` vient d'une séquence : stable, jamais réutilisé, avec des trous possibles. Référencer un article par son **UUID**, pas par son code ni son nom (le nom n'est pas unique et reste modifiable).
- La gamme est un UUID du référentiel SYS-05 (`/api/v1/product-ranges`) : afficher son libellé en le résolvant côté client, ne pas coder « Botanika » en dur ([ART-03](../ART-03/description.md) pour la couleur de la gamme).
- Le fournisseur n'est contrôlé que s'il **change** : une fiche dont le fournisseur a été désactivé depuis reste modifiable. Pour un réapprovisionnement ([STK-05](../STK-05/description.md)), filtrer les articles d'un fournisseur par `supplierId`.
- Écrire dans le journal avec `JournalService.record` dans la même transaction, comme le fait `ArticleServiceImpl` ([guide JNL-01](../JNL-01/guide.md)).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article/`)
- `controller/ArticleController.java`
- `domain/Article.java`, `ArticleDetails.java`, `ArticleFilter.java`, `ArticleType.java`, `ArticleUnit.java`
- `dto/ArticleRDto.java`, `ArticleWDto.java`, `ArticleStatusWDto.java`, `mapper/ArticleMapper.java`
- `repository/ArticleRepository.java`
- `service/ArticleService.java`, `service/impl/ArticleServiceImpl.java`, `service/impl/ArticleReferenceChecker.java`

**Modifiés dans les autres features** : `feature/supplier/service/SupplierService.java` (+ `impl`, `SupplierRepository.existsByIdAndActiveTrue`), `feature/referential/service/ReferentialService.java` (+ `impl`, `ProductRangeRepository.existsByIdAndActiveTrue`).

**Migration** : `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_26-01-changelog.sql`, déclarée dans `db.changelog-master.yaml`.

**Frontend** (`valimbavaka-ui/src/app/features/article/`) : `data-access/article-models.ts`, `data-access/article-service.ts`, `pages/article-sheet-page/*` ; routes ajoutées dans `features/articles/articles.routes.ts` ; bloc « articles fournis » dans `features/supplier/pages/supplier-form-page/*`.

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/article/domain/ArticleTest.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/article/service/impl/ArticleServiceImplTest.java`, `ArticleReferenceCheckerTest.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/article/repository/ArticleRepositoryIT.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/article/controller/ArticleSecurityIT.java`, `ArticleCreationIT.java`, `ArticleUpdateIT.java` (socle `ArticleApiTestSupport.java`)
- `valimbavaka-ui/src/app/features/article/data-access/article-service.spec.ts`, `pages/article-sheet-page/article-sheet-page.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** : [FRS-01](../FRS-01/guide.md) (fournisseur référencé par UUID, bloc « articles fournis »), [JNL-01](../JNL-01/guide.md) (journal), [SYS-05](../SYS-05/guide.md) (gammes, rôles), [AUTH-01](../AUTH-01/guide.md) (permissions `articles.*`).

**Ces tickets s'appuient sur lui** : [ART-02](../ART-02/description.md), [ART-03](../ART-03/description.md), [ART-04](../ART-04/description.md), [ART-05](../ART-05/description.md), [ART-07](../ART-07/description.md), [ART-08](../ART-08/description.md), [SRV-01](../SRV-01/description.md), [STK-01](../STK-01/description.md), [LIV-01](../LIV-01/description.md), [PRO-01](../PRO-01/description.md), [VTE-01](../VTE-01/description.md), [SYS-03](../SYS-03/description.md).

## Comment l'utiliser depuis un autre ticket

```ts
// Sélecteur de produits finis actifs (ex. vente, promotion)
this.articleService
  .list({ page: 1, size: 100, sortBy: 'name', type: 'FINISHED', active: true })
  .subscribe((page) => this.articles.set(page.items));
```

```java
// Vérifier qu'un article existe depuis une autre feature : passer par ArticleService, jamais par le repository
ArticleRDto article = articleService.getArticleById(articleId); // 404 NotFoundException si inconnu
```

Appel direct : `GET /api/v1/articles?supplierId=<uuid>&active=true` pour les articles d'un fournisseur.

## Points d'attention

- **Ce qui reste de la maquette** : la liste `/articles` et le détail `/articles/:id` utilisent encore des données de démonstration (`features/articles/data-access/article-fixtures.ts`). [ART-02](../ART-02/guide.md) a remplacé le formulaire de maquette par `article-sheet-page` ; [ART-05](../ART-05/description.md) branche la liste sur `GET /api/v1/articles` ; [ART-06](../ART-06/description.md) la fiche 360°.
- La maquette connaît un type `PACKAGING` (emballage) et des drapeaux `manufactured` / `salonUsable` qui ne font pas partie du contrat d'ART-01 (MP / SF / Fini) ; à trancher avec le métier avant [ART-04](../ART-04/description.md).
- Pas de stock total, d'état de stock ni de lots dans `ArticleRDto` : ils viendront de [STK-01](../STK-01/description.md).
- Le coût d'achat (`cost`) est visible de toute personne qui a `articles.read`, donc aussi de la vendeuse ; à masquer si le métier le demande.
- Le code-barres est contrôlé par son format (13 chiffres), pas par la clé de contrôle EAN-13 (la maquette `ean13.ts` la calcule côté client).
- `ArticleRepository.search` a 6 paramètres, au-dessus de la limite de 4 de la convention (simple liaison de requête) ; à justifier dans la PR.
- L'écriture n'utilise pas `OutboxService` : un article ne peut pas être saisi hors ligne ([SYS-07](../SYS-07/description.md)).
- Photo de l'article : la maquette a un composant de dépôt de photo, sans stockage serveur ; hors périmètre d'ART-01.
- Les tests n'ont pas été exécutés par l'auteur : voir le scénario avant de s'appuyer sur ce guide pour un comportement limite.
