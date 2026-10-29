# Guide d'intégration — ART-05 · Recherche et filtrage des articles

Statut : ✅ écrit le 2026-10-29 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#25](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/25) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La liste `/articles` interroge maintenant l'API : la recherche (nom, code, code-barres), le filtre de gamme et le filtre de type sont envoyés au serveur, qui renvoie une page de résultats. La liste se met à jour à chaque changement de critère, sans recharger la page. Ticket **frontend seul** : aucun endpoint ni migration ajouté, tout le filtrage vient de `GET /api/v1/articles` ([ART-01](../ART-01/guide.md)).

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint. La page appelle `GET /api/v1/articles` avec `page`, `size=10`, `sortBy=name`, `sortDirection=ASC` et, seulement s'ils sont choisis, `search` (rognée), `type`, `rangeId`. Le contrat complet est dans le [guide ART-01](../ART-01/guide.md#contrat-exposé). Les gammes viennent de `GET /api/v1/product-ranges` ([SYS-05](../SYS-05/guide.md)).

### Frontend

- `features/articles/pages/article-list-page/article-list-page.ts` :
  - recherche temporisée à 300 ms et rognée (`SEARCH_DEBOUNCE_MS`, signal `search`, ligne 90) ;
  - `articles` (`rxResource`, ligne 103) : un nouveau critère annule la requête en cours ;
  - `page` (`linkedSignal`, ligne 97) : retour à la page 1 dès que la recherche, la gamme ou le type change ;
  - filtre de gamme construit depuis le référentiel (`ranges`, ligne 78) : la valeur de chaque pastille est l'**UUID** de la gamme, envoyé en `rangeId` ;
  - `rangeOf` (ligne 130) traduit l'UUID en code `BOTANIKA` / `ZEZIKA` / `SOLIKA` pour la couleur de la gamme ([ART-03](../ART-03/guide.md)) ; une autre gamme s'affiche sans couleur.
- `article-list-page.html` : mêmes composants partagés qu'avant ; le sous-titre n'affiche plus que le total renvoyé par le serveur (`totalCount`).
- La liste utilise `features/article/data-access/article-service.ts` (`list`), plus le service de démonstration du dossier `features/articles/data-access`.
- Traductions : `articles.subtitle` raccourci, nouvelles clés `articles.active`, `articles.inactive`, `articles.stockPending`, `articles.filter.lowOnlyPending` (`fr.json` et `mg.json`).

## Règles métier à respecter par les autres tickets

- Le filtrage se fait **côté serveur** : ne pas refiltrer en local une page déjà reçue, le résultat serait faux (la page ne contient qu'une partie des articles).
- Le filtre de type propose `FINISHED`, `SEMI_FINISHED`, `RAW_MATERIAL` : l'ancien « Emballage » de la maquette n'existe pas dans le contrat.
- Pour un autre écran qui a besoin des mêmes critères (vente, promotion), reprendre `ArticleService.list(query)` plutôt que d'ajouter un endpoint.

## Fichiers concernés

**Frontend** (`valimbavaka-ui/src/app/features/articles/pages/article-list-page/`) : `article-list-page.ts`, `article-list-page.html`, `article-list-page.spec.ts`, `article-list-page.fixtures.ts` ; `valimbavaka-ui/public/i18n/fr.json` et `mg.json`.

**Backend** : aucun fichier modifié.

**Tests** : `article-list-page.spec.ts` (13 cas, voir le scénario). Le côté serveur est couvert par `ArticleRepositoryIT` et `ArticleSecurityIT` (ART-01).

## Dépendances

**Ce ticket s'appuie sur** : [ART-01](../ART-01/guide.md) (API `GET /articles` et ses filtres), [SYS-05](../SYS-05/guide.md) (gammes), [ART-04](../ART-04/guide.md) (types MP / SF / produit fini).

**Ces tickets s'appuient sur lui** : aucun.

## Comment l'utiliser depuis un autre ticket

```ts
// Même requête que la liste : produits finis de la gamme choisie dont le nom contient « savon »
this.articleService.list({ page: 1, size: 10, sortBy: 'name', search: 'savon', type: 'FINISHED', rangeId });
```

## Points d'attention

- **Stock vide** : `ArticleRDto` n'a ni stock ni état de stock. La colonne « Stock total » affiche `—` et le bouton « Stock faible » est désactivé ; [STK-01](../STK-01/description.md) devra les brancher (un filtre « stock faible » demandera un paramètre côté serveur).
- Le statut affiché est l'état actif / inactif de l'article, pas la disponibilité.
- Les actions groupées (exporter, changer de gamme) et les boutons Importer / Exporter restent des actions de démonstration ([ART-07](../ART-07/description.md), [ART-08](../ART-08/description.md)). La sélection porte sur la page courante seulement.
- `features/articles/data-access/article-service.ts` et ses fixtures servent encore à la fiche détail ([ART-06](../ART-06/description.md)) et à la recherche globale (`core/layout/search-dialog/search-index.ts`) ; à supprimer quand ces deux usages seront branchés.
- Si `/product-ranges` échoue, la liste fonctionne mais seule la pastille « Toutes gammes » est proposée.
- Les tests n'ont pas été exécutés par l'auteur : voir le scénario.
