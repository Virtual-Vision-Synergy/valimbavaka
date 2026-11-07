# Guide d'intégration — PRD-01 · Création d'un lot de production

Statut : ✅ rédigé le 2026-11-08 — **tests écrits, non exécutés** (voir le [scénario](scenario-test.md)) · Ticket : [#38](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/38) · [description](description.md)

## En bref

PRD-01 crée la feature `production` : un **lot de production** associe un produit fini obtenu, une quantité, une
vendeuse responsable et ses entrants (matières premières, semi-finis). Le lot est créé **planifié** (`PLANNED`) :
il est tracé au journal mais **ne touche pas au stock**. Le domaine est prévu pour que PRD-02 (mouvements à la
validation), PRD-03 (statuts), PRD-04 (indicateurs) et PRD-05 (coût de revient) s'y greffent sans migration de
reprise.

## Contrat exposé

### Endpoints backend (`/api/v1/production`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/batches` + `page`, `size`, `search` (numéro), `sortBy` (`createdAt`, `number`, `status`), `sortDirection`, `status` | `production.read` | `PageQuery`, `ProductionBatchStatus` facultatif | 200 `PageResponse<ProductionBatchRDto>`, plus récents d'abord | 400, 401, 403, 422 (`size` > 100) |
| GET | `/batches/{id}` | `production.read` | — | 200 `ProductionBatchRDto` | 401, 403, 404 |
| POST | `/batches` | `production.create` | `ProductionBatchWDto` | 201 `ProductionBatchRDto` + `Location` | 401, 403, 404 (article inconnu), 422 |
| GET | `/operators` + `PageQuery` (recherche prénom / nom, tri `firstName` par défaut) | `production.read` | — | 200 `PageResponse<ProductionOperatorRDto>` | 401, 403 |

Contrôleur : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production/controller/ProductionController.java:59`.

- `ProductionBatchWDto` : `outputArticleId`, `quantity` (> 0, 9 chiffres + 3 décimales), `operatorId`,
  `inputs` (1 à 50 `ProductionInputWDto` : `articleId`, `quantity` > 0, 3 décimales). Aucun `id`, `number`,
  `status` : envoyés, ils sont ignorés.
- `ProductionBatchRDto` : `id`, `number` (`LOT-00001`), `outputArticleId`, `quantity`, `operatorId`, `status`,
  `inputs` (`articleId`, `quantity`, dans l'ordre de saisie), `createdAt`, `updatedAt`, `version`. Articles et
  vendeuse sont des UUID : le libellé se résout côté appelant (voir « Points d'attention »).
- `ProductionOperatorRDto` : `id`, `firstName`, `lastName` (ni email ni taux de commission).

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.production.service.ProductionBatchService` | `getBatches(PageQuery, @Nullable ProductionBatchStatus)`, `getBatchById(UUID)`, `createBatch(ProductionBatchWDto)` | Lots. PRD-02 / PRD-03 y ajoutent leurs méthodes d'écriture. |
| `feature.production.service.ProductionOperatorService` | `getOperators(PageQuery)` | Vendeuses actives proposables. |
| `ProductionBatchPolicy.check(ProductionBatchWDto)` (package `service.impl`, interne) | contrôle | Entrants distincts, responsable vendeuse active, sortie `OUTPUT` et chaque entrant `INPUT` via `ArticleService.requireProductionArticle` (ART-04). |
| `ProductionBatch.plan(String, ProductionBatchDetails)` (`domain/ProductionBatch.java:60`) | fabrique | Seule façon de créer un lot : statut `PLANNED`, entrants copiés. |

Ajouts à la feature `user` : `UserService.getActiveSellers(PageQuery)` et `UserService.isActiveSeller(UUID)`
(`feature/user/service/UserService.java:19`), requêtes `UserRepository.searchActiveByRoleName` et `isActiveWithRole`.

### Frontend (`features/production`)

- Route `/production` (permission `production.read`, désormais visible par la vendeuse), page
  `pages/production-page` conservée ; le marqueur « En construction » reste (indicateurs PRD-04).
- `data-access/production-service.ts` : `context()` assemble l'écran (produits finis et entrants via
  `ArticleService.listForProduction`, disponible en entrepôt via `GET /api/v1/stocks`, vendeuses, 10 derniers
  lots) ; `batches(query)`, `getById(id)`, `operators()` ; `createLot(lot, libellés)` passe par la file hors
  ligne (`OutboxService`, `Idempotency-Key`) et renvoie `CreatedLot` (`queued`, `lot`).
