# Scénarios de test — COM-05 · Recalcul et export des commissions

Statut : ✅ rédigé · Ticket : [#89](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/89) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Décision PO du 07/11/2026 : la commission la plus précise gagne, sans cumul (service salon, puis règle du canal, puis taux de la vendeuse). Le recalcul passe par le même résolveur et le même calcul que COM-02 (`CommissionRateResolver`, `Commission.applyTerms`), il n'en existe pas de copie.

Hypothèses prises (la description ne les tranche pas) :
- Seul l'administrateur recalcule (permission dédiée `commissions.recalculate`, comme `commissions.validate`). Une vendeuse ne recalcule jamais ses commissions.
- Le recalcul ne touche que les commissions `CALCULATED`. Une `VALIDATED` est définitive (COM-03) et une `CANCELLED` ne compte plus : ni l'une ni l'autre n'est modifiée ; le résultat compte les validées laissées telles quelles.
- Le recalcul applique les taux et règles en vigueur au jour du recalcul et relit le coût d'achat de la vente ; le chiffre d'affaires, l'opération, la vendeuse et la journée ne changent jamais.
- Période obligatoire (`from`, `to`, incluses), un an au plus (366 jours d'écart ou plus : 422). La vendeuse est facultative : sans elle, toutes les vendeuses.
- L'export réutilise les routes jumelles de SYS-01 sur deux listes existantes : `GET /api/v1/commissions/export` (administrateur, toutes vendeuses ou une) et `GET /api/v1/commissions/follow-up/lines/export` (relevé du mois : la vendeuse n'a que le sien, l'administrateur désigne la vendeuse). Les annulées figurent dans le fichier avec leur statut. Aucune trace de l'export au journal (SYS-01 ne l'a pas prévu).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` : recalcule et exporte tout ; `SELLER` : exporte son relevé, ne recalcule pas ; `DELIVERY_DRIVER` pour le refus 403 |
| Données de départ | journée du jour ouverte (JR-01) ; un canal neuf par test, vendeuses « Miora Rakoto » et « Hanta Rakoto » à 5,00 % (la base est partagée) ; chaque vente d'un article à 8 500 Ar rapporte 425 Ar de commission |
| Tickets à avoir déployés avant | COM-02 (calcul), COM-03 (statut `VALIDATED`), COM-04 (relevé du mois), SYS-01 (export), JNL-01 (journal) ; migration `2026_11_11-62` (permission `commissions.recalculate`) |

## Scénarios issus des critères d'acceptation

Critère : « Les actions Recalculer et Exporter confirment leur exécution. »

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Recalcul après un changement de taux | vente à 5 % (425 Ar), le taux de la vendeuse passe à 8 %, `POST /commissions/recalculation` | 200 : `updatedCount = 1`, `previousAmount = 425`, `newAmount = 680` ; la liste montre 680 Ar à 8 % | `CommissionRecalculationIT.recalculate_sellerRateChanged_updatesTheCalculatedCommissions` | ☐ auto |
| 1b | Confirmation du recalcul à l'écran | « Recalculer » sur Commissions calculées, confirmation, réponse 3 mises à jour / 1 inchangée / 2 validées | le message chiffré « 3 commission(s) mise(s) à jour, 1 inchangée(s), 2 validée(s) laissée(s) telle(s) quelle(s) » s'affiche, vendeuses et détail rechargés | `commission-list-page-actions.spec.ts` › should recalculate the displayed month for all sellers only after the confirmation, then say what changed | ☐ auto |
| 1c | Rien n'est envoyé sans confirmation | « Recalculer » puis « Annuler » | aucune requête, dialogue fermé | `commission-list-page-actions.spec.ts` › should send nothing when the recalculation is cancelled | ☐ auto |
| 1d | Refus du serveur affiché | 422 « La période est trop longue. » | message d'erreur lisible, bouton de nouveau actif | `commission-list-page-actions.spec.ts` › should show the server error when the recalculation is refused | ☐ auto |
| 2 | Même règle que COM-02 : la plus précise gagne, sans cumul | règle du canal 3 % du CA créée après la vente | 8 500 × 3 % = 255 Ar (et non 5 % + 3 %), `rateSource = SALES_CHANNEL` | `CommissionRecalculationIT.recalculate_channelRuleCreatedAfterTheSale_winsOverTheSellerRateWithoutAccumulating` | ☐ auto |
| 2b | Unité : le recalcul passe par le résolveur, une fois par vendeuse et canal | 2 ventes de la même vendeuse sur le même canal | `resolve` appelé une seule fois, 2 commissions mises à jour | `CommissionRecalculationServiceImplTest.recalculate_sameSellerAndChannel_asksTheResolverOnce` | ☐ auto |
| 2c | Calcul de l'entité identique à COM-02 | 25 500 Ar à 4 % puis taux 5 % | 1 020 Ar puis 1 275 Ar, origine du taux et règle mises à jour, CA inchangé | `CommissionTest.recalculate_newRate_appliesItToTheSameRevenueAndReportsTheChange` | ☐ auto |
| 2d | Base marge : le coût d'achat est relu | CA 25 500, coût 20 000 puis 22 000, 10 % | 550 Ar puis 350 Ar (marge 3 500) | `CommissionTest.recalculate_marginRule_usesThePurchaseCostGivenAgain` | ☐ auto |
| 2e | Une prestation n'a pas de coût d'achat | commission de prestation salon | le coût n'est pas lu (`SaleService` jamais appelé), le taux s'applique au CA | `CommissionRecalculationServiceImplTest.recalculate_salonService_doesNotReadAPurchaseCost` | ☐ auto |
| 3 | Idempotent : un deuxième lancement ne change rien | deux `POST` identiques | 2e réponse : `updatedCount = 0`, `unchangedCount = 1`, montants égaux ; une seule ligne de journal | `CommissionRecalculationIT.recalculate_twice_secondRunChangesNothingAndWritesNoJournal` ; `CommissionRecalculationServiceImplTest.recalculate_nothingChanged_writesNothingToTheJournal` ; `CommissionTest.recalculate_sameTerms_changesNothing` | ☐ auto |
| 4 | Tracé au journal | commission recalculée 425 → 680 Ar | une ligne `UPDATE` sur `COMMISSION` : « Commission VTE-… · Miora Rakoto recalculée : 425.00 → 680.00 Ar (taux 5.00 % → 8.00 %) » | `CommissionRecalculationIT.recalculate_changedCommission_isTracedInTheJournal` ; `CommissionRecalculationServiceImplTest.recalculate_rateChanged_updatesTheCommissionAndTracesTheJournal` | ☐ auto |
| 5 | Une commission validée n'est jamais modifiée | commission validée (COM-03), puis le taux passe à 8 % | `updatedCount = 0`, `validatedCount = 1` ; la commission reste `VALIDATED` à 425 Ar | `CommissionRecalculationIT.recalculate_validatedCommission_isLeftUntouchedAndCounted` ; `CommissionRecalculationServiceImplTest.recalculate_validatedCommissions_areCountedButNeverRead` ; `CommissionTest.recalculate_validatedOrCancelledCommission_throwsAndKeepsItsAmount` | ☐ auto |
| 6 | Une commission annulée n'est jamais modifiée | vente annulée par un avoir, puis le taux change | `updatedCount = 0` ; reste `CANCELLED` à 425 Ar | `CommissionRecalculationIT.recalculate_cancelledCommission_isLeftUntouched` | ☐ auto |
| 7 | Recalcul d'une seule vendeuse | deux vendeuses passent à 8 %, `sellerId` = Miora | Miora 680 Ar, Hanta reste à 425 Ar | `CommissionRecalculationIT.recalculate_oneSellerOnly_doesNotTouchTheOthers` | ☐ auto |
| 8 | Export Excel du relevé de la vendeuse | Miora : 2 ventes ; Hanta : 1 vente ; Miora exporte son relevé du mois | feuille « Relevé » : en-têtes (Date, Opération, Vendeuse, CA (Ar), Base (Ar), Taux (%), Commission (Ar), Statut), 2 lignes de Miora, aucune de Hanta, commission numérique 425 | `CommissionExportIT.exportStatement_seller_returnsOnlyHerCommissionsAsXlsx` | ☐ auto |
| 8b | Export PDF du relevé | Miora exporte en PDF | 200 `application/pdf`, fichier qui commence par `%PDF-` | `CommissionExportIT.exportStatement_pdf_returnsAPdfOfHerCommissions` | ☐ auto |
| 8c | Export de l'administrateur : toutes les vendeuses ou une | `/commissions/export` avec puis sans `sellerId` | une seule vendeuse, ou les deux vendeuses du test ; aucun identifiant dans le fichier | `CommissionExportIT.exportCommissions_adminForOneSeller_returnsHerRowsOnlyWithoutIdentifiers` ; `CommissionExportIT.exportCommissions_adminWithoutSeller_containsEverySellersCommissions` | ☐ auto |
| 8d | Relevé d'une vendeuse choisie par l'administrateur | `sellerId` de Hanta | ses commissions, nom de fichier `releve-commissions.xlsx` | `CommissionExportIT.exportStatement_adminNamingASeller_returnsThatSellersCommissionsWithTheFileName` | ☐ auto |
| 8e | Confirmation de l'export à l'écran | clic sur « Exporter mon relevé » (Excel) puis « Relevé en PDF » ; l'administrateur sur Commissions calculées : « Exporter en Excel » / « Exporter en PDF » | route `/export` ou `/export/pdf` appelée avec le mois (et la vendeuse pour l'administrateur), fichier téléchargé, notification « Fichier … généré. » | `commissions-page.spec.ts` › should download the xlsx of the month without naming any seller, then confirm it ; › should export the seller and the month chosen by the administrator, as PDF too ; `commission-list-page-actions.spec.ts` › should export the displayed month as xlsx and confirm the file ; › should export the displayed month as PDF | ☐ auto |
| 8f | Échec d'export signalé | 403 (relevé) ou 500 (Commissions calculées) | aucun fichier, notification d'erreur (`export.forbidden`, `export.failed`), bouton de nouveau actif | `commissions-page.spec.ts` › should tell the user when the export is refused and let her try again ; `commission-list-page-actions.spec.ts` › should tell the administrator when the export fails | ☐ auto |
| 9 | Service front du recalcul | `recalculate({from, to})` | `POST /api/v1/commissions/recalculation` avec le corps tel quel | `commission-service.spec.ts` › recalcule la période demandée avec la vendeuse facultative | ☐ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST /commissions/recalculation`, `GET /commissions/export`, `GET /commissions/follow-up/lines/export` | 401 `UNAUTHENTICATED` | `CommissionRecalculationIT.recalculate_withoutToken_returns401` ; `CommissionExportIT.exportStatement_withoutToken_returns401` ; `CommissionExportIT.exportCommissions_sellerOrWithoutToken_returns403Or401` | ☐ auto |
| Mauvais rôle : recalcul | jeton vendeuse ou livreur sur `POST /commissions/recalculation` | 403 `FORBIDDEN`, rien n'est modifié (425 Ar) | `CommissionRecalculationIT.recalculate_sellerOrDeliveryDriver_returns403AndChangesNothing` | ☐ auto |
| Mauvais rôle : export | vendeuse sur `/commissions/export` et `/export/pdf` (réservés à `commissions.read-all`) ; livreur sur le relevé | 403 `FORBIDDEN` | `CommissionExportIT.exportCommissions_sellerOrWithoutToken_returns403Or401` ; `CommissionExportIT.exportStatement_deliveryDriver_returns403` | ☐ auto |
| Le serveur impose la vendeuse (règle VTE-07) | la vendeuse envoie le `sellerId` d'une collègue sur le relevé | elle ne reçoit que ses lignes | `CommissionExportIT.exportStatement_sellerNamingAnotherSeller_stillGetsHerOwn` | ☐ auto |
| Mass assignment | corps du recalcul avec `status = VALIDATED`, `amount = 1` | champs ignorés : 200, montant calculé par le serveur (680 Ar), commission toujours `CALCULATED` | `CommissionRecalculationIT.recalculate_extraFieldsInTheBody_areIgnored` | ☐ auto |
| Période invalide ou champ absent | `from` après `to` ; période de 729 jours ; `to` absent | 422 `VALIDATION_FAILED` | `CommissionRecalculationIT.recalculate_invalidPeriodOrMissingField_returns422` ; `CommissionRecalculationServiceImplTest.recalculate_periodInverted_throwsBusinessException` ; `CommissionRecalculationServiceImplTest.recalculate_periodLongerThanAYear_throwsBusinessException` | ☐ auto |
| Période d'exactement 365 jours d'écart | `from` + 365 jours | acceptée | `CommissionRecalculationServiceImplTest.recalculate_periodOfExactlyAYear_isAccepted` | ☐ auto |
| Export : période inversée | `from` après `to` sur `/commissions/export` | 422 `VALIDATION_FAILED` | `CommissionExportIT.exportCommissions_periodInverted_returns422` | ☐ auto |
| Relevé : administrateur sans vendeuse | aucun `sellerId` | 422 `VALIDATION_FAILED` (comme COM-04) | `CommissionExportIT.exportStatement_adminWithoutSeller_returns422` | ☐ auto |
| Ressource introuvable / conflit | sans objet : le recalcul d'une période vide répond 200 avec des compteurs à 0 (pas de 404) ; il n'y a pas de doublon possible (idempotent, pas de 409) | | | |

## Cas limites et hors ligne

- Période sans commission : 200, tous les compteurs à 0, rien n'est modifié ni journalisé (`CommissionRecalculationIT.recalculate_periodWithoutTheCommissions_changesNothing`, appel sans `sellerId`).
- Relevé d'une vendeuse sans commission du mois : fichier avec la seule ligne d'en-têtes (`CommissionExportIT.exportStatement_sellerWithoutCommission_returnsHeaderRowOnly`).
- Plus de 10 000 lignes à exporter : 422 sans fichier partiel (mécanisme SYS-01 `ExcelExportIT`, non rejoué ici).
- Concurrence : une validation lancée pendant un recalcul est départagée par le verrou optimiste de `BaseEntity` (version) : l'un des deux échoue (409) plutôt que d'écraser une commission validée. Non rejoué en test (hypothèse à vérifier en recette).
- Hors ligne : recalcul et export sont des actions en ligne (pas de `Idempotency-Key`, pas de file hors ligne) ; hors connexion, le bouton échoue avec le message d'erreur habituel et rien n'est modifié.
- Aucun test de recalcul ne lance le recalcul de « toutes les vendeuses » sur la période du jour, pour ne pas modifier les commissions d'autres tests (base partagée) ; ce cas est couvert en unité (`recalculate_sameSellerAndChannel_asksTheResolverOnce`, sans `sellerId`).

## Tests existants

- Backend : `feature/commission/controller/CommissionRecalculationIT.java`, `CommissionExportIT.java`, `feature/commission/service/impl/CommissionRecalculationServiceImplTest.java`, `feature/commission/domain/CommissionTest.java` (4 tests ajoutés). Les tests de COM-02 à COM-04 couvrent toujours `calculate` (qui passe désormais par `ApplicableCommissionRDto.toTerms`).
- Frontend : `commission-list-page-actions.spec.ts` (nouveau), `commissions-page.spec.ts`, `commission-service.spec.ts` ; `demo-pages.a11y.spec.ts` charge toujours « Mes commissions ».

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Admin : changer le taux d'une vendeuse (ou créer une règle de canal), puis Commissions calculées › « Recalculer », confirmer | message chiffré ; montants du mois mis à jour avec le taux le plus précis ; ligne « recalculée » au journal | 🧪 |
| Admin : valider une vendeuse (COM-03), changer son taux, « Recalculer » | ses commissions validées restent identiques et sont comptées « laissée(s) telle(s) quelle(s) » | 🧪 |
| Admin : « Recalculer » deux fois de suite | 2e message : 0 mise à jour | 🧪 |
| Admin : « Exporter en Excel » et « Exporter en PDF » sur un mois | fichiers téléchargés, notification de confirmation, totaux conformes à l'écran | 🧪 |
| Vendeuse : Mes commissions › « Exporter mon relevé » et « Relevé en PDF » | uniquement ses commissions du mois affiché ; notification de confirmation | 🧪 |
| Vendeuse : vérifier l'absence du bouton « Recalculer » et l'appel direct `POST /commissions/recalculation` | bouton absent ; 403 | 🧪 |
| Accessibilité : boutons d'action, dialogue de confirmation au clavier | libellés lus, focus visible, Échap ferme le dialogue | 🧪 |

## Résultat de l'exécution

- Date : à lancer (CI ou local).
- Environnement : tests non exécutés à la rédaction (règle n°3 de CLAUDE.md) ; la CI GitHub (« CI », « Convention VVS », « Qodana for JVM ») les lance sur la branche `feature/COM-05-recalcul-export-commissions`.
- Anomalies ouvertes : aucune connue.
- Étapes 🧪 restantes : 7 (recette manuelle ci-dessus).
