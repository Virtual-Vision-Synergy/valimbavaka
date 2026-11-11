# Guide d'intégration — JR-04 · Réconciliation automatique de clôture

Statut : ✅ écrit le 11/11/2026 — tests verts en CI le 11/11/2026 (voir « Résultat de l'exécution » du scénario) · Ticket : [#91](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/91) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

`GET /api/v1/days/closing` réconcilie **aujourd'hui** à partir des mouvements de stock : pour chaque article de la boutique, de la livraison et du salon, il renvoie le report à l'ouverture, les six composantes (transféré, retour, vendu, livré, salon, shooting), le théorique, le stock du système et l'écart. Rien n'est stocké : tout est recalculé à chaque lecture, donc toujours cohérent avec le registre de stock. L'écran `/day/close` affiche ces colonnes ; le comptage du soir, les motifs d'écart (JR-05) et le verrouillage (JR-06) restent à brancher dessus.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/days/closing` | `days.close` (administrateur) | aucune (ni corps ni paramètre ; `date` est ignoré) | 200 `DayClosingRDto` | 401, 403 (vendeuse, livreur), 409 (journée du jour non ouverte) |

- `DayClosingRDto` : `businessDate` (jour d'Antananarivo), `status` (`OPEN`, `CLOSED`), `locations` (`ClosingLocationRDto`, toujours dans l'ordre Boutique, Livraison, Salon).
- `ClosingLocationRDto` : `locationId`, `code` (`STORE`, `DELIVERY`, `TRADE_SHOW`), `label`, `lines` (triées par nom sans tenir compte de la casse, puis par code).
- `ClosingLineRDto` : `articleId`, `reference` (code article), `name`, `opening` (stock à l'ouverture de la journée), `transferred`, `returned`, `sold`, `delivered`, `salon`, `shooting` (**quantités positives**), `theoretical`, `systemStock`, `gap`.
- Contrôleur : `feature/dayclosing/controller/DayClosingController.java:42`, même préfixe `/api/v1/days` que `BusinessDayController` (JR-01) et `DayLiveController` (JR-03) sans les modifier.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.dayclosing.service.DayClosingService` | `DayClosingRDto getClosing()` | Réconciliation d'aujourd'hui (`DayClosingServiceImpl.java:70`). |
| `feature.stock.service.StockDayLedgerService` | `List<StockMovementTotalRDto> getMovementTotals(Instant from, Instant to, Collection<UUID> locationIds)` | **Nouveau** : mouvements de `[from, to[` sommés par article, emplacement et type (signés : une sortie est négative). Requête `StockMovementRepository.totalsByType` (`StockMovementRepository.java:55`). |
| idem | `List<StockBalanceRDto> getBalancesBefore(Instant before, Collection<UUID> locationIds)` | **Nouveau** : soldes non nuls, par article et emplacement, des mouvements enregistrés strictement avant l'instant (`balancesBefore`, `StockMovementRepository.java:66`). Deux lectures donnent le report (avant minuit) et le stock du système (avant minuit suivant). |

**Pourquoi une feature `dayclosing` et pas `day`** : même raison que JR-03 (ArchUnit `no_cycles_between_features` : `sale` dépend de `day`). `dayclosing` appelle `BusinessDayService.findToday()`, `ReferentialService`, `ArticleService` et `StockDayLedgerService`. **Pour JR-05 et JR-06** : y ajouter l'enregistrement des comptages et le verrouillage (ou créer une feature voisine qui s'appuie sur `DayClosingService`) ; ne pas faire dépendre `feature/day` de `sale`, `delivery` ou `stock`.

Classement d'un mouvement dans une composante (`ClosingTally.add`, `ClosingTally.java:34`) :

