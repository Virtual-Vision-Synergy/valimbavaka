# Guide d'intégration — DSH-03 · Alertes de stock sur le cockpit

Statut : ✅ rédigé (tests verts en CI) · Ticket : [#96](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/96) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le cockpit (`/dashboard`) affiche les articles sous leur seuil minimum calculés par le serveur (STK-04), dans l'ordre
du serveur (ruptures d'abord). Chaque nom d'article est un lien vers sa fiche `/articles/<id>`. Le bloc est chargé à part
de l'agrégat du cockpit : il fonctionne dès maintenant, alors que le reste du cockpit (DSH-01) est encore une maquette.

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint. Le cockpit consomme celui de STK-04 :

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/stocks/alerts?page=1&size=100` | `stocks.read` (ADMIN, SELLER) | `PageQuery` | `PageResponse<StockAlertRDto>` | 401, 403, 422 |

Le dossier `feature/dashboard` côté backend n'est **pas** créé par ce ticket (voir « Points d'attention »).

### Services et ports Java réutilisables

- Aucun. Côté serveur, réutiliser `StockService.getAlerts(PageQuery)` / `getAllAlerts()` (STK-04, STK-05).

### Frontend (`features/dashboard`)

- Route : aucune nouvelle ; le bloc est dans `DashboardPage` (onglet « Vue d'ensemble » limité à 3 lignes,
  onglet « Analyse » sans limite), `pages/dashboard-page/dashboard-page.html:43` et `:64`.
- Données : `DashboardPage.stockAlerts` (`pages/dashboard-page/dashboard-page.ts:89`), un `rxResource` sur
  `StockService.alerts()` de `features/stocks/data-access/stock-service.ts` (STK-04). Il ne part que si
  l'utilisateur a `stocks.read` (`canSeeStockAlerts`, `:82`) ; sinon la ressource reste au repos et le bloc n'est pas
  rendu. `stockAlertsPage` / `stockAlertsFailed` (`:93`, `:96`) évitent de lire `value()` en état d'erreur.
- Composant `vvs-stock-alerts-card` (`components/stock-alerts-card/stock-alerts-card.ts`), présentationnel :
  - entrées `page: PageResponse<StockAlertRDto> | undefined` (absent = squelette), `failed` (état d'erreur),
    `limit` (nombre de lignes affichées) ; sortie `retry` (bouton « Réessayer ») ;
  - colonnes Article (lien + code), Stock / seuil (jauge), À commander (`suggestedQuantity` + unité), État
    (« Rupture » si stock ≤ 0, sinon « Stock faible ») ;
  - sous-titre = `totalCount` du serveur, pas le nombre de lignes affichées ;
  - « Tout voir » → `/stocks` ; « Générer le bon de réappro. » → `/stocks`, visible avec `stocks.restock` seulement
    (`stock-alerts-card.html:16`).
- Le type `StockAlertRDto` est celui de `features/stocks/data-access/stock-models.ts` ; l'ancien type simulé du même
  nom (gamme, emplacement) et le champ `DashboardRDto.stockAlerts` ont été supprimés de `dashboard-models.ts` et des
  fixtures.
- Traductions `dashboard.stockAlerts.*` (fr, mg) : `suggested`, `emptyTitle`, `emptyMessage` ajoutées ; `range`,
  `location` supprimées ; `subtitle` passe à « {{count}} article(s) sous le seuil minimum ».

### Données

- Ni table, ni migration, ni permission nouvelle. Rien n'est écrit au journal (lecture seule).

## Règles métier à respecter par les autres tickets

- **Source unique** : les alertes du cockpit sont celles de STK-04 (actif, seuil > 0, stock total < seuil). Ne pas
  les recalculer ailleurs (ni dans l'agrégat DSH-01, ni côté front).
- **Ordre** : celui du serveur ; le front ne retrie pas.
- **Droits** : visible avec `stocks.read` (administrateur et vendeuse) ; le livreur a `dashboard.read` sans
  `stocks.read` : aucun appel, pas de bloc. Le raccourci de réappro suit `stocks.restock` (STK-05).
- **Lien** : toujours `['/articles', articleId]` (UUID serveur), comme la liste des articles.

## Fichiers concernés

- Front :
  - `valimbavaka-ui/src/app/features/dashboard/components/stock-alerts-card/stock-alerts-card.{ts,html,css}`
  - `valimbavaka-ui/src/app/features/dashboard/pages/dashboard-page/dashboard-page.{ts,html}`
  - `valimbavaka-ui/src/app/features/dashboard/data-access/dashboard-models.ts`, `dashboard-fixtures.ts`
  - `valimbavaka-ui/public/i18n/{fr,mg}.json`
- Tests : `stock-alerts-card.spec.ts`, `dashboard-page.spec.ts`, `features/demo-pages.a11y.spec.ts` (page
  `dashboard` ajoutée). Détail dans le [scénario](scenario-test.md#tests-existants).
- Back : aucun.

## Dépendances

**Ce ticket s'appuie sur** :
- [STK-04](../STK-04/description.md) · [guide](../STK-04/guide.md) : endpoint et calcul des alertes.
- [STK-05](../STK-05/description.md) · [guide](../STK-05/guide.md) : permission `stocks.restock` du raccourci.

**Ces tickets s'appuient sur lui** :
- [DSH-01](../DSH-01/description.md) · [guide](../DSH-01/guide.md) : intégration dans le cockpit réel (ci-dessous).

## Comment l'utiliser depuis un autre ticket

```html
<vvs-stock-alerts-card [page]="stockAlertsPage()" [failed]="stockAlertsFailed()" [limit]="3"
  (retry)="stockAlerts.reload()" />
```

### Intégration par DSH-01 (cockpit journalier)

1. Quand DSH-01 remplace `DashboardService.get()` par `GET /api/v1/dashboard`, **ne pas** remettre de champ
   `stockAlerts` dans `DashboardRDto` : le bloc garde son propre appel. Raisons : droits différents (`stocks.read`
   contre `dashboard.read`), un échec des alertes ne casse pas le cockpit, et le calcul reste celui de STK-04.
2. Garder `stockAlerts`, `canSeeStockAlerts`, `stockAlertsPage` et `stockAlertsFailed` de `DashboardPage` tels quels ;
   seule la ressource `dashboard` change de source.
3. Si DSH-01 veut un compteur dans les KPI ou le bandeau, lire `stockAlertsPage()?.totalCount` (déjà chargé) plutôt
   qu'un second appel ; à défaut, `GET /api/v1/stocks/alerts?size=1` puis `totalCount` (guide STK-04).
4. Si l'agrégat serveur doit malgré tout embarquer les alertes (un seul appel), le service `dashboard` appellera
   `StockService.getAlerts(new PageQuery(1, 3, null, null, null))` et renverra la page telle quelle, en ne la
   remplissant que si l'utilisateur a `stocks.read` ; le composant n'a alors rien à changer (même `PageResponse`).

## Points d'attention

- **Hypothèse : pas de backend `feature/dashboard`** pour ce ticket. L'endpoint STK-04 donne déjà tout ce que le bloc
  affiche ; créer `/api/v1/dashboard/stock-alerts` aurait dupliqué le contrat. DSH-01 créera le module s'il en a besoin.
- **Hypothèse : visibles par la vendeuse**, comme sur la page des stocks (`stocks.read`), alors que la user story
  parle de l'administrateur. Pour restreindre, il faudra une permission dédiée (migration), à décider avec le client.
- **Colonnes gamme et emplacement retirées** de la maquette : l'alerte porte sur le stock total de l'article (pas par
  emplacement, STK-04) et `StockAlertRDto` n'a pas de gamme. Remplacées par « À commander ».
- **La fiche article est encore une maquette** (`ArticleService.get`, fixtures) : le lien mène à la bonne URL, mais la
  fiche n'affichera les vraies données qu'avec ART-06.
- **Le raccourci « Générer le bon de réappro. »** ouvre la page des stocks ; il n'ouvre pas directement le brouillon
  de STK-05 (pas de paramètre d'URL pour cela).
- **100 alertes au plus** sont chargées (`StockService.alerts()`) ; le sous-titre affiche quand même le total réel.
- Le reste du cockpit (KPI, CA, activité) est toujours simulé : la route garde `underConstruction: true`.
- Tests automatiques verts en CI le 08/11/2026 ; la recette manuelle reste à passer (voir le scénario).
