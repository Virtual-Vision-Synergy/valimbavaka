# Guide d'intégration — LIV-02 · Consultation de la tournée du jour

Statut : ✅ rédigé, tests verts en CI (commit `254a538`) · Ticket : [#47](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/47) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le livreur ouvre `/round` et voit **sa tournée du jour** (jour d'Antananarivo) lue sur l'API : un arrêt mis en avant puis les suivants, chacun avec le client, l'adresse, le téléphone et le montant à encaisser. LIV-02 ne crée ni endpoint ni migration : il lit `GET /api/v1/deliveries` de [LIV-01](../LIV-01/guide.md) (cloisonné par le serveur) et branche l'écran mobile qui était en démonstration. Les boutons « Livrée » et « Échec » restent simulés (LIV-03, LIV-04).

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint. L'écran appelle celui de LIV-01 :

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/deliveries?driverId=<moi>&date=<aujourd'hui>&page=1&size=100&sortBy=stopNumber&sortDirection=ASC` | `deliveries.read` (administrateur, vendeuse, livreur) | `PageQuery` + filtres | 200 `PageResponse<DeliveryOrderRDto>` | 401, 403, 422 (`size` > 100) |

- Pour un livreur, le serveur remplace `driverId` par le sien (`DeliveryOrderServiceImpl.ownDriverId`, LIV-01) : il ne peut pas lire la tournée d'un autre.
- Le front envoie quand même `driverId` = utilisateur connecté : un administrateur ou une vendeuse qui ouvre `/round` voit **sa** tournée (vide), pas toutes les tournées mélangées.
- Champs utilisés par arrêt : `customerName`, `address` + `district`, `customerPhone`, `amount` (à encaisser), `stopNumber`, `status`, `lines.length`.

### Services et ports Java réutilisables

- Aucun ajout. Voir `DeliveryOrderService.getOrders(PageQuery, DeliveryOrderFilter)` dans le [guide de LIV-01](../LIV-01/guide.md#services-et-ports-java-réutilisables).

### Frontend

- Route : `/round` (coquille `DriverLayout`, garde `deliveries.read`, `app.routes.ts`) → `features/deliveries/driver.routes.ts` : `''` tournée, `history` arrêts terminés du jour, `profile`.
- `data-access/delivery-service.ts:58` `myRound(day = businessToday()): Observable<DriverRoundRDto>` : lit les commandes du livreur connecté (`AuthStore.currentUser().id`) pour le jour, puis `toDriverRound`. Paramètres communs avec `board()` dans `dayParams` (`delivery-service.ts:23`).
- `data-access/delivery-mapping.ts:91` `toDriverRound(orders, driver, day)` : arrêts triés par `stopNumber`, commandes `CANCELLED` écartées, tournée nommée `driverLabel` (« Tojo N. »).
- `data-access/delivery-mapping.ts:83` `isPendingStop(status)` : un arrêt est **à faire** s'il est `TO_PREPARE` ou `ON_ROUND`, **fait** s'il est `DELIVERED` ou `FAILED`.
- `pages/driver-round-page` : `upcoming` (`driver-round-page.ts:98`) utilise `isPendingStop` (avant : `ON_ROUND` seul, donc une commande neuve de LIV-01 n'apparaissait jamais) ; l'arrêt mis en avant affiche le numéro de téléphone (`driver-round-page.html:56`) ; la liste « Ensuite » affiche adresse, téléphone cliquable (`tel:`) et montant complet en ariary (`driver-round-page.html:113`, plus en « k ») ; une journée sans arrêt affiche « Aucun arrêt aujourd'hui » (`driver-round-page.html:90`).
- `pages/driver-history-page` lit aussi `myRound()` : il montre maintenant les arrêts `DELIVERED` / `FAILED` réels du jour.
- Traductions : `deliveries.driver.noStops`, `deliveries.driver.noStopsHint` (`public/i18n/fr.json`, `mg.json`).

### Données

- Aucune table ni migration. Aucune ligne de journal (lecture seule).

## Règles métier à respecter par les autres tickets

- **Jour de tournée** = jour d'Antananarivo (`businessToday()`), calculé sur le téléphone du livreur.
- **Ordre de passage** = `stopNumber` croissant ; une commande annulée disparaît sans renuméroter les autres arrêts (1 puis 3).
- **À faire / fait** : `isPendingStop` est la règle unique de l'écran (compteur « n/x », arrêt mis en avant, « Ensuite », montants « Encaissé » / « Reste »). LIV-03 et LIV-04 doivent garder ces quatre statuts ; si un nouveau statut apparaît, mettre `isPendingStop` à jour.
- **Montant à encaisser** = `amount` de la commande (prix figés par LIV-01). `prepaid` vaut toujours `false` tant que le paiement MVola d'avance n'est pas modélisé (LIV-03).
- Le livreur ne voit que ses commandes : c'est le serveur qui décide (LIV-01), le `driverId` envoyé par le front n'est qu'un confort.

## Fichiers concernés

**Backend (tests seulement)**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/delivery/controller/DeliveryDriverRoundIT.java`

**Frontend** (`valimbavaka-ui/src/app/features/deliveries/`)
- `data-access/delivery-service.ts`, `data-access/delivery-mapping.ts`
- `pages/driver-round-page/driver-round-page.ts`, `.html`, `.css`
- `valimbavaka-ui/public/i18n/fr.json`, `valimbavaka-ui/public/i18n/mg.json`

**Tests** : `data-access/delivery-service.spec.ts`, `data-access/delivery-mapping.spec.ts`, `pages/driver-round-page/driver-round-page.spec.ts` ; voir [scénario](scenario-test.md#tests-existants).

## Dépendances

**Ce ticket s'appuie sur** :
- [LIV-01](../LIV-01/guide.md) : `GET /api/v1/deliveries`, cloisonnement du livreur, statuts, `stopNumber`, `businessToday`, `driverLabel`, `toDelivery`.

**Ces tickets s'appuient sur lui** :
- [LIV-03](../LIV-03/description.md) · marquage « livrée » : remplacer `DeliveryService.updateStatus` (simulation) par l'appel réel en file hors ligne ; l'écran et son annulation de 5 s sont prêts.
- [LIV-04](../LIV-04/description.md) · échec avec motif : même méthode, motif déjà saisi par la feuille « Échec ».
- [JR-03](../JR-03/description.md) · suivi temps réel : peut relire la même requête par livreur, ou `board()` pour toutes les tournées.

## Comment l'utiliser depuis un autre ticket

```ts
// Tournée du jour du livreur connecté, arrêts à faire en premier
const round = rxResource({ stream: () => inject(DeliveryService).myRound() });
const todo = computed(() => (round.value()?.stops ?? []).filter((stop) => isPendingStop(stop.status)));
```

## Points d'attention

- **Hypothèses prises (session de nuit, sans validation)** :
  - aucun endpoint dédié (« ma tournée ») : la liste de LIV-01 suffit, comme l'indiquait son guide ;
  - une commande `TO_PREPARE` figure dans la tournée comme arrêt à faire : le départ en tournée (`ON_ROUND`) n'est pas exposé (LIV-01 l'attribue à LIV-02 ou JR-01) ; LIV-02 ne l'ajoute pas, faute de critère. À exposer par JR-01 (répartition du matin) ou un ticket dédié si le client veut distinguer « chargé » de « à préparer » ;
  - le montant des arrêts suivants est affiché en entier (« 25 500 Ar ») au lieu de « 26 k » : le livreur encaisse un montant exact ;
  - un administrateur ou une vendeuse sur `/round` voit une tournée vide (leur propre `driverId`).
- **Simulation restante** : « Livrée » et « Échec » appellent `DeliveryService.updateStatus` (démonstration) : après rechargement, l'arrêt redevient à faire (LIV-03, LIV-04). L'historique (`/round/history`) ne montrera donc des arrêts terminés qu'une fois LIV-03 et LIV-04 livrés.
- **Hors ligne** : la tournée n'est pas mise en cache ; sans réseau à l'ouverture, l'écran affiche l'erreur et « Réessayer ». Cache local de la tournée à prévoir par LIV-08.
- **Plus de 100 arrêts** : une seule page (100) est lue. Largement au-delà d'une tournée réelle ; pagination à ajouter si besoin.
- Jour pris sur l'horloge du téléphone (fuseau forcé à Antananarivo) : un téléphone dont la **date** est fausse lit la tournée d'un autre jour.
- `DRIVER_ROUND_FIXTURE` (`data-access/delivery-fixtures.ts`) n'est plus utilisé par le code ; gardé avec les autres fixtures de la maquette, à supprimer quand LIV-06 retirera la démonstration du tableau.
- PR empilée sur LIV-01 (`feature/LIV-01-creation-commande-a-livrer`) : à merger après elle.
