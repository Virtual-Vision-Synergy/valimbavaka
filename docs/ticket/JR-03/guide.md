# Guide d'intégration — JR-03 · Suivi temps réel de la journée

Statut : ✅ écrit le 2026-11-11 (tests à lancer en CI, voir « Résultat de l'exécution » du scénario) · Ticket : [#57](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/57) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

`GET /api/v1/days/live` renvoie en une lecture la photo d'**aujourd'hui** : statut de la journée, indicateurs des ventes (CA, nombre, par canal), de la tournée (commandes par statut) et des prestations salon, plus le flux des 30 dernières opérations (ventes, livraisons, prestations) fusionnées du plus récent au plus ancien. Réservé à l'administrateur (`reports.read`). L'écran `/day/live` relit ce point toutes les 15 secondes : les indicateurs et le flux se mettent à jour sans rechargement manuel. Lecture seule : rien n'est ouvert, fermé ni écrit.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/days/live` | `reports.read` (administrateur) | aucune (ni corps ni paramètre) | 200 `DayLiveRDto` | 401, 403 (vendeuse, livreur) |

- `DayLiveRDto` : `businessDate` (jour d'Antananarivo), `status` (`OPEN`, `CLOSED`, `null` si la journée n'est pas ouverte), `generatedAt` (heure serveur de la photo), `sales`, `deliveries`, `services`, `feed`.
- `DaySalesRDto` : `revenue`, `count` (ventes `VALIDATED` seulement), `channels` (`ChannelSalesRDto` de VTE-08, tous les canaux actifs, même à 0).
- `DayDeliveriesRDto` : `total`, `toPrepare`, `onRound`, `delivered`, `failed`, `cancelled` (commandes dont `scheduled_date` = aujourd'hui).
- `DayServicesRDto` : `count`, `revenue` (somme des prix facturés).
- `DayFeedItemRDto` : `type` (`SALE`, `DELIVERY`, `SERVICE`), `id`, `reference` (numéro), `label` (canal, client ou prestation), `amount`, `status` (statut de la vente ou de la livraison, `null` pour une prestation), `actorName` (vendeuse, `null` pour une livraison), `occurredAt` (heure de la vente, dernière modification de la commande, heure de la prestation).
- Contrôleur : `feature/dayoverview/controller/DayLiveController.java:40`, même préfixe `/api/v1/days` que `BusinessDayController` (JR-01) sans le modifier.

### Services et ports Java réutilisables

| Port | Méthode | Rôle |
|---|---|---|
| `feature.dayoverview.service.DayLiveService` | `DayLiveRDto getLive()` | Photo d'aujourd'hui (`DayLiveServiceImpl.java:62`). |
| `feature.day.service.BusinessDayService` | `@Nullable BusinessDayRDto findToday()` (`BusinessDayService.java:29`) | **Nouveau** : journée du jour quel que soit son statut, `null` si pas ouverte (contrairement à `requireOpenDay()` qui lève 409). Utile à JR-04 à JR-06. |
| `feature.delivery.service.DeliveryDayService` | `Map<DeliveryStatus, Long> countByStatus(LocalDate)` ; `List<DeliveryOrderRDto> getRecentOrders(LocalDate, int)` | **Nouveau** : commandes d'un jour par statut (tous les statuts, 0 compris) et dernières commandes modifiées. Requête d'agrégat `DeliveryOrderRepository.countByStatus` (`DeliveryOrderRepository.java:53`). |

Sont réutilisés tels quels : `ChannelDashboardService.getDashboard(jour, jour)` (VTE-08), `SaleQueryService.getSales` (VTE-07), `ServiceRecordService.getServiceRecordsBetween` (SRV-02, prévu pour JR-03).

**Pourquoi une feature `dayoverview` et pas `day`** : `sale` dépend déjà de `day` (`requireOpenDay()`), donc `day` ne peut pas dépendre de `sale` (ArchUnit `no_cycles_between_features`). Même découpage que `articleoverview` (ART-06). **Pour JR-04 à JR-06** : la réconciliation de clôture lit aussi ventes, livraisons et prestations ; elle devra donc, elle aussi, vivre hors de `feature/day` (ou dans une feature de lecture à elle), en appelant `BusinessDayService.findToday()`, `DeliveryDayService` et les ports ci-dessus.

Fichiers : `service/impl/DayLiveServiceImpl.java` (orchestration, `FEED_LIMIT` = 30, fuseau `Indian/Antananarivo`), `service/impl/DayFeed.java` (fusion et tri du flux).

### Frontend (`features/day`)

- Route `/day/live` (`day.routes.ts`), garde `permissionGuard('reports.read')` ; entrée de menu « Suivi du jour » (`core/layout/menu.ts`, permission `reports.read`).
- `data-access/day-live-service.ts` : `live()` → `GET /api/v1/days/live` ; `data-access/day-live-models.ts` : `DayLiveRDto`, `DaySalesRDto`, `DayDeliveriesRDto`, `DayServicesRDto`, `DayFeedItemRDto` (noms du backend).
- `pages/day-live-page` : statut de la journée, heure de la dernière mise à jour, trois cartes (ventes, tournée « livrées / à livrer », prestations), tableau des ventes par canal, flux des opérations avec badge de statut. La relecture (`LIVE_REFRESH_MS` = 15 s, `exhaustMap` donc sans chevauchement, suspendue onglet masqué) garde la dernière vue si elle échoue et affiche une alerte (`role="alert"`) ; le bouton « Actualiser » relit tout de suite.
- Traductions (fr, mg) : `day.live.*`, `menu.item.dayLive`.

### Données

- Aucune migration, aucune permission nouvelle (`reports.read` existe depuis AUTH-01, ADMIN seul, comme VTE-08).
- Aucune écriture au journal (lecture).

## Règles métier à respecter par les autres tickets

- **Jour** = jour d'Antananarivo. Ventes sur `sales.business_date`, commandes sur `delivery_orders.scheduled_date`, prestations sur la plage `[00:00, 24:00[` locale (horloge `Clock`, `DayLiveServiceImpl.BUSINESS_ZONE`).
- **CA et nombre de ventes** : ventes `VALIDATED` seulement (règle de VTE-07 / VTE-08). Les ventes annulées ou contre-passées restent visibles dans le flux avec leur statut.
- **Vente issue d'une livraison** (LIV-03) : elle est déjà une vente du canal Livraison, donc comptée dans `sales` ; `deliveries` ne porte que les compteurs de commandes (pas de montant) pour ne pas compter deux fois.
- **Prestations** : `services.revenue` = somme des `price` des prestations du jour ; la commission (COM-02) n'entre pas ici.
- **Flux** : 30 éléments au plus, 30 lus dans chaque activité avant fusion ; heure d'une livraison = dernière modification (`updatedAt`) pour qu'un changement de statut remonte en tête.
- **Droit** : `reports.read` (administrateur). La vendeuse et le livreur ont leurs propres écrans (ventes du jour, tournée).

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/`)
- `dayoverview/controller/DayLiveController.java` ; `dayoverview/service/DayLiveService.java` ; `dayoverview/service/impl/DayLiveServiceImpl.java`, `DayFeed.java` ; `dayoverview/domain/DayFeedType.java` ; `dayoverview/dto/DayLiveRDto.java`, `DaySalesRDto.java`, `DayDeliveriesRDto.java`, `DayServicesRDto.java`, `DayFeedItemRDto.java`
- `day/service/BusinessDayService.java`, `day/service/impl/BusinessDayServiceImpl.java` (méthode `findToday`)
- `delivery/service/DeliveryDayService.java`, `delivery/service/impl/DeliveryDayServiceImpl.java`, `delivery/repository/DeliveryStatusCount.java`, `DeliveryOrderRepository.java` (méthode `countByStatus`)

**Frontend** (`valimbavaka-ui/src/app/`)
- `features/day/data-access/day-live-models.ts`, `day-live-service.ts` ; `features/day/pages/day-live-page/` (`.ts`, `.html`, `.css`) ; `features/day/day.routes.ts` ; `core/layout/menu.ts` ; `public/i18n/fr.json`, `mg.json`

**Tests**
- Backend : `feature/dayoverview/controller/DayLiveIT.java`, `feature/dayoverview/service/impl/DayLiveServiceImplTest.java`, `feature/delivery/service/impl/DeliveryDayServiceImplTest.java`, `feature/day/service/impl/BusinessDayServiceImplTest.java` (2 tests ajoutés)
- Frontend : `features/day/data-access/day-live-service.spec.ts`, `features/day/pages/day-live-page/day-live-page.spec.ts`, `core/layout/menu.spec.ts` (assertions ajoutées)

## Dépendances

**Ce ticket s'appuie sur** :
- [LIV-02](../LIV-02/description.md) : commandes à livrer et tournée (`DeliveryOrder`, `scheduled_date`, statuts)
- [SRV-02](../SRV-02/guide.md) : `ServiceRecordService.getServiceRecordsBetween`
- [VTE-01](../VTE-01/guide.md), [VTE-07](../VTE-07/guide.md), [VTE-08](../VTE-08/guide.md) : ventes, `SaleQueryService`, `ChannelDashboardService`
- [JR-01](../JR-01/guide.md) : journée (`BusinessDayService`, `DayStatus`) ; [JR-02](../JR-02/guide.md) : même écran de journée (aucun lien fonctionnel)

**Ces tickets s'appuient sur lui** :
- Aucun. JR-04 à JR-06 peuvent réutiliser `BusinessDayService.findToday()` et `DeliveryDayService`.

## Comment l'utiliser depuis un autre ticket

```java
var live = dayLiveService.getLive();            // indicateurs + flux d'aujourd'hui
var today = businessDayService.findToday();     // null si la journée n'est pas ouverte
var byStatus = deliveryDayService.countByStatus(LocalDate.of(2026, 11, 11));
```

```ts
this.dayLiveService.live(); // Observable<DayLiveRDto>, à relire à intervalle régulier
```

## Points d'attention

- **Hypothèse (« temps réel »)** : relecture par interrogation toutes les 15 s plutôt que push (SSE ou WebSocket). Aucun mécanisme de push n'existe dans le projet et l'écran n'a qu'un lecteur (l'administrateur) ; le coût d'une lecture est faible (1 requête d'agrégat par activité et 30 lignes de flux). À valider par le PO ; l'intervalle est la constante `LIVE_REFRESH_MS`.
- **Hypothèse (permission)** : « administrateur » traduit par `reports.read` (ADMIN seul), comme VTE-08, sans migration ; une permission dédiée (`days.read`) serait une migration de trois lignes si le PO veut dissocier.
- **Hypothèse (indicateurs)** : ventes = CA, nombre et répartition par canal ; tournée = commandes par statut (« livrées / à livrer » à l'écran, hors annulées) ; prestations = nombre et CA. Le flux réunit les trois activités. Pas de graphique ni de comparaison à la veille.
- **Hypothèse (acteur du flux)** : le livreur d'une commande n'est pas affiché (la commande ne porte que son id) ; la vendeuse l'est pour les ventes et les prestations.
- **Limite** : le flux est borné à 30 éléments, sans pagination ; les listes complètes sont sur les écrans de chaque module.
- **Journée non ouverte** : l'écran s'affiche quand même (badge « Journée non ouverte », indicateurs à 0) ; il n'ouvre pas la journée (JR-01).
- **Tests d'intégration** : base partagée entre classes ; `DayLiveIT` compare des différences avant / après ses opérations plutôt que des valeurs absolues, ouvre la journée au besoin et appelle `endAutomaticPromotions()`.
- La recette manuelle 🧪 reste à passer (écran ouvert pendant qu'une vente est saisie sur un autre poste).
