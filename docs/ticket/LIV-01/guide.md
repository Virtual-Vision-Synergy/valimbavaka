# Guide d'intégration — LIV-01 · Création d'une commande à livrer

Statut : ✅ rédigé, tests verts en CI (commit `fd2846a`) · Ticket : [#46](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/46) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La vendeuse (ou l'administrateur) crée une **commande à livrer** : client, téléphone, adresse, quartier, livreur, jour de livraison et articles. Le serveur copie le prix de chaque article, calcule le montant à encaisser, attribue un numéro `LIV-0001`… et place la commande **en fin de tournée** du livreur pour ce jour (`stopNumber`), au statut `TO_PREPARE`. Ce ticket pose le domaine `feature/delivery` (entité, statuts, transitions, port de lecture) sur lequel LIV-02 à LIV-08 s'appuient ; il ne bouge pas le stock et ne crée ni vente ni facture.

**Décision PO du 07/11/2026** : une commande livrée devient une vente (canal livraison) avec sa facture, au moment où la livraison est marquée livrée (LIV-03). Voir [Contrat pour LIV-03](#contrat-pour-liv-03--commande-livrée--vente--facture).

## Contrat exposé

### Endpoints backend (`/api/v1/deliveries`)

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| POST | `/api/v1/deliveries` | `deliveries.create` (administrateur, vendeuse) | `DeliveryOrderWDto` | 201 `DeliveryOrderRDto` + `Location` | 400, 401, 403, 404 (article inconnu), 422 |
| GET | `/api/v1/deliveries` | `deliveries.read` (administrateur, vendeuse, livreur) | `PageQuery` + `driverId`, `date` (`yyyy-MM-dd`), `status` | 200 `PageResponse<DeliveryOrderRDto>` | 400 (statut inconnu), 401, 403, 422 (`size` > 100) |
| GET | `/api/v1/deliveries/{id}` | `deliveries.read` | — | 200 `DeliveryOrderRDto` | 401, 403, 404 |
| GET | `/api/v1/deliveries/drivers` | `deliveries.create` | `PageQuery` (`search` sur prénom et nom) | 200 `PageResponse<DeliveryDriverRDto>` | 401, 403 |

- **Livreur cloisonné** : pour un appelant livreur actif, la liste est toujours limitée à **ses** commandes (le `driverId` demandé est remplacé par le sien) et la commande d'un autre livreur répond 404 (`DeliveryOrderServiceImpl.ownDriverId`, `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/delivery/service/impl/DeliveryOrderServiceImpl.java:106`).
- Tri autorisé : `createdAt` (défaut, décroissant), `number`, `scheduledDate`, `stopNumber`, `status`. **Tournée d'un livreur** = `?driverId=<id>&date=<jour>&sortBy=stopNumber&sortDirection=ASC`.
- `DeliveryOrderWDto` : `customerName` (obligatoire, ≤ 120), `customerPhone` (obligatoire, chiffres / espaces / `.` / `-`, `+` initial, 7 chiffres au moins), `address` (obligatoire, ≤ 255), `district` (≤ 80, facultatif), `driverId`, `scheduledDate`, `lines` (1 à 100 `DeliveryOrderLineWDto` : `articleId`, `quantity` > 0, 3 décimales), `note` (≤ 500). **Ni `id`, ni numéro, ni statut, ni arrêt, ni prix, ni montant** : envoyés, ils sont ignorés.
- `DeliveryOrderRDto` : `id`, `number`, `status`, `customerName`, `customerPhone`, `address`, `district`, `driverId`, `scheduledDate`, `stopNumber`, `amount` (montant à encaisser), `note`, `lines` (`articleId`, `quantity`, `unitPrice`, `amount`), `createdAt`, `createdBy` (la vendeuse qui a saisi), `version`.
- `DeliveryDriverRDto` : `id`, `firstName`, `lastName` (ni email ni autre donnée du compte), livreurs actifs triés par prénom.
- Pas de `DELETE`, pas de changement de statut exposé : c'est le travail de LIV-03, LIV-04 et LIV-05.

### Services et ports Java réutilisables

- `feature.delivery.service.DeliveryOrderService` : `createOrder(DeliveryOrderWDto)`, `getOrder(UUID)`, `getOrders(PageQuery, DeliveryOrderFilter)`, `getDrivers(PageQuery)`. `DeliveryOrderFilter(driverId, scheduledDate, status)` est un `record` du paquet `domain` ; un critère `null` veut dire « tous ».
- `feature.delivery.domain.DeliveryOrder.moveTo(DeliveryStatus)` (`domain/DeliveryOrder.java:99`) : **seule** façon de changer le statut ; applique la table de transitions et lève 409 sinon.
- `feature.delivery.domain.DeliveryStatus` (`domain/DeliveryStatus.java:19`) : `canMoveTo`, `ensureCanMoveTo` (409 `CONFLICT` « La livraison ne peut pas passer de « livrée » à « en échec ». »), `isFinal`, `label()` (libellé français pour le journal).
- Ajouts à la feature utilisateur : `UserService.getActiveDrivers(PageQuery)` et `UserService.isActiveDriver(UUID)` (`feature/user/service/UserService.java:25`), sur le modèle de `getActiveSellers` / `isActiveSeller`.
- Les autres features ne lisent jamais `DeliveryOrderRepository` : elles passent par `DeliveryOrderService` (ou un port à ajouter dans ce service) et référencent la commande par son UUID.

### Frontend (`features/deliveries`)

- Routes (`deliveries.routes.ts`, montées sur `/deliveries`, garde `deliveries.read`) : `''` tableau du jour (inchangé visuellement) et `new` (création, garde `deliveries.create`).
- `data-access/delivery-service.ts` : `board(day?)` lit `GET /api/v1/deliveries?date=<jour>&size=100&sortBy=stopNumber&sortDirection=ASC` et `GET /api/v1/deliveries/drivers`, puis construit le tableau ; `drivers()` ; `create(body)` passe par la file hors ligne (`OutboxService.submit('POST', '/api/v1/deliveries', body)`, SYS-07). `myRound()` et `updateStatus()` restent en démonstration (LIV-02, LIV-03, LIV-04).
- `data-access/delivery-mapping.ts` : `businessToday()` (jour d'Antananarivo, `yyyy-MM-dd`), `driverLabel()` (« Tojo N. »), `toBoard()` / `toDelivery()` (commande API → carte du tableau ; une tournée par livreur ayant des commandes, commandes `CANCELLED` écartées).
- `data-access/delivery-models.ts` : types miroirs `DeliveryOrderRDto`, `DeliveryOrderWDto`, `DeliveryOrderLineRDto`, `DeliveryOrderLineWDto`, `DeliveryDriverRDto`, `DeliveryOrderStatus` (= `DeliveryStatus` du tableau + `CANCELLED`). `DeliveryRDto` reste le modèle d'affichage ; son `channel` devient `SaleChannel | null` (`null` pour une commande de l'API).
- `pages/delivery-order-page` : formulaire de création (client, tournée, lignes d'articles finis actifs avec prix, montant estimé, note) ; toast « Commande LIV-… ajoutée à la tournée (arrêt n). » puis retour au tableau ; le message du `ProblemDetail` s'affiche sans perdre la saisie.
- `pages/deliveries-page` : le bouton « Nouvelle commande » mène à `new` (affiché seulement avec `deliveries.create`) ; `components/delivery-card` : une carte « À préparer » sans canal affiche « Arrêt n/x ».
- Traductions : `deliveries.form.*` dans `public/i18n/fr.json` et `mg.json`.

### Données

- Migration `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-64-changelog.sql` (4 changesets, rollback fourni) :
  - séquence `delivery_order_number_seq` ;
  - table `delivery_orders` (`number` unique, `status` CHECK sur les 5 statuts, `customer_name`, `customer_phone`, `address`, `district`, `driver_id` → `users`, `scheduled_date`, `stop_number` > 0, `amount` ≥ 0 `NUMERIC(14,2)`, `note`, audit, `version`), index `(driver_id, scheduled_date)`, `created_at`, `status` ;
  - table `delivery_order_lines` (`delivery_order_id`, `line_number`, `article_id` → `articles`, `quantity` > 0 `NUMERIC(12,3)`, `unit_price` ≥ 0 `NUMERIC(12,2)`), un article une fois par commande ;
  - permission `deliveries.create` (administrateur, vendeuse) et `deliveries.read` donnée en plus à la vendeuse (elle ne l'avait pas : AUTH-01 la réservait à l'administrateur et au livreur).
- Journal : ressource `DELIVERY_ORDER`, action `CREATE`, libellé « Création de la commande à livrer LIV-0042 : 2 article(s), 17000.00 Ar, tournée du 2026-11-09 (arrêt 3) » (`DeliveryOrderServiceImpl.describe`, ligne 110). Ni nom ni téléphone du client dans le journal.

## Règles métier à respecter par les autres tickets

### Statuts et transitions

| Statut | Libellé | Colonne du tableau | Atteint par |
|---|---|---|---|
| `TO_PREPARE` | à préparer (« en attente » de LIV-06) | À préparer | création (LIV-01) |
| `ON_ROUND` | en tournée (« en cours ») | En tournée | départ en tournée (LIV-02 ou l'ouverture de journée JR-01) |
| `DELIVERED` | livrée | Livrées | LIV-03 |
| `FAILED` | en échec (« retour » de LIV-06) | Échecs | LIV-04 (motif obligatoire) |
| `CANCELLED` | annulée | aucune (écartée du tableau) | annulation avant départ (à exposer par LIV-06 ou un ticket dédié) |

Transitions autorisées (`DeliveryStatus.canMoveTo`, `domain/DeliveryStatus.java:43`) :

- `TO_PREPARE` → `ON_ROUND`, `DELIVERED`, `FAILED`, `CANCELLED` (« un geste » de LIV-03 possible sans départ explicite) ;
- `ON_ROUND` → `DELIVERED`, `FAILED` (une commande partie ne s'annule plus : elle échoue) ;
- `DELIVERED`, `FAILED`, `CANCELLED` sont **finaux** : aucune transition ordinaire n'en sort.
- **LIV-05** (annulation d'une action récente) est la seule exception : un retour au statut précédent pendant un délai limité, qui doit **défaire** ce que la transition a produit (contre-passer la vente et la facture de LIV-03, le mouvement de retour de LIV-04) et tracer `REVERSE` ou `CANCEL`. Ne pas passer par `moveTo` pour cela : ajouter une méthode dédiée (ex. `undo(previous)`) dans `DeliveryOrder`.
- Toute transition se fait par `DeliveryOrder.moveTo` dans une méthode `@Transactional` du service, avec une ligne de journal (`UPDATE`, ressource `DELIVERY_ORDER`, libellé avec `DeliveryStatus.label()`). LIV-07 (historique horodaté et attribué) aura besoin d'une table d'historique des statuts : la créer dans `feature/delivery`, alimentée au même endroit.

### Tournée

- Une tournée = un livreur + un jour (`driver_id`, `scheduled_date`) ; l'ordre de passage = `stop_number` croissant. La création ajoute en fin de tournée (`max + 1`) sous un verrou transactionnel par tournée (`DeliveryOrderRepository.lockRound`, `pg_advisory_xact_lock`) : deux créations simultanées ont des arrêts distincts. Un ticket qui réordonne ou réaffecte une commande doit prendre le même verrou (`"delivery-round:" + driverId + ":" + jour`).
- Le jour est celui d'**Antananarivo** (`Indian/Antananarivo`), jamais UTC : refus d'un jour passé, jour du tableau côté front (`businessToday`).
- LIV-02 (tournée du jour du livreur) : lire `GET /api/v1/deliveries?date=<aujourd'hui>&sortBy=stopNumber&sortDirection=ASC` avec le jeton du livreur suffit (cloisonnement serveur) ; le montant à encaisser est `amount`, le téléphone `customerPhone`.

### Contrat pour LIV-03 : commande livrée → vente + facture

À appliquer quand VTE-01 (vente) et FAC-01 (facture) seront livrés ; **décision PO du 07/11/2026** :

1. Le passage à `DELIVERED` (`moveTo(DELIVERED)`) et la création de la vente et de la facture se font **dans la même transaction** : tout ou rien.
2. La vente reprend la commande : canal = canal de vente de code `DELIVERY` du référentiel (`sales_channels`, SYS-05), vendeuse = `createdBy` de la commande (c'est elle qui touche la commission, COM-xx), lignes = `lines` avec **le prix unitaire figé de la commande** (`unitPrice`, pas le prix actuel de l'article), montant = `amount`, référence = id de la commande (à stocker dans la vente pour retrouver l'une depuis l'autre).
3. Le mouvement de stock de la vente (type `DELIVERY` ou `SALE`, quantité négative, emplacement `DELIVERY`) est porté par la vente via `StockMovementService.record` (STK-01) ; LIV-01 n'en écrit aucun.
4. La facture est générée par le mécanisme de FAC-01 à l'encaissement (numéro unique croissant) ; une commande prépayée (MVola) n'est pas encore modélisée : à ajouter par LIV-03 si besoin.
5. Rejeu hors ligne (LIV-08) : l'appel de LIV-03 passe par la file avec `Idempotency-Key` ; une commande déjà `DELIVERED` répond 409 par `ensureCanMoveTo`, donc jamais deux ventes pour une commande.
6. Tant que VTE-01 / FAC-01 n'existent pas, LIV-03 peut passer la commande à `DELIVERED` seule et laisser ce branchement à faire (à noter dans son guide), plutôt que d'inventer une vente.

### Stock

- La création **ne réserve pas et ne bouge pas** le stock, et ne vérifie pas sa disponibilité (hypothèse) : les articles partent vers l'emplacement `DELIVERY` par les transferts du matin (JR-01 / TRF-02) et sortent par la vente de LIV-03. LIV-04 (échec) remet les articles « en stock retour » par un mouvement `RETURN` ou un retour de stock TRF-03 (`TransferService.recordReturn`) de `DELIVERY` vers l'emplacement de retour.

### Droits

- Créer : administrateur et vendeuse (`deliveries.create`). Lire : administrateur, vendeuse, livreur (`deliveries.read`), le livreur seulement ses commandes. Les actions du livreur (LIV-03, LIV-04) auront besoin d'une permission dédiée (ex. `deliveries.update`) donnée au livreur, en vérifiant côté serveur que la commande lui est assignée.
- Seuls les articles `FINISHED` et actifs sont acceptés (comme une vente, voir [ART-01](../ART-01/guide.md)).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/delivery/`)
- `controller/DeliveryController.java`
- `domain/DeliveryOrder.java`, `DeliveryOrderLine.java`, `DeliveryStatus.java`, `DeliveryCustomer.java`, `DeliveryOrderCommand.java`, `DeliveryOrderLineCommand.java`, `DeliveryOrderFilter.java`
- `dto/DeliveryOrderWDto.java`, `DeliveryOrderLineWDto.java`, `DeliveryOrderRDto.java`, `DeliveryOrderLineRDto.java`, `DeliveryDriverRDto.java`
- `mapper/DeliveryMapper.java`, `repository/DeliveryOrderRepository.java`
- `service/DeliveryOrderService.java`, `service/impl/DeliveryOrderServiceImpl.java`, `service/impl/DeliveryOrderPolicy.java`
- `feature/user/service/UserService.java`, `feature/user/service/impl/UserServiceImpl.java` (livreurs actifs)

**Migration** : `valimbavaka-core/src/main/resources/db/changelog/changes/2026_11_08-64-changelog.sql`, déclarée dans `db.changelog-master.yaml`.

**Frontend** (`valimbavaka-ui/src/app/`)
- `features/deliveries/deliveries.routes.ts`
- `features/deliveries/data-access/delivery-service.ts`, `delivery-mapping.ts`, `delivery-models.ts`
- `features/deliveries/pages/delivery-order-page/` (ts, html, css, spec)
- `features/deliveries/pages/deliveries-page/deliveries-page.ts` et `.html`, `features/deliveries/components/delivery-card/delivery-card.ts`
- `core/auth/auth-testing.ts` (permissions de test), `public/i18n/fr.json`, `public/i18n/mg.json`

**Tests** : voir [scénario](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** :
- [ART-01](../ART-01/guide.md) : `ArticleService.getArticleById` (type, état, prix).
- [AUTH-03](../AUTH-03/guide.md) : utilisateurs et rôles (livreur = `DELIVERY_DRIVER`).
- [JNL-01](../JNL-01/guide.md) : `JournalService.record` dans la transaction.
- [JNL-02](../JNL-02/guide.md) : `OperationRepository` (aucune suppression).
- [STK-01](../STK-01/guide.md) : non utilisé par la création (voir Stock) ; règles reprises pour LIV-03 / LIV-04.

**Ces tickets s'appuient sur lui** :
- [LIV-02](../LIV-02/description.md) · [LIV-03](../LIV-03/description.md) · [LIV-04](../LIV-04/description.md) · [LIV-05](../LIV-05/description.md) · [LIV-06](../LIV-06/description.md) · [LIV-07](../LIV-07/description.md) · [LIV-08](../LIV-08/description.md)

## Comment l'utiliser depuis un autre ticket

```java
// LIV-03 (dans feature/delivery/service/impl, méthode @Transactional) : marquer livrée
var order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("La commande à livrer est introuvable."));
order.moveTo(DeliveryStatus.DELIVERED); // 409 si déjà livrée, en échec ou annulée
journalService.record(JournalAction.UPDATE, "DELIVERY_ORDER", order.getId(),
        "Commande %s %s".formatted(order.getNumber(), DeliveryStatus.DELIVERED.label()));
// puis, quand VTE-01 / FAC-01 existent : créer la vente (canal DELIVERY, vendeuse = order.getCreatedBy(),
// lignes au prix order.getLines().get(i).getUnitPrice()) et sa facture, dans la même transaction.
```

```ts
// LIV-02 : tournée du jour du livreur connecté (le serveur limite aux commandes du livreur)
const params = new HttpParams().set('date', businessToday()).set('size', 100)
  .set('sortBy', 'stopNumber').set('sortDirection', 'ASC');
this.http.get<PageResponse<DeliveryOrderRDto>>('/api/v1/deliveries', { params });
```

## Points d'attention

- **Hypothèses prises (sans validation, session de nuit)** :
  - le statut initial est `TO_PREPARE` (nom de la maquette) ; « attente / en cours / livrée / retour / annulée » de LIV-06 correspondent à `TO_PREPARE` / `ON_ROUND` / `DELIVERED` / `FAILED` / `CANCELLED` ;
  - le jour de livraison est saisi (aujourd'hui par défaut) et ne peut pas être passé ; une commande pour demain n'apparaît pas sur le tableau du jour ;
  - la création ne touche pas au stock et n'en vérifie pas la disponibilité ;
  - le prix est copié de la fiche article à la création (ni remise ni promotion : PRO-xx à brancher par LIV-03 ou VTE-01) ;
  - la vendeuse reçoit `deliveries.read` (nécessaire pour ouvrir `/deliveries` et y trouver le bouton « Nouvelle commande ») ; `AuthSecurityIT.me_withSellerToken_returnsOnlyTheSellerPermissions` et `menu.spec.ts` ont été adaptés ;
  - le cloisonnement du livreur (ses commandes seulement) est fait par le service, pas par une permission séparée.
- **Tableau du jour** : seules les colonnes et cartes viennent de l'API ; le panneau de détail, son historique (vide pour une commande de l'API, LIV-07) et ses boutons d'action restent en démonstration (LIV-06, LIV-07) ; la route garde `underConstruction`. La tournée mobile du livreur (`/round`) reste sur les données de démonstration (LIV-02).
- Le canal de prise de commande (Facebook, téléphone) de la maquette n'est pas stocké : la carte affiche « Arrêt n/x » à la place. À ajouter si le client le demande (LIV-06).
- Le paiement d'avance (MVola, « Payée MVola » de la maquette) n'est pas modélisé : `prepaid` vaut toujours `false` côté front (LIV-03).
- Piège PostgreSQL : un filtre `(:date is null or d.scheduledDate = :date)` avec une date renseignée échoue (500 « could not determine data type of parameter ») ; `DeliveryOrderRepository.search` utilise `d.scheduledDate = coalesce(:scheduledDate, d.scheduledDate)`. Garder cette forme pour tout nouveau filtre de date (LIV-06).
- Pas de recherche texte sur `GET /api/v1/deliveries` (le paramètre `search` est ignoré) : à ajouter par LIV-06 si besoin.
- Pas d'endpoint d'annulation ni de réaffectation de livreur : `CANCELLED` existe dans le domaine, l'exposition revient à LIV-06 ou à un ticket dédié.
- Taille de la PR au-delà de 400 lignes (domaine complet + écran + tests) : avertissement P2 du contrôle de convention.
