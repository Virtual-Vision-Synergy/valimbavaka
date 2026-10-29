# Scénarios de test — STK-01 · Suivi du stock par emplacement

Statut : ✍ rédigé le 2026-10-29 — **tests écrits, non exécutés** (voir « Résultat de l'exécution ») · Ticket : [#30](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/30) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible. Légende : ☐ écrit, pas encore exécuté · 🧪 recette manuelle.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôles utilisés | administrateur (`admin.test@valimbavaka.mg`), vendeuse (`stocks.read`), livreur (sans `stocks.read`) |
| Données de départ | emplacements `WAREHOUSE`, `STORE`, `DELIVERY`, `TRADE_SHOW` (migration SYS-05) ; un article actif « Savon noir 100 g » créé par `POST /api/v1/articles` (ART-01) |
| Tickets à avoir déployés avant | ART-01, JNL-01, SYS-05 |

## Scénarios issus des critères d'acceptation

### Critère 1 — Le stock d'un article détaille la quantité par emplacement et le total

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Réception, transfert, vente | Entrepôt +30 (réception), −10 puis boutique +10 (transfert), boutique −3 (vente) | `GET /api/v1/stocks/{id}` : total 27, entrepôt 20, boutique 7 | `StockReadIT.getStock_afterReceiptAndTransfer_returnsQuantityPerLocationAndTotal` | ☐ |
| 1b · Calcul du détail | entrepôt 30, boutique 12,5 | total 42,5 ; un `StockLocationQuantityRDto` par emplacement actif, dans l'ordre du code | `StockServiceImplTest.getStocks_articleWithMovements_returnsQuantityPerLocationAndTotal` | ☐ |
| 1c · Somme par emplacement en base | 30 − 10 en entrepôt, +10 en boutique | deux soldes : 20 et 10 | `StockMovementRepositoryIT.balances_movementsInTwoLocations_groupsByArticleAndLocation` | ☐ |
| 1d · Article jamais mouvementé | article créé, aucun mouvement | total 0 et les 4 emplacements à 0 (jamais une liste vide) | `StockReadIT.getStock_articleNeverMoved_returnsEveryActiveLocationAtZero`, `StockServiceImplTest.getStock_articleWithoutMovement_returnsZeroInEveryLocation`, `StockMovementRepositoryIT.balance_neverMoved_returnsZero` | ☐ |
| 1e · Liste paginée | recherche du nom d'un article ayant 5 en salon | une page, `totalCount` 1, total 5 | `StockReadIT.getStocks_search_returnsThePagedStockOfTheMatchingArticle` | ☐ |
| 1f · Article désactivé | article passé à `active: false` | absent de `GET /api/v1/stocks` | `StockReadIT.getStocks_inactiveArticle_isNotListed`, `StockServiceImplTest.getStocks_onlyActiveArticlesAreRequested` | ☐ |
| 1g · Page vide | aucun article actif | liste vide, aucune requête de soldes | `StockServiceImplTest.getStocks_emptyPage_doesNotQueryTheBalances` | ☐ |
| 1h · Page des stocks (front) | réponse serveur avec `STORE` 7 et `TRADE_SHOW` 0 | la ligne affichée a `SHOP` 7 et `SALON` 0, gamme retrouvée par `rangeId` | `stock-mapping.spec.ts` (« range les quantités… », « retrouve la gamme… »), `stock-service.spec.ts` (« lit les stocks du serveur… ») | ☐ |

### Critère 2 — Le stock n'est jamais modifié directement : il vient de mouvements typés, tracés au journal

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 2 · Aucune écriture exposée | `POST /api/v1/stocks` `{"quantity":10}`, `PUT`, `PATCH`, `DELETE` en administrateur | 405 ou 4xx, rien n'est écrit | `StockSecurityIT.stocks_writeVerbs_areNotExposed` | ☐ |
| 2b · Un mouvement est tracé | réception de 12 sur l'entrepôt | une ligne `journal_entries` `CREATE` / `STOCK_MOVEMENT` ; libellé « Mouvement RECEIPT de l'article ART-0031 · Savon noir 100 g : 12 » | `StockReadIT.record_validMovement_isTracedInTheJournal`, `StockMovementServiceImplTest.record_validMovement_savesItAndTracesItInTheJournal` | ☐ |
| 2c · Mouvement refusé = rien d'écrit | vente de −5 refusée par la règle | aucun `save`, aucune ligne de journal | `StockMovementServiceImplTest.record_rejectedByThePolicy_savesNothingAndTracesNothing` | ☐ |
| 2d · Mouvement non modifiable | `update stock_movements set quantity = 500` | exception « append-only » de la base | `StockMovementRepositoryIT.update_existingMovement_isRejectedByTheDatabase` | ☐ |
| 2e · Mouvement non supprimable | `delete from stock_movements`, `truncate` | exception « append-only » | `StockMovementRepositoryIT.delete_existingMovement_isRejectedByTheDatabase`, `StockMovementRepositoryIT.truncate_table_isRejectedByTheDatabase` | ☐ |
| 2f · Types et signe | `RECEIPT`/`RETURN` > 0 ; `SALE`/`DELIVERY`/`SHOOTING` < 0 ; `TRANSFER`, `TRADE_SHOW`, `PRODUCTION`, `INVENTORY` de l'un ou l'autre signe ; 0 jamais | 9 types connus, signe contrôlé | `StockMovementTypeTest` (6 tests) | ☐ |

## Sécurité et erreurs

Endpoints : `GET /api/v1/stocks`, `GET /api/v1/stocks/{articleId}` (lecture seule). Le port Java `StockMovementService.record` n'est pas un endpoint.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/stocks` | 401 `UNAUTHENTICATED` | `StockSecurityIT.getStocks_withoutToken_returns401`, `StockSecurityIT.getStock_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `StockSecurityIT.getStocks_withDeliveryDriverRole_returns403` | ☐ |
| Rôle autorisé | jeton vendeuse (`stocks.read`) | 200 | `StockSecurityIT.getStocks_withSellerRole_returns200` | ☐ |
| Mass assignment | — | sans objet : aucun corps d'entrée, aucune écriture par l'API | — | ⏭ hors périmètre |
| Pagination invalide | `size=101` | 422 `VALIDATION_FAILED` | `StockReadIT.getStocks_sizeAbove100_returns422` | ☐ |
| Article inconnu | `GET /api/v1/stocks/{uuid aléatoire}` | 404 `NOT_FOUND` | `StockReadIT.getStock_unknownArticle_returns404`, `StockMovementPolicyTest.check_unknownArticle_propagatesNotFound` | ☐ |
| Stock insuffisant | 3 en entrepôt, vente de −4 | `ConflictException` (409 `CONFLICT`), message « 3 disponible », aucun mouvement ajouté | `StockMovementPolicyTest.check_withdrawalAboveBalance_throwsConflict`, `StockReadIT.record_withdrawalAboveBalance_throwsConflictAndLeavesNoTrace` | ☐ |
| Emplacement inconnu ou inactif | UUID d'emplacement absent du référentiel | `BusinessException` (422) | `StockMovementPolicyTest.check_inactiveLocation_throwsBusinessException` | ☐ |
| Quantité incompatible | `INVENTORY` à 0 ; `SALE` à +2 | 422 « nulle » / « de ce signe » | `StockMovementPolicyTest.check_zeroQuantity_throwsBusinessException`, `StockMovementPolicyTest.check_positiveSale_throwsBusinessException` | ☐ |
| Trop de décimales | `RECEIPT` de 0,1234 | 422 « 3 décimales au maximum » | `StockMovementPolicyTest.check_fourDecimals_throwsBusinessException` | ☐ |
| Journal | chaque mouvement accepté | une ligne `CREATE` `STOCK_MOVEMENT` | voir 2b | ☐ |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Borne exacte du stock | 3 en stock, sortie de −3 | acceptée, solde 0 | `StockMovementPolicyTest.check_withdrawalOfTheWholeBalance_isAccepted` | ☐ |
| Borne des décimales | `RECEIPT` de 0,125 (3 décimales) | acceptée | `StockMovementPolicyTest.check_threeDecimals_isAccepted` | ☐ |
| Quantité nulle en base | insertion directe de 0 | refusée par `ck_stock_movements_quantity` | `StockMovementRepositoryIT.save_zeroQuantity_isRejectedByTheDatabase` | ☐ |
| Concurrence | 3 en stock, 8 ventes de −1 en parallèle | exactement 3 acceptées, solde final 0 (verrou `pg_advisory_xact_lock`) | `StockReadIT.record_concurrentWithdrawals_neverTakeTheStockBelowZero` | ☐ |
| Verrou avant lecture du solde | mouvement valide | `lock` appelé avant `balance` | `StockMovementPolicyTest.check_validMovement_returnsTheArticleAndLocksBeforeReadingTheBalance` | ☐ |
| Emplacement inconnu de l'interface | code serveur `MOON` | ignoré dans le tableau des quantités | `stock-mapping.spec.ts` (« ignore un emplacement inconnu… ») | ☐ |
| Emplacement absent de la réponse, coût inconnu | `locations: []`, `cost: null` | quantités à 0, `unitCost` 0 | `stock-mapping.spec.ts` (« met zéro aux emplacements absents… ») | ☐ |
| Article sans gamme | `rangeId: null` | `range` nul | `stock-mapping.spec.ts` (« laisse la gamme vide… ») | ☐ |
| Aucun article actif (front) | page vide | `list()` renvoie `[]` | `stock-service.spec.ts` (« renvoie une liste vide… ») | ☐ |
| Plus de 100 articles actifs | 101 articles | seule la première page de 100 est affichée (limite connue, voir guide) | — | ⏭ STK-02 / STK-05 |
| Hors ligne | lecture seule | sans objet : pas de file d'écriture, `Idempotency-Key` non concernée | — | ⏭ hors périmètre |

## Tests existants

**Backend** (`valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/stock/`)
- `domain/StockMovementTypeTest.java`
- `service/impl/StockMovementPolicyTest.java`, `StockMovementServiceImplTest.java`, `StockServiceImplTest.java` (données : `StockTestData.java`)
- `repository/StockMovementRepositoryIT.java`
- `controller/StockSecurityIT.java`, `StockReadIT.java` (socle : `StockApiTestSupport.java`)
- `ArchitectureTest` (existant) couvre aussi la nouvelle feature : contrôleur mince, `@HasPermission`, DTO en `record`, repository sans `delete`.

**Frontend** (`valimbavaka-ui/src/app/features/stocks/data-access/`)
- `stock-mapping.spec.ts`, `stock-service.spec.ts`

## Recette manuelle (🧪)

| Cas | Exemple | Résultat attendu | Statut |
|---|---|---|---|
| Page des stocks avec de vrais mouvements | se connecter en administrateur, ouvrir `/stocks` après une réception de 30 en entrepôt | la ligne de l'article affiche 30 en entrepôt, 0 ailleurs, total 30 | 🧪 |
| Vendeuse | se connecter en vendeuse, ouvrir `/stocks` | page visible, aucun bouton de modification du stock | 🧪 |
| Livreur | se connecter en livreur | entrée « Stocks » absente du menu, `/stocks` refusé | 🧪 |

## Résultat de l'exécution

- Date : à lancer (CI ou local) — **aucun test n'a été exécuté** par le traiteur (CLAUDE.md règle n°3). Seules la compilation `mvn compile` / `test-compile`, `spotless:apply` et `prettier --write` ont tourné ; la vérification de types du front n'a pas pu être faite (dépendances npm non installées dans l'environnement de rédaction).
- Environnement : à renseigner (`mvn -B -ntp verify` avec Docker pour Testcontainers ; `npm ci && npx ng test --watch=false`).
- Tests écrits : 41 backend (21 unitaires, 20 d'intégration Testcontainers), 7 frontend.
- Anomalies ouvertes : aucune connue. Points à surveiller au premier passage : comparaisons JSON des montants (`value(27.0)`) dans `StockReadIT`, et le test de concurrence qui crée des threads sur la vraie base.
- Étapes 🧪 restantes : 3.
