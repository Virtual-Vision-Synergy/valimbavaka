# Guide d'intégration — STK-01 · Suivi du stock par emplacement

Statut : ✅ écrit le 2026-10-29 — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#30](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/30) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le stock d'un article n'est plus une donnée saisie : c'est la **somme de ses mouvements typés** (`stock_movements`), par emplacement. L'API en lecture seule donne, pour chaque article actif, la quantité dans chacun des quatre emplacements et le total. Les autres tickets ne modifient jamais un stock : ils appellent le port Java `StockMovementService.record(...)`, qui contrôle la règle, écrit le mouvement et le trace au journal dans leur transaction.

## Contrat exposé

### Endpoints backend (`/api/v1/stocks`)

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/stocks` | `stocks.read` (administrateur, vendeuse) | `PageQuery` (`page` base 1, `size` ≤ 100, `search`, `sortBy`, `sortDirection`) | 200 `PageResponse<StockRDto>` | 401, 403, 422 (`size` > 100) |
| GET | `/api/v1/stocks/{articleId}` | `stocks.read` | — | 200 `StockRDto` | 401, 403, 404 |

- Aucune écriture : pas de `POST`, `PUT`, `PATCH` ni `DELETE` (`StockSecurityIT.stocks_writeVerbs_areNotExposed`).
- La liste ne contient que les **articles actifs** ; tri, recherche et pagination sont ceux des articles (`createdAt` par défaut, `code`, `name`, `type`, `price`, voir [ART-01](../ART-01/guide.md)).
- `StockRDto` : `articleId`, `code`, `name`, `rangeId`, `unit`, `threshold`, `cost` (peut être nul), `total`, `locations`.
- `locations` : une entrée par emplacement **actif du référentiel**, zéro compris : `locationId`, `code` (`WAREHOUSE`, `STORE`, `DELIVERY`, `TRADE_SHOW`), `label`, `quantity`. Quantités à 3 décimales au maximum.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.stock.service.StockMovementService` | `StockMovementRDto record(StockMovementCommand command)` | **Seule** façon de changer un stock. À appeler depuis une méthode `@Transactional` de la feature appelante. |
| `feature.stock.service.StockService` | `getStocks(PageQuery)`, `getStock(UUID articleId)` | Lecture du stock (utilisée par le contrôleur ; réutilisable par STK-02, STK-03, STK-04, ART-06). |

`StockMovementCommand(articleId, locationId, type, quantity, referenceId)` : `quantity` est **signée** (positive = entrée dans l'emplacement, négative = sortie), `referenceId` est l'opération métier à l'origine (vente, transfert, lot…), facultatif.

Types (`StockMovementType`) et signe accepté (`accepts`) : `RECEIPT` et `RETURN` > 0 ; `SALE`, `DELIVERY` et `SHOOTING` < 0 ; `TRANSFER`, `TRADE_SHOW`, `PRODUCTION` et `INVENTORY` de l'un ou l'autre signe ; zéro jamais.

Ajouts à d'autres features : `ReferentialService.isActiveStockLocation(UUID)` (vrai si l'emplacement existe et est actif), voir [SYS-05](../SYS-05/guide.md).

### Frontend (`features/stocks`)

