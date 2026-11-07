# Guide d'intégration — PRD-05 · Détail et coût de revient d'un lot

Statut : ✅ rédigé le 2026-11-08 — **tests exécutés en CI, verts** (voir le [scénario](scenario-test.md)) · Ticket : [#42](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/42) · [description](description.md)

## En bref

Le détail d'un lot (`/production/{id}`) affiche ses entrants, le coût de chaque ligne, le coût matière, le **coût de
revient unitaire** (coût matière ÷ quantité produite) et la marge sur le prix de vente. Le serveur calcule tout à partir
des entrants (`GET /api/v1/production/batches/{id}/cost`) : coûts **figés** à la validation pour un lot terminé
(PRD-02), **estimés** avec les coûts d'achat actuels pour un lot planifié ou en cours.

## Contrat exposé

### Endpoint backend (`/api/v1/production`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/batches/{id}/cost` | `production.read` (administrateur, vendeuse) | — | 200 `ProductionBatchCostRDto` | 400 (identifiant mal formé), 401, 403, 404 (lot inconnu) |

Contrôleur : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production/controller/ProductionController.java:93`.

`ProductionBatchCostRDto` (`dto/ProductionBatchCostRDto.java`) : `id`, `number`, `outputArticleId`,
`outputArticleName`, `quantity`, `status`, `completedAt`, `estimated` (lot non terminé), `inputs`
(`ProductionInputCostRDto` : `articleId`, `articleCode`, `articleName`, `unit`, `quantity`, `unitCost`, `lineCost`),
`materialCost`, `unitCost` (coût de revient unitaire, 2 décimales), `uncostedInputs`, `salePrice` (prix de vente
actuel du produit obtenu), `marginPercent` (entier arrondi). Les montants sont **null** dès qu'un entrant n'a pas de
coût ; `marginPercent` est null sans prix de vente.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.production.service.ProductionCostService` | `getBatchCost(UUID)` (`service/impl/ProductionCostServiceImpl.java:42`) | Coût de revient d'un lot ; 404 si le lot est inconnu. Réutilisable par un rapport de rentabilité (RPT-*). |

Lectures utilisées : `ProductionBatchRepository.findById` et `ArticleService.getArticleById` (nom, code, unité, coût
d'achat actuel, prix de vente) pour le produit obtenu et chaque entrant.

### Frontend (`features/production`)

- Route `production.routes.ts` : `:id` → `pages/production-lot-page/production-lot-page.ts` (`ProductionLotPage`,
  paramètre `id` par `withComponentInputBinding`), sous la garde `production.read` de `/production`.
- `data-access/production-service.ts:101` `cost(id)` : `GET /batches/{id}/cost`, converti par
  `production-mapping.ts:124` `toLotCost` (champs absents → null, statut écran `DRAFT`/`CURING`/`VALIDATED`, jour
  `completedOn`, unités `kg`/`l`/`u.`).
- `data-access/production-models.ts` : miroirs `ProductionBatchCostRDto`, `ProductionInputCostRDto` ; modèles d'écran
  `LotCostRDto`, `LotCostLineRDto`.
- `pages/production-page/production-page.html:263` : le numéro de chaque lot de « Lots récents » est un lien vers le
  détail (nom accessible « Détail du lot LOT-00003 »).
- Libellés `production.detail.*` dans `public/i18n/fr.json` et `mg.json`.

### Données

- Aucune migration, aucune table : lecture de `production_batches`, `production_batch_inputs` (`quantity`,
  `unit_cost`) et `articles` (`cost`, `price`).
- Aucune écriture, donc aucune ligne de journal.

## Règles métier pour les autres tickets

- **Coût de revient unitaire** = Σ (quantité × coût unitaire, arrondi à 2 décimales) ÷ quantité produite, arrondi à
  2 décimales (HALF_UP). Le coût matière est la somme des lignes arrondies, pour que le détail s'additionne.
- **Lot terminé** : coûts figés à la validation (`unit_cost`, PRD-02) ; un changement ultérieur du coût d'achat ne
  change plus le coût du lot. **Lot planifié ou en cours** : coût d'achat actuel des articles, `estimated` = true.
- **Entrant sans coût** : jamais compté pour 0 (règle de PRD-02) ; la ligne est à null, `uncostedInputs` le compte et
  les totaux, le coût unitaire et la marge sont null.
- **Marge** = (prix de vente − coût de revient unitaire) ÷ prix de vente, en % arrondi ; elle utilise le prix de vente
  **actuel** du produit (aucun prix n'est figé sur le lot).
- Même définition du coût matière que PRD-04 (indicateurs), au détail près de l'arrondi par ligne.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production/`)
- `controller/ProductionController.java` (endpoint `getBatchCost`)
- `dto/ProductionBatchCostRDto.java`, `dto/ProductionInputCostRDto.java` (créés)
- `service/ProductionCostService.java`, `service/impl/ProductionCostServiceImpl.java` (créés)

**Migration** : aucune.

**Frontend** (`valimbavaka-ui/`)
- `src/app/features/production/pages/production-lot-page/` (`.ts`, `.html`, `.css`, `.spec.ts`, créés)
- `src/app/features/production/production.routes.ts`, `pages/production-page/production-page.html`
- `src/app/features/production/data-access/production-service.ts`, `production-mapping.ts`, `production-models.ts`
- `public/i18n/fr.json`, `mg.json` (`production.detail.*`)

**Tests** : voir « Tests existants » du [scénario](scenario-test.md).

## Dépendances

**Ce ticket s'appuie sur** : [PRD-01](../PRD-01/guide.md) (lot, entrants), [PRD-02](../PRD-02/guide.md) (`unit_cost`
figé et `completed_at` à la validation), [PRD-03](../PRD-03/guide.md) (statuts, lots récents),
[PRD-04](../PRD-04/guide.md) (même règle de coût matière ; PR empilée), [ART-04](../ART-04/guide.md)
(`ArticleService`).

**Ces tickets s'appuient sur lui** : aucun pour l'instant (un rapport de rentabilité pourra appeler
`ProductionCostService.getBatchCost`).

## Comment l'utiliser depuis un autre ticket

```java
var cost = productionCostService.getBatchCost(batchId); // 404 si le lot est inconnu
if (cost.unitCost() != null) { /* coût de revient unitaire, figé si !cost.estimated() */ }
```

```ts
this.productionService.cost(lotId).subscribe(({ unitCost, estimated }) => ...);
```

## Points d'attention

- **Hypothèses prises (session sans validation) — à faire valider par Randy** :
  - permission `production.read` (administrateur **et vendeuse**, comme l'écran et les indicateurs PRD-04) : la user
    story vise la vendeuse ; aucune nouvelle permission, donc aucune migration ;
  - un lot **non terminé** affiche une **estimation** avec les coûts d'achat actuels (bandeau d'avertissement), car
    PRD-02 ne fige les coûts qu'à la validation ; un lot terminé n'utilise que les coûts figés ;
  - la **marge** (rentabilité) est calculée sur le prix de vente **actuel** du produit obtenu ; le coût de revient ne
    compte que les matières (pas de main-d'œuvre ni de frais : aucune donnée en base) ;
  - un endpoint dédié `GET /batches/{id}/cost` plutôt qu'un enrichissement de `ProductionBatchRDto`, pour ne pas
    alourdir la liste paginée des lots.
- Une lecture d'article par entrant (`ArticleService.getArticleById`, au plus 50 entrants par lot) : suffisant pour un
  détail ; à remplacer par une lecture groupée si un rapport parcourt de nombreux lots.
- Le détail n'est accessible que depuis les 10 « Lots récents » (pas de liste complète, voir PRD-03).
- Compilation, tests, Spotless et Checkstyle vérifiés en CI (runs 42418133221 / 42418135044).
