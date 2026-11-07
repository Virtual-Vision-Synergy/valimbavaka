# Guide d'intégration — JR-01 · Répartition matinale du stock

Statut : ✅ écrit le 2026-11-08 — tests verts en CI (voir « Résultat de l'exécution » du scénario) · Ticket : [#54](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/54) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Ce ticket pose le **domaine « journée de vente »** (`feature/day`) : une journée par date, **ouverte** par la répartition du matin. En un seul écran, l'administrateur répartit le stock de l'entrepôt vers la boutique, la livraison et le salon ; la validation crée **un bon de transfert du matin par destination** (origine `MORNING`, statut `VALIDATED`, port `TransferService.record` de TRF-03), déplace le stock et ouvre la journée, tout ou rien. Les autres tickets s'appuient sur le port `BusinessDayService.requireOpenDay()` (vente VTE-01) et sur la journée enregistrée (reprise de la veille JR-02, clôture JR-04 à JR-06).

## Contrat exposé

### Endpoints backend (`/api/v1/days`)

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/days/opening` | `days.open` (administrateur) | — | 200 `DayOpeningRDto` | 401, 403 |
| POST | `/api/v1/days` | `days.open` (administrateur) | `DayOpeningWDto` | 201 `BusinessDayRDto` (pas d'en-tête `Location` : aucune lecture par id) | 401, 403, 404 (article inconnu), 409 (journée déjà ouverte, stock insuffisant à l'entrepôt), 422 (voir règles) |

- `DayOpeningWDto` : `lines` (0 à 300 `DispatchLineWDto` : `articleId`, `destinationId` (UUID d'emplacement SYS-05), `quantity` > 0, 9 chiffres et 3 décimales au maximum). **Aucune date** : c'est toujours la journée du jour. `id`, `businessDate`, `status` glissés dans le corps sont ignorés.
- `BusinessDayRDto` : `id`, `businessDate` (`2026-11-08`), `status` (`OPEN`, `CLOSED`), `transfers` (`DayTransferRDto` : `transferId`, `transferNumber`, `locationId`, `lineCount`, `quantity`, dans l'ordre de première apparition des destinations dans la requête), `createdAt` (heure d'ouverture), `createdBy` (qui a ouvert).
- `DayOpeningRDto` : `businessDate` (aujourd'hui), `previousDay` (dernière journée ouverte avant aujourd'hui, `null` la toute première fois), `day` (`BusinessDayRDto` si aujourd'hui est déjà ouvert, sinon `null`), `warehouseLocationId`, `destinations` (`StockLocationRDto` de tous les emplacements actifs sauf l'entrepôt), `lines` (`DayOpeningLineRDto` : `articleId`, `code`, `name`, `unit`, `threshold`, `warehouse`).
- Écriture rejouable : avec `Idempotency-Key` (file hors ligne SYS-07 / SYS-12), un rejeu renvoie la même réponse sans seconde ouverture.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.day.service.BusinessDayService` (`BusinessDayService.java:26`) | `BusinessDayRDto requireOpenDay()` | **Journée du jour, ouverte** ; sinon `ConflictException` (409) « La journée du 08/11/2026 n'est pas ouverte. » (pas encore ouverte, ou déjà close). À appeler au début de toute écriture du jour (vente VTE-01, livraison, prestation salon). |
| idem (`BusinessDayService.java:23`) | `BusinessDayRDto openDay(DayOpeningWDto request)` | Ouverture + transferts du matin, utilisée par le contrôleur. |
| idem (`BusinessDayService.java:15`) | `DayOpeningRDto getOpening()` | Grille du jour (lecture). |
| `BusinessDayRepository` (`repository/BusinessDayRepository.java`) | `findByBusinessDate`, `findFirstByBusinessDateLessThanOrderByBusinessDateDesc` | Journée d'une date ; dernière journée ouverte avant une date (« dernier jour ouvré », JR-02). Hérite d'`OperationRepository` : pas de suppression (JNL-02). |

Implémentation : `service/impl/BusinessDayServiceImpl.java` (`openDay` l.74), `DayOpeningPolicy.java` (règles, `today()` l.56, `check` l.61), `DayOpeningGrid.java` (grille, `build` l.48), `DispatchPlan.java` (répartition regroupée par destination).

### Frontend (`features/day`)

- Route `/day/open` gardée par `permissionGuard('days.open')` (posée dans `day.routes.ts`) ; `/day/close` garde `sales.read` et reste en démonstration. Le garde global `sales.read` de `app.routes.ts` a été retiré ; l'entrée de menu « Ouverture de journée » demande `days.open` (`core/layout/menu.ts`).
- `data-access/day-service.ts` : `opening()` → `GET /api/v1/days/opening`, converti en `DayOpeningGrid` (`toOpeningGrid`) ; `open(body)` → `OutboxService.submit('POST', '/api/v1/days', body)` (`SubmitResult<BusinessDayRDto>`, `queued` si hors ligne). `closing()` et `lock()` restent des données de démonstration (JR-04, JR-06).
- `data-access/day-models.ts` : types du serveur `DayOpeningRDto`, `DayOpeningLineRDto`, `BusinessDayRDto`, `DayTransferRDto`, `DayStatus`, `DayOpeningWDto`, `DispatchLineWDto` ; modèle d'écran `DayOpeningGrid` (ancien `DayOpeningRDto` de la maquette, renommé) ; `DESTINATION_BY_LOCATION_CODE` (`STORE` → colonne Boutique `SHOP`, `DELIVERY` → `DELIVERY`, `TRADE_SHOW` → Salon `SALON`) ; fonctions `toOpeningGrid`, `toVouchers`, `toDayOpeningWDto`.
- `pages/day-open-page` : maquette conservée ; la validation envoie les quantités > 0 (une ligne par article et par colonne), affiche les numéros des bons renvoyés, le refus du serveur dans une alerte (`role="alert"`), et bloque un second envoi si la saisie est en file hors ligne. Si la journée est déjà ouverte, l'écran affiche directement les bons du matin.
- Traductions : `day.open.validated` sans la mention « démonstration » (fr, mg).

### Données

- Migration `2026_11_08-10-changelog.sql` (3 changesets, rollback fourni) :
  - table `business_days` : `business_date` **unique** (une journée par date), `status` (CHECK `OPEN`, `CLOSED`), champs d'audit et `version` (`BaseEntity`) ;
  - table `business_day_transfers` (`@ElementCollection`, ordre `line_number`) : `transfer_id` (→ `transfers`, unique), `transfer_number`, `location_id` (→ `stock_locations`, unique par journée), `line_count` > 0, `quantity` > 0 — résumé figé des bons du matin ;
  - permission `days.open` donnée à `ADMIN`.
- Mouvements : ceux de TRF-03 (deux `TRANSFER` par ligne, `reference_id` = id du transfert).
- Journal : ressource `BUSINESS_DAY`, action `CREATE`, libellé `Ouverture de la journée du 08/11/2026 : 3 transfert(s) du matin (TRF-0093, TRF-0094, TRF-0095)` (ou `… sans transfert du matin`), en plus des lignes `TRANSFER` (« Transfert du matin TRF-0093 · Entrepôt → Boutique … ») et `STOCK_MOVEMENT`.

## Règles métier à respecter par les autres tickets

- **Une journée par date**, la date étant celle d'**Antananarivo** (`DayOpeningPolicy.BUSINESS_ZONE`, horloge `Clock`) : à 21 h 30 UTC on est déjà le lendemain. On n'ouvre que la journée du jour (ni passé ni futur) ; une seconde ouverture le même jour → 409.
- **Cycle de vie** : pas de journée → `OPEN` (JR-01) → `CLOSED` (JR-06, à implémenter : méthode de domaine à ajouter sur `BusinessDay`, verrouillage des écritures). Une journée n'est jamais supprimée.
- **Vente et autres écritures du jour (VTE-01…)** : appeler `businessDayService.requireOpenDay()` dans la méthode `@Transactional` avant d'écrire ; ne pas relire `business_days` à la main.
- **Répartition** : la source est toujours l'emplacement `WAREHOUSE` ; chaque destination est un emplacement actif autre que l'entrepôt (sinon 422). Un bon par destination ; un même article saisi deux fois pour une destination est additionné. Le **total demandé par article, toutes destinations confondues**, est comparé au stock de l'entrepôt avant toute écriture (409 nommant l'article) ; 100 articles au plus par destination (règle de TRF-03).
- **TRF-02** : les transferts générés par l'ouverture sont déjà `MORNING` / `VALIDATED` dès leur création (statut fixé par `TransferOrigin.initialStatus`) et apparaissent dans l'historique TRF-01 avec l'administrateur comme émetteur (requête authentifiée). Il reste à TRF-02 de vérifier / présenter ce comportement (par exemple un lien depuis la carte de fin vers `/transfers?origin=MORNING`), sans regénérer de transferts.
- **JR-02 (reprendre hier)** : la veille = `DayOpeningRDto.previousDay` (dernière journée ouverte, pas forcément la date d'hier) ; ses quantités par article et destination se lisent dans les lignes des transferts `business_day_transfers.transfer_id` (TRF-04 exposera `GET /api/v1/transfers/{id}` ; sinon ajouter un port de lecture des lignes côté `transfer`). À renvoyer dans `DayOpeningLineRDto` puis à mapper dans `DispatchLineRDto.yesterday` (aujourd'hui à zéro) : le bouton « Reprendre hier » de l'écran l'utilise déjà.
- Grille : seulement les articles **finis** (`ArticleType.FINISHED`) **actifs** ayant du stock à l'entrepôt (> 0), triés par nom, 2 000 au plus (`DayOpeningGrid.MAX_PAGES`). Matières premières et semi-finis restent à l'entrepôt.
- Droits : `days.open` (administrateur) pour la grille et l'ouverture.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/day/`)
- `controller/BusinessDayController.java`
- `service/BusinessDayService.java` ; `service/impl/BusinessDayServiceImpl.java`, `DayOpeningPolicy.java`, `DayOpeningGrid.java`, `DispatchPlan.java`
- `domain/BusinessDay.java` (entité), `DayTransfer.java` (`@Embeddable`), `DayStatus.java`
- `repository/BusinessDayRepository.java` ; `mapper/BusinessDayMapper.java`
- `dto/DayOpeningWDto.java`, `DispatchLineWDto.java`, `DayOpeningRDto.java`, `DayOpeningLineRDto.java`, `BusinessDayRDto.java`, `DayTransferRDto.java`