| Type de mouvement | Composante |
|---|---|
| `TRANSFER` | `transferred`, **solde net** de l'emplacement (arrivées du matin moins renvois vers l'entrepôt) |
| `RETURN` | `returned` (annulation de vente, réintégration d'un shooting) |
| `SALE` | `sold`, sauf sur l'emplacement `DELIVERY` où c'est `delivered` (une commande livrée devient une vente du canal Livraison, LIV-03) |
| `DELIVERY` | `delivered` |
| `TRADE_SHOW` | `salon` (consommation d'une prestation, SRV-02) |
| `SHOOTING` | `shooting` |
| `RECEIPT`, `PRODUCTION`, `INVENTORY` | aucune : le mouvement reste visible dans l'écart |

### Frontend (`features/day`)

- Route `/day/close` : garde `permissionGuard('days.close')` (avant `sales.read`), entrée de menu « Clôture de journée » sur `days.close` (`core/layout/menu.ts`) ; la route garde `data: { underConstruction: true }` tant que la caisse et le verrouillage sont en démonstration.
- `data-access/day-service.ts` : `closing()` → `GET /api/v1/days/closing`, converti par `toDayClosing` (`day-models.ts:243`) : codes d'emplacement → colonnes `SHOP` / `DELIVERY` / `SALON` (`DESTINATION_BY_LOCATION_CODE`), comptage proposé = `systemStock`, motif `null`. `lock()` reste une simulation (JR-06).
- `data-access/day-models.ts` : types du serveur `DayClosingRDto`, `ClosingLocationRDto`, `ClosingLineRDto` ; modèle d'écran `DayClosing`, `ClosingLocation`, `ClosingLine` ; `theoretical(line)` (`day-models.ts:226`) recalculé à l'écran. `data-access/day-fixtures.ts` : `closingExtras()`, la caisse et la liste de contrôle encore en démonstration.
- `pages/day-close-page` : colonnes Report, Transféré, Retour, Vendu, Livré, Salon, Shooting, Théorique, Compté, Écart, Motif ; la saisie du compté recalcule l'écart ; alerte `role="alert"` quand le serveur répond 409.
- Traductions (fr, mg) : `day.col.opening`, `day.close.notOpened`, `day.close.formula` modifiée ; `day.col.otherOut` supprimée.

### Données

- Migration `2026_11_11-20-changelog.sql` (2 changesets, rollback fourni) : permission `days.close` (« Réconcilier et clôturer la journée »), donnée à `ADMIN`. JR-06 la réutilisera pour le verrouillage.
- Aucune table : pas de nouvelle donnée à conserver.
- Aucune écriture au journal (lecture).

## Règles métier à respecter par les autres tickets

- **Jour** = jour d'Antananarivo, `[00:00, 24:00[` locale, mêmes bornes que JR-03 ; les mouvements sont datés par leur heure d'enregistrement (`stock_movements.occurred_at`).
- **Théorique** = report + transféré + retour − vendu − livré − salon − shooting, calculé par `ClosingTally.theoretical` (`ClosingTally.java:87`). Le **report** (stock présent à minuit) est une septième colonne ajoutée par ce ticket : sans lui, le reste d'hier laisserait un écart permanent.
- **Compté** : le serveur ne connaît pas encore de comptage. `systemStock` (stock du registre à l'instant de la lecture) est le point de départ proposé ; `gap` = `systemStock` − théorique n'est donc non nul que si un mouvement hors des six composantes a touché l'emplacement (inventaire, production, réception). **JR-05** (livré : voir [son guide](../JR-05/guide.md)) enregistre désormais le comptage réel et le motif obligatoire (`PUT /api/v1/days/closing/counts`, champs `counted` et `reason` ajoutés à `ClosingLineRDto`, `businessDayId` à `DayClosingRDto`) ; **JR-06** doit les verrouiller et calculer l'écart sur le comptage, pas sur `systemStock`.
- **Entrepôt non réconcilié** : c'est la source des transferts du matin. Les emplacements sont ceux de codes `STORE`, `DELIVERY`, `TRADE_SHOW` ; un emplacement désactivé disparaît du résultat.
- **Une ligne par article actif ou non** ayant un report ou un mouvement du jour ; les articles sans activité n'ont pas de ligne (une ligne dont tout est à zéro est omise).
- **SRV-04** (consommation salon dans la clôture) : la consommation d'une prestation écrit déjà un mouvement `TRADE_SHOW` négatif sur l'emplacement du salon ; elle est donc déjà dans la colonne « Salon ». SRV-04 n'a qu'à présenter ou rapprocher les produits par prestation.
- **Droit** : `days.close` (administrateur seul).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/`)
- `dayclosing/controller/DayClosingController.java` ; `dayclosing/service/DayClosingService.java` ; `dayclosing/service/impl/DayClosingServiceImpl.java`, `ClosingTally.java`, `ClosingTallies.java` ; `dayclosing/dto/DayClosingRDto.java`, `ClosingLocationRDto.java`, `ClosingLineRDto.java`
- `stock/service/StockDayLedgerService.java`, `stock/service/impl/StockDayLedgerServiceImpl.java` ; `stock/dto/StockMovementTotalRDto.java`, `StockBalanceRDto.java` ; `stock/repository/StockTypeTotal.java`, `StockMovementRepository.java` (méthodes `totalsByType`, `balancesBefore`)
- Migration : `src/main/resources/db/changelog/changes/2026_11_11-20-changelog.sql`, déclarée dans `db.changelog-master.yaml`

**Frontend** (`valimbavaka-ui/src/app/`)
- `features/day/data-access/day-models.ts`, `day-service.ts`, `day-fixtures.ts`, `closing-state.ts` ; `features/day/pages/day-close-page/` (`.ts`, `.html`, `.css`) ; `features/day/day.routes.ts` ; `core/layout/menu.ts` ; `core/auth/auth-testing.ts` ; `app.routes.ts` (commentaire) ; `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/dayclosing/controller/DayClosingIT.java`, `feature/dayclosing/service/impl/DayClosingServiceImplTest.java`, `ClosingTallyTest.java`, `feature/stock/service/impl/StockDayLedgerServiceImplTest.java`, `feature/stock/repository/StockMovementRepositoryIT.java` (4 tests ajoutés)
- Frontend : `features/day/pages/day-close-page/day-close-page.spec.ts`, `features/day/data-access/day-service.spec.ts` (2 tests ajoutés), `core/layout/menu.spec.ts` (assertions ajoutées), `features/demo-pages.a11y.spec.ts` (entrée « day close » retirée : l'axe est dans le spec de la page)

## Dépendances

**Ce ticket s'appuie sur** :
- [JR-01](../JR-01/guide.md) : journée (`BusinessDayService.findToday()` de [JR-03](../JR-03/guide.md)), transferts du matin ; [JR-02](../JR-02/guide.md) : même famille d'écrans
- [VTE-01](../VTE-01/guide.md) : ventes (mouvement `SALE`, annulation en `RETURN`) ; [LIV-04](../LIV-04/description.md) : retour d'une livraison en échec (transfert `RET`) ; [SHO-02](../SHO-02/description.md) : réintégration (mouvement `RETURN`) ; [SRV-02](../SRV-02/guide.md) : consommation salon (mouvement `TRADE_SHOW`) ; [TRF-02](../TRF-02/description.md) : transferts du matin
- [JNL-01](../JNL-01/guide.md) : sans objet en lecture, aucune ligne de journal écrite

**Ces tickets s'appuient sur lui** :
- [JR-05](../JR-05/description.md) (motif obligatoire), [JR-06](../JR-06/description.md) (verrouillage), [SRV-04](../SRV-04/description.md), [DSH-01](../DSH-01/description.md) (cockpit journalier)

## Comment l'utiliser depuis un autre ticket

```java
var closing = dayClosingService.getClosing();             // 409 si la journée du jour n'est pas ouverte
var store = closing.locations().getFirst();               // code STORE
var lines = store.lines();                                // ClosingLineRDto : composantes + théorique + écart
var totals = stockDayLedgerService.getMovementTotals(from, to, List.of(storeId)); // mouvements par type
```

```ts
this.dayService.closing(); // Observable<DayClosing> : lignes par colonne, comptage proposé = stock du système
```

## Points d'attention

- **Hypothèse (« compté »)** : aucun comptage n'existe côté serveur avant JR-05 / JR-06. Le comptage proposé est le stock du registre ; l'écart du serveur (`gap`) mesure donc les mouvements hors des six composantes, et l'écart affiché à l'écran change dès que l'administrateur saisit un autre comptage. À valider par le PO.
- **Hypothèse (report)** : la colonne « Report veille » (septième) s'ajoute aux six demandées ; le stock laissé la veille n'est pas renvoyé à l'entrepôt automatiquement. Décision de présentation à confirmer ; la formule affichée en tient compte.
- **Hypothèse (« transféré »)** : solde net des transferts de l'emplacement dans la journée (arrivées moins renvois), pas seulement le transfert du matin ; un retour de stock `RET` vers l'entrepôt diminue donc « transféré » et non « retour ».
- **Hypothèse (« retour »)** : mouvements `RETURN` (vente annulée, shooting réintégré), qui augmentent le stock de l'emplacement.
- **Hypothèse (« livré »)** : vente sur l'emplacement Livraison (commande livrée devenue vente, LIV-03) ou mouvement `DELIVERY` (type réservé, aucun code ne l'écrit aujourd'hui).
- **Permission** : `days.close` créée par ce ticket (migration 20), plutôt que `reports.read` ou `sales.read`, pour que JR-06 verrouille avec le même droit. La vendeuse ne voit plus l'entrée « Clôture » du menu (elle avait `sales.read`).
- **Reste en démonstration** : la caisse (espèces attendues / comptées / autres moyens), la liste de contrôle (livraisons soldées, tickets en attente, shootings en retard) et le verrouillage de l'écran (`closingExtras()`, `DayService.lock()`) ; chacun relève de JR-05 / JR-06 (liste de contrôle et caisse comprises). La route garde `underConstruction`.
- **Journée non ouverte** : 409, contrairement à JR-03 qui reste lisible ; une journée close reste lisible (le verrouillage JR-06 ne devra pas l'interdire).
- **Limites** : lecture complète sans pagination (au plus quelques centaines de lignes : articles finis du catalogue × 3 emplacements) ; la lecture fait trois requêtes d'agrégat et une lecture des articles.
- La recette manuelle 🧪 reste à passer (trois étapes, voir le scénario).
