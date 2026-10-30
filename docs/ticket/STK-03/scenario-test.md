# Scénarios de test — STK-03 · Quantité et valeur du stock par emplacement

Statut : ✅ exécuté le 2026-10-30 (local + CI de la PR #169) · Ticket : [#32](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/32) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible. Légende : ✅ auto exécuté et vert · 🧪 recette manuelle.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôles utilisés | administrateur (`admin.test@valimbavaka.mg`, permission `stocks.value`), vendeuse (`stocks.read` sans `stocks.value`), livreur (ni l'une ni l'autre) |
| Données de départ | emplacements `WAREHOUSE`, `STORE`, `DELIVERY`, `TRADE_SHOW` (SYS-05) ; article « Savon noir 100 g » au coût d'achat de 3 120,50 Ar ; un article sans coût d'achat ; mouvements enregistrés par `StockMovementService.record` (STK-01) |
| Tickets à avoir déployés avant | STK-01, SYS-05 |

## Scénarios issus des critères d'acceptation

### Critère 1 — Chaque emplacement affiche sa quantité totale et sa valorisation en Ariary

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Réception puis transfert | coût 3 120,50 ; entrepôt +10 (réception), −4 puis boutique +4 (transfert) | `GET /api/v1/stocks/valuation` : entrepôt +6 et +18 723,00 Ar, boutique +4 et +12 482,00 Ar, total +31 205,00 Ar | `StockValuationIT.getValuation_afterReceiptAndTransfer_addsQuantityAndValueAtCostToEachLocation` | ✅ auto |
| 1b · Calcul par emplacement | entrepôt : 6 × 3 120,50 ; boutique : 4 × 3 120,50 + 2,5 d'un article sans coût | entrepôt 6 / 18 723,00 ; boutique 6,5 / 12 482,00 dont 2,5 non valorisés ; totaux 12,5 / 31 205,00 / 2,5 | `StockServiceImplTest.getValuation_articlesWithAndWithoutCost_returnsQuantityAndValuePerLocation` | ✅ auto |
| 1c · Arrondi à l'ariary près (2 décimales) | 0,333 × 1 000,05 = 333,01665 | valeur 333,02 (arrondi `HALF_UP`, une fois par emplacement) | `StockServiceImplTest.getValuation_fractionalQuantity_roundsTheValueToTwoDecimals` | ✅ auto |
| 1d · Article sans coût d'achat | réception de 7 en salon d'un article sans coût | quantité +7, `unvaluedQuantity` +7, valeur inchangée | `StockValuationIT.getValuation_articleWithoutCost_isCountedInQuantityButNotInValue` | ✅ auto |
| 1e · Tous les emplacements | base migrée | 4 emplacements actifs, libellés compris (zéro inclus) | `StockValuationIT.getValuation_returnsEveryActiveLocation` | ✅ auto |
| 1f · Coûts lus par le port article | `getCosts([ART-0031])` | `{ART-0031: 3120.50}` ; un article sans coût est absent | `ArticleServiceImplTest.getCosts_articlesWithCost_returnsTheCostOfEachArticle` | ✅ auto |
| 1g · Cartes de la page (administrateur) | serveur : entrepôt 1 250 unités, 3 840 000 Ar, dont 12 sans coût | carte « Entrepôt » : « 1 250 unités », « Valeur 3,84 M Ar », « dont 12 sans coût d'achat » ; carte « Tous » : « Valeur 3,84 M Ar » | `stock-page.spec.ts` (« affiche pour l'administrateur la quantité et la valeur de tout le stock… ») | ✅ auto |
| 1h · Rangement côté interface | `STORE` 6,5 / 12 482 ; totaux 12,5 / 31 205 | `SHOP` = 6,5 / 12 482 ; `ALL` = totaux du serveur ; emplacements absents à 0 | `stock-mapping.spec.ts` (`toLocationTotals`, 3 tests), `stock-service.spec.ts` (« lit la valorisation par emplacement du serveur ») | ✅ auto |

## Sécurité et erreurs

