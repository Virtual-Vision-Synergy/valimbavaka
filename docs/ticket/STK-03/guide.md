# Guide d'intégration — STK-03 · Quantité et valeur du stock par emplacement

Statut : ✅ écrit le 2026-10-30 · Ticket : [#32](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/32) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le serveur calcule, pour chaque emplacement actif, la **quantité totale** et la **valeur au coût d'achat en ariary** de tout le stock (tous les articles, désactivés compris, sans la limite des 100 lignes de la page). C'est une lecture réservée à l'administrateur (permission `stocks.value`). Sur la page `/stocks`, les cartes d'emplacement affichent ces totaux ; sans la permission, elles gardent le calcul local de STK-01.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/stocks/valuation` | `stocks.value` (administrateur seulement) | — | 200 `StockValuationRDto` | 401, 403 |

- `StockValuationRDto` : `quantity`, `value`, `unvaluedQuantity` (totaux de tous les emplacements actifs) et `locations`.
- `locations` : une entrée `StockLocationValueRDto` par emplacement **actif**, zéro compris, triée par code : `locationId`, `code` (`DELIVERY`, `STORE`, `TRADE_SHOW`, `WAREHOUSE`), `label`, `quantity`, `value`, `unvaluedQuantity`.
- Ce n'est pas une liste paginée : un seul objet, borné par le nombre d'emplacements.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.stock.service.StockService` | `StockValuationRDto getValuation()` | Valorisation de tout le stock par emplacement (réutilisable par un tableau de bord ou un rapport, ex. RAP-xx). |
| `feature.article.service.ArticleService` | `Map<UUID, BigDecimal> getCosts(Collection<UUID> articleIds)` | Coût d'achat de plusieurs articles en une requête ; un article inconnu ou sans coût est **absent** de la map. Ne pas appeler avec une collection vide. |
| `feature.stock.repository.StockMovementRepository` | `List<StockBalance> nonZeroBalances()` | Soldes non nuls de chaque article × emplacement, articles désactivés compris. |

### Frontend (`features/stocks`)

- Route `/stocks` inchangée (permission `stocks.read`).
- `data-access/stock-service.ts` : `valuation()` lit `GET /api/v1/stocks/valuation` et renvoie des `LocationTotals`.
- `data-access/stock-mapping.ts` : `toLocationTotals(valuation)` range la réponse sous les emplacements de l'interface (`STORE` → `SHOP`, `TRADE_SHOW` → `SALON`) ; `ALL` reprend les totaux du serveur. Types `LocationTotal` (`units`, `value`, `unvaluedUnits`) et `LocationTotals`.
- `data-access/stock-models.ts` : `StockValuationRDto`, `StockLocationValueRDto` (types du backend).
- `components/location-cards` : nouvelle entrée facultative `totals: LocationTotals | null`. Fournie, elle remplace le calcul local des unités et de la valeur, et affiche « dont N sans coût d'achat » quand une partie n'est pas valorisée.
- `pages/stock-page` : ne demande la valorisation que si `AuthStore.hasPermission('stocks.value')` ; en cas d'erreur, les cartes gardent le calcul local.
- i18n : `stocks.unvalued` (fr, mg).

### Données

- Migration `2026_10_30-01-changelog.sql` (2 changesets, rollback fourni) : permission `stocks.value` « Consulter la valeur du stock », accordée à `ADMIN`.
- Aucune table, aucune écriture, aucune ligne de journal.

## Règles métier pour les autres tickets

- **Valeur = Σ (solde de l'article dans l'emplacement × coût d'achat de l'article)**, avec le coût **actuel** de la fiche article (pas de coût moyen pondéré ni historique).
- **Arrondi** : la somme d'un emplacement est arrondie à 2 décimales (`HALF_UP`) une seule fois ; les totaux additionnent les valeurs arrondies des emplacements (ils correspondent donc à la somme des cartes).
- **Article sans coût d'achat** : sa quantité compte dans `quantity` et dans `unvaluedQuantity`, pas dans `value`. Ne pas le valoriser à 0 en silence.
- **Article désactivé** : toujours valorisé (le stock existe physiquement). Le tableau de STK-01 n'affiche que les articles actifs : la somme des lignes peut donc différer des cartes.
- **Emplacement désactivé** : ignoré, comme dans STK-01.
- Les quantités additionnent toutes les unités (pièce, kg, litre…) : c'est un indicateur, pas une grandeur physique homogène.
- La valeur est une donnée réservée à l'administrateur : ne pas l'exposer à un autre rôle sans nouvelle permission.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/`)
- `stock/controller/StockController.java` (`getValuation`)
- `stock/service/StockService.java`, `stock/service/impl/StockServiceImpl.java` (`getValuation`, `valueOf`)
- `stock/repository/StockMovementRepository.java` (`nonZeroBalances`)
- `stock/dto/StockValuationRDto.java`, `stock/dto/StockLocationValueRDto.java`
- `article/service/ArticleService.java`, `article/service/impl/ArticleServiceImpl.java` (`getCosts`)
- `article/repository/ArticleRepository.java` (`findCosts`), `article/repository/ArticleCost.java`

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_10_30-01-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/`)
- `features/stocks/data-access/stock-service.ts`, `stock-mapping.ts`, `stock-models.ts`
- `features/stocks/components/location-cards/location-cards.ts`, `.html`, `.css`
- `features/stocks/pages/stock-page/stock-page.ts`, `.html`
- `core/auth/auth-testing.ts` (`stocks.value` dans les permissions ADMIN de test)
- `public/i18n/fr.json`, `public/i18n/mg.json`

**Tests**
- Backend : `feature/stock/controller/StockValuationIT.java`, `feature/stock/service/impl/StockServiceImplTest.java`, `feature/article/service/impl/ArticleServiceImplTest.java`
- Frontend : `features/stocks/pages/stock-page/stock-page.spec.ts`, `data-access/stock-mapping.spec.ts`, `data-access/stock-service.spec.ts`

## Dépendances

- Dépend de : [STK-01](../STK-01/guide.md) (soldes par `stock_movements`, emplacements actifs), [SYS-05](../SYS-05/guide.md) (référentiel des emplacements, rôle administrateur), [ART-01](../ART-01/guide.md) (coût d'achat de la fiche article).
- Débloque : aucun ticket déclaré ; utile aux rapports et au tableau de bord.

## Comment l'utiliser depuis un autre ticket

Backend, dans un service en lecture :

```java
var valuation = stockService.getValuation();
var warehouse = valuation.locations().stream()
        .filter(location -> location.code().equals("WAREHOUSE"))
        .findFirst()
        .orElseThrow();