**Migrations Liquibase**
- `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-10-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/`)
- `features/day/data-access/day-models.ts`, `day-service.ts`, `day-fixtures.ts` (fixture d'ouverture retirée) ; `features/day/day.routes.ts` ; `features/day/pages/day-open-page/` (`.ts`, `.html`)
- `app.routes.ts` (garde déplacé), `core/layout/menu.ts` (`days.open`), `core/auth/auth-testing.ts` (`days.open`), `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/day/controller/BusinessDayIT.java`, `feature/day/service/impl/DayOpeningPolicyTest.java`, `BusinessDayServiceImplTest.java`
- Frontend : `features/day/data-access/day-service.spec.ts`, `features/day/pages/day-open-page/day-open-page.spec.ts`, `core/layout/menu.spec.ts` (1 assertion), `features/demo-pages.a11y.spec.ts` (page retirée)

## Dépendances

**Ce ticket s'appuie sur** :
- [TRF-03](../TRF-03/guide.md) : `TransferService.record` avec `TransferOrigin.MORNING` (règles, numéros, mouvements, journal)
- [TRF-01](../TRF-01/guide.md) : historique où apparaissent les transferts du matin
- [STK-01](../STK-01/guide.md) : `StockService.getQuantities` (stock de l'entrepôt)
- [JNL-01](../JNL-01/guide.md) : `JournalService.record` ; [JNL-02](../JNL-02/guide.md) : `OperationRepository`
- [SYS-05](../SYS-05/guide.md) : emplacements (`requireActiveStockLocationId`, `isActiveStockLocation`, `getStockLocations`)
- [SYS-07](../SYS-07/guide.md) / [SYS-12](../SYS-12/guide.md) : file hors ligne et `Idempotency-Key`

**Ces tickets s'appuient sur lui** :
- [TRF-02](../TRF-02/guide.md) · transferts du matin : déjà générés et validés par l'ouverture (voir règles)
- [JR-02](../JR-02/guide.md) · reprise de la veille : `previousDay` + `business_day_transfers`
- [VTE-01](../VTE-01/guide.md) · vente : `BusinessDayService.requireOpenDay()`
- JR-04 à JR-06 · clôture : statut `CLOSED`, transferts du jour (composante « transféré »)

## Comment l'utiliser depuis un autre ticket

Vente (VTE-01), dans une méthode `@Transactional` :

```java
var day = businessDayService.requireOpenDay(); // 409 si la journée n'est pas ouverte
// … enregistrer la vente, éventuellement avec day.id() comme référence de journée
```

Côté interface :

```ts
this.dayService.open({ lines: [{ articleId, destinationId: storeId, quantity: 12 }] });
```

## Points d'attention

- **Hypothèse (ouverture = validation de la répartition)** : la journée est ouverte par la validation de l'écran de répartition, une seule fois par jour ; une ouverture sans aucune ligne est acceptée par le serveur (stock déjà sur place) mais l'écran garde le bouton désactivé tant qu'aucune quantité n'est saisie (comportement de la maquette).
- **Hypothèse (date)** : toujours la date du jour à Antananarivo, fixée par le serveur ; pas d'ouverture rétroactive ni anticipée.
- **Hypothèse (destinations)** : tout emplacement actif autre que l'entrepôt ; l'écran affiche les trois colonnes de la maquette (Boutique = `STORE`, Livraison = `DELIVERY`, Salon = `TRADE_SHOW`) ; un nouvel emplacement n'aurait pas de colonne sans modifier `DESTINATION_BY_LOCATION_CODE`.
- **Hypothèse (articles)** : seulement les articles finis actifs en stock à l'entrepôt ; quantités proposées à zéro (aucune règle de proposition dans le ticket).
- **Résumé figé** : `business_day_transfers` recopie numéro, nombre de lignes et quantité des bons. Si l'annulation d'un transfert (JNL-02) arrive, ce résumé devra être relu depuis `transfers`.
- **Non livré (maquette)** : « reste de la veille » (`carryOver`, JR-04/JR-06), quantités d'hier (JR-02 : le bouton « Reprendre hier » remet à zéro tant que `yesterday` vaut zéro), nombre de tournées (`rounds` à 0, module livraison).
- **Tests d'intégration** : une journée par date et une base partagée entre les classes d'IT ; `BusinessDayIT` vide `business_day_transfers` et `business_days` avant chaque test. Un futur test de vente (VTE-01) devra ouvrir la journée (ou la créer) dans son `@BeforeEach`, et ne pas supposer qu'elle est fermée.
- `/day/close` reste une maquette gardée par `sales.read` (JR-04 à JR-06).
- Tests exécutés et verts en CI le 2026-11-08 ; la recette manuelle (🧪) reste à passer.
