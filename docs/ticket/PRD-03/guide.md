# Guide d'intégration — PRD-03 · Statut d'un lot de production

Statut : ✅ rédigé le 2026-11-08 — **tests exécutés en CI, verts** (voir le [scénario](scenario-test.md)) · Ticket : [#40](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/40) · [description](description.md)

## En bref

Le statut d'un lot avance dans un seul sens : **Planifié** (`PLANNED`) → **En cours** (`IN_PROGRESS`) → **Terminé**
(`COMPLETED`). PRD-03 ajoute le démarrage (`POST /batches/{id}/start`, sans mouvement de stock) et branche sur l'écran
les boutons « Démarrer » et « Terminer » des lots récents ; « Terminer » réutilise la validation de PRD-02 (stock
mouvementé). La vendeuse peut désormais aller jusqu'à « Terminé ».

## Contrat exposé

### Endpoints backend (`/api/v1/production`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| POST | `/batches/{id}/start` | `production.update` | — (un corps éventuel est ignoré) | 200 `ProductionBatchRDto` (`IN_PROGRESS`) | 401, 403, 404 (lot inconnu), 409 (déjà en cours ou terminé) |
| POST | `/batches/{id}/completion` (PRD-02, inchangé) | `production.complete` | — | 200 `ProductionBatchRDto` (`COMPLETED`) | 401, 403, 404, 409, 422 |
| GET | `/batches?status=…` (PRD-01, inchangé) | `production.read` | `PageQuery`, `ProductionBatchStatus` | 200 `PageResponse<ProductionBatchRDto>` | 400, 401, 403, 422 |

Contrôleur : `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production/controller/ProductionController.java:94`.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.production.service.ProductionBatchService` | `startBatch(UUID)` (`ProductionBatchService.java:36`) | Lot verrouillé en écriture (`findByIdForUpdate`), passé en cours, `saveAndFlush`, journal `UPDATE`. 404 / 409. |
| `ProductionBatch.start()` (`domain/ProductionBatch.java:83`) | — | Seule façon de passer à `IN_PROGRESS`. |
| `ProductionBatchStatus.ensureCanStart()` (`domain/ProductionBatchStatus.java:15`) | — | 409 si le lot est déjà en cours ou terminé. |

### Frontend (`features/production`)

- `data-access/production-service.ts:110` `startLot(id)` : `POST /batches/{id}/start` par la file hors ligne
  (`OutboxService`, `Idempotency-Key`) ; `completeLot(id)` (PRD-02) sert au bouton « Terminer ».
- `data-access/production-models.ts` : `LotRDto.id` ajouté (rempli par `toLot`, `production-mapping.ts`).
- `pages/production-page/production-page.ts:193` `advance(lot, 'start' | 'finish')` : le statut affiché change avec
  la réponse du serveur (signal `statuses`), sans recharger la liste ; hors ligne, il ne change pas (toast « en file »).
  `canStart` = `production.update`, `canComplete` = `production.complete`.
- Libellés (`public/i18n/fr.json`, `mg.json`) : `production.status.CURING` = « En cours », `VALIDATED` = « Terminé » ;
  nouvelles clés `start`, `startLot`, `finish`, `finishLot`, `startedToast`, `statusError`.

### Données

- Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-42-changelog.sql` (2 changesets,
  rollback fourni) : permission `production.update` (administrateur, vendeuse) ; `production.complete` donnée aussi à
  la vendeuse. Aucune table modifiée (la contrainte CHECK des statuts existe depuis PRD-01).
- Journal : ressource `PRODUCTION_BATCH`, action `UPDATE`, « Démarrage du lot LOT-00012 : planifié → en cours ».

## Règles métier pour les autres tickets

- **En avant seulement** : pas de retour à « Planifié », pas de changement après « Terminé ». Une annulation exigerait
  un statut `CANCELLED` (nouvelle migration sur la contrainte CHECK) et, après validation, des mouvements inverses.
- **Démarrer ne touche pas au stock** ; seul « Terminé » mouvemente, via `ProductionBatchService.completeBatch` (PRD-02).
  Un lot planifié peut être terminé sans passer par « En cours ».
- **Indicateurs (PRD-04)** : « en cours » = `status = 'IN_PROGRESS'` (pas de date de démarrage stockée) ; lots
  produits = `COMPLETED` datés par `completed_at`.
- Toute personne ayant la permission agit sur **tous** les lots, pas seulement ceux dont elle est responsable.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production/`)
- `controller/ProductionController.java`
- `domain/ProductionBatch.java`, `domain/ProductionBatchStatus.java`
- `service/ProductionBatchService.java`, `service/impl/ProductionBatchServiceImpl.java`

**Migration** : `db/changelog/changes/2026_11_08-42-changelog.sql`, déclarée dans `db.changelog-master.yaml`.

**Frontend** (`valimbavaka-ui/`)
- `src/app/features/production/data-access/production-service.ts`, `production-models.ts`, `production-mapping.ts`, `production-fixtures.ts`
- `src/app/features/production/pages/production-page/production-page.ts`, `.html`, `.css`
- `src/app/core/auth/auth-testing.ts` (`production.update` ; `production.complete` pour `SELLER`), `public/i18n/fr.json`, `mg.json`

**Tests** : voir « Tests existants » du [scénario](scenario-test.md).

## Dépendances

**Ce ticket s'appuie sur** : [PRD-01](../PRD-01/guide.md) (lot, statuts, liste filtrable),
[PRD-02](../PRD-02/guide.md) (`completeBatch`, `completeLot`, `production.complete` ; PR empilée).

**Ces tickets s'appuient sur lui** : [PRD-04](../PRD-04/description.md) (compter les lots en cours).

## Comment l'utiliser depuis un autre ticket

```java
productionBatchService.startBatch(batchId); // 404 inconnu, 409 déjà en cours ou terminé ; le stock ne bouge pas
```

```ts
this.productionService.startLot(lot.id).subscribe(({ queued, body }) => ...); // body absent = en file
```

## Points d'attention

- **Hypothèse prise (session sans validation) — à faire valider par Randy** : la user story de PRD-03 dit que la
  **vendeuse** modifie le statut « jusqu'à Terminé ». PRD-02 avait supposé la validation réservée à l'administrateur ;
  PRD-03 donne donc `production.complete` à la vendeuse (changeset `2026_11_08-42-2`) et adapte deux tests de PRD-02
  (`ProductionSecurityIT` : le 403 est désormais vérifié avec un livreur). Conséquence : le bouton « Valider le lot »
  de création apparaît aussi pour la vendeuse. Pour revenir en arrière, retirer ce changeset et les deux libellés.
- Démarrage = action dédiée `POST /start` (et non un `PATCH /status` générique), car « Terminer » exige une autre
  permission et mouvemente le stock.
- Aucune date de démarrage n'est stockée (le journal la porte) ; à ajouter si PRD-04 en a besoin.
- Pas de liste complète des lots : « Tous → » reste un lien vers `/production` ; seuls les 10 lots récents ont les
  boutons. Une page liste paginée relèverait d'un ticket à part.
- Libellé « En cure » de la maquette remplacé par « En cours » (vocabulaire de la user story) ; la note KPI
  « dont … en cours de cure » attend PRD-04.
- Compilation, tests, Spotless et Checkstyle vérifiés en CI (runs 42414631257 / 42414633613).
