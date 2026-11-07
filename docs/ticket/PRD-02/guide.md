# Guide d'intégration — PRD-02 · Cohérence stock lors de la validation d'un lot

Statut : ✅ rédigé le 2026-11-08 — **tests exécutés en CI, verts** (voir le [scénario](scenario-test.md)) · Ticket : [#39](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/39) · [description](description.md)

## En bref

Valider un lot de production (passage à `COMPLETED`) sort les entrants de l'entrepôt et y entre le produit fini
**dans une seule transaction** : un mouvement refusé (stock insuffisant) annule tout, lot compris. Au même moment, le
coût unitaire de chaque entrant est figé sur le lot (`unit_cost`) et la date de validation est enregistrée
(`completed_at`), pour PRD-04 et PRD-05. Le bouton « Valider le lot » de l'écran crée et valide en une seule requête.

## Contrat exposé

### Endpoints backend (`/api/v1/production`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| POST | `/batches/{id}/completion` | `production.complete` | — | 200 `ProductionBatchRDto` (`COMPLETED`) | 401, 403, 404 (lot inconnu), 409 (déjà validé, stock insuffisant), 422 (entrepôt inconnu ou inactif) |
| POST | `/completed-batches` | `production.complete` | `ProductionBatchWDto` (celui de PRD-01) | 201 `ProductionBatchRDto` (`COMPLETED`) + `Location` `/api/v1/production/batches/{id}` | 401, 403, 404 (article inconnu), 409 (stock insuffisant : aucun lot créé), 422 |

Contrôleur : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production/controller/ProductionController.java:95` et `:106`.

Ajouts aux DTO de sortie (PRD-01) : `ProductionBatchRDto.completedAt` (null tant que le lot n'est pas validé) et
`ProductionInputRDto.unitCost` (coût d'achat figé à la validation ; null avant, ou si l'article n'a pas de coût).

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.production.service.ProductionBatchService` | `completeBatch(UUID)` (`ProductionBatchService.java:39`) | Valide un lot `PLANNED` ou `IN_PROGRESS`, verrouillé en écriture (`ProductionBatchRepository.findByIdForUpdate`). **PRD-03** l'appelle pour sa transition vers `COMPLETED` au lieu de changer le statut lui-même. |
| idem | `createCompletedBatch(ProductionBatchWDto)` (`:45`) | Planifie puis valide dans la même transaction. |
| `ProductionCompletion.complete(ProductionBatch)` (`service/impl/ProductionCompletion.java:46`, interne) | — | Fige les coûts (`ArticleService.getCosts`), récupère l'entrepôt, enregistre les mouvements `PRODUCTION` via `StockMovementService.record`. |
| `ProductionBatch.complete(Map, Instant)` (`domain/ProductionBatch.java:84`) | — | Seule façon de passer à `COMPLETED` ; `ProductionBatchStatus.ensureCanComplete()` refuse un lot déjà validé (409). |
| `feature.referential.service.ReferentialService.requireActiveStockLocationId(String code)` (`ReferentialService.java:35`) | — | **Nouveau port** : identifiant de l'emplacement actif par code stable (`WAREHOUSE`…), 422 sinon. Réutilisable par toute feature qui mouvemente un emplacement fixe. |

### Frontend (`features/production`)

- `data-access/production-service.ts:99` `createLot(lot, libellés)` : `lot.validate = true` envoie
  `POST /completed-batches` (une seule saisie dans la file hors ligne, rejouable avec sa `Idempotency-Key`) ;
  sinon `POST /batches` (lot planifié, PRD-01).
- `production-service.ts:110` `completeLot(id)` : `POST /batches/{id}/completion` par la file hors ligne, prêt pour
  PRD-03 (bouton de validation d'un lot planifié).
- `production-models.ts` : `ProductionBatchRDto.completedAt`, `ProductionInputRDto.unitCost` ; `ProductionInputWDto`
  devient une interface séparée (le coût n'est jamais envoyé).
- `pages/production-page/production-page.ts:112` `canComplete` : le bouton « Valider le lot » n'apparaît qu'avec
  `production.complete` ; sans elle, la touche Entrée enregistre un lot planifié.

### Données

- Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-41-changelog.sql` (3 changesets,
  rollback fourni) :
  - `production_batch_inputs.unit_cost NUMERIC(14,2)` (CHECK ≥ 0, nullable) ;
  - `production_batches.completed_at TIMESTAMPTZ` + CHECK « `completed_at` renseigné ⇔ statut `COMPLETED` » + index ;
  - permission `production.complete` donnée à `ADMIN`.
- Mouvements : `stock_movements` type `PRODUCTION`, emplacement `WAREHOUSE`, `reference_id` = id du lot ; un négatif
  par entrant (triés par identifiant) puis un positif pour le produit fini.
- Journal : ressource `PRODUCTION_BATCH`, action `UPDATE`, « Validation du lot LOT-00003 : 2 entrant(s) sortis,
  30 unité(s) entrées à l'entrepôt » ; plus une ligne `CREATE` `STOCK_MOVEMENT` par mouvement (STK-01).

## Règles métier pour les autres tickets

- **Jamais l'un sans l'autre** : toute validation passe par `ProductionBatchService.completeBatch` ou
  `createCompletedBatch`. Ne pas écrire de mouvement `PRODUCTION` ailleurs ni changer le statut en `COMPLETED` à la main.
- Un lot ne se valide **qu'une fois** (409 ensuite). Annuler un lot validé exigera des mouvements inverses
  (contre-passation, hors PRD-02).
- **Coût de revient (PRD-05)** : coût matière d'un lot = Σ `quantity × unit_cost` des entrants ; un entrant sans coût
  (`unit_cost` null) doit être signalé, pas compté pour 0. Le coût n'est figé qu'à la validation.
- **Indicateurs (PRD-04)** : filtrer sur `status = 'COMPLETED'` et dater par `completed_at` (pas `created_at`).
- La validation est réservée à l'administrateur (`production.complete`) ; la vendeuse crée des lots planifiés.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/`)
- `production/controller/ProductionController.java`
- `production/domain/ProductionBatch.java`, `ProductionBatchStatus.java`, `ProductionInput.java`
- `production/dto/ProductionBatchRDto.java`, `ProductionInputRDto.java`
- `production/repository/ProductionBatchRepository.java` (`findByIdForUpdate`, `saveAndFlush`)
- `production/service/ProductionBatchService.java` ; `production/service/impl/ProductionBatchServiceImpl.java`, `ProductionCompletion.java` (créé)
- `referential/repository/StockLocationRepository.java`, `referential/service/ReferentialService.java`, `referential/service/impl/ReferentialServiceImpl.java`

**Migration** : `db/changelog/changes/2026_11_08-41-changelog.sql`, déclarée dans `db.changelog-master.yaml`.

**Frontend** (`valimbavaka-ui/`)
- `src/app/features/production/data-access/production-service.ts`, `production-models.ts`
- `src/app/features/production/pages/production-page/production-page.ts`, `.html`
- `src/app/core/auth/auth-testing.ts` (`production.complete` pour `ADMIN`), `public/i18n/fr.json`, `mg.json` (`production.validatedToast`)

**Tests** : voir « Tests existants » du [scénario](scenario-test.md).

## Dépendances

**Ce ticket s'appuie sur** : [PRD-01](../PRD-01/guide.md) (lot, `ProductionBatchWDto`, PR empilée),
[STK-01](../STK-01/guide.md) (`StockMovementService.record`, verrou et contrôle du solde),
[JNL-01](../JNL-01/guide.md) (`JournalService.record`), [ART-04](../ART-04/guide.md) (`ArticleService.getCosts`).

**Ces tickets s'appuient sur lui** : [PRD-03](../PRD-03/description.md), [PRD-04](../PRD-04/description.md),
[PRD-05](../PRD-05/description.md).

## Comment l'utiliser depuis un autre ticket

```java
// PRD-03 : transition vers COMPLETED
productionBatchService.completeBatch(batchId); // 409 si déjà validé ou stock insuffisant, rien n'est écrit
```

```ts
// PRD-03 : bouton « Valider » sur un lot planifié
this.productionService.completeLot(lot.id).subscribe(({ queued, body }) => ...);
```

## Points d'attention

- **Hypothèses prises (session sans validation)** : validation réservée à l'**administrateur** (user story) ;
  lieu de production = **entrepôt** `WAREHOUSE` (hypothèse de PRD-01) ; lot validable depuis `PLANNED` ou
  `IN_PROGRESS` ; un entrant sans coût est accepté avec `unit_cost` null ; « Valider le lot » sur l'écran = une
  seule requête de création + validation (`/completed-batches`) pour rester atomique et rejouable hors ligne.
- **Concurrence** : deux validations du même lot sont sérialisées par le verrou `PESSIMISTIC_WRITE` (la seconde
  reçoit 409) ; les verrous de stock sont pris dans un ordre stable (entrants triés par UUID, produit en dernier).
  Pas de test de concurrence automatisé.
- Le contrôle « matière disponible » de l'écran reste indicatif (stock lu au chargement) : le serveur tranche (409).
- `ProductionBatchServiceImpl` a désormais 5 dépendances injectées (comme `JournalServiceImpl`) : la logique de
  mouvement est isolée dans `ProductionCompletion` pour garder le service court.
- Pas encore d'écran pour valider un lot planifié existant : `completeLot` est prêt, le bouton relève de PRD-03.
- Compilation Java et Spotless non vérifiés en local (Maven Central inaccessible) : la CI tranche.
