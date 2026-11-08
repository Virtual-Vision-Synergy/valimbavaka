# Guide d'intégration — ART-06 · Fiche article unifiée (360°)

Statut : ✅ rédigé (tests à lancer en CI) · Ticket : [#26](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/26) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La page `/articles/:id` n'affiche plus de données de démonstration : elle lit en **un seul appel** `GET /api/v1/articles/{id}/overview` l'identité de l'article, son stock par emplacement, les promotions actives qui le visent, les 10 derniers lots de production où il entre ou sort et ses 10 derniers mouvements de stock (le « journal de l'article »). Lecture seule, aucune migration ni permission nouvelle (`articles.read`). Les liens existants vers `/articles/<uuid>` (recherche globale SYS-03, cockpit DSH-03) ouvrent désormais la vraie fiche.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/articles/{articleId}/overview` | `articles.read` (ADMIN, SELLER) | — | 200 `ArticleOverviewRDto` | 400 (UUID mal formé), 401, 403, 404 (article inconnu) |

Contrôleur : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/articleoverview/controller/ArticleOverviewController.java:44`.

- `ArticleOverviewRDto` (`…/articleoverview/dto/ArticleOverviewRDto.java`) :
  - `article` : `ArticleRDto` (ART-01), actif ou désactivé ;
  - `stock` : `StockRDto` (STK-01), une entrée par emplacement actif, zéro compris ;
  - `activePromotions` : `PromotionRDto` (PRO-01) actives **aujourd'hui à Antananarivo** dont le périmètre atteint l'article (`ALL`, `RANGES` contenant sa gamme, `ARTICLES` le contenant), fin la plus proche d'abord ; toujours vide pour une matière première ou un semi-fini ;
  - `productionBatches` : 10 lots au plus, plus récents d'abord (`createdAt`), en `ArticleBatchRDto` ;
  - `recentMovements` : 10 `StockMovementRDto` au plus, plus récents d'abord (`occurredAt`), tous emplacements.
- `ArticleBatchRDto` (`…/articleoverview/dto/ArticleBatchRDto.java`) : `id`, `number`, `status` (`PLANNED` / `IN_PROGRESS` / `COMPLETED`), `role` (`OUTPUT` = l'article est le produit obtenu, `INPUT` = il est consommé), `quantity` (quantité **de cet article** dans le lot), `outputArticleId`, `outputArticleCode`, `outputArticleName`, `createdAt`, `completedAt`.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.articleoverview.service.ArticleOverviewService` | `getOverview(UUID articleId)` | Fiche complète ; 404 avant toute autre lecture si l'article est inconnu. |
| `feature.promotion.service.PromotionService` | `getActivePromotionsFor(UUID articleId, @Nullable UUID rangeId)` (`PromotionService.java:29`) | Promotions actives aujourd'hui qui atteignent un article ; requête `PromotionRepository.findActiveTargeting` (`PromotionRepository.java:64`). Ne tient pas compte du plafond d'utilisation ni du canal. |
| `feature.production.service.ProductionBatchService` | `getRecentBatchesOf(UUID articleId, int limit)` (`ProductionBatchService.java:23`) | Lots où l'article est produit ou entrant ; requête `ProductionBatchRepository.findInvolving` (`ProductionBatchRepository.java:69`). |
| `feature.stock.service.StockMovementService` | `getRecentMovements(UUID articleId, int limit)` (`StockMovementService.java:31`) | Derniers mouvements d'un article ; requête dérivée `StockMovementRepository.findByArticleIdOrderByOccurredAtDescIdDesc`. |

La nouvelle feature `articleoverview` dépend de `article`, `stock`, `promotion` et `production` ; aucune d'elles ne dépend d'elle (pas de cycle, vérifié par `ArchitectureTest`). Elle n'a ni entité ni repository.

Le nom du produit obtenu par un lot vient de `ArticleService.getArticlesByIds` (une seule lecture pour tous les lots). Lors du merge de `develop`, les deux variantes de cette méthode (TRF-04 et PRO-01) ont été fusionnées en **une seule** (`ArticleService.java:33`, implémentation avec garde-fou « liste vide » et `ArticleRepository.findByIdIn`) : ne pas en réintroduire une autre.

### Frontend

- Route `/articles/:id` (`features/articles/articles.routes.ts`) : marqueur « En construction » retiré.
- `features/article/data-access/article-service.ts:63` : `overview(id)` → `ArticleOverviewRDto`.
- `features/article/data-access/article-models.ts` : `ArticleOverviewRDto`, `ArticleBatchRDto`, `ArticleBatchRole` ; `features/stocks/data-access/stock-models.ts` : `StockMovementRDto`, `StockMovementType`.
- `features/articles/data-access/article-service.ts` : `sheet(id)` = `overview(id)` + gammes du référentiel, transformés par `toArticleSheet` (`features/articles/data-access/article-sheet.ts`) en `ArticleSheet` (marge, valeur au coût, statut de stock, libellés d'emplacement, remise via `discountLabel` de PRO-04). `get(id)` et la fiche de démonstration (`articleDetailFixture`, `ArticleDetailRDto`) sont supprimés ; `list()` reste sur les fixtures (ART-05).
- `features/articles/data-access/article-tones.ts` : `BATCH_STATUS_TONES`.
- `features/articles/data-access/article-sheet-testing.ts` : `buildOverview()`, `SHEET_RANGES` pour les tests.
- i18n : `articles.detail.*` revu (fr, mg) : `role.*`, `batchStatus.*`, `movementType.*`, `unit.*`, `noPromotion`, `noMovement`, `inactive`, `allStocks`, `promoCode` ; clés de la maquette sans données retirées (`sold30`, `coverage`, `curing`, `endedPromotions`…).

### Données

Aucune migration, aucune écriture, aucune ligne de journal (lecture seule).

## Règles métier à respecter par les autres tickets

- **Une seule source par bloc** : le stock reste la somme des mouvements (STK-01), le statut d'une promotion se déduit de sa période (PRO-01/PRO-04), un lot est lu par `ProductionBatchService`. La fiche ne recalcule rien de son côté.
- **Promotions** : seul un produit fini se vend, donc seul lui affiche des promotions (`ArticleOverviewServiceImpl.java:66`). Si PRO-03 ajoute un statut « épuisée », l'appliquer aussi dans `PromotionRepository.findActiveTargeting`.
- **Statut de stock affiché** : rupture si total ≤ 0, « stock faible » si total < seuil, sinon disponible (`stockStatusOf`, même règle que la liste ART-03).
- **Lots** : la quantité affichée est celle de l'article (produite ou consommée), pas celle du lot.

## Fichiers concernés

- Backend (nouveau) : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/articleoverview/` (`controller/ArticleOverviewController.java`, `service/ArticleOverviewService.java`, `service/impl/ArticleOverviewServiceImpl.java`, `dto/ArticleOverviewRDto.java`, `dto/ArticleBatchRDto.java`, `domain/ArticleBatchRole.java`).
- Backend (modifié) : `feature/promotion/{repository/PromotionRepository,service/PromotionService,service/impl/PromotionServiceImpl}.java`, `feature/production/{repository/ProductionBatchRepository,service/ProductionBatchService,service/impl/ProductionBatchServiceImpl}.java`, `feature/stock/{repository/StockMovementRepository,service/StockMovementService,service/impl/StockMovementServiceImpl}.java`, `feature/article/{repository/ArticleRepository,service/ArticleService,service/impl/ArticleServiceImpl}.java` (fusion de `getArticlesByIds`).
- Frontend : `valimbavaka-ui/src/app/features/articles/` (page `article-detail-page`, `data-access/article-sheet.ts`, `article-service.ts`, `article-tones.ts`, `article-models.ts`, `article-fixtures.ts`, `articles.routes.ts`), `features/article/data-access/article-{models,service}.ts`, `features/stocks/data-access/stock-models.ts`, `public/i18n/{fr,mg}.json`.
- Tests : voir [scénario](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** :
- [JNL-01](../JNL-01/guide.md) (journal ; ici, le « journal de l'article » est celui de ses mouvements de stock)
- [ART-01](../ART-01/guide.md) · [STK-01](../STK-01/guide.md) · [PRD-01](../PRD-01/guide.md) (et PRD-02 pour la validation des lots) · [PRO-01](../PRO-01/guide.md) · [PRO-04](../PRO-04/guide.md)

**Ces tickets s'appuient sur lui** :
- SYS-03 (recherche globale) et DSH-03 (alertes du cockpit), qui ouvrent `/articles/<uuid>`.

## Comment l'utiliser depuis un autre ticket

```ts
// Ouvrir la fiche : un simple lien suffit, la page charge tout.
<a [routerLink]="['/articles', article.id]">{{ article.code }}</a>

// Lire la fiche ailleurs (ex. un panneau latéral)
inject(ArticleService /* features/article */).overview(articleId).subscribe((sheet) => sheet.stock.total);
```

## Points d'attention

- **PR empilée** sur `feature/PRO-04-liste-detail-promotions` (elle-même sur PRO-01) et `develop` a été fusionné dans la branche (production, transferts) : le diff de la PR contient ces commits tant que PRO-04 n'est pas mergée.
- La liste des articles (ART-05) et l'index de la recherche globale (`core/layout/search-dialog/search-index.ts`) utilisent encore les identifiants de démonstration : un clic depuis ces écrans ouvre la fiche en état d'erreur (« Réessayer ») tant qu'ART-05 / SYS-03 ne lisent pas l'API.
- Le « journal » de la fiche = les 10 derniers **mouvements de stock** de l'article (comme la maquette). Les lignes `ARTICLE` du journal (création, modification) ne sont pas affichées : `GET /api/v1/journal` n'a pas de filtre par ressource et est réservé à l'administrateur (`journal.read`) ; à ajouter avec JNL-03 si besoin.
- Indicateurs de la maquette sans source pour l'instant : ventes sur 30 jours et couverture (ventes VTE-xx), coût unitaire de revient d'un lot (PRD-05, encore en PR), nom de la vendeuse du lot. Ils ont été retirés de l'écran plutôt qu'inventés.
- Les promotions affichées ne tiennent pas compte du plafond d'utilisation ni du canal de vente : c'est la caisse (PRO-02, VTE-04) qui décide de l'application.
