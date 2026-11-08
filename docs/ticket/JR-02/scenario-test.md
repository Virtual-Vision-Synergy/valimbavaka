# Scénarios de test — JR-02 · Reprise des quantités de la veille

Statut : ✅ auto (CI verte le 08/11/2026), 🧪 recette à passer · Ticket : [#56](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/56) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (permission `days.open`, posée par JR-01) ; `SELLER` pour le refus |
| Données de départ | Entrepôt : Savon noir (ART-0031) 20 pièces, Huile de coco (ART-0087) 8 ; une journée ouverte le vendredi 06/11/2026 avec TRF-0093 (Boutique : savon 12) et TRF-0094 (Livraison : savon 5) |
| Tickets à avoir déployés avant | JR-01 (journée, grille, transferts du matin), TRF-03 (transferts) |

« Dernier jour ouvré » = la dernière journée **ouverte** avant aujourd'hui (`DayOpeningRDto.previousDay` de JR-01), pas forcément la date d'hier : le lundi, c'est le vendredi ou le samedi si le dimanche n'a pas été ouvert.

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Le bouton « Reprendre hier » applique les quantités du dernier jour ouvré. | Étant donné la journée du vendredi ouverte avec savon 12 → Boutique et 5 → Livraison, quand l'administrateur ouvre la grille du lundi, alors chaque ligne porte les quantités envoyées ce jour-là, par emplacement | `GET /api/v1/days/opening` → 200, `previousDay` = date du vendredi (J-3), ligne du savon : `yesterday` = Boutique 12, Livraison 5 ; huile : `yesterday` vide | `BusinessDayIT.getOpening_afterAPreviousWorkingDay_returnsTheQuantitiesItSentToEachDestination` | ✅ auto |
| 1 bis | idem (lecture des transferts) | Deux transferts du matin la veille (Boutique : savon 12, huile 4 ; Livraison : savon 5) | quantités regroupées par article puis par emplacement | `PreviousDayQuantitiesTest.of_twoMorningTransfers_groupsTheQuantitiesByArticleAndDestination` | ✅ auto |
| 1 ter | idem (port transfert) | Lecture des transferts de la veille par identifiant | chaque transfert avec ses lignes, doublons d'id fusionnés | `TransferServiceImplTest.getTransfersByIds_knownIds_returnsEachTransferWithItsLines` | ✅ auto |
| 1 quater | idem (écran) | Quand l'administrateur a saisi 2 huiles au salon puis clique « Reprendre les quantités d'hier », alors la grille affiche savon Boutique 12, Livraison 5 et l'huile revient à 0 ; la validation envoie ces quantités | `POST /api/v1/days` avec les deux lignes du savon | `day-open-page.spec.ts` « reprend les quantités du dernier jour ouvré et les envoie à la validation (JR-02) » | ✅ auto |
| 1 quinquies | idem (conversion) | Le serveur renvoie Boutique 12, Salon 3 et un emplacement inconnu de la grille | colonnes `{ SHOP: 12, DELIVERY: 0, SALON: 3 }`, emplacement inconnu ignoré | `day-service.spec.ts` « range les quantités du dernier jour ouvré par colonne (JR-02) » | ✅ auto |

## Sécurité et erreurs

Aucun nouvel endpoint : les quantités de la veille passent par `GET /api/v1/days/opening` (JR-01), déjà protégé par `days.open`. Pas d'écriture nouvelle (la validation reste `POST /api/v1/days`, couvert par JR-01).

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `BusinessDayIT.openDay_withoutToken_returns401` (JR-01, même contrôleur) | ✅ auto |
| Appel avec un mauvais rôle (`SELLER`) | 403 `FORBIDDEN` | `BusinessDayIT.getOpening_withSellerRole_returns403` (JR-01) | ✅ auto |
| Quantités de la veille supérieures au stock de l'entrepôt du jour | l'écran les applique, signale la ligne en erreur et bloque la validation ; le serveur refuserait de toute façon (409, JR-01) | `day-open-page.spec.ts` « n'envoie rien quand on demande plus que le stock de l'entrepôt » (JR-01) ; `BusinessDayIT.openDay_totalAboveWarehouseStock_returns409AndWritesNothing` | ✅ auto |

## Cas limites et hors ligne

- **Toute première journée** (aucune journée ouverte avant) : `previousDay` absent, `yesterday` vide, bouton désactivé — `BusinessDayIT.getOpening_veryFirstDay_hasNoPreviousDayAndNoQuantityToTakeAgain`, `PreviousDayQuantitiesTest.of_noPreviousDay_returnsEmptyWithoutReadingTransfers`, `day-open-page.spec.ts` « désactive « Reprendre hier » la toute première journée (pas de veille) » ✅ auto
- **Veille ouverte sans transfert** (ouverture sans ligne) : rien à reprendre, aucune lecture des transferts — `PreviousDayQuantitiesTest.of_previousDayWithoutMorningTransfer_returnsEmptyWithoutReadingTransfers` ✅ auto
- **Aucun identifiant** à lire côté transfert : liste vide sans requête — `TransferServiceImplTest.getTransfersByIds_noId_returnsEmptyWithoutReading` ✅ auto
- **Article envoyé la veille mais absent de la grille du jour** (plus de stock à l'entrepôt, article désactivé) : il n'a pas de ligne, sa quantité de la veille n'est pas reprise (comportement attendu : on ne peut pas répartir ce qu'on n'a pas).
- **Emplacement de la veille sans colonne** (désactivé depuis, ou nouveau code hors `DESTINATION_BY_LOCATION_CODE`) : ignoré à l'écran (test « range les quantités… », emplacement `unknown-id`).
- **Hors ligne** : la grille est lue en ligne (lecture) ; la validation après reprise passe par la file hors ligne de JR-01 sans changement.
- **Recette manuelle** 🧪 : ouvrir une journée, avancer l'horloge (ou attendre le lendemain), ouvrir l'écran, cliquer « Reprendre les quantités d'hier » et vérifier les colonnes Boutique / Livraison / Salon.

## Résultat de l'exécution

- Date : 08/11/2026, commit `1a7bcf00` (branche à jour de `develop`)
- Environnement : CI GitHub Actions — `ci.yml` vert (backend : 573 tests unitaires + 601 tests d'intégration Testcontainers, 0 échec ; frontend : 615 tests Vitest, 0 échec), `conventions.yml` vert (Spotless, Checkstyle, Prettier, ESLint, gitleaks), Qodana for JVM : aucun nouveau problème (0 annotation)
- Tests JR-02 : `BusinessDayIT` (22, dont les 2 de JR-02), `PreviousDayQuantitiesTest` (3), `TransferServiceImplTest` (10, dont 2 de JR-02), `day-open-page.spec.ts`, `day-service.spec.ts`
- Anomalies ouvertes : aucune connue
