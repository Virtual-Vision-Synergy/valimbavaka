# Scénarios de test — LIV-02 · Consultation de la tournée du jour

Statut : ☐ tests écrits, à lancer (CI) · Ticket : [#47](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/47) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

> **Décision PO du 07/11/2026** (LIV-01) : une commande livrée devient une vente (canal livraison) avec sa facture, au moment où la livraison est marquée livrée. LIV-02 ne fait que **lire** la tournée : la vente et la facture relèvent de [LIV-03](../LIV-03/description.md).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôles utilisés | livreur « Tojo Nirina » (`deliveries.read`) ; administrateur (`admin.test@valimbavaka.mg`) pour saisir les commandes ; côté front, livreur « Hanta Rasolofoson » (`buildUser('DELIVERY_DRIVER')`) |
| Données de départ | article fini actif à 8 500 Ar (`StockApiTestSupport.createArticle`) ; deux livreurs actifs (`DeliveryApiTestSupport.createDriver`) ; commandes « Rakoto Be », 034 12 345 67, « Lot II J 45, rue Pasteur », Ivandry, saisies **aujourd'hui** (jour d'Antananarivo) et demain |
| Tickets à avoir déployés avant | LIV-01 (commandes à livrer, `GET /api/v1/deliveries`, cloisonnement du livreur), AUTH-01 (`deliveries.read` du livreur), SYS-07 (bandeau hors ligne de l'écran) |
| Migration | aucune : LIV-02 réutilise l'endpoint et la permission de LIV-01 |

## Scénarios issus des critères d'acceptation

### Critère 1 — Chaque arrêt affiche client, adresse, téléphone et montant à encaisser

La tournée du jour = les commandes du livreur connecté pour aujourd'hui à Antananarivo, dans l'ordre de `stopNumber`, lues par `GET /api/v1/deliveries?driverId=<moi>&date=<aujourd'hui>&page=1&size=100&sortBy=stopNumber&sortDirection=ASC`.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · API : tournée du jour du livreur | Tojo : 2 commandes aujourd'hui (2 et 1 savons), 1 demain ; Ando : 1 aujourd'hui ; lecture avec le jeton de Tojo | 200, `totalCount` 2 ; arrêt 1 puis arrêt 2 ; arrêt 1 : `customerName` « Rakoto Be », `address` « Lot II J 45, rue Pasteur », `district` « Ivandry », `customerPhone` « 034 12 345 67 », `amount` 17 000 ; arrêt 2 : 8 500. Ni la commande de demain ni celle d'Ando | `DeliveryDriverRoundIT.getOrders_driverRoundOfToday_returnsHisStopsInOrderWithContactAndAmount` | ☐ |
| 1b · Appel du service front | `myRound('2026-11-09')`, livreur connecté Hanta Rasolofoson | `GET /api/v1/deliveries` avec `driverId` = id du livreur, `date` 2026-11-09, `size` 100, `sortBy` `stopNumber`, `sortDirection` `ASC` ; tournée « Hanta R. » du 2026-11-09 | `delivery-service.spec.ts` (« lit la tournée du jour du livreur connecté, arrêt par arrêt ») | ☐ |
| 1c · Conversion en arrêts | commandes arrêt 3 (`ON_ROUND`, Ivandry), arrêt 2 (`CANCELLED`), arrêt 1 (`TO_PREPARE`, sans quartier, 8 500) | arrêts dans l'ordre 1 puis 3, l'annulée écartée ; arrêt 1 : « Rakoto Be », « Lot II J 45, rue Pasteur », 034 12 345 67, 8 500, non payée d'avance ; arrêt 3 : adresse « Ivandry, Lot II J 45, rue Pasteur » | `delivery-mapping.spec.ts` (« range la tournée du livreur par arrêt… ») | ☐ |
| 1d · Arrêt mis en avant | arrêts 1 « Rakoto Be » livrée, 2 « Rasoa Vola » à préparer (17 000), 3 « Hery Andry » en tournée | « Prochain arrêt » = Rasoa Vola, adresse « Ivandry, Lot II J 2, rue Pasteur », téléphone affiché « 034 12 345 62 », bouton Appeler `tel:0341234562`, montant à encaisser 17 000 Ar | `driver-round-page.spec.ts` (« met en avant le premier arrêt à faire… ») | ☐ |
| 1e · Arrêts suivants | même tournée | « Ensuite » liste Hery Andry avec son adresse, son téléphone (lien `tel:0341234563`) et 25 500 Ar (montant complet, plus en « k ») ; compteur « 1/3 » | `driver-round-page.spec.ts` (« liste les arrêts suivants dans l'ordre… ») | ☐ |
| 1f · Arrêt « à préparer » compté comme à faire | statuts `TO_PREPARE`, `ON_ROUND`, `DELIVERED`, `FAILED` | à faire : `TO_PREPARE` et `ON_ROUND` ; faits : `DELIVERED` et `FAILED` (une commande de LIV-01 naît `TO_PREPARE` et doit apparaître) | `delivery-mapping.spec.ts` (« compte comme à faire les arrêts à préparer ou en tournée… ») | ☐ |

## Sécurité et erreurs

Endpoint lu : `GET /api/v1/deliveries` (écrit par LIV-01, permission `deliveries.read`). LIV-02 n'écrit rien.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/deliveries?date=<aujourd'hui>` sans `Authorization` | 401 `UNAUTHENTICATED` | `DeliveryDriverRoundIT.getOrders_driverRoundWithoutToken_returns401` | ☐ |
| Livreur qui demande la tournée d'un autre | jeton de Tojo, `driverId` d'Ando | seulement les commandes de Tojo (le serveur remplace `driverId`) | `DeliveryOrderIT.getOrders_driverAskingForAnotherRound_onlyGetsHisOwnOrders` (LIV-01) | ✅ auto (CI de LIV-01) |
| Mauvais rôle | — | ⏭ sans objet : les 3 rôles ont `deliveries.read` ; un administrateur ou une vendeuse qui ouvre `/round` voit sa propre tournée, vide (le front envoie son `driverId`) | — | ⏭ hors périmètre |
| Mass assignment, 409, journal | — | ⏭ sans objet : lecture seule, aucune écriture ni ligne de journal | — | ⏭ hors périmètre |
| `size` 101 | `size=101` | 422 `VALIDATION_FAILED` | `DeliveryOrderIT.getOrders_sizeAbove100_returns422` (LIV-01) | ✅ auto (CI de LIV-01) |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Journée sans arrêt (API) | livreur créé sans commande | 200, `totalCount` 0, `items` vide | `DeliveryDriverRoundIT.getOrders_driverWithoutStopToday_returnsAnEmptyRound` | ☐ |
| Journée sans arrêt (écran) | tournée vide | « Aucun arrêt aujourd'hui » au lieu de « Tournée terminée » (qui annonçait « Bravo ! 0 Ar encaissés ») | `driver-round-page.spec.ts` (« annonce une journée sans arrêt… ») | ☐ |
| Tournée vide (conversion) | aucune commande | `stops` = `[]` | `delivery-mapping.spec.ts` (« donne une tournée vide… ») | ☐ |
| Tri hors liste blanche | `sortBy=customerPhone` | 200, les arrêts du livreur quand même (tri par défaut `createdAt`) ; l'écran envoie toujours `stopNumber` | `DeliveryDriverRoundIT.getOrders_driverRoundSortedOutsideTheWhitelist_stillReturnsHisStops` | ☐ |
| Minuit | 22:30 UTC le 08/11 | jour de tournée 2026-11-09 (Antananarivo, UTC+3) | `delivery-mapping.spec.ts` (« donne le jour d'Antananarivo… », LIV-01) | ✅ auto (CI de LIV-01) |
| Plus de 100 arrêts | 101 commandes un même jour | seule la 1ʳᵉ page (100 arrêts) est lue : limite connue, voir guide | — | ⏭ hors périmètre (pas de pagination sur l'écran mobile) |
| Commande annulée | `CANCELLED` à l'arrêt 2 | n'apparaît pas ; les numéros d'arrêt ne sont pas renumérotés (1 puis 3) | `delivery-mapping.spec.ts` (« range la tournée du livreur par arrêt… ») | ☐ |
| Hors ligne | réseau coupé à l'ouverture | état d'erreur avec « Réessayer » (`ListState`) ; la tournée n'est pas mise en cache (LIV-08) | — | 🧪 recette |
| Accessibilité | tournée de 3 arrêts | aucune violation axe (WCAG 2.2 AA, hors contraste) | `driver-round-page.spec.ts` (« n'a aucune violation d'accessibilité détectable ») | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/delivery/controller/DeliveryDriverRoundIT.java` (4 tests, nouveaux) ; `DeliveryOrderIT` (LIV-01, cloisonnement et `size`).
- Frontend :
  - `valimbavaka-ui/src/app/features/deliveries/pages/driver-round-page/driver-round-page.spec.ts` (4 tests, nouveau) ;
  - `valimbavaka-ui/src/app/features/deliveries/data-access/delivery-mapping.spec.ts` (3 tests ajoutés) ;
  - `valimbavaka-ui/src/app/features/deliveries/data-access/delivery-service.spec.ts` (1 test ajouté).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Vendeuse : créer 2 commandes pour aujourd'hui pour le livreur Tojo ; se connecter en Tojo sur un téléphone | `/round` affiche « Tournée Tojo N. », le 1ᵉʳ arrêt avec client, adresse, téléphone, montant ; le 2ᵉ dans « Ensuite » | 🧪 |
| R2 | Toucher « Appeler » puis le numéro de l'arrêt suivant | le téléphone propose d'appeler le bon numéro | 🧪 |
| R3 | Couper le réseau puis rouvrir `/round` | message d'erreur et « Réessayer » ; au retour du réseau, la tournée se charge | 🧪 |
| R4 | Marquer un arrêt « Livrée » puis recharger la page | l'arrêt redevient « à faire » : le marquage est encore simulé (LIV-03) | 🧪 |

## Résultat de l'exécution

- Date : à lancer (CI)
- Environnement : CI GitHub (`ci.yml`, `conventions.yml`)
- Résultat : à lancer (CI ou local)
- Anomalies ouvertes : aucune
- Étapes 🧪 restantes : 4 (R1 à R4)
