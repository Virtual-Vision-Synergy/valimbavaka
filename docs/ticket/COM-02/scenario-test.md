# Scénarios de test — COM-02 · Calcul automatique de la commission

Statut : ✅ rédigé et exécuté en CI · Ticket : [#86](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/86) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Règle appliquée (**décision PO du 07/11/2026**) : la commission la plus précise gagne, sans cumul — taux du service salon, sinon taux du canal (règle active COM-01), sinon taux de la vendeuse. Une vente sans vendeuse (commande saisie par l'administrateur, LIV-03) ne génère aucune commission.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (`admin.test@valimbavaka.mg`) : saisie des ventes et lecture des commissions ; `SELLER` et `DELIVERY_DRIVER` pour les refus 403 |
| Données de départ | journée du jour ouverte (JR-01) ; un canal neuf par test (`IT-xxxxxxxx`, `CommissionCalculationIT.createChannel`, la base est partagée) ; vendeuse « Miora Rakoto » à 5,00 % sur ce canal ; article fini à 8 500 Ar, coût 3 120,50 Ar, 10 en stock Boutique ; service salon « Soin-… » à 35 000 Ar et 10 % (`ServiceRecordCommissionIT.createServiceWithRate`) |
| Tickets à avoir déployés avant | COM-01 (résolveur), SRV-01/SRV-02 (prestations), VTE-01 (`SaleRecordedEvent`), PRO-02 (total remisé), LIV-05 (`SaleCancelledEvent`), SYS-05 ; migration `2026_11_08-83` |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Montant = CA × taux applicable (règle du canal sur le CA) | règle du canal 4 %, vendeuse 5 %, vente 3 × 8 500 = 25 500 Ar | commission 1 020 Ar, base `REVENUE` 25 500, taux 4, `rateSource = SALES_CHANNEL`, `ruleId` de la règle, numéro de la vente, « Miora Rakoto » | `CommissionCalculationIT.createSale_channelRuleOnRevenue_commissionIsTheTotalTimesTheRuleRate` ; `CommissionServiceImplTest.calculate_activeSeller_savesRevenueTimesResolvedRateAndTracesTheJournal` | ✅ auto |
| 1b | Règle du canal sur la marge | règle 10 % `MARGIN`, 2 × 8 500 = 17 000 Ar, coût 2 × 3 120,50 = 6 241 Ar | base `MARGIN` 10 759 Ar, commission 1 076 Ar (1 075,90 arrondi) | `CommissionCalculationIT.createSale_channelRuleOnMargin_commissionIsTheMarginTimesTheRuleRate` ; `CommissionTest.calculate_marginRuleWithCost_appliesTheRateToRevenueMinusCost` | ✅ auto |
| 1c | Pas de règle sur le canal : taux de la vendeuse | canal sans règle, vendeuse 5 %, 25 500 Ar | 1 275 Ar, `rateSource = SELLER`, pas de `ruleId` | `CommissionCalculationIT.createSale_noRuleOnTheChannel_commissionUsesTheSellerRate` | ✅ auto |
| 1d | Prestation salon : le taux du service gagne | service 35 000 Ar à 10 %, vendeuse 5 % | 3 500 Ar, `SALON_SERVICE`, `source_type = SERVICE_RECORD`, `salon_service_id` renseigné | `ServiceRecordCommissionIT.recordService_serviceWithItsOwnRate_commissionIsThePriceTimesTheServiceRate` ; `ServiceRecordServiceImplTest.recordService_validRecord_creditsTheCommissionOnThePriceOfTheServiceAndTheBusinessDay` | ✅ auto |
| 1e | Vente sans vendeuse (décision PO) | `SaleService.record` avec `sellerId = null` | vente enregistrée, aucune ligne dans `commissions` | `CommissionCalculationIT.record_saleWithoutSeller_createsNoCommission` ; `SaleCommissionListenerTest.onSaleRecorded_saleWithoutSeller_calculatesNothing` | ✅ auto |
| 1f | Prestation saisie par l'administrateur | admin enregistre une prestation | aucune commission (le compte n'est pas une vendeuse) | `ServiceRecordCommissionIT.recordService_byAdministrator_createsNoCommission` ; `CommissionServiceImplTest.calculate_accountIsNotAnActiveSeller_returnsEmptyAndSavesNothing` | ✅ auto |
| 1g | Données transmises au calcul | vente VTE-0042, total 25 500, coût 9 361,50 | `CommissionSource(SALE, id, VTE-0042, vendeuse, canal, null, journée, 25500.00, 9361.50)` | `SaleCommissionListenerTest.onSaleRecorded_saleWithSeller_calculatesOnTheTotalWithThePurchaseCost` | ✅ auto |
| 1h | Montant affiché à l'administrateur | page `/commissions/calculated` | opération (+ « Vente » / « Prestation salon »), date, vendeuse, base (+ CA / marge), taux (+ origine), commission ; compteur | `commission-list-page.spec.ts` › should show the operation, the seller, the base, the rate with its origin and the commission | ✅ auto |
| 1i | Mois courant demandé, plus récentes d'abord | ouverture de la page | `from` = 1er du mois, `to` = dernier jour, `sortBy=createdAt`, `DESC` | `commission-list-page.spec.ts` › should request the current month, newest first ; `commission-service.spec.ts` (2 tests) | ✅ auto |
| 1j | Base de calcul = total remisé (PRO-02) | vente de 28 000 Ar avec 2 500 Ar de remise, total 25 500 Ar | la commission est calculée sur `sale.total()` (25 500), jamais sur le sous-total | `SaleCommissionListenerTest.onSaleRecorded_saleWithSeller_calculatesOnTheTotalWithThePurchaseCost` | ✅ auto |
| 1k | Vente annulée (LIV-05) : la commission est annulée | vente de 8 500 Ar (commission 425 Ar) puis `SaleService.cancel` | commission au statut `CANCELLED` (montant conservé pour la trace), une ligne `CANCEL` au journal ; écran : mention « Annulée avec la vente » | `CommissionCalculationIT.cancel_saleWithACommission_commissionIsCancelledAndTraced` ; `SaleCommissionListenerTest.onSaleCancelled_anySale_cancelsTheCommissionOfTheSale` ; `CommissionServiceImplTest.cancelBySource_calculatedCommission_becomesCancelledAndIsJournaled` ; `CommissionTest.cancel_calculatedCommission_becomesCancelledAndKeepsItsAmount` ; `commission-list-page.spec.ts` › should mark the commission of a cancelled sale and only that one | ✅ auto |
| 1l | Commission neuve au statut `CALCULATED` | vente de 8 500 Ar | `status = CALCULATED` | `CommissionCalculationIT.createSale_validSale_commissionIsCalculated` | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | GET `/api/v1/commissions` | 401 `UNAUTHENTICATED` | `CommissionCalculationIT.getCommissions_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton vendeuse, jeton livreur | 403 `FORBIDDEN` (`commissions.read-all` réservé à `ADMIN`) | `CommissionCalculationIT.getCommissions_sellerOrDeliveryDriver_returns403` | ✅ auto |
| Période invalide | `from=2026-11-30&to=2026-11-01` | 422 `VALIDATION_FAILED` | `CommissionCalculationIT.getCommissions_fromAfterToOrSizeAbove100_returns422` ; `CommissionServiceImplTest.getCommissions_fromAfterTo_throwsBusinessException` | ✅ auto |
| Taille de page hors borne | `size=101` | 422 `VALIDATION_FAILED` | `CommissionCalculationIT.getCommissions_fromAfterToOrSizeAbove100_returns422` | ✅ auto |
| Mass assignment | aucun endpoint d'écriture : la commission n'est jamais saisie, seulement calculée par le serveur | — | ArchUnit (`no_delete_endpoints`, contrôleur en lecture seule) | ⏭ sans objet |
| Ressource introuvable | — | pas d'endpoint par id (lecture par opération via le port `findBySource`, VTE-09) | — | ⏭ VTE-09 |
| Annulation répétée ou opération sans commission | rejeu de `SaleCancelledEvent`, vente sans vendeuse | rien n'est modifié ni journalisé | `CommissionServiceImplTest.cancelBySource_alreadyCancelledOrUnknown_doesNothing` | ✅ auto |
| Doublon | même opération calculée deux fois (rejeu de l'événement) | la commission existante est renvoyée, rien n'est recalculé ni journalisé ; contrainte `uq_commissions_source` | `CommissionServiceImplTest.calculate_operationAlreadyCommissioned_returnsTheExistingCommissionWithoutRecalculating` | ✅ auto |
| Journal | vente de 8 500 Ar, vendeuse 5 % | ligne `CREATE`, `resourceType = COMMISSION`, « Commission VTE-… · Miora Rakoto · 5.00 % de 8500.00 Ar (CA) = 425.00 Ar » | `CommissionCalculationIT.createSale_validSale_commissionIsTracedInTheJournal` | ✅ auto |
| Ligne de la prestation au journal | recherche « Prestation PRE-… » | la ligne `SERVICE_RECORD` reste trouvable (la commission la cite aussi) | `ServiceRecordIT.recordService_bySellerWithAdjustedProducts_deductsSalonStockAndTracesJournal` | ✅ auto |
| Menu | admin / vendeuse | entrée « Commissions calculées » visible pour l'admin seulement | `menu.spec.ts` (2 tests modifiés) | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Arrondi à l'ariary (HALF_UP) | 25 500 × 3,33 % = 849,15 ; 50 × 1 % = 0,50 | 849 Ar ; 1 Ar | `CommissionTest.calculate_fractionOfAriary_isRoundedHalfUpToTheAriary` | ✅ auto |
| Vendu sous le coût (marge négative) | CA 5 000, coût 6 000, règle marge 10 % | base 0, commission 0 (jamais négative) | `CommissionTest.calculate_marginRuleSoldBelowCost_baseAndAmountAreZero` | ✅ auto |
| Règle « marge » sans coût connu | prestation salon (aucun coût figé), règle du canal boutique en marge | taux appliqué au prix, base enregistrée `REVENUE` | `CommissionTest.calculate_marginRuleWithoutKnownCost_appliesTheRateToTheRevenue` | ✅ auto |
| Aucun taux trouvé | ni service, ni règle, ni taux vendeuse | commission 0 Ar, `rateSource = NONE` (ligne gardée pour la traçabilité) | `CommissionTest.calculate_noRateFound_amountIsZero` | ✅ auto |
| Champs copiés | vente VTE-0042 | type, id, numéro, vendeuse, nom figé, journée, règle | `CommissionTest.calculate_revenueRule_appliesTheRateToTheRevenue` | ✅ auto |
| Coût d'achat de la vente | 2 × 3 120,50 + 1,5 × 1 000,005 + ligne sans coût | 7 741,01 Ar ; aucune ligne avec coût → 0,00 | `SaleTest.purchaseCost_linesWithAndWithoutCost_sumsQuantityTimesUnitCostOfTheCostedLines` ; `SaleTest.purchaseCost_noLineWithCost_isZero` | ✅ auto |
| Journée d'une prestation après minuit | 22:30 UTC le 8/11 = 01:30 le 9/11 à Antananarivo | `businessDate = 2026-11-09` | `ServiceRecordServiceImplTest.recordService_validRecord_creditsTheCommissionOnThePriceOfTheServiceAndTheBusinessDay` | ✅ auto |
| Période vide | `from=2020-01-01&to=2020-01-31` | 200, page vide | `CommissionCalculationIT.getCommissions_periodWithoutTheSale_returnsAnEmptyPage` | ✅ auto |
| Sans période | ni `from` ni `to` | toutes les journées (bornes 2000-01-01 → 9999-12-31) | `CommissionServiceImplTest.getCommissions_withoutPeriod_searchesEveryBusinessDayOfTheSeller` | ✅ auto |
| Bornes de mois | février bissextile, changement d'année | `2028-02` → 01 au 29 ; janvier − 1 mois → décembre précédent | `commission-list-page.spec.ts` › should give the month key with an offset and the first and last days of the month | ✅ auto |
| Changement de mois | mois précédent choisi | rechargement en page 1, état vide « Aucune commission ce mois-ci » | `commission-list-page.spec.ts` › should reload the chosen month from its first page | ✅ auto |
| Tri hors liste blanche | `sortBy=password` | tri par défaut `createdAt` décroissant (`PageQuery.toPageable`) | — (comportement commun de `PageQuery`) | 🔍 code |
| Vente refusée | stock insuffisant, journée fermée | rien n'est écrit, ni vente ni commission (même transaction, écouteur synchrone) | `SaleIT.createSale_notEnoughStock_returns409AndRecordsNothing` (la commission est calculée après le contrôle) | 🔍 code |
| Hors ligne | vente saisie hors connexion puis rejouée (`Idempotency-Key`) | la vente n'est enregistrée qu'une fois (SYS-12), donc une seule commission ; contrainte unique en dernier recours | — | 🔍 code |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/commission/domain/CommissionTest.java`, `feature/commission/service/impl/CommissionServiceImplTest.java`, `feature/commission/service/impl/SaleCommissionListenerTest.java`, `feature/commission/controller/CommissionCalculationIT.java`, `feature/salonservice/controller/ServiceRecordCommissionIT.java` ; modifiés : `feature/sale/domain/SaleTest.java`, `feature/salonservice/service/impl/ServiceRecordServiceImplTest.java`, `feature/salonservice/controller/ServiceRecordIT.java`.
- Frontend : `valimbavaka-ui/src/app/features/commissions/data-access/commission-service.spec.ts`, `features/commissions/pages/commission-list-page/commission-list-page.spec.ts`, `core/layout/menu.spec.ts`.

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Admin : créer une règle Facebook 5 % (COM-01), encaisser au poste une vente Facebook de 64 000 Ar pour Miora, ouvrir Administration › Commissions calculées | ligne VTE-… · Miora · 64 000 Ar CA · 5 % Règle du canal · 3 200 Ar | 🧪 |
| Vendeuse : enregistrer une prestation d'un service à taux propre | ligne PRE-… avec « Service salon » et le taux du service | 🧪 |
| Changer de mois dans la liste | la liste se recharge, état vide si aucun calcul | 🧪 |
| Accessibilité de la page (lecteur d'écran, contraste) : la page n'est pas dans `demo-pages.a11y.spec.ts` | libellé du sélecteur de mois lu, tableau navigable | 🧪 |

## Résultat de l'exécution

- Date : 11/11/2026, commit `29616059` de la branche (après fusion de `develop`).
- Environnement : CI GitHub : `ci.yml` vert (backend 754 tests, 0 échec ; frontend 122 fichiers de test, tous réussis), `conventions.yml` vert (Spotless, Checkstyle, Prettier, ESLint, gitleaks), `qodana_code_quality.yml` vert, 0 annotation sur « Qodana for JVM ».
- Anomalies ouvertes : aucune connue. Les lignes 🔍 (code) et 🧪 (recette Njara) restent à confirmer à la main.
