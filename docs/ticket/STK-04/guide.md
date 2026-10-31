# Guide d'intégration — STK-04 · Alerte de seuil minimum

Statut : ✅ rédigé (tests à exécuter) · Ticket : [#33](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/33) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le serveur liste les articles actifs dont le stock total est sous leur seuil minimum, en commençant par les plus
urgents. Pour chaque article, il renvoie le stock actuel, le seuil, le manque et une quantité suggérée à commander.
La page des stocks affiche cette liste au-dessus du tableau. STK-05 (bon de réapprovisionnement), FRS-04 et DSH-03
peuvent repartir de cette liste au lieu de recalculer les seuils.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/stocks/alerts?page&size&search` | `stocks.read` (ADMIN, SELLER) | `PageQuery` (`sortBy` ignoré) | `PageResponse<StockAlertRDto>` | 401, 403, 422 (pagination) |

`StockAlertRDto` (`feature/stock/dto/StockAlertRDto.java`) contient `articleId`, `code`, `name`, `unit`,
`quantity`, `threshold`, `shortage` et `suggestedQuantity`. Les quantités sont des `BigDecimal` (3 décimales en base).
Endpoint : `StockController.getAlerts` (`feature/stock/controller/StockController.java:57`).

### Services et ports Java réutilisables

- `StockService.getAlerts(PageQuery)` (`feature/stock/service/StockService.java:24`), implémenté dans `StockServiceImpl.java:80`.
- `ArticleService.getArticlesWithThreshold()` (`feature/article/service/ArticleService.java:33`) renvoie les articles
  actifs de seuil > 0, triés par code. Il s'appuie sur
  `ArticleRepository.findByActiveTrueAndThresholdGreaterThanOrderByCodeAsc` (`ArticleRepository.java:93`).
- Les règles sont dans `StockAlertPolicy` (`feature/stock/service/impl/StockAlertPolicy.java`), une classe
  package-private : `isUnderThreshold`, `toAlert`, `suggestedQuantity` et `MOST_URGENT_FIRST`.

### Frontend (`features/stocks`)

- Route : aucune nouvelle route ; le bloc s'affiche dans la page des stocks existante.
- `StockService.alerts()` (`data-access/stock-service.ts:36`) lit la page 1 de taille 100, sans tri imposé.
  Le type `StockAlertRDto` est défini dans `data-access/stock-models.ts:31`.
- Composant `vvs-threshold-alerts` (`components/threshold-alerts/`) : entrées `alerts` et `total`. Il est
  purement présentationnel et garde l'ordre du serveur.
- `StockPage` charge les alertes avec un `rxResource` (`pages/stock-page/stock-page.ts:67`) et n'affiche le bloc que
  si `totalCount > 0` (`stock-page.html:22`).

### Données

- Ni table ni migration Liquibase : le seuil vient de `articles.threshold` (ART-01) et le stock de
  `stock_movements` (STK-01). La permission `stocks.read` existait déjà.
- Rien n'est écrit dans le journal : l'endpoint est en lecture seule.

## Règles métier pour les autres tickets

- **Condition d'alerte** : l'article est actif, son seuil est > 0 et son stock total est **strictement inférieur**
  au seuil. Un seuil à 0 signifie « pas d'alerte ». Un stock égal au seuil ne déclenche pas d'alerte.
- **Stock total** : somme des soldes des **emplacements actifs**, le même total que `GET /api/v1/stocks`. Un solde
  resté dans un emplacement désactivé n'est pas compté.
- **Manque** = seuil − stock. Il est toujours > 0 puisque le stock n'est jamais négatif (STK-01).
- **Quantité suggérée** = manque arrondi à la dizaine supérieure, avec un minimum de 10 : 0,5 → 10, 12,5 → 20,
  27,5 → 30.
- **Ordre** : ratio stock / seuil croissant (ruptures d'abord), puis code. L'ordre est fixé par le serveur.
- **Recherche** : `search` filtre sur le code ou le nom, sans tenir compte de la casse ni des espaces autour.

## Fichiers concernés

- Back :
  - `feature/stock/dto/StockAlertRDto.java`
  - `feature/stock/service/impl/StockAlertPolicy.java`
  - `StockService`, `StockServiceImpl` et `StockController`
  - `ArticleService`, `ArticleServiceImpl` et `ArticleRepository`
- Tests back : `StockAlertPolicyTest`, `StockServiceImplAlertTest`, `StockAlertIT` et `StockSecurityIT`. Les tests
  ont aussi touché `StockTestData` (seuil en paramètre) et `StockApiTestSupport` (`createArticle(name, cost, threshold)`).
- Front :
  - `components/threshold-alerts/*`
  - `data-access/stock-models.ts`, `data-access/stock-service.ts`
  - `pages/stock-page/stock-page.{ts,html}`
  - `public/i18n/{fr,mg}.json` (clé `stocks.thresholdAlerts`)
- Tests front : `threshold-alerts.spec.ts`, `stock-page.spec.ts` et `stock-service.spec.ts`.

## Dépendances

**Ce ticket s'appuie sur** :
- [STK-01](../STK-01/description.md) · [guide](../STK-01/guide.md)

**Ces tickets s'appuient sur lui** :
- [FRS-04](../FRS-04/description.md) · [guide](../FRS-04/guide.md)
- [STK-05](../STK-05/description.md) · [guide](../STK-05/guide.md)
- [DSH-03](../DSH-03/description.md) · [guide](../DSH-03/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
Back  : stockService.getAlerts(new PageQuery(1, 100, null, null, null)).items()  // StockAlertRDto, plus urgents d'abord
Front : inject(StockService).alerts()                                            // Observable<PageResponse<StockAlertRDto>>
DSH-03 (compteur) : lire totalCount de GET /api/v1/stocks/alerts?size=1
```

## Points d'attention

- **Calcul en mémoire** : tous les articles avec un seuil sont chargés, puis filtrés et paginés en Java. C'est
  adapté à quelques milliers d'articles ; au-delà, il faudra passer à une requête SQL.
- **La quantité suggérée ignore les ventes et le délai fournisseur**. STK-05 pourra l'affiner sans changer le
  contrat. Le bon de réappro du front utilise encore `proposedQuantity` (`stock-levels.ts:31`), qui repose sur
  30 jours de ventes provisoires à 0.
- **Le seuil par emplacement n'a pas été modifié** : la règle `seuil / 4` du front (`thresholdAt`,
  `stock-levels.ts:18`) sert toujours à la jauge et au filtre « Sous le seuil » quand on regarde un seul emplacement.
  L'alerte serveur porte uniquement sur le total de l'article ; il n'existe pas de seuil par emplacement côté métier.
  À trancher avec le client (STK-02 ou STK-05).
- **Visibilité** : la user story parle de l'administrateur, mais l'endpoint suit `stocks.read`, donc la vendeuse voit
  aussi les alertes. Pour la restreindre, il faudra une permission dédiée (migration).
- **Échec de chargement** : si `/alerts` échoue (hors ligne ou erreur 5xx), le bloc ne s'affiche pas, sans message.
  La page des stocks reste utilisable.
- Les tests n'ont pas encore été exécutés : se reporter au scénario.