Endpoint : `GET /api/v1/stocks/valuation` (lecture seule, permission `stocks.value`).

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/stocks/valuation` | 401 `UNAUTHENTICATED` | `StockValuationIT.getValuation_withoutToken_returns401` | ✅ auto |
| Mauvais rôle : vendeuse | jeton vendeuse (`stocks.read` seulement) | 403 `FORBIDDEN` | `StockValuationIT.getValuation_withSellerRole_returns403` | ✅ auto |
| Mauvais rôle : livreur | jeton livreur | 403 `FORBIDDEN` | `StockValuationIT.getValuation_withDeliveryDriverRole_returns403` | ✅ auto |
| Vendeuse côté interface | vendeuse sur `/stocks` | aucune requête de valorisation ; cartes calculées sur les articles chargés, sans mention « sans coût » | `stock-page.spec.ts` (« ne demande pas la valorisation pour une vendeuse ») | ✅ auto |
| Mass assignment | — | sans objet : aucun corps d'entrée | — | ⏭ hors périmètre |
| Données invalides (422) | — | sans objet : aucun paramètre | — | ⏭ hors périmètre |
| Ressource introuvable (404), conflit (409) | — | sans objet : agrégat de lecture, toujours présent | — | ⏭ hors périmètre |
| Journal | — | sans objet : lecture, aucune écriture | — | ⏭ hors périmètre |
| Architecture | contrôleur mince, `@HasPermission`, DTO en `record`, aucune dépendance stock → repository article | règles ArchUnit respectées | `ArchitectureTest` (16 règles) | ✅ auto |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Aucun stock | aucun mouvement | 4 emplacements à 0, totaux à 0, coûts non lus | `StockServiceImplTest.getValuation_noMovement_returnsEveryLocationAtZeroWithoutReadingTheCosts` | ✅ auto |
| Emplacement désactivé | solde de 9 dans un emplacement absent des emplacements actifs | ignoré : ni dans les emplacements ni dans les totaux | `StockServiceImplTest.getValuation_balanceInAnInactiveLocation_isLeftOut` | ✅ auto |
| Article désactivé | réception de 3 en livraison, puis article passé à `active: false` | toujours compté (il est physiquement en stock) : livraison +3 | `StockValuationIT.getValuation_inactiveArticle_isStillValued` | ✅ auto |
| Plus de 100 articles | 150 articles en stock | valorisation sur tout le stock : `getValuation` ne pagine pas (somme SQL sur `stock_movements`), alors que le tableau reste limité à 100 lignes ; la page affiche la valeur serveur et non le calcul local | lecture de `StockServiceImpl.getValuation` ; `stock-page.spec.ts` (valeur serveur affichée à la place du calcul local) | 🔍 code |
| Valorisation en échec | 500 sur `/valuation` | les cartes restent affichées, calculées sur les articles chargés | `stock-page.spec.ts` (« garde le calcul sur les articles chargés si la valorisation échoue ») | ✅ auto |
| Hors ligne | lecture seule | sans objet : pas de file d'écriture, `Idempotency-Key` non concernée | — | ⏭ hors périmètre |

## Tests existants

**Backend** (`valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/`)
- `stock/service/impl/StockServiceImplTest.java` (4 tests `getValuation_*` ajoutés)
- `stock/controller/StockValuationIT.java` (7 tests, socle `StockApiTestSupport.java` : `createArticle(name, cost)` ajouté)
- `article/service/impl/ArticleServiceImplTest.java` (`getCosts_*`)
- `ArchitectureTest` (existant)

**Frontend** (`valimbavaka-ui/src/app/features/stocks/`)
- `pages/stock-page/stock-page.spec.ts` (nouveau, 3 tests)
- `data-access/stock-mapping.spec.ts` (3 tests `toLocationTotals`), `data-access/stock-service.spec.ts` (1 test `valuation`)

## Recette manuelle (🧪)

| Cas | Exemple | Résultat attendu | Statut |
|---|---|---|---|
| Cartes en administrateur | réception de 10 « Savon noir 100 g » (coût 3 120,50) en entrepôt, ouvrir `/stocks` | carte « Entrepôt » : 10 unités, « Valeur 0,03 M Ar » ; carte « Tous » identique | 🧪 |
| Article sans coût | réception d'un article sans coût d'achat en salon | carte « Salon » : « dont N sans coût d'achat » | 🧪 |
| Vendeuse | se connecter en vendeuse, ouvrir `/stocks` | cartes visibles ; `GET /api/v1/stocks/valuation` absent de l'onglet réseau | 🧪 |

## Résultat de l'exécution

- Date : 2026-10-30, en local (Windows, JDK 21, Node 24.15).
- Backend : `mvn -B -ntp clean test` → **237 tests unitaires verts** (dont `StockServiceImplTest` 8, `ArticleServiceImplTest` 12, `ArchitectureTest` 16), aucun warning de compilation ; `checkstyle:check` (`-Dcheckstyle.skip=false`) sans violation ; `spotless:apply` passé.
- Intégration (`*IT`, Testcontainers) : non exécutés en local (Docker indisponible) ; **exécutés et verts dans la CI** de la PR #169 (job « Backend (Spring Boot) », `mvn verify`, 2026-10-30), comme tous les autres jobs (Spotless/Checkstyle, Angular, Prettier/ESLint, Qodana, CodeQL, gitleaks).
- Frontend : `npm ci && npx ng test --watch=false` → **49 fichiers, 264 tests verts** ; `ng lint` sans erreur (aucun warning sur les fichiers du ticket) ; `ng build` réussi.
- Anomalies ouvertes : aucune.
- Étapes 🧪 restantes : 3.
