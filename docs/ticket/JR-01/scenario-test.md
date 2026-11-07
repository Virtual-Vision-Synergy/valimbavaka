# Scénarios de test — JR-01 · Répartition matinale du stock

Statut : ☐ tests écrits, à lancer (CI ou local) · Ticket : [#54](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/54) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (permission `days.open`) ; `SELLER` et `DELIVERY_DRIVER` pour les refus |
| Données de départ | Entrepôt : « Savon noir » 20, « Huile de coco » 10 (articles finis actifs) ; emplacements actifs `WAREHOUSE`, `STORE` (Boutique), `DELIVERY` (Livraison), `TRADE_SHOW` (Salon) ; aucune journée ouverte pour la date du jour (fuseau Indian/Antananarivo) |
| Tickets à avoir déployés avant | STK-01, TRF-03 (port `TransferService.record`), JNL-01, JNL-02, SYS-05 (emplacements), SYS-07 / SYS-12 (file hors ligne, `Idempotency-Key`) |
| Migration | `2026_11_08-10-changelog.sql` : tables `business_days`, `business_day_transfers`, permission `days.open` (ADMIN) |

## Scénarios issus des critères d'acceptation

### Critère 1 — La validation génère un bon de transfert par destination et met à jour les stocks

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| 1 · Deux destinations | `POST /api/v1/days` : Savon 12 → Boutique, Huile 4 → Boutique, Savon 5 → Livraison | 201, `status` = `OPEN`, 2 `transfers` dans l'ordre Boutique puis Livraison ; Boutique : 2 lignes, 16 u. ; chaque bon est `MORNING` / `VALIDATED`, numéro `TRF-…` ; entrepôt Savon 3, boutique Savon 12 / Huile 4, livraison Savon 5 | `BusinessDayIT.openDay_twoDestinations_returns201WithOneValidatedMorningTransferPerDestination` | ☐ |
| 1b · Un bon par destination (service) | plan Boutique (Savon 12, Huile 4) + Livraison (Savon 5) | 2 appels à `TransferService.record`, origine `MORNING`, depuis l'entrepôt ; la journée garde `TRF-0093`, `TRF-0094`, 2 lignes / 16 u. pour la boutique | `BusinessDayServiceImplTest.openDay_twoDestinations_recordsOneMorningTransferPerDestinationFromTheWarehouse` | ☐ |
| 1c · Même article saisi deux fois pour une destination | Savon 2 + 3 → Boutique | une seule ligne de 5 dans le bon de la boutique | `BusinessDayIT.openDay_sameArticleTwiceForADestination_addsTheQuantitiesInOneLine`, `DayOpeningPolicyTest.check_sameArticleTwiceForADestination_addsTheQuantitiesUpPerDestination` | ☐ |
| 1d · Ouverture sans répartition | `{"lines":[]}` | 201, journée `OPEN`, aucun transfert | `BusinessDayIT.openDay_noLine_opensTheDayWithoutTransfer` | ☐ |
| 1e · Grille du matin | `GET /api/v1/days/opening` avant ouverture ; Savon 20 à l'entrepôt, un article 0 à l'entrepôt (3 en boutique) | 200 ; `day` absent ; `warehouseLocationId` = entrepôt ; destinations `STORE`, `DELIVERY`, `TRADE_SHOW` (pas l'entrepôt) ; ligne Savon `warehouse` = 20 ; article sans stock à l'entrepôt absent | `BusinessDayIT.getOpening_beforeOpening_listsTheWarehouseArticlesAndTheDestinations` | ☐ |
| 1f · Grille après ouverture | ouverture avec Savon 6 → Boutique puis `GET /api/v1/days/opening` | `day.status` = `OPEN`, 1 transfert vers la boutique | `BusinessDayIT.getOpening_afterOpening_returnsTodayWithItsMorningTransfers` | ☐ |
| 1g · Écran : validation | saisie Boutique 12, Livraison 5 pour Savon, clic « Valider et générer 2 bons » | corps envoyé : 2 lignes (`store-id` 12, `delivery-id` 5) ; carte « Journée ouverte » avec `TRF-0093`, `TRF-0094` ; toast « 2 bons de transfert générés » | `day-open-page.spec.ts` « ouvre la journée : un bon par destination, numéros affichés » | ☐ |
| 1h · Écran : grille | 2 articles reçus du serveur | 2 lignes, quantités à 0, bouton de validation désactivé | `day-open-page.spec.ts` « liste les articles disponibles à l’entrepôt, quantités à zéro » | ☐ |
| 1i · Écran : journée déjà ouverte | grille avec bons `TRF-0093` | carte de fin affichée, pas de tableau de saisie | `day-open-page.spec.ts` « montre les bons du matin quand la journée est déjà ouverte » | ☐ |
| 1j · Data-access | réponse `DayOpeningRDto` (STORE, DELIVERY, TRADE_SHOW) | colonnes Boutique / Livraison / Salon reliées aux UUID ; `vouchers` null ; quantités proposées à 0 ; bons repris si `day` est posé | `day-service.spec.ts` « lit la grille du jour… », « reprend les bons du matin… » | ☐ |
| 1k · Corps envoyé | Savon Boutique 12, Salon 3 (colonne sans UUID) ; Huile Livraison 5 | seulement les quantités > 0 et les colonnes reliées à un emplacement | `day-service.spec.ts` « n’envoie que les quantités positives… » | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST /api/v1/days` sans `Authorization` | 401 `UNAUTHENTICATED` | `BusinessDayIT.openDay_withoutToken_returns401` | ☐ |
| Mauvais rôle (écriture) | jeton vendeuse | 403 `FORBIDDEN` | `BusinessDayIT.openDay_withSellerRole_returns403` | ☐ |
| Mauvais rôle (grille) | `GET /api/v1/days/opening`, jeton vendeuse | 403 `FORBIDDEN` | `BusinessDayIT.getOpening_withSellerRole_returns403` | ☐ |
| Mass assignment | `{"id":"…","businessDate":"2020-01-01","status":"CLOSED","lines":[]}` | 201 ; id généré, date du jour, `status` = `OPEN` | `BusinessDayIT.openDay_massAssignmentOfIdDateAndStatus_isIgnored` | ☐ |
| Destination = entrepôt | Savon 1 → `WAREHOUSE` | 422 `VALIDATION_FAILED` | `BusinessDayIT.openDay_destinationIsTheWarehouse_returns422`, `DayOpeningPolicyTest.check_destinationIsTheWarehouse_throwsBusinessException` | ☐ |
| Destination inconnue ou inactive | `destinationId` aléatoire | 422 `VALIDATION_FAILED` | `BusinessDayIT.openDay_unknownDestination_returns422`, `DayOpeningPolicyTest.check_inactiveDestination_throwsBusinessException` | ☐ |
| Lignes absentes | `{}` | 422 `VALIDATION_FAILED` | `BusinessDayIT.openDay_missingLines_returns422` | ☐ |
| Article inconnu | `articleId` aléatoire → Boutique | 404 `NOT_FOUND` | `BusinessDayIT.openDay_unknownArticle_returns404` | ☐ |
| Journée déjà ouverte | deuxième `POST /api/v1/days` le même jour | 409 `CONFLICT` « La journée du 08/11/2026 est déjà ouverte. » | `BusinessDayIT.openDay_alreadyOpenedToday_returns409`, `DayOpeningPolicyTest.check_dayAlreadyOpened_throwsConflict` | ☐ |
| Stock insuffisant à l'entrepôt (toutes destinations) | Savon 8 → Boutique + 5 → Livraison, 12 à l'entrepôt | 409 `CONFLICT` « Stock insuffisant à l'entrepôt pour Savon noir : 13 demandé(s), 12 disponible(s). » ; rien n'est écrit (entrepôt 12, aucune journée) | `BusinessDayIT.openDay_totalAboveWarehouseStock_returns409AndWritesNothing`, `DayOpeningPolicyTest.check_totalAcrossDestinationsAboveWarehouseStock_throwsConflictNamingTheArticle` | ☐ |
| Journal | ouverture avec Savon 2 → Salon | ligne `CREATE` / `BUSINESS_DAY` « Ouverture de la journée du … : 1 transfert(s) du matin (TRF-…) », en plus des lignes `TRANSFER` de TRF-03 | `BusinessDayIT.openDay_validDispatch_isTracedInTheJournal`, `BusinessDayServiceImplTest.openDay_validDispatch_tracesTheOpeningInTheJournal` | ☐ |
| Écran : refus du serveur | 409 « déjà ouverte » | message dans une alerte (`role="alert"`), bouton de nouveau actif | `day-open-page.spec.ts` « affiche le refus du serveur (journée déjà ouverte) » | ☐ |
| Écran : accès | vendeuse | entrée « Ouverture de journée » absente du menu ; route `/day/open` gardée par `days.open` | `menu.spec.ts` « should show sales, catalogue and production modules… » | ☐ |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Quantité nulle | Savon 0 → Boutique | 422 | `BusinessDayIT.openDay_zeroQuantity_returns422` | ☐ |
| 4 décimales | 1.0005 | 422 (3 décimales au maximum) | `BusinessDayIT.openDay_quantityWithFourDecimals_returns422` | ☐ |
| Dépassement à l'écran | Huile : Boutique 4 + Salon 3 pour 6 à l'entrepôt | ligne en erreur, rien n'est envoyé | `day-open-page.spec.ts` « n’envoie rien quand on demande plus que le stock de l’entrepôt » | ☐ |
| Minuit à Antananarivo | horloge 2026-11-08T21:30Z | la journée du jour est le 09/11/2026 (UTC+3) | `DayOpeningPolicyTest.today_lateEveningInUtc_isTheNextDayInAntananarivo` | ☐ |
| Aucune ligne (service) | `lines` vide | plan vide, le stock n'est pas lu | `DayOpeningPolicyTest.check_noLine_returnsAnEmptyPlanWithoutReadingTheStock` | ☐ |
| Port pour VTE-01 | `requireOpenDay()` avant puis après l'ouverture | 409 `ConflictException` puis la journée `OPEN` | `BusinessDayIT.requireOpenDay_beforeAndAfterOpening_throwsThenReturnsTheOpenDay`, `BusinessDayServiceImplTest.requireOpenDay_noDayToday_throwsConflict`, `requireOpenDay_openedToday_returnsTheDay` | ☐ |
| Hors ligne | réseau coupé au clic | toast « Saisie enregistrée hors connexion… », bouton désactivé (pas de second envoi) | `day-open-page.spec.ts` « met l’ouverture en file hors connexion et empêche un second envoi » | ☐ |
| Rejeu avec la même `Idempotency-Key` | même corps, même clé, deux fois | 201 rejoué (`Idempotency-Replayed: true`), même id, stock déplacé une seule fois | `BusinessDayIT.openDay_replayedWithSameIdempotencyKey_opensTheDayOnce` | ☐ |
| File hors ligne (data-access) | `open(body)` | `OutboxService.submit('POST', '/api/v1/days', body)` | `day-service.spec.ts` « ouvre la journée par la file hors ligne (POST /api/v1/days) » | ☐ |
| Deux administrateurs en même temps | deux ouvertures simultanées | la première passe ; la seconde : 409 (contrainte `uq_business_days_business_date` via `saveAndFlush`, ou stock déjà sorti) et tout est annulé | — (garanti par la contrainte unique et la transaction) | 🔍 code `BusinessDayServiceImpl.openDay` |
| Plus de 100 articles pour une destination | 101 lignes vers la boutique | 422 (règle de TRF-03 : 100 lignes par bon) | ⏭ couvert par `StockReturnIT.recordReturn_101Lines_returns422` (même `TransferPolicy`) | ⏭ hors périmètre (TRF-03) |
| Accessibilité | page chargée | aucune violation axe (WCAG 2.2 AA, hors contraste) | `day-open-page.spec.ts` « ne présente aucune violation d’accessibilité détectable » | ☐ |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/day/controller/BusinessDayIT.java` ; `feature/day/service/impl/DayOpeningPolicyTest.java`, `BusinessDayServiceImplTest.java`.
- Frontend : `valimbavaka-ui/src/app/features/day/data-access/day-service.spec.ts`, `features/day/pages/day-open-page/day-open-page.spec.ts`, `core/layout/menu.spec.ts` (ajout) ; la page est retirée de `features/demo-pages.a11y.spec.ts` (elle appelle l'API) et contrôlée par axe dans sa propre spec.

## Recette manuelle (🧪)

| Cas | Exemple | Résultat attendu | Statut |
|---|---|---|---|
| Parcours complet | administrateur, `/day/open`, saisie au clavier (Entrée descend d'une ligne), validation | carte « Journée ouverte · 3 bons générés », bons visibles dans `/transfers` avec l'origine « Matin », statut Validé et l'administrateur comme émetteur | 🧪 |
| Retour sur l'écran | recharger `/day/open` après ouverture | carte des bons du matin, pas de nouvelle saisie possible | 🧪 |
| Stocks | `/stocks` après ouverture | entrepôt diminué, boutique / livraison / salon augmentés des quantités saisies | 🧪 |
| Vendeuse | connexion vendeuse | pas d'entrée « Ouverture de journée » ; `/day/open` → page « accès refusé » | 🧪 |
| Mobile | écran de 375 px | tableau défilant, bouton de validation accessible | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-08 — résultat : à lancer (CI ou local).
- Anomalies ouvertes : aucune connue.
- Étapes 🧪 restantes : 5.