BigDecimal assetValue = valuation.value(); // ariary, 2 décimales
```

Frontend :

```ts
private readonly stockService = inject(StockService);
protected readonly valuation = rxResource({ stream: () => this.stockService.valuation() });
// valuation.value()?.ALL.value : valeur de tout le stock en ariary
```

## Points d'attention

- **Coût actuel, pas historique** : modifier le coût d'achat d'un article revalorise immédiatement tout son stock. Une valorisation au coût moyen pondéré (CMUP) demanderait de tracer le coût sur chaque mouvement d'entrée (STK-06, PRD-01).
- **Vendeuse** : `StockRDto.cost` (STK-01) reste lisible avec `stocks.read`, et la page calcule toujours la valeur des cartes côté interface pour la vendeuse. Pour cacher toute valeur à la vendeuse, il faudrait retirer `cost` de `StockRDto` pour ce rôle : décision à prendre avec le client, hors de ce ticket.
- **Cartes et tableau** : en administrateur, les cartes couvrent tout le stock, le tableau seulement les 100 premiers articles actifs (limite de STK-01). Les alertes de rupture des cartes restent calculées sur ces lignes.
- La requête lit tous les soldes non nuls en une fois (`group by` sur `stock_movements`) ; adapté au volume du catalogue, à revoir avec un solde matérialisé si les mouvements deviennent très nombreux.