- `data-access/production-mapping.ts` : `toProducible`, `toMaterial` (disponible = emplacement `WAREHOUSE`),
  `toOperator`, `toLot`, `toLotStatus`, `toBatchWDto` ; constante `PRODUCTION_LOCATION`.
- `data-access/production-models.ts` : miroirs `ProductionBatchRDto`, `ProductionBatchWDto`,
  `ProductionInputRDto`, `ProductionOperatorRDto`, `ProductionBatchStatus` ; modèles d'écran (`LotRDto`,
  `MaterialRDto`…) inchangés sauf `operators` (`OperatorOption`) et `LotWDto.operatorId`.
- Gabarit : seul le sélecteur « Responsable » change (valeur = id de la vendeuse).

### Données

- Tables : `production_batches` (`number` unique, `output_article_id` → `articles`, `quantity` `NUMERIC(12,3)` > 0,
  `operator_id` → `users`, `status` CHECK `PLANNED|IN_PROGRESS|COMPLETED`, colonnes d'audit + `version`) ;
  `production_batch_inputs` (`batch_id`, `line_no`, `article_id`, `quantity` > 0, unique `(batch_id, article_id)`).
- Séquence `production_batch_number_seq` (numéros jamais réutilisés, trous possibles).
- Permissions : `production.create` (administrateur, vendeuse) ; `production.read` donnée à la vendeuse.
- Migration : `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-40-changelog.sql` (5 changesets, rollback fourni).
- Journal : ressource `PRODUCTION_BATCH`, action `CREATE`, libellé
  « Création du lot LOT-00012 : 30 × ART-0031 · Savon noir 100 g (2 entrant(s)) ».

## Règles métier pour les autres tickets

- **Un lot ne se supprime jamais** : `ProductionBatchRepository` étend `OperationRepository` (JNL-02). Une erreur
  se corrige par un changement de statut (PRD-03) ou, après validation, par des mouvements inverses.
- **Statuts** (`ProductionBatchStatus`) : `PLANNED` (création) → `IN_PROGRESS` → `COMPLETED`. Les transitions et
  leur contrôle (`ensureCan…` sur l'enum, à la manière de `OperationStatus`) sont à écrire par **PRD-03** ; une
  annulation exigerait d'ajouter `CANCELLED` à l'enum et à la contrainte CHECK (nouvelle migration).
- **Validation et stock (PRD-02)** : au passage à `COMPLETED`, dans **une seule** méthode `@Transactional`, appeler
  `StockMovementService.record` avec le type `PRODUCTION` et `referenceId = batch.getId()` : un mouvement négatif
  par entrant puis un positif pour le produit obtenu, sur l'emplacement `WAREHOUSE` (hypothèse de PRD-01, le
  disponible affiché vient de là). Un stock insuffisant lève 409 et annule tout.
- **Coût de revient (PRD-05)** : aucun coût n'est stocké à la création. PRD-02 ou PRD-05 figera le coût unitaire
  des entrants au moment de la validation (colonne `unit_cost` à ajouter sur `production_batch_inputs`), sinon un
  changement de prix d'achat modifierait les lots passés.
- **Indicateurs (PRD-04)** : compter sur `production_batches` (lots, unités = `sum(quantity)` des lots `COMPLETED`) ;
  l'écran a encore des KPI à zéro (`NO_KPIS` dans `production-service.ts`).
- Le serveur contrôle tout : ne pas se fier aux listes de l'écran (un client peut envoyer n'importe quel UUID).
- La responsable doit être un compte **vendeuse actif** ; l'administrateur peut créer un lot pour une vendeuse.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production/`)
- `controller/ProductionController.java`
- `domain/ProductionBatch.java`, `ProductionBatchDetails.java`, `ProductionBatchStatus.java`, `ProductionInput.java`
- `dto/ProductionBatchWDto.java`, `ProductionInputWDto.java`, `ProductionBatchRDto.java`, `ProductionInputRDto.java`, `ProductionOperatorRDto.java`
- `mapper/ProductionMapper.java` ; `repository/ProductionBatchRepository.java`
- `service/ProductionBatchService.java`, `ProductionOperatorService.java` ; `service/impl/ProductionBatchServiceImpl.java`, `ProductionBatchPolicy.java`, `ProductionOperatorServiceImpl.java`
- Feature `user` modifiée : `repository/UserRepository.java`, `service/UserService.java`, `service/impl/UserServiceImpl.java`

