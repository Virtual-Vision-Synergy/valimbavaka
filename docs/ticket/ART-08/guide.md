# Guide d'intégration — ART-08 · Export de la liste des articles

Statut : ✅ écrit le 2026-10-29 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#28](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/28) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur télécharge la liste des articles en XLSX depuis le bouton « Exporter » de la page liste, avec les filtres affichés (recherche, type, gamme). Le fichier ne contient jamais le coût d'achat ni les identifiants internes. C'est le premier usage réel de la brique d'export de [SYS-01](../SYS-01/guide.md) et un modèle pour FAC-06, COM-05 et RPT-06.

## Contrat exposé

### Endpoints backend (`/api/v1/articles`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/articles/exportable` | `articles.export` (administrateur) | `PageQuery` + `type`, `rangeId`, `supplierId`, `active` | 200 `PageResponse<ArticleExportRDto>` (aperçu JSON des lignes) | 400 (type inconnu), 401, 403, 422 |
| GET | `/api/v1/articles/exportable/export` | `articles.export` | mêmes paramètres | 200 `articles.xlsx`, onglet « Articles » | 400, 401, 403, 422 (`size` > 10 000 ou plus de 10 000 lignes) |

La seconde route est la route jumelle générée par `@ExcelExport` sur `ArticleController.getExportableArticles`. Elle est volontairement distincte de `GET /api/v1/articles` : la route jumelle hérite de la permission de la méthode annotée, et la liste reste ouverte à la vendeuse (`articles.read`). Sans `size`, l'export renvoie jusqu'à 10 000 lignes (réglage `vvs.excel.max-rows`).

### Services et ports Java réutilisables

- `ArticleService.getExportableArticles(PageQuery, ArticleFilter)` : mêmes filtres et même recherche que `getArticles`, tri par code croissant par défaut ; renvoie des `ArticleExportRDto`.
- `ArticleExportRDto` : seuls les champs portant `@ExcelColumn` sortent (code, nom, code-barres, type, unité, prix, seuil, actif). Ajouter une colonne = annoter un champ ici, jamais `cost`.
- `ArticleMapper.toExportRDto`.

### Frontend

- `ArticleService.exportList(query: ArticleExportQuery)` (`features/article/data-access/article-service.ts`) : `GET …/exportable/export` en `blob`, critères non vides seulement, aucune pagination.
- `downloadFile` / `filenameOf` (`shared/data-access/download-file.ts`) : réutilisables pour tout export XLSX (nom lu dans `Content-Disposition`).
- Page `article-list-page` : méthode `exportList()` et signal `exporting` ; le bouton n'apparaît qu'avec `articles.export` (`*vvsHasPermission`). Clé de traduction `articles.exportFailed` (fr, mg).

### Données

- Migration `2026_10_29-01-changelog.sql` : permission `articles.export`, donnée au seul rôle ADMIN (rollback fourni).
- Journal : aucune ligne écrite (voir « Points d'attention »).

## Règles métier à respecter par les autres tickets

- Le coût d'achat (`cost`) ne sort jamais dans un fichier d'export d'articles.
- L'export est réservé à `articles.export` ; la vendeuse lit la liste mais ne télécharge pas.
- L'export reprend les critères de la liste ; sans critère, tous les articles (actifs et inactifs), dans la limite de 10 000 lignes.
- Un autre ticket qui a besoin de la même liste (ex. STK-01) réutilise `getExportableArticles` plutôt que de recréer une requête.
- `ADMIN` dans `core/auth/auth-testing.ts` a reçu `articles.export` : les tests de rôle doivent rester alignés sur le seed Liquibase.

## Fichiers concernés

**Backend**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article/controller/ArticleController.java` (`getExportableArticles`)
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article/dto/ArticleExportRDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article/service/ArticleService.java` et `service/impl/ArticleServiceImpl.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article/mapper/ArticleMapper.java`
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_29-01-changelog.sql` et `db.changelog-master.yaml`

**Frontend**
- `valimbavaka-ui/src/app/features/article/data-access/article-service.ts`, `article-models.ts`
- `valimbavaka-ui/src/app/features/articles/pages/article-list-page/article-list-page.ts` et `.html`
- `valimbavaka-ui/src/app/shared/data-access/download-file.ts`
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json`

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/article/controller/ArticleExportIT.java`
- `valimbavaka-ui/src/app/features/article/data-access/article-service.spec.ts`
- `valimbavaka-ui/src/app/features/articles/pages/article-list-page/article-list-page.export.spec.ts`
- `valimbavaka-ui/src/app/shared/data-access/download-file.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [ART-01](../ART-01/description.md) · [guide](../ART-01/guide.md)
- [SYS-01](../SYS-01/description.md) · [guide](../SYS-01/guide.md)

**Ces tickets s'appuient sur lui** : aucun.

## Comment l'utiliser depuis un autre ticket

```ts
this.articleService
  .exportList({ search: 'savon', type: 'FINISHED' })
  .subscribe((response) => downloadFile(response, 'articles.xlsx'));
```

## Points d'attention

- **Journal non alimenté** : `journal_entries` n'accepte que CREATE / UPDATE / DELETE (CHECK de `2026_10_19-02`). Tracer un export demande une action `EXPORT` (migration + `JournalAction`) et un écouteur de `ExcelExportedEvent` ; à confier au ticket journal (JNL).
- **Pas de PDF**, voir [SYS-01](../SYS-01/guide.md).
- **Route `/exportable`** : choix d'ingénierie dû à la route jumelle de SYS-01 (permission héritée). Si SYS-01 permettait une permission propre à la route jumelle, l'export pourrait revenir sur `GET /api/v1/articles/export`.
- Flux limité à une page de 10 000 lignes (`Page`, pas de `Stream`) : pas de souci de transaction ouverte.
- Frontend non compilé ni testé dans cet environnement (`node_modules` absent) : à vérifier par la CI.
