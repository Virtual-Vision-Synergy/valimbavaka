# Guide d'intégration — LIV-06 · Suivi des livraisons par statut

Statut : ✅ rédigé, tests verts en CI (commit `f16612a`) · Ticket : [#51](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/51) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur suit les livraisons sur `/deliveries` : le tableau d'un jour (aujourd'hui par défaut, un jour passé donne son historique) se filtre par tournée **et par statut** (« Tous les statuts », à préparer, en tournée, livrée, échec, annulée), avec un compteur par statut. Le panneau de détail et l'historique de la commande, jusqu'ici en démonstration, sont branchés sur l'API : l'historique est lu par le nouvel endpoint `GET /api/v1/deliveries/{id}/history`, qui renvoie les lignes du journal de la commande (heure, auteur, libellé). Aucune migration, aucune écriture.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/deliveries?status=<S>&date=<jour>&driverId=<id>` (LIV-01, inchangé) | `deliveries.read` | `PageQuery` + filtres ; `status` ∈ `TO_PREPARE`, `ON_ROUND`, `DELIVERED`, `FAILED`, `CANCELLED` | 200 `PageResponse<DeliveryOrderRDto>` | 400 `MALFORMED_REQUEST` (statut inconnu), 401, 422 (`size` > 100) |
| GET | `/api/v1/deliveries/{id}/history` (**nouveau**) | `deliveries.read` (administrateur, vendeuse, livreur) | `PageQuery` (tri `occurredAt` croissant par défaut) | 200 `PageResponse<DeliveryEventRDto>` | 401, 404 (commande inconnue ou d'un autre livreur), 422 (`size` > 100) |

- `DeliveryEventRDto` (`feature/delivery/dto/DeliveryEventRDto.java`) : `occurredAt` (instant UTC), `actorName` (nom au moment de l'opération, « Système » hors requête authentifiée), `action` (`CREATE`, `UPDATE`, `CANCEL`, `REVERSE`… de `JournalAction`, en texte), `description` (libellé du journal, sans nom ni téléphone du client). Pas d'`actorId`.
- Livreur cloisonné comme pour `GET /{id}` : l'historique d'une commande d'un autre livreur répond 404 (`DeliveryOrderServiceImpl.findVisible`).
- Correspondance avec le critère : attente = `TO_PREPARE`, en cours = `ON_ROUND`, livrée = `DELIVERED`, retour = `FAILED`, annulée = `CANCELLED`.

### Services et ports Java réutilisables

- `DeliveryOrderService.getHistory(UUID orderId, PageQuery query)` (`feature/delivery/service/DeliveryOrderService.java`) : vérifie la visibilité puis lit le journal.
- `JournalService.getResourceEntries(String resourceType, UUID resourceId, PageQuery query)` (`feature/journal/service/JournalService.java`) : **historique de n'importe quelle fiche** (lignes d'une ressource, plus ancienne d'abord), servi par l'index `ix_journal_entries_resource`. Ne vérifie aucun droit : la feature appelante vérifie que l'utilisateur peut lire la ressource avant d'appeler.
- `DeliveryMapper.toEventRDto(JournalEntryRDto)`.

### Frontend (`features/deliveries`)

- `pages/deliveries-page` : champ « Jour » (`day`, `pickDay`), pastilles « Filtrer par statut » (`statusOptions`, `DELIVERY_STATUS_FILTERS`, compteur par statut de la tournée choisie) ; `ALL` = les 4 colonnes sans les annulées, un statut = sa seule colonne (cartes en grille). Le résumé et « Arrêt n/x » ne comptent pas les annulées. La route n'a plus `underConstruction`.
- `components/delivery-detail` : charge lui-même l'historique (`rxResource` sur `DeliveryService.history(id)`) ; états chargement / erreur (« Réessayer ») / vide ; affiche aussi « Tournée du <jour> · arrêt n » et la note. Les boutons de démonstration (« Marquer préparée », « Affecter », « Reprogrammer », « Retour en stock ») sont retirés ; restent « Appeler le client » (en tournée) et « Voir dans les ventes » (livrée).
- `data-access/delivery-service.ts` : `history(orderId): Observable<DeliveryEventRDto[]>` (page 1, 100 étapes) ; `board(day)` accepte un jour passé.
- `data-access/delivery-models.ts` : `DeliveryEventRDto` devient le miroir du backend, `JournalAction`, `DeliveryStatusFilter`, `DELIVERY_STATUS_FILTERS` ; `DeliveryRDto.status` est un `DeliveryOrderStatus` (annulée comprise), avec `scheduledDate` et `note` ; plus de `history` dans la carte.
- `data-access/delivery-mapping.ts` : `toBoard` **garde** les commandes annulées (avant : écartées) ; `eventTime(instant)` (« 08/11 09:15 », heure d'Antananarivo) ; `isPendingStop` accepte `DeliveryOrderStatus`.
- `data-access/delivery-tones.ts` : `DELIVERY_STATUS_TONES` couvre `CANCELLED` ; `DELIVERY_EVENT_TONES` est indexé par `JournalAction`.
- Fixtures de démonstration supprimées : `delivery-fixtures.ts`, `delivery-fixture-rows.ts` (plus utilisées, voir LIV-02).
- Traductions (`fr.json`, `mg.json`) : `deliveries.dayLabel`, `statusFilter`, `allStatuses`, `status.CANCELLED`, `column.CANCELLED`, `event.<JournalAction>`, `detail.round`, `detail.noHistory` ; `deliveries.subtitle` ne dit plus « aujourd'hui ».

### Données

- Aucune table ni migration (plage Liquibase 40-59 non utilisée). Aucune ligne de journal (lecture seule).

## Règles métier à respecter par les autres tickets

- **L'historique d'une commande = ses lignes de journal** (ressource `DELIVERY_ORDER`, id de la commande). LIV-03, LIV-04, LIV-05 n'ont rien à brancher côté lecture : il suffit que chaque transition écrive sa ligne (`journalService.record(UPDATE | CANCEL | REVERSE, "DELIVERY_ORDER", order.getId(), libellé)`), comme le demande déjà le guide de LIV-01. Le libellé est affiché tel quel à l'administrateur, à la vendeuse et au livreur de la commande : **ni nom ni téléphone du client**, mettre le numéro `LIV-…` et le nouveau statut (`DeliveryStatus.label()`), le motif d'échec pour LIV-04.
- LIV-07 (historique horodaté et attribué) est couvert par ce mécanisme pour l'affichage ; s'il faut un statut structuré par étape (ancien → nouveau), ajouter une table dédiée dans `feature/delivery` et faire évoluer `getHistory` sans changer l'endpoint.
- Un nouveau statut doit être ajouté à `DELIVERY_STATUS_FILTERS`, `DELIVERY_STATUS_TONES`, aux traductions `deliveries.status.*` / `column.*` et à `DeliveryCard.footer`.
- Le tableau se lit par jour (≤ 100 commandes) et filtre côté client ; pour une liste multi-jours, utiliser directement `GET /api/v1/deliveries?status=` paginé.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/`)
- `delivery/controller/DeliveryController.java` (`getHistory`)
- `delivery/dto/DeliveryEventRDto.java` (nouveau), `delivery/mapper/DeliveryMapper.java`
- `delivery/service/DeliveryOrderService.java`, `delivery/service/impl/DeliveryOrderServiceImpl.java` (`getHistory`, `findVisible`)
- `journal/repository/JournalEntryRepository.java` (`findByResourceTypeAndResourceId`), `journal/service/JournalService.java`, `journal/service/impl/JournalServiceImpl.java` (`getResourceEntries`)

**Frontend** (`valimbavaka-ui/src/app/features/deliveries/`)
- `deliveries.routes.ts`
- `pages/deliveries-page/deliveries-page.ts`, `.html`, `.css`, `.spec.ts` (nouveau)
- `components/delivery-detail/delivery-detail.ts`, `.html`, `.css` ; `components/delivery-card/delivery-card.ts`
- `data-access/delivery-models.ts`, `delivery-mapping.ts`, `delivery-service.ts`, `delivery-tones.ts` (+ specs) ; suppression de `delivery-fixtures.ts`, `delivery-fixture-rows.ts`
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json`

**Tests** : voir [scénario](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** :
- [LIV-01](../LIV-01/guide.md) : `GET /api/v1/deliveries` et son filtre `status`, cloisonnement du livreur, statuts.
- [LIV-02](../LIV-02/guide.md) : `toDriverRound`, `isPendingStop` (inchangés pour le livreur).
- [JNL-01](../JNL-01/guide.md) : lignes du journal, index par ressource.

**Ces tickets s'appuient sur lui** :
- [LIV-03](../LIV-03/description.md), [LIV-04](../LIV-04/description.md), [LIV-05](../LIV-05/description.md) : leurs lignes de journal apparaissent seules dans l'historique ; leurs boutons d'action se placent dans `delivery-detail.html` (bloc `@switch`).
- [LIV-07](../LIV-07/description.md) : voir règles ci-dessus.

## Comment l'utiliser depuis un autre ticket

```java
// Historique d'une autre fiche (ex. un transfert) : vérifier d'abord le droit de lecture, puis
var steps = journalService.getResourceEntries("TRANSFER", transferId, query);
```

```ts
// Historique d'une commande dans un composant
const history = rxResource({
  params: () => orderId(),
  stream: ({ params }) => inject(DeliveryService).history(params),
});
```

## Points d'attention

- **Hypothèses prises (session de nuit, sans validation)** :
  - « retour » du critère = `FAILED` (« Échec ») et « attente » = `TO_PREPARE` (« À préparer ») : libellés de l'écran gardés ;
  - le suivi reste un **tableau par jour** (champ « Jour ») plutôt qu'une liste paginée multi-jours ; le filtre de statut s'applique côté client sur le jour chargé (≤ 100 commandes, la liste de LIV-01 filtre déjà côté serveur) ;
  - l'historique est **lu dans le journal** (pas de nouvelle table) : visible par les trois rôles qui ont `deliveries.read`, livreur limité à ses commandes ; les libellés de LIV-01 ne contiennent aucune donnée client ;
  - « Tous les statuts » cache les commandes annulées (comme avant LIV-06) ; elles ne se voient qu'avec la pastille « Annulée ».
- Boutons de démonstration retirés du panneau (préparer, affecter, reprogrammer, remettre en stock) : à rajouter par les tickets qui exposent ces transitions (LIV-04 pour l'échec, un ticket dédié pour l'annulation et la réaffectation, non exposées).
- Pas d'endpoint d'annulation : le filtre « Annulée » ne montre rien tant qu'aucun ticket n'expose `CANCELLED`.
- Plus de 100 commandes un jour ou plus de 100 étapes pour une commande : seule la première page est lue.
- Le canal de prise de commande et le paiement MVola restent non modélisés (voir LIV-01).
