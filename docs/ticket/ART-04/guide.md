# Guide d'intégration — ART-04 · Type d'article et usage en production

Statut : ✅ rédigé le 2026-10-28 (tests écrits, pas encore exécutés) · Ticket : [#24](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/24) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le type d'article (ART-01) décide seul de son usage en production : les matières premières (MP) et les semi-finis (SF)
sont des **entrants**, les produits finis des **sortants**. ART-04 expose une liste paginée des articles actifs pour
chaque usage (sélecteurs du lot de production) et un contrôle serveur que PRD-01 appelle avant d'enregistrer un lot.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/articles/production?usage=INPUT\|OUTPUT` + `page`, `size`, `search`, `sortBy`, `sortDirection` | `articles.read` | paramètres de requête (`PageQuery` + `ProductionUsage`) | `PageResponse<ArticleRDto>` (articles actifs seulement, tri par défaut `name` ASC) | 400 `MALFORMED_REQUEST` (usage absent ou inconnu), 401, 403 |

Contrôleur : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article/controller/ArticleController.java:73`.
La recherche est la même que la liste ART-01 (nom, code, code-barres, insensible à la casse).

### Services et ports Java réutilisables

- `ProductionUsage` (`feature/article/domain/ProductionUsage.java:8`) : `INPUT` = `{RAW_MATERIAL, SEMI_FINISHED}`,
  `OUTPUT` = `{FINISHED}` ; `accepts(ArticleType)` et `types()`.
- `ArticleService.getProductionArticles(PageQuery, ProductionUsage)` (`feature/article/service/ArticleService.java:19`) :
  la liste ci-dessus.
- `ArticleService.requireProductionArticle(UUID, ProductionUsage)` (`ArticleService.java:25`, implémentation
  `service/impl/ArticleServiceImpl.java:81`) : renvoie l'`ArticleRDto` si l'article est actif et de bon type ;
  sinon `NotFoundException` (404) ou `BusinessException` (422 `VALIDATION_FAILED`, messages en
  `ArticleServiceImpl.java:39`).
- Requête : `ArticleRepository.searchActiveByTypes` (`feature/article/repository/ArticleRepository.java:75`), interne
  à la feature article.

### Frontend

- Routes : aucune (pas d'écran dans ce ticket).
- Services / stores (`data-access`) : `ArticleService.listForProduction(usage, query)`
  (`valimbavaka-ui/src/app/features/article/data-access/article-service.ts:42`) ; type `ProductionUsage`
  (`article-models.ts:7`).
- Composants réutilisables : aucun.

### Données

- Tables et migrations Liquibase : aucune (le type existe déjà dans `articles.type`, ART-01).
- Écriture dans le journal : aucune (lecture seule).

## Règles métier à respecter par les autres tickets

- L'usage se déduit du **type uniquement** : ne pas ajouter de drapeau « fabriqué » ni « utilisable en production ».
- Un article **désactivé** n'est ni entrant ni sortant.
- Le serveur décide : un lot de production doit appeler `requireProductionArticle` pour chaque ligne, même si l'écran
  n'a proposé que des articles de la bonne liste (un client peut envoyer n'importe quel identifiant).
- Lecture autorisée à tout rôle qui a `articles.read` (administrateur, vendeuse), refusée au livreur.

## Fichiers concernés

- Backend (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article/`) : `domain/ProductionUsage.java`
  (créé), `repository/ArticleRepository.java`, `service/ArticleService.java`, `service/impl/ArticleServiceImpl.java`,
  `controller/ArticleController.java`.
- Tests : `ProductionUsageTest`, `ArticleProductionServiceTest`, `ArticleProductionIT`, `article-service.spec.ts`.
- Frontend : `valimbavaka-ui/src/app/features/article/data-access/article-service.ts`, `article-models.ts`.
  La maquette `features/articles/` n'est pas modifiée.

## Dépendances

**Ce ticket s'appuie sur** :
- [ART-01](../ART-01/description.md) · [guide](../ART-01/guide.md)

**Ces tickets s'appuient sur lui** :
- [PRD-01](../PRD-01/description.md) · [guide](../PRD-01/guide.md)

## Comment l'utiliser depuis un autre ticket

```java
// Service de PRD-01 : contrôle de chaque ligne du lot (404 / 422 si refusé)
articleService.requireProductionArticle(line.articleId(), ProductionUsage.INPUT);
articleService.requireProductionArticle(request.outputArticleId(), ProductionUsage.OUTPUT);
```

```ts
// Sélecteur d'entrants dans l'écran du lot
this.articleService.listForProduction('INPUT', { page: 1, size: 20, search: term });
```

## Points d'attention

- Un semi-fini n'est **pas** un sortant : les critères le classent seulement comme entrant. Si le métier veut produire
  des semi-finis, ajouter `SEMI_FINISHED` à `ProductionUsage.OUTPUT` et adapter
  `ProductionUsageTest.types_everyArticleType_hasExactlyOneUsage` (décision à prendre dans PRD-01).
- La maquette `features/articles/` contient encore `PACKAGING`, `manufactured` et `salonUsable` : décision du
  2026-10-28, on les ignore, seuls les 3 types du backend comptent. Ajouter un type exigerait une migration de la
  contrainte CHECK et de le classer dans `ProductionUsage` (le test ci-dessus échoue tant que ce n'est pas fait).
- Pas de cache côté front : chaque ouverture de sélecteur relit le serveur. Hors ligne, la lecture échoue (pas de file
  pour les lectures).
- Le formatage Spotless n'a pas été appliqué en local : lancer `mvn spotless:apply` si la CI le signale.
