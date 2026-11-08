# Scénarios de test — LIV-06 · Suivi des livraisons par statut

Statut : ☐ tests écrits, exécution CI à lancer · Ticket : [#51](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/51) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

> **Décision PO du 07/11/2026** (LIV-01) : une commande livrée devient une vente (canal livraison) avec sa facture, au moment où la livraison est marquée livrée. LIV-06 ne fait que **lire** : la vente et la facture relèvent de [LIV-03](../LIV-03/description.md).

Correspondance des statuts du critère (hypothèse reprise du [guide de LIV-01](../LIV-01/guide.md#statuts-et-transitions)) : attente = `TO_PREPARE` (« À préparer »), en cours = `ON_ROUND` (« En tournée »), livrée = `DELIVERED`, retour = `FAILED` (« Échec »), annulée = `CANCELLED`.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôles utilisés | administrateur (`admin.test@valimbavaka.mg`) ; vendeuse (`tokenOf(RoleName.SELLER)`) ; livreur « Tojo Nirina » (`DeliveryApiTestSupport.createDriver`) ; côté front, administrateur `buildUser('ADMIN')` |
| Données de départ | article fini actif à 8 500 Ar (`StockApiTestSupport.createArticle`) ; commandes « Rakoto Be », 034 12 345 67, « Lot II J 45, rue Pasteur », Ivandry, saisies pour demain puis passées à chaque statut en base (`update delivery_orders set status = …`), les transitions n'étant pas encore exposées (LIV-03, LIV-04) |
| Tickets à avoir déployés avant | LIV-01 (commandes, `GET /api/v1/deliveries`, filtre `status`, cloisonnement du livreur), JNL-01 (journal), LIV-02 (tournée du livreur, inchangée) |
| Migration | aucune : réutilise `deliveries.read` et l'index `ix_journal_entries_resource` (JNL-01) |

## Scénarios issus des critères d'acceptation

### Critère 1 — La liste est filtrable par statut (attente / en cours / livrée / retour / annulée)

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · API : un filtre par statut | 5 commandes d'un même livreur, une par statut ; `GET /api/v1/deliveries?driverId=<id>&status=<S>` pour chacun des 5 statuts | 200, `totalCount` 1, la commande du statut demandé seulement ; sans `status` : `totalCount` 5 | `DeliveryTrackingIT.getOrders_filteredByEachStatus_returnsOnlyTheOrderInThatStatus` | ☐ |
| 1b · API : historique d'un jour passé | 2 commandes livrées, l'une hier, l'autre demain ; vendeuse : `?date=<hier>&status=DELIVERED` | 200, `totalCount` 1, la commande d'hier | `DeliveryTrackingIT.getOrders_pastDayFilteredByStatus_returnsOnlyTheOrdersOfThatDay` | ☐ |
| 1c · Écran : tableau par défaut | jour avec LIV-0001 à préparer, LIV-0002 livrée, LIV-0003 annulée, LIV-0004 en échec | 4 colonnes « À préparer 1 », « En tournée 0 », « Livrées 1 », « Échecs 1 » ; LIV-0003 absente ; pastilles « Tous les statuts 3 », « Annulée 1 » | `deliveries-page.spec.ts` (« montre les quatre colonnes du jour sans les commandes annulées par défaut ») | ☐ |
| 1d · Écran : filtre « Annulée » | même jour, clic sur « Annulée » | une seule colonne « Annulées 1 » avec LIV-0003 ; pastille `aria-pressed="true"` | `deliveries-page.spec.ts` (« filtre par statut : « Annulée » ne montre que les commandes annulées ») | ☐ |
| 1e · Écran : filtre « Échec » (retour) puis « Tous » | clic « Échec », puis « Tous les statuts » | colonne « Échecs 1 » avec LIV-0004, puis retour aux 4 colonnes | `deliveries-page.spec.ts` (« filtre par statut : « Échec » (retour)… ») | ☐ |
| 1f · Écran : jour passé | champ « Jour » passé à 2026-11-01 | `board('2026-11-01')` : le tableau de ce jour est relu | `deliveries-page.spec.ts` (« relit le tableau du jour choisi… ») | ☐ |
| 1g · Conversion : commandes annulées gardées | LIV-0001 annulée (Tojo), LIV-0002 livrée (livreur désactivé « gone ») | les deux commandes gardées avec leur statut ; tournées « Tojo N. » et « — » | `delivery-mapping.spec.ts` (« garde les commandes annulées (filtre « Annulée »)… ») | ☐ |

### Complément demandé — Panneau de détail et historique de la commande branchés sur l'API

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 2 · API : étape de création | commande créée par l'administrateur, lue par la vendeuse : `GET /api/v1/deliveries/{id}/history` | 200, `totalCount` 1 ; `action` `CREATE`, `occurredAt` et `actorName` renseignés, `description` contient le numéro `LIV-…`, ni nom du client ni `actorId` | `DeliveryTrackingIT.getHistory_createdOrder_returnsTheCreationStepWithItsTimeAndAuthor` | ☐ |
| 2b · API : ordre et cloisonnement par ressource | ligne `UPDATE` « Commande LIV-… en tournée » une heure plus tard + ligne d'une autre ressource | 200, `totalCount` 2 : `CREATE` puis `UPDATE` (auteur « Système ») ; la ligne de l'autre ressource absente | `DeliveryTrackingIT.getHistory_linesOfTheOrderAndOfAnotherResource_listsOnlyTheOrderLinesOldestFirst` | ☐ |
| 2c · Service : étapes = lignes du journal | commande visible, journal renvoyant 1 ligne `CREATE` de « Hanta Rabe » | page d'une étape mappée par `DeliveryMapper.toEventRDto` | `DeliveryOrderServiceImplTest.getHistory_visibleOrder_returnsItsJournalLinesAsSteps` | ☐ |
| 2d · Journal : lecture par ressource | `getResourceEntries("DELIVERY_ORDER", id, PageQuery par défaut)` | requête `findByResourceTypeAndResourceId`, page 0, tri `occurredAt` croissant | `JournalServiceImplTest.getResourceEntries_oneOrder_queriesOnlyItsLinesOldestFirst` | ☐ |
| 2e · Front : appel du service | `history('id-1')` | `GET /api/v1/deliveries/id-1/history?page=1&size=100`, renvoie les `items` | `delivery-service.spec.ts` (« lit l'historique d'une commande… ») | ☐ |
| 2f · Front : détail | clic sur la carte LIV-0001 | `history('id-LIV-0001')` ; étape « Commande créée », « 08/11 09:15 · Hanta Rabe », libellé du journal ; aucune violation axe dans le panneau | `deliveries-page.spec.ts` (« ouvre le détail avec l'historique horodaté et attribué… ») | ☐ |
| 2g · Front : heure d'Antananarivo | `2026-11-08T06:15:00Z` ; `2026-11-30T21:30:00Z` | « 08/11 09:15 » ; « 01/12 00:30 » (passage de minuit et de fin de mois) | `delivery-mapping.spec.ts` (« date une étape de l'historique… ») | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Liste sans jeton | `GET /api/v1/deliveries` | 401 `UNAUTHENTICATED` | `DeliveryDriverRoundIT.getOrders_driverRoundWithoutToken_returns401` (LIV-02) | ☐ |
| Historique sans jeton | `GET /api/v1/deliveries/{id}/history` | 401 `UNAUTHENTICATED` | `DeliveryTrackingIT.getHistory_withoutToken_returns401` | ☐ |
| Mauvais rôle (403) | — | sans objet : les trois rôles (administrateur, vendeuse, livreur) ont `deliveries.read` ; le livreur est cloisonné par le serveur (lignes suivantes) | — | ⏭ hors périmètre (aucun rôle sans `deliveries.read`) |
| Livreur filtrant par statut | livreur Tojo : `?status=TO_PREPARE`, une commande d'Ando aussi à préparer | seulement sa commande (`totalCount` 1) | `DeliveryTrackingIT.getOrders_driverFilteringByStatus_onlyGetsHisOwnOrders` | ☐ |
| Livreur lisant l'historique d'un autre | Tojo lit l'historique de sa commande puis de celle d'un autre livreur | 200 puis 404 `NOT_FOUND` | `DeliveryTrackingIT.getHistory_driverReadingHisOwnOrAnotherDriversOrder_returns200Or404` ; `DeliveryOrderServiceImplTest.getHistory_driverReadingAnotherDriversOrder_throwsNotFoundWithoutReadingTheJournal` | ☐ |
| Statut inconnu | `?status=RETURNED` | 400 `MALFORMED_REQUEST` | `DeliveryTrackingIT.getOrders_unknownStatus_returns400` | ☐ |
| Commande inconnue | historique d'un UUID aléatoire | 404 `NOT_FOUND` | `DeliveryTrackingIT.getHistory_unknownOrder_returns404` | ☐ |
| Pagination invalide | historique avec `size=101` | 422 `VALIDATION_FAILED` | `DeliveryTrackingIT.getHistory_sizeAbove100_returns422` | ☐ |
| Mass assignment, 409, ligne de journal | — | sans objet : LIV-06 n'ajoute que des lectures (aucun corps, aucune écriture, aucune ligne de journal) | — | ⏭ hors périmètre (lecture seule) |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Statut sans commande | filtre « En tournée » un jour sans départ | colonne « En tournée 0 » avec « Aucune commande » | `deliveries-page.spec.ts` (« montre les quatre colonnes… », colonne « En tournée 0 ») | ☐ |
| Commande sans autre étape | commande juste créée | historique d'une seule étape « Commande créée » | `DeliveryTrackingIT.getHistory_createdOrder_returnsTheCreationStepWithItsTimeAndAuthor` | ☐ |
| Historique vide | aucune ligne (commande antérieure au journal) | « Aucune étape enregistrée. » | 🔍 code : `delivery-detail.html` (`@empty`) | 🔍 code |
| Historique hors ligne | `history()` en erreur | état d'erreur avec « Réessayer », pas de frise | `deliveries-page.spec.ts` (« propose de réessayer quand l'historique ne se charge pas ») | ☐ |
| Champ jour vidé | effacement du champ date | le jour affiché est gardé (`DeliveriesPage.pickDay`) | 🔍 code | 🔍 code |
| Tri hors liste blanche | historique `?sortBy=description` | ignoré, tri `occurredAt` croissant (`PageQuery.toPageable`, liste `occurredAt`, `actorName`, `action`, `resourceType`) | 🔍 code | 🔍 code |
| Plus de 100 commandes un jour / 100 étapes | — | une seule page lue (100) : au-delà, non affiché (voir guide, points d'attention) | — | ⏭ hors périmètre |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/delivery/controller/DeliveryTrackingIT.java` ; `.../feature/delivery/service/impl/DeliveryOrderServiceImplTest.java` (2 tests `getHistory_*`) ; `.../feature/journal/service/impl/JournalServiceImplTest.java` (`getResourceEntries_*`).
- Frontend : `valimbavaka-ui/src/app/features/deliveries/pages/deliveries-page/deliveries-page.spec.ts` ; `.../data-access/delivery-service.spec.ts` ; `.../data-access/delivery-mapping.spec.ts` ; page couverte aussi par `src/app/features/demo-pages.a11y.spec.ts` (`deliveries`).

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Administrateur sur `/livraisons` (`/deliveries`) : choisir chaque pastille de statut | une colonne par statut choisi, compteurs cohérents avec le tableau | 🧪 recette |
| Choisir hier dans « Jour » | tableau d'hier ; « Nouvelle commande » reste disponible | 🧪 recette |
| Ouvrir une commande sur mobile (tiroir) puis sur grand écran (panneau ancré) | historique avec heure d'Antananarivo et auteur ; « Appeler le client » pour une commande en tournée | 🧪 recette |

## Résultat de l'exécution

- Date : à lancer (CI ou local).
- Exécution : tests écrits, non exécutés localement (règle n°3) ; à lancer par la CI (`ci.yml`, `conventions.yml`, Qodana).
- Anomalies ouvertes : aucune connue.
- Étapes 🧪 restantes : 3.