**Migration** : `db/changelog/changes/2026_11_08-40-changelog.sql`, déclarée dans `db.changelog-master.yaml`.

**Frontend** (`valimbavaka-ui/src/app/`)
- `features/production/data-access/production-service.ts`, `production-mapping.ts` (créé), `production-models.ts`, `production-fixtures.ts`
- `features/production/pages/production-page/production-page.ts`, `.html` ; `production.routes.ts` (commentaire)
- `core/auth/auth-testing.ts` (permissions de test), `public/i18n/fr.json`, `mg.json`

**Tests** : voir « Tests existants » du [scénario](scenario-test.md).

## Dépendances

**Ce ticket s'appuie sur** : [ART-04](../ART-04/guide.md) (`requireProductionArticle`, `listForProduction`),
[STK-01](../STK-01/guide.md) (lecture du stock ; écriture réservée à PRD-02), [JNL-02](../JNL-02/guide.md)
(`OperationRepository`), [JNL-01](../JNL-01/guide.md) (`JournalService.record`).

**Ces tickets s'appuient sur lui** : [PRD-02](../PRD-02/description.md), [PRD-03](../PRD-03/description.md),
[PRD-04](../PRD-04/description.md), [PRD-05](../PRD-05/description.md), [ART-06](../ART-06/description.md).

## Comment l'utiliser depuis un autre ticket

```java
// PRD-02, dans ProductionBatchServiceImpl (méthode @Transactional) : mouvements du lot validé
for (var input : batch.getInputs()) {
    stockMovementService.record(new StockMovementCommand(
            input.getArticleId(), warehouseId, StockMovementType.PRODUCTION, input.getQuantity().negate(), batch.getId()));
}
stockMovementService.record(new StockMovementCommand(
        batch.getOutputArticleId(), warehouseId, StockMovementType.PRODUCTION, batch.getQuantity(), batch.getId()));
```

```ts
// PRD-03 / PRD-05 : relire un lot
this.productionService.getById(id).subscribe((batch) => ...);
```

## Points d'attention

- **Hypothèses prises (session sans validation)** : la responsable est une **vendeuse active** (rôle `SELLER`) ; la
  vendeuse reçoit `production.read` et `production.create` ; le lieu de production est l'**entrepôt**
  (`WAREHOUSE`) ; un semi-fini reste un entrant uniquement (cf. ART-04) ; numéro `LOT-%05d` par séquence.
- **« Valider le lot »** sur l'écran crée, comme « Enregistrer », un lot **planifié** : le serveur ignore la
  validation tant que **PRD-02** n'existe pas (le toast l'indique). PRD-02 doit brancher ce bouton.
- **Pas de recette** côté serveur : les entrants se choisissent un à un (le pré-remplissage de la maquette est
  vide). Une recette par produit serait un ticket à part.
- **Libellés des lots récents** : résolus à partir des listes chargées (produits finis actifs, vendeuses actives) ;
  un produit ou une vendeuse désactivé depuis s'affiche « — ». ART-06 / PRD-05 pourront enrichir le RDto.
- **Une page de 100** pour les articles, le stock et les vendeuses (limite serveur) : au-delà, il faudra une
  recherche dans les sélecteurs.
- Le numéro du prochain lot n'est plus affiché avant l'enregistrement (attribué par le serveur).
- Indicateurs en tête d'écran à zéro jusqu'à **PRD-04**.
- Compilation Java et Spotless non vérifiés en local (Maven Central inaccessible) : la CI tranche ; lancer
  `mvn spotless:apply` si elle le signale.
