# Guide d'intégration — INV-01 · Ouverture d'une session d'inventaire

Statut : ✅ écrit le 2026-11-08 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#77](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/77) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur ouvre une **session d'inventaire** sur un emplacement en donnant quatre informations : emplacement, périmètre (tout l'emplacement ou une gamme), responsable du comptage et date du comptage. Le serveur numérote la session (`INV-0001`…), crée **une ligne à compter par article du périmètre** et trace l'ouverture au journal. Une seule session peut être en cours par emplacement. Ce ticket pose tout le domaine `feature/inventory` (sessions, lignes, statuts, motifs) sur lequel INV-02 à INV-05 se greffent sans nouvelle table.

## Contrat exposé

### Endpoints backend (`/api/v1/inventories`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/inventories` | `inventory.read` | `PageQuery` (`page`, `size` ≤ 100, `sortBy` ∈ `createdAt`, `number`, `countDate` ; défaut `createdAt` DESC) + `status`, `locationId` facultatifs | 200 `PageResponse<InventorySessionRDto>` | 401, 403, 422 |
| GET | `/api/v1/inventories/{id}` | `inventory.read` | — | 200 `InventorySessionRDto` | 401, 403, 404 |
| GET | `/api/v1/inventories/{id}/lines` | `inventory.read` | `PageQuery` (`sortBy` ∈ `articleCode`, `articleName` ; défaut `articleCode` ASC) | 200 `PageResponse<InventoryLineRDto>` | 401, 403, 404, 422 |
| POST | `/api/v1/inventories` | `inventory.create` | `InventorySessionWDto` | 201 `InventorySessionRDto` + `Location` | 401, 403, 409, 422 |

