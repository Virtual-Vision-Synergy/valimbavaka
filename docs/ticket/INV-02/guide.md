# Guide d'intégration — INV-02 · Comptage physique avec calcul d'écart

Statut : ✅ écrit le 2026-11-08 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#78](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/78) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur saisit, ligne par ligne, la quantité comptée d'une session d'inventaire en cours. L'écart (**compté − théorique**) s'affiche dès la frappe ; à la validation du champ, le serveur enregistre le comptage en **figeant le stock théorique de l'emplacement à cet instant**, de sorte que les ventes postérieures ne déplacent plus l'écart. La page `/inventory` n'est plus une démonstration pour la lecture et le comptage : elle charge la session en cours et toutes ses lignes depuis l'API.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| PUT | `/api/v1/inventories/{id}/lines/{lineId}/count` | `inventory.update` (administrateur) | `InventoryCountWDto` : `countedQuantity` (`@NotNull`, ≥ 0, 9 chiffres entiers et 3 décimales au plus) | 200 `InventoryLineRDto` (contrat d'INV-01) avec `theoreticalQuantity` figé, `countedQuantity`, `gap` | 401, 403, 404 (session inconnue, ou ligne absente de cette session), 409 (session plus `IN_PROGRESS`), 422 |

Les endpoints de lecture d'INV-01 (`GET /api/v1/inventories`, `/{id}`, `/{id}/lines`) sont inchangés ; une ligne comptée y renvoie désormais le théorique figé et son écart.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.inventory.service.InventoryCountService` | `InventoryLineRDto countLine(UUID sessionId, UUID lineId, InventoryCountWDto request)` | Comptage d'une ligne (`InventoryCountServiceImpl.java:65`). Service séparé d'`InventoryService` pour ne pas alourdir l'ouverture. |
| `feature.inventory.domain.InventoryLine` | `boolean count(BigDecimal counted, BigDecimal expected, Instant at)` (`InventoryLine.java:76`) | Seule façon d'écrire `counted_quantity`, `expected_quantity` et `counted_at` ; renvoie `false` si le même comptage est rejoué (rien ne change). |
| `feature.inventory.repository.InventoryLineRepository` | `findByIdAndSessionId(UUID, UUID)` (`InventoryLineRepository.java:23`) | Une ligne n'est atteinte qu'à travers sa session. |

Contrôleur : `feature/inventory/controller/InventoryCountController.java` (même préfixe `/api/v1/inventories` que `InventoryController`).

### Frontend (`features/inventory`)

- `data-access/inventory-service.ts` :
  - `current()` (l. 65) lit la session `IN_PROGRESS` la plus récente puis **toutes** ses lignes (pages de 100 lues en parallèle, `allLines`, l. 95) ; `session` vaut `null` s'il n'y a pas de session en cours ; `previous` est vide (INV-05).
  - `count(sessionId, lineId, { countedQuantity })` (l. 80) : `PUT` via `OutboxService` (file hors ligne, `Idempotency-Key`).
  - `close()` reste une démonstration (INV-04).
- `data-access/inventory-sheet.ts` : `toInventorySheet` (session + lignes serveur → feuille affichée), `toLocalMinute` (instant ISO → `AAAA-MM-JJTHH:MM` local), `parseQuantity` (vide = non compté, négatif ramené à 0, arrondi à 3 décimales, virgule acceptée), `gapOf` (écart arrondi à 3 décimales).
- `data-access/inventory-models.ts` : `InventoryCountWDto` ; `InventorySheet` porte maintenant `id`, `locationLabel` (au lieu du code de démonstration), `openedBy` = responsable ; `InventorySheetLine` porte `lineId` et un `unitCost` nullable ; `InventoryContextRDto.session` est nullable ; `PreviousSessionRDto.locationLabel`.
- `data-access/inventory-state.ts` : `setCount` (l. 77) met à jour l'écart à chaque frappe ; `applyCount` (l. 83) remplace le théorique par celui figé par le serveur ; chaque `InventoryRow` expose `theoretical`.
- `pages/inventory-page` : `saveCount` (l. 113) sur l'événement `change` du champ (sortie du champ ou Entrée) ; toast `danger` `inventory.countFailed` si le serveur refuse ; état « aucune session en cours » avec lien vers `/inventory/new` ; liste des sessions précédentes vide avec message `inventory.previousEmpty`.
- `data-access/inventory-fixtures.ts` supprimé (plus aucun usage).
- Traductions ajoutées : `inventory.noSession`, `inventory.countFailed`, `inventory.previousEmpty` (fr, mg).

### Données

- Migration `2026_11_08-81-changelog.sql` (1 changeset, rollback fourni) : permission `inventory.update` accordée à `ADMIN`. Aucune nouvelle table : les colonnes `expected_quantity`, `counted_quantity`, `counted_at` d'`inventory_lines` existaient (INV-01).
- Journal : action `UPDATE`, ressource `INVENTORY_SESSION`, id de la session, libellé `Comptage de ART-0031 dans INV-0012 : 10 compté(s), 12 attendu(s), écart -2`. Pas de ligne si le même comptage est rejoué.

