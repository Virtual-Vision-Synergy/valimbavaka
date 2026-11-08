# Guide d'intégration — INV-05 · Historique des sessions d'inventaire

Statut : ✅ écrit le 2026-11-08 — tests exécutés en CI (voir « Résultat de l'exécution » du scénario) · Ticket : [#81](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/81) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Chaque session d'inventaire porte désormais son **nombre d'écarts** (`gapCount` : lignes comptées dont le compté diffère du théorique figé) à côté de son statut et de son nombre d'articles. La page `/inventory/history` liste toutes les sessions, les plus récentes d'abord, filtrables par statut ; la feuille de comptage affiche les 5 dernières sessions clôturées avec leur nombre d'écarts. Aucun nouvel endpoint ni aucune migration : on enrichit la lecture d'INV-01. Plus rien n'est en démonstration dans l'inventaire : le bandeau « en construction » est retiré.

## Contrat exposé

### Endpoints backend

Inchangés (INV-01), sortie enrichie :

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/inventories` | `inventory.read` | `PageQuery` (`sortBy` ∈ `createdAt`, `number`, `countDate` ; défaut `createdAt` DESC) + `status`, `locationId` facultatifs | 200 `PageResponse<InventorySessionRDto>` | 401, 403, 422 |
| GET | `/api/v1/inventories/{id}` | `inventory.read` | — | 200 `InventorySessionRDto` | 401, 403, 404 |

- `InventorySessionRDto` (`dto/InventorySessionRDto.java`) gagne **`gapCount`** (`long`), placé juste après `lineCount`. Une session ouverte renvoie `gapCount` 0 (`POST /api/v1/inventories`).
- Historique des sessions closes : `GET /api/v1/inventories?status=CLOSED` ; une session close a toujours `closedAt`.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.inventory.service.InventoryService` | `getSessions(PageQuery, InventoryStatus, UUID)`, `getSession(UUID)` | Sessions avec `lineCount` et `gapCount` (`InventoryServiceImpl.java:84`, `:94`). |
| `feature.inventory.repository.InventoryLineRepository` | `List<InventoryLineTally> tallies(Collection<UUID> sessionIds)` (l. 34) | **Une** requête groupée pour toute une page : nombre de lignes et de lignes en écart par session (remplace le `countBySessionId` par session d'INV-01, supprimé). |
| `feature.inventory.domain.InventoryLineTally` | record `(sessionId, lineCount, gapCount)`, `none(UUID)` | Résultat de la requête ; `none` pour une session sans ligne lue. |
| `feature.inventory.mapper.InventoryMapper` | `toRDto(InventorySession, StockLocationRDto, InventoryLineTally)` (l. 19) | Signature changée : reçoit le décompte au lieu du seul nombre de lignes. |

Règle SQL de l'écart (`InventoryLineRepository.tallies`) : `counted_quantity <> expected_quantity` ; une ligne non comptée (nulls) n'est pas un écart, et la comparaison est numérique (`12.000` = `12`), comme `InventoryLine.hasGap`.

### Frontend (`features/inventory`)

- Route `/inventory/history` (`inventory.routes.ts:24`, protégée par le guard `inventory.read` de la route parente) : page `pages/inventory-history-page` (en-tête, pastilles de statut « Toutes / Clôturée / En cours / Annulée », tableau session · comptage prévu · clôturée le · statut · articles · écarts, pagination serveur de 20).
- `data-access/inventory-service.ts` : `current()` (l. 66) lit, si une session est en cours, ses lignes **et** les 5 dernières sessions `CLOSED` en parallèle (`forkJoin`) ; `previous` vaut `[]` sans session en cours. L'historique utilise `list(query)` (inchangé).
- `data-access/inventory-models.ts` : `InventorySessionRDto.gapCount` ; `InventoryContextRDto.previous` est maintenant `InventorySessionRDto[]` ; le type de maquette `PreviousSessionRDto` (avec `gapUnits`) est **supprimé**.
- `data-access/inventory-sheet.ts` : `closedOn(session)` (l. 18) = jour local de `closedAt`, sinon `countDate`.
- `pages/inventory-page` : carte « Sessions précédentes » branchée (« 3 écart(s) »), lien « Voir l'historique des sessions » dans la carte et dans l'état « aucune session en cours ».
- Route `''` : `data: { underConstruction: true }` retiré.
- Traductions ajoutées (fr, mg) : `inventory.gapCount`, `inventory.historyLink`, `inventory.history.*`.

### Données

- Aucune migration (plage 80 à 89 de la voie E non utilisée par ce ticket). Colonnes `inventory_lines.counted_quantity` / `expected_quantity` (INV-01, remplies par INV-02).
- Journal : rien (lecture seule).

## Règles métier à respecter par les autres tickets

- **Écart = ligne comptée dont le compté ≠ théorique figé** (`InventoryLine.hasGap`). On compte des lignes, pas des unités ni une valeur.
- Le nombre d'écarts d'une session close **ne bouge plus** : la clôture refuse toute nouvelle saisie (INV-04).
- Toute nouvelle vue de sessions doit passer par `InventoryServiceImpl.toRDto` / `talliesOf` pour garder une seule requête par page.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/inventory/`)
- `domain/InventoryLineTally.java` (nouveau)
- `repository/InventoryLineRepository.java` (`tallies`, `countBySessionId` retiré)
- `dto/InventorySessionRDto.java` (`gapCount`), `mapper/InventoryMapper.java`
- `service/InventoryService.java` (javadoc), `service/impl/InventoryServiceImpl.java` (`talliesOf`, `toRDto`)
- `controller/InventoryController.java` (résumé OpenAPI)

**Frontend** (`valimbavaka-ui/src/app/features/inventory/`)
- `pages/inventory-history-page/` (nouveau : ts, html, css, spec)
- `inventory.routes.ts`, `data-access/inventory-models.ts`, `inventory-service.ts`, `inventory-sheet.ts`
- `pages/inventory-page/inventory-page.ts`, `.html`, `.css`
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `InventoryServiceImplTest` (3 tests ajoutés), `InventoryClosingIT` (4 tests ajoutés), `InventoryClosingServiceImplTest` (`closedView`)
- Frontend : `inventory-history-page.spec.ts` (nouveau), `inventory-page.spec.ts`, `inventory-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [INV-04](../INV-04/guide.md) (sessions `CLOSED`, `closedAt`), [INV-02](../INV-02/guide.md) (`expected_quantity` figé au comptage), [INV-03](../INV-03/guide.md) (`hasGap`), [INV-01](../INV-01/guide.md) (lecture paginée des sessions).

**Ces tickets s'appuient sur lui** :
- _(aucun déclaré)_ ; utile aux rapports (RPT) qui voudraient la régularité des inventaires.

## Comment l'utiliser depuis un autre ticket

```ts
inventoryService
  .list({ page: 1, size: 20, sortBy: 'createdAt', sortDirection: 'DESC', status: 'CLOSED' })
  .subscribe((page) => page.items.map((s) => `${s.number} : ${s.gapCount} écart(s)`));
```

## Points d'attention

- **Hypothèse : « nombre d'écarts » = nombre de lignes en écart** (critère), et non la somme signée des unités qu'affichait la maquette (`gapUnits`, supprimé). Si le PO veut aussi l'écart en unités ou en valeur, l'ajouter à `tallies` (somme de `counted_quantity − expected_quantity`).
- **Hypothèse : « consultable » = listé avec statut, nombre d'articles et nombre d'écarts** sur la page d'historique ; il n'y a pas d'écran de détail d'une session passée (ses lignes restent lisibles par `GET /api/v1/inventories/{id}/lines`). Un panneau de détail (lignes en écart et motifs) est à ajouter si le PO le demande.
- **Hypothèse : l'historique liste toutes les sessions** (en cours, clôturées, annulées) avec un filtre de statut ; la carte de la feuille de comptage n'affiche que les 5 dernières **clôturées**.
- « Clôturée le » affiche `—` pour une session non close ; la date est le jour local (fuseau du navigateur) de `closedAt`.
- Pas de tri par date de clôture (`closedAt` n'est pas dans la liste blanche) : l'ordre est celui de l'ouverture (`createdAt` DESC).
- Le guard front `inventory.read` est porté par la route parente ; la vendeuse reçoit 403 côté serveur (INV-01).
- Traductions malgaches rédigées sans relecture native.
- Branche empilée sur `feature/INV-04-cloture-session-inventaire` : à merger après INV-04.