- `InventorySessionWDto` : `locationId`, `scope` (`ALL` \| `RANGE`), `rangeId` (obligatoire avec `RANGE`, interdit avec `ALL`), `responsibleId`, `countDate` (`AAAA-MM-JJ`). Les 4 premiers champs requis sont `@NotNull` (422 si absents) ; `id`, `status`, `number` envoyés sont ignorés.
- `InventorySessionRDto` : `id`, `number`, `locationId`, `locationCode`, `locationLabel`, `scope`, `rangeId`, `responsibleId`, `responsibleName`, `countDate`, `status`, `openedAt`, `openedBy`, `closedAt`, `lineCount`, `version`.
- `InventoryLineRDto` : `id`, `articleId`, `articleCode`, `articleName`, `barcode`, `unitCost` (coût actuel de l'article, peut être nul), `theoreticalQuantity`, `countedQuantity`, `gap` (= compté − théorique, nul si non compté), `reason`.
- 422 à l'ouverture : emplacement inconnu ou inactif, gamme absente / en trop / inactive, responsable inconnu ou désactivé, date avant aujourd'hui (fuseau `Indian/Antananarivo`), périmètre sans article. 409 : session déjà `IN_PROGRESS` sur l'emplacement.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.inventory.service.InventoryService` | `openSession(InventorySessionWDto)`, `getSession(UUID)`, `getSessions(PageQuery, InventoryStatus, UUID)`, `getLines(UUID, PageQuery)` | Ouverture et lecture (INV-02 à INV-05 y ajoutent leurs méthodes). |
| `feature.stock.service.StockService` | `Map<UUID, BigDecimal> getQuantities(UUID locationId, Collection<UUID> articleIds)` (**ajouté ici**, `StockService.java:39`) | Stock actuel d'articles dans un emplacement ; article absent = 0. Sert au théorique des lignes et au choix des articles désactivés à compter. |

Classes internes (`service/impl`) : `InventoryOpeningPolicy.check` (`InventoryOpeningPolicy.java:45`, règles d'ouverture), `InventoryScopeResolver.articlesToCount` (`InventoryScopeResolver.java:36`, articles du périmètre), `InventoryServiceImpl.openSession` (`InventoryServiceImpl.java:108`).

### Frontend (`features/inventory`)

- Route `/inventory/new` (guard `inventory.create`) : page `pages/inventory-open-page` (formulaire réactif, 4 champs, gamme affichée seulement en `RANGE`, responsable = administrateur connecté par défaut parmi les comptes actifs, date = aujourd'hui). Lien « Ouvrir une session » ajouté dans l'en-tête de `pages/inventory-page`.
- `data-access/inventory-service.ts` : `list(query)`, `getById(id)`, `lines(id, query)` en `HttpClient` ; `open(body)` via `OutboxService` (file hors ligne, `Idempotency-Key`). `current()` et `close()` restent en démonstration (fixtures).
- `data-access/inventory-models.ts` : types serveur `InventorySessionWDto`, `InventorySessionRDto`, `InventoryLineRDto`, `InventoryScope`, `InventoryStatus` (+ `CANCELLED`), `InventorySessionQuery`. Les types de la maquette ont été **renommés** `InventorySheet` / `InventorySheetLine` (ils ne correspondent à aucun DTO serveur).
- Traductions `inventory.open.*`, `inventory.scope.*`, `inventory.status.CANCELLED` (fr et mg).

### Données

- Migration `2026_11_08-80-changelog.sql` (4 changesets, rollback fourni) :
  - séquence `inventory_session_number_seq` (numéro `INV-%04d`, trous possibles, jamais réutilisé) ;
  - table `inventory_sessions` : `number` unique, `location_id` (→ `stock_locations`), `scope` + `range_id` (CHECK : `ALL` sans gamme, `RANGE` avec), `responsible_id` (→ `users`), `responsible_name` (nom figé à l'ouverture), `count_date`, `status` (CHECK `IN_PROGRESS`, `CLOSED`, `CANCELLED`), `closed_at`, colonnes d'audit et `version` ; **index unique partiel** `uq_inventory_sessions_location_in_progress` (une session en cours par emplacement) ;
  - table `inventory_lines` : `session_id`, `article_id`, unique (`session_id`, `article_id`), `article_code` / `article_name` / `barcode` figés, et colonnes **prévues pour la suite**, nulles à l'ouverture : `expected_quantity`, `counted_quantity` (CHECK ≥ 0), `reason` (CHECK sur les 5 motifs de la maquette), `counted_at` ;
  - permission `inventory.create` (administrateur). `inventory.read` existait (AUTH-01).
- Journal : ressource `INVENTORY_SESSION`, action `CREATE`, libellé `Ouverture de la session d'inventaire INV-0012 · Boutique (12 articles)`.

## Règles métier pour les autres tickets

- **Cycle de vie** (`InventoryStatus`) : `IN_PROGRESS` à l'ouverture → `CLOSED` (INV-04, renseigner `closed_at`) ou `CANCELLED` (pas encore d'endpoint). Une session ne se supprime jamais (`OperationRepository`, aucun `delete`). Refuser toute saisie de comptage ou de motif sur une session qui n'est plus `IN_PROGRESS` (409).
- **Une seule session en cours par emplacement** : contrôle applicatif + index unique partiel. Toute nouvelle façon de remettre une session en cours doit respecter cette règle.
- **Périmètre figé à l'ouverture** : les lignes sont créées une fois pour toutes (articles actifs du périmètre + articles désactivés encore en stock dans l'emplacement, triés par code). Un article créé après l'ouverture n'est pas dans la session.
- **Théorique** (`InventoryMapper.toRDto(InventoryLine, …)`, `InventoryMapper.java:39`) : tant que la ligne n'est pas comptée, c'est le stock **actuel** de l'emplacement (`StockService.getQuantities`) ; une fois comptée, c'est `expected_quantity`. **INV-02 doit donc écrire, au moment du comptage, `counted_quantity`, `expected_quantity` = stock de l'emplacement à cet instant et `counted_at`**, puis l'écart affiché est `counted − expected`. Ajouter pour cela une méthode métier sur `InventoryLine` (aucun setter) et une permission `inventory.update` (nouvelle migration de la plage de la voie).
- **Motif** (INV-03) : colonne `reason`, enum `InventoryReason` (`BREAKAGE`, `LOSS`, `ENTRY_ERROR`, `UNDECLARED_SHOOTING`, `OTHER`) déjà en base et dans `InventoryLineRDto`. Exiger un motif seulement si l'écart ≠ 0.
- **Clôture** (INV-04) : ne jamais écrire le compté dans le stock ; passer par `StockMovementService.record` avec le type `INVENTORY` et la quantité **signée** `counted − stock actuel de l'emplacement`, `referenceId` = id de la session (règle STK-01), dans une seule transaction, puis `status = CLOSED`. Une ligne à écart nul ne crée pas de mouvement (quantité 0 interdite).
- **Historique** (INV-05) : `GET /api/v1/inventories?status=CLOSED` donne déjà la liste paginée ; le nombre d'écarts est à ajouter (compter les lignes à `counted_quantity <> expected_quantity`).
- Le responsable est **tout compte actif** (administrateur, vendeuse ou livreur) ; seule l'ouverture exige `inventory.create`.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/inventory/`)
- `controller/InventoryController.java`
- `service/InventoryService.java` ; `service/impl/InventoryServiceImpl.java`, `InventoryOpeningPolicy.java`, `InventoryScopeResolver.java`
- `domain/InventorySession.java`, `InventoryLine.java`, `InventoryStatus.java`, `InventoryScope.java`, `InventoryReason.java`, `InventoryOpening.java`, `InventoryArticle.java`
- `repository/InventorySessionRepository.java`, `InventoryLineRepository.java` (étendent `OperationRepository`)
- `dto/InventorySessionWDto.java`, `InventorySessionRDto.java`, `InventoryLineRDto.java` ; `mapper/InventoryMapper.java`
- Autre feature modifiée : `feature/stock/service/StockService.java` et `service/impl/StockServiceImpl.java` (`getQuantities`)

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-80-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/features/inventory/`)
- `inventory.routes.ts`, `pages/inventory-open-page/` (ts, html, css), `pages/inventory-page/` (lien), `data-access/inventory-models.ts`, `inventory-service.ts`, `inventory-state.ts` et `inventory-fixtures.ts` (renommage de types)
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json` ; `src/app/core/auth/auth-testing.ts` (`inventory.create` pour l'administrateur de test)

**Tests**
- Backend : `feature/inventory/service/impl/InventoryOpeningPolicyTest`, `InventoryScopeResolverTest`, `InventoryServiceImplTest`, `InventoryTestData`, `feature/inventory/controller/InventoryOpeningIT`, `feature/stock/service/impl/StockServiceImplTest` (`getQuantities_*`)
- Frontend : `data-access/inventory-service.spec.ts`, `pages/inventory-open-page/inventory-open-page.spec.ts`

## Dépendances

- Dépend de : [STK-01](../STK-01/guide.md) (stock par emplacement, `StockMovementService` pour INV-04), [JNL-01](../JNL-01/guide.md) (`JournalService.record`), [SYS-05](../SYS-05/guide.md) (emplacements, gammes, comptes), [ART-01](../ART-01/guide.md) (`ArticleService.getArticles`, `getCosts`), [SYS-07](../SYS-07/guide.md) (file hors ligne).
- Débloque : [INV-02](../INV-02/description.md) (comptage), puis INV-03, INV-04, INV-05.

## Comment l'utiliser depuis un autre ticket

Lire la session et ses lignes (INV-02, page de comptage) :

```ts
const session = await firstValueFrom(inventoryService.getById(id));
const lines = await firstValueFrom(inventoryService.lines(id, { page: 1, size: 100 }));
```

Côté Java, vérifier l'état avant une saisie (INV-02 / INV-03) :

```java
InventorySessionRDto session = inventoryService.getSession(sessionId);
if (session.status() != InventoryStatus.IN_PROGRESS) {
    throw new ConflictException("La session " + session.number() + " n'est plus en cours.");
}
```

## Points d'attention

- **Hypothèse sur les « 4 informations »** : le critère n'en nomme que trois (emplacement, périmètre, responsable) ; la 4e retenue est la **date du comptage** (aujourd'hui ou plus tard). À confirmer par le PO.
- **Périmètre** : « tout l'emplacement » ou « une gamme » (référentiel SYS-05). Avec une gamme, seuls les articles rattachés à cette gamme (produits finis) sont comptés ; matières premières et emballages n'ont pas de gamme. Un périmètre par type d'article ou par liste libre n'est pas prévu.
- **Page de comptage encore en démonstration** : `pages/inventory-page` lit toujours `INVENTORY_FIXTURE` (`current()`), avec le bandeau « en construction ». Après une ouverture, l'utilisateur revient sur cette page de démonstration ; **INV-02** doit la brancher sur `GET /api/v1/inventories?status=IN_PROGRESS` puis `/{id}/lines` et remplacer `InventorySheet` / `InventorySheetLine` par les DTO serveur.
- **Ventes bloquées** : la maquette affiche « Ventes bloquées sur cet emplacement pendant le comptage ». Rien ne bloque les ventes côté serveur ; à décider (VTE / INV-02). Tant que ce n'est pas fait, le théorique d'une ligne non comptée bouge avec les ventes, d'où le gel de `expected_quantity` au moment du comptage.
- **Nombre de lignes** : la liste des sessions fait une requête `count` par session de la page (≤ 100) ; acceptable au volume prévu, à remplacer par une requête groupée si l'historique grossit (INV-05).
- **Emplacement désactivé après l'ouverture** : la session reste lisible, avec `locationCode` `INACTIVE` et le libellé « Emplacement désactivé ».
- **Annulation** d'une session : statut prévu (`CANCELLED`) mais aucun endpoint ; à ajouter si le PO le demande (`ReversibleOperationService` ne s'applique pas : ce n'est pas une opération validée).
- Traductions malgaches rédigées sans relecture native : à faire relire.
- Tests non exécutés, backend non compilé localement : voir le scénario.
