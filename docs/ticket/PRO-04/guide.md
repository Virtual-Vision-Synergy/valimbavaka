# Guide d'intégration — PRO-04 · Liste et détail des promotions

Statut : ✅ rédigé le 2026-11-08 sur le code livré, tests verts en CI · Ticket : [#61](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/61) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'écran Promotions n'affiche plus de données de démonstration : la liste vient de `GET /api/v1/promotions` (paginée, filtrée par statut côté serveur, recherche sur nom et code) et un clic sur une carte ouvre la fiche détaillée, relue par `GET /api/v1/promotions/{id}` (PRO-01). Aucune migration ni permission nouvelle : la lecture utilise `promotions.read` (ADMIN, SELLER).

## Contrat exposé

### Endpoints backend (`/api/v1/promotions`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| `GET` | `/api/v1/promotions?page&size&search&sortBy&sortDirection&status` | `promotions.read` (ADMIN, SELLER) | `PageQuery` + `status?` (`SCHEDULED` / `ACTIVE` / `ENDED`) | `PageResponse<PromotionRDto>` | 401, 403, 400 (statut inconnu), 422 (`size` > 100) |
| `GET` | `/api/v1/promotions/{id}` | `promotions.read` | — | `PromotionRDto` | 401, 403, 404 (inchangé, PRO-01) |

- `search` : sous-chaîne insensible à la casse du nom ou du code.
- Tri en liste blanche : `startsOn` (défaut, décroissant), `endsOn`, `name`, `createdAt` (`PromotionServiceImpl.java:39`) ; tout autre `sortBy` est ignoré.
- Sans `status` : toutes les promotions.

### Service Java

`PromotionService.getPromotions(PageQuery query, @Nullable PromotionStatus status)` (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/promotion/service/PromotionService.java:20`) : le statut de chaque ligne est calculé sur le jour d'Antananarivo, une seule fois par appel (`PromotionServiceImpl.java:61`).

`PromotionRepository.search(search, status, today, pageable)` (`…/promotion/repository/PromotionRepository.java:49`) : `status` vaut `PromotionRepository.ANY_STATUS` (`"ALL"`) ou le nom d'un `PromotionStatus` ; la requête JPQL reprend la règle de `PromotionPeriod.statusOn` (programmée : début > J ; active : début ≤ J ≤ fin ; terminée : fin < J).

Les collections `targetIds` et `channelIds` de `Promotion` sont chargées par lots (`@BatchSize(size = 100)`, `Promotion.java:38`) : une page coûte une requête par collection, pas une par ligne.

### Frontend (`valimbavaka-ui/src/app/features/promotions`)

- `data-access/promotion-service.ts` : `list(query: PromotionListQuery)` (ligne 31, `status` envoyé seulement s'il est choisi), `getById(id)` (ligne 38), `context()` (ligne 43 : gammes et canaux du référentiel + choix du périmètre, sans liste de promotions).
- `data-access/promotion-models.ts` : `PromotionListQuery` (`PageQuery` + `status?`) ; `PromotionContext` n'a plus de champ `promotions`.
- `data-access/promotion-labels.ts` : `PROMOTION_STATUS_TONES`, `discountLabel(kind, value)`, `channelLabels(promotion, refs)`, `rangeLabels(promotion, refs)` (identifiants → libellés du référentiel).
- `components/promotion-detail/promotion-detail.ts` : `<vvs-promotion-detail [promotionId] [references]>` — fiche autonome (charge la promotion, états chargement / erreur avec « Réessayer »), réutilisable ailleurs (ART-06, caisse).
- `components/promotion-card/promotion-card.ts` : nouvelles entrées `selected` et sortie `opened` (ligne 21-23) ; le titre est un bouton étendu à toute la carte (clavier et lecteur d'écran).
- `pages/promotion-list-page` : filtre par statut envoyé au serveur (retour en page 1 au changement), pagination (`PROMOTION_PAGE_SIZE` = 12), fiche dans `vvs-side-panel` (tiroir sous 1600 px ; ancrée à la place du formulaire au-delà, avec « Fermer la fiche »), liste rechargée après une création.
- `data-access/promotion-testing.ts` : `PROMOTION_REFS`, `buildPromotion()`, `pageOf()` pour les tests.
- Supprimés : `data-access/promotion-status.ts` (le statut vient du serveur) et la liste de démonstration de `promotion-fixtures.ts` (seul `PROMOTION_SCOPES_FIXTURE` reste).
- i18n : `promotions.listLabel`, `promotions.detail.*` (fr, mg).

### Données et journal

Aucune migration, aucune écriture, aucune ligne de journal (lecture seule).

## Règles métier pour les autres tickets

- **Le statut ne se stocke pas** : il est déduit de la période et du jour à Antananarivo, à chaque lecture (liste et fiche). Ne pas le recalculer côté écran.
- **Un statut « épuisée »** (plafond atteint) n'existe pas : s'il est ajouté (PRO-03), l'étendre à la fois dans `PromotionPeriod.statusOn` / le mapper et dans la requête `PromotionRepository.search`.
- La vendeuse lit la liste et les fiches (`promotions.read`), comme pour la caisse (PRO-02, VTE-04).

## Fichiers concernés

- Backend : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/promotion/` — `controller/PromotionController.java`, `service/PromotionService.java`, `service/impl/PromotionServiceImpl.java`, `repository/PromotionRepository.java`, `domain/Promotion.java`.
- Frontend : `valimbavaka-ui/src/app/features/promotions/` — `data-access/{promotion-service,promotion-models,promotion-labels,promotion-fixtures,promotion-testing}.ts`, `components/promotion-detail/*`, `components/promotion-card/*`, `pages/promotion-list-page/*`, `promotions.routes.ts` ; `valimbavaka-ui/public/i18n/{fr,mg}.json`.
- Tests : voir [scénario](scenario-test.md#tests-existants).

## Dépendances

- Dépend de : [PRO-01](../PRO-01/guide.md) (modèle, `GET /{id}`, création).
- Utilisé par : aucun ticket déclaré ; [PRO-03](../PRO-03/description.md) (statut et compteur) et [ART-06](../ART-06/description.md) peuvent réutiliser `PromotionDetail` et `list()`.

## Comment l'utiliser

```ts
// Promotions actives, première page
this.promotionService.list({ page: 1, size: 20, status: 'ACTIVE' }).subscribe((page) => show(page.items));
```

```html
<vvs-promotion-detail [promotionId]="id" [references]="{ ranges, channels }" />
```

## Points d'attention

- **Hypothèses (non validées par le PO)** : la liste et la fiche sont ouvertes à la vendeuse (permission `promotions.read` déjà accordée par PRO-01) ; tri par défaut sur la date de début décroissante ; filtre « Toutes » par défaut ; pas de compteur par statut dans les puces (il demanderait une requête par statut).
- **Route encore « en construction »** : les nombres d'articles du choix de périmètre viennent du catalogue de démonstration et la « remise accordée » des cartes reste « — » (PRO-03).
- **Fiche en lecture seule** : ni modification ni désactivation (aucun endpoint d'écriture hors création) ; le périmètre `ARTICLES` est affiché en nombre d'articles, pas en noms.
- **Recherche** : exposée par l'API (`search`) mais pas encore de champ à l'écran.
- **Gammes inconnues** : une gamme visée absente du référentiel actif s'affiche « Gammes introuvables dans le référentiel ».