## Règles métier pour les autres tickets

- **Théorique figé au comptage** : `expected_quantity` = stock de l'emplacement (`StockService.getQuantities`) au moment du `PUT`. Un recomptage refige le théorique. L'écart affiché d'une ligne comptée est donc `counted_quantity − expected_quantity`.
- **Ligne comptée** = `counted_quantity` non nul. Un comptage ne peut pas être « effacé » (pas de `null` accepté) : on le corrige par un nouveau comptage.
- **Session close** : toute saisie est refusée en 409 (`InventoryCountServiceImpl.sessionInProgress`, l. 83). INV-03 (motif) doit reprendre le même contrôle.
- **Motif (INV-03)** : il n'est pas encore enregistré ; il reste local à la page (`InventoryState.reasons`). INV-03 ajoute son endpoint et l'exige seulement si `gap ≠ 0`.
- **Clôture (INV-04)** : ne pas réutiliser `expected_quantity` pour l'ajustement ; la règle d'INV-01 s'applique (quantité signée `counted − stock actuel de l'emplacement`, via `StockMovementService.record`, type `INVENTORY`).
- **Droits** : seul l'administrateur compte (`inventory.update`), même si le responsable de la session est une vendeuse ou un livreur.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/inventory/`)
- `controller/InventoryCountController.java`
- `service/InventoryCountService.java` ; `service/impl/InventoryCountServiceImpl.java`
- `domain/InventoryLine.java` (méthode `count`)
- `repository/InventoryLineRepository.java` (`findByIdAndSessionId`)
- `dto/InventoryCountWDto.java`

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-81-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/features/inventory/`)
- `data-access/inventory-service.ts`, `inventory-sheet.ts` (nouveau), `inventory-models.ts`, `inventory-state.ts` ; `inventory-fixtures.ts` supprimé
- `pages/inventory-page/inventory-page.ts`, `.html`, `.css` ; `inventory.routes.ts` (commentaire)
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json` ; `src/app/features/demo-pages.a11y.spec.ts` (page d'inventaire retirée de la liste des pages de démonstration)

**Tests**
- Backend : `feature/inventory/service/impl/InventoryCountServiceImplTest`, `feature/inventory/controller/InventoryCountIT`
- Frontend : `pages/inventory-page/inventory-page.spec.ts` (avec contrôle axe), `data-access/inventory-service.spec.ts`

## Dépendances

- Dépend de : [INV-01](../INV-01/guide.md) (sessions, lignes, `InventoryMapper.toRDto`), [JNL-01](../JNL-01/guide.md) (`JournalService.record`), [STK-01](../STK-01/guide.md) (`StockService.getQuantities`), [SYS-07](../SYS-07/guide.md) (file hors ligne).
- Débloque : [INV-03](../INV-03/description.md) (motif d'écart par ligne), [INV-04](../INV-04/description.md) (clôture).

## Comment l'utiliser depuis un autre ticket

Enregistrer un comptage depuis le front :

```ts
inventoryService
  .count(session.id, line.lineId, { countedQuantity: 10 })
  .subscribe((result) => result.body && state.applyCount(line.articleId, result.body.theoreticalQuantity));
```

Compter les lignes encore à compter (INV-04, avant clôture) : `InventoryLine.getCountedQuantity() == null`.

## Points d'attention

- **Hypothèse : session affichée** = la session `IN_PROGRESS` la plus récente, tous emplacements confondus. S'il y a plusieurs sessions en cours (une par emplacement), les autres ne sont pas accessibles depuis la page : un sélecteur de session ou une route `/inventory/:id` est à ajouter (INV-05 ou ticket UX).
- **Hypothèse : enregistrement à la validation du champ** (`change` : sortie du champ ou Entrée), pas à chaque frappe ; l'écart, lui, est recalculé à chaque frappe (critère). Vider le champ n'envoie rien : la valeur serveur revient au rechargement.
- **Hypothèse : quantités décimales** acceptées (3 décimales, comme `stock_movements.quantity`) pour les matières premières au kg/L ; aucune règle n'impose un entier pour les articles à la pièce.
- **Hypothèse : comptage réservé à l'administrateur** (user story) via la nouvelle permission `inventory.update`, même si le responsable désigné à l'ouverture est une vendeuse.
- **Concurrence** : pas de `version` dans `InventoryCountWDto` ; deux saisies simultanées sur la même ligne → la dernière gagne (verrou optimiste JPA seulement pendant la transaction).
- **Mention « Ventes bloquées sur cet emplacement pendant le comptage »** de la maquette : toujours rien côté serveur ; le gel du théorique au comptage limite l'effet des ventes, mais une vente entre le comptage et la clôture sera traitée par INV-04 (ajustement sur le stock actuel).
- **Sessions précédentes** : liste vide en attendant INV-05 (les données de démonstration ont été retirées).
- Motifs (INV-03) et clôture (INV-04) toujours locaux / simulés : le bandeau « en construction » de la route reste affiché.
- Traductions malgaches rédigées sans relecture native.
- Branche empilée sur `feature/INV-01-ouverture-session-inventaire` : à merger après INV-01.