- Route `/stocks` (permission `stocks.read`), page `pages/stock-page` **inchangée**.
- `data-access/stock-service.ts` : `list()` lit `GET /api/v1/stocks` (une page de 100, triée par nom) et les gammes, puis renvoie des `StockRowRDto` ; lecture seule, pas de file hors ligne.
- `data-access/stock-mapping.ts` : `toStockRow` convertit un `StockRDto` en ligne d'affichage. Les codes du serveur `STORE` et `TRADE_SHOW` deviennent `SHOP` et `SALON` côté interface.
- `data-access/stock-models.ts` : `StockRDto`, `StockLocationQuantityRDto` (types du backend) et `StockRowRDto` (ligne d'affichage).
- Les fixtures (`stock-fixtures.ts`) ne servent plus au service ; elles restent pour les tests éventuels.

### Données

- Table `stock_movements` : `id` UUID v7, `article_id` (→ `articles`), `location_id` (→ `stock_locations`), `type` (CHECK sur les 9 types), `quantity` `NUMERIC(12,3)` non nulle, `reference_id`, `occurred_at` `TIMESTAMPTZ` (posé à l'insertion).
- Append-only garanti **en base** par les triggers `trg_stock_movements_append_only` (UPDATE / DELETE) et `trg_stock_movements_no_truncate` : une correction est un mouvement inverse.
- Pas de table de solde : le stock est toujours recalculé par `sum(quantity)`.
- Journal : ressource `STOCK_MOVEMENT`, action `CREATE`, libellé `Mouvement RECEIPT de l'article ART-0031 · Savon noir 100 g : 12`.
- Migration : `2026_10_29-02-changelog.sql` (2 changesets, rollback fourni). La permission `stocks.read` existait déjà (AUTH-01).

## Règles métier pour les autres tickets

- **Ne jamais écrire dans `stock_movements` ni calculer un stock à la main** : passer par `StockMovementService.record`. Ne pas créer de repository qui modifie ou efface une ligne.
- **Un transfert = deux mouvements** de type `TRANSFER` dans la même transaction : une quantité négative sur la source, la même quantité positive sur la destination. Les deux réussissent ou aucun. Même logique pour une production (`PRODUCTION` : sortie des matières, entrée du fini).
- **Un retour ou une annulation** se fait par un mouvement inverse (`RETURN`, ou le type d'origine avec le signe contraire), jamais par suppression.
- **Le stock ne descend jamais sous zéro** dans un emplacement : le service lève `ConflictException` (409) avec le solde disponible. L'inventaire (`INV-xx`) enregistre donc un **écart** (différence entre le compté et le théorique), pas le compté.
- Le contrôle du solde est protégé par un verrou transactionnel (`pg_advisory_xact_lock`) sur le couple article / emplacement : deux sorties simultanées ne dépassent pas le stock.
- Un mouvement sur un article **désactivé** est accepté (un retour reste possible) ; sur un article inconnu, 404.
- Les emplacements se référencent par UUID (`locationId`) ; le `code` ne sert qu'aux règles (ex. `DELIVERY`).
- Réconciliation de clôture (JR-04) : le solde d'un article et d'un emplacement est `StockMovementRepository.balance(articleId, locationId)`, ou `balances(articleIds)` pour plusieurs articles.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/stock/`)
- `controller/StockController.java`
- `service/StockService.java`, `service/StockMovementService.java` ; `service/impl/StockServiceImpl.java`, `StockMovementServiceImpl.java`, `StockMovementPolicy.java` (règles d'un mouvement : article, emplacement, signe, décimales, solde)
- `domain/StockMovement.java` (entité `@Immutable`), `domain/StockMovementType.java`, `domain/StockMovementCommand.java`
- `repository/StockMovementRepository.java` (étend `Repository`, pas `JpaRepository`), `repository/StockBalance.java`
- `dto/StockRDto.java`, `dto/StockLocationQuantityRDto.java`, `dto/StockMovementRDto.java` ; `mapper/StockMapper.java`, `mapper/StockMovementMapper.java`
- Autres features modifiées : `feature/referential/service/ReferentialService.java` (+ `impl`), `feature/referential/repository/StockLocationRepository.java` (`existsByIdAndActiveTrue`)

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_29-02-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/features/stocks/`)
- `data-access/stock-service.ts`, `stock-mapping.ts`, `stock-models.ts` ; `stocks.routes.ts` (commentaire)

**Tests**
- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/stock/` (`StockMovementTypeTest`, `StockMovementPolicyTest`, `StockMovementServiceImplTest`, `StockServiceImplTest`, `StockMovementRepositoryIT`, `StockSecurityIT`, `StockReadIT`)
- Frontend : `data-access/stock-mapping.spec.ts`, `data-access/stock-service.spec.ts`

## Dépendances

- Dépend de : [ART-01](../ART-01/guide.md) (`ArticleService.getArticles`, `getArticleById`), [JNL-01](../JNL-01/guide.md) (`JournalService.record`), [SYS-05](../SYS-05/guide.md) (emplacements, permission `stocks.read`).
- Débloque : ART-06, STK-02, STK-03, STK-04, TRF-03, PRD-01, SHO-01, LIV-01, JR-01, VTE-01, INV-01, SRV-02, STK-06 (voir la liste de la [description](description.md)).

## Comment l'utiliser depuis un autre ticket

Réception d'un lot fournisseur dans l'entrepôt (STK-06), dans une méthode `@Transactional` :

```java
stockMovementService.record(new StockMovementCommand(
        articleId, warehouseLocationId, StockMovementType.RECEIPT, new BigDecimal("12"), receiptId));
```

Côté interface, ne rien écrire : relire `StockService.list()` (`GET /api/v1/stocks`) après l'opération.

## Points d'attention

- **Aucun endpoint de saisie** : ce ticket livre le port Java et la lecture. Les écrans qui créent des mouvements (réception, transfert, vente, inventaire…) sont dans leurs tickets ; avant eux, le stock réel reste à 0.
- **Une seule page de 100 articles** est affichée par la page des stocks (limite du serveur). Au-delà, il faut paginer ou filtrer côté interface : à reprendre avec **STK-02** (jauge) ou **STK-05**.
- **Colonnes encore provisoires** de la page : fournisseur principal, délai de livraison et ventes journalières valent `null`, `null`, `0` (le bon de réapprovisionnement ne propose donc rien d'utile) — à brancher par **STK-05**. `unitCost` vient du coût de l'article, 0 s'il est vide ; la valeur du stock est reprise par **STK-03**.
- **Seuil par emplacement** : la règle `seuil / 4` du front (`thresholdAt`) est inchangée ; **STK-04** la remplacera.
- Aucune donnée existante n'est migrée : il n'y avait pas de stock côté serveur avant ce ticket.
- Le journal indique l'auteur du mouvement ; la table ne porte pas d'`created_by` (évite de dupliquer le journal).
- Tests non exécutés : voir le scénario avant de s'appuyer sur ce contrat.
