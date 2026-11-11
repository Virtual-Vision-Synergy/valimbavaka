# Scénarios de test — JR-03 · Suivi temps réel de la journée

Statut : ☐ tests à lancer (CI) · Ticket : [#57](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/57) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (permission `reports.read`) ; `SELLER` et `DELIVERY_DRIVER` pour le refus |
| Données de départ | Journée du jour ouverte (JR-01) ; un article fini en stock à la boutique (50 pièces, prix 8 500 Ar) ; une vendeuse (canal Boutique) ; un livreur ; une prestation du catalogue salon à 35 000 Ar |
| Tickets à avoir déployés avant | LIV-02 (tournée, commandes à livrer), SRV-02 (prestations), VTE-01 (vente), JR-01 (journée), VTE-08 (CA par canal) |

« Temps réel » = relecture automatique du serveur toutes les 15 secondes par l'écran `/day/live` (décision d'implémentation, voir le guide) : aucun rechargement manuel n'est nécessaire. Les chiffres sont ceux d'**aujourd'hui** à Antananarivo.

## Scénarios issus des critères d'acceptation

Critère unique : « Les indicateurs et le flux d'opérations se mettent à jour sans rechargement manuel. » Il est découpé en scénarios.

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Indicateurs de la journée | Étant donné la journée ouverte, quand l'administrateur lit `GET /api/v1/days/live`, alors la réponse porte la date du jour, le statut `OPEN`, les trois blocs d'indicateurs et le flux | 200, `businessDate` = aujourd'hui, `status` = `OPEN` | `DayLiveIT.getLive_administrator_returnsTodayAndOpenStatus` | ☐ |
| 2 | Indicateurs : ventes | Quand une vente de 2 articles à 8 500 Ar est enregistrée | `sales.count` +1, `sales.revenue` +17 000, la vente est en tête du flux (type `SALE`, statut `VALIDATED`, vendeuse « Miora Rakoto ») | `DayLiveIT.getLive_newSale_raisesTheSalesIndicatorsAndTopsTheFeed` | ☐ |
| 3 | Indicateurs : ventes annulées | Quand une vente de 8 500 Ar est annulée | elle sort du CA et du nombre de ventes, mais reste dans le flux avec le statut `CANCELLED` | `DayLiveIT.getLive_cancelledSale_staysInTheFeedButLeavesTheIndicators` | ☐ |
| 4 | Indicateurs : tournée | Quand deux commandes à livrer sont créées pour aujourd'hui puis l'une passe `ON_ROUND` | `deliveries.total` +2, `onRound` +1, `toPrepare` +1 ; les commandes figurent dans le flux (type `DELIVERY`) | `DayLiveIT.getLive_ordersToday_countsOrdersPerStatus` | ☐ |
| 5 | Indicateurs : tournée d'un autre jour | Une commande est prévue demain | elle n'est pas comptée aujourd'hui | `DayLiveIT.getLive_orderOfAnotherDay_isNotCounted` | ☐ |
| 6 | Indicateurs : prestations | Quand une prestation à 35 000 Ar est enregistrée | `services.count` +1, `services.revenue` +35 000, la prestation figure dans le flux (type `SERVICE`, sans statut) | `DayLiveIT.getLive_performedService_raisesTheServicesIndicatorsAndShowsInTheFeed` | ☐ |
| 7 | Flux d'opérations | Plusieurs opérations de la journée | au plus 30 éléments, du plus récent au plus ancien | `DayLiveIT.getLive_manyOperations_returnsAtMostThirtyFeedItemsMostRecentFirst` | ☐ |
| 7 bis | Flux (fusion) | Ventes à 09:00 et 07:00, livraison à 08:00, prestation à 09:30 | ordre `PRE-0001`, `VTE-0002`, `LIV-0001`, `VTE-0001` ; statuts conservés | `DayLiveServiceImplTest.getLive_operationsOfTheThreeActivities_areMergedMostRecentFirst` | ☐ |
| 7 ter | Flux (borne) | 30 ventes et 1 prestation plus ancienne | 30 éléments, la prestation la plus ancienne est écartée | `DayLiveServiceImplTest.getLive_moreOperationsThanTheFeedLimit_keepsTheLatestOnes` | ☐ |
| 8 | Indicateurs (calcul) | Ventes 15 500 Ar sur 3 ventes, 5 commandes (2 en tournée, 3 livrées), 2 prestations de 35 000 et 20 000 | CA 15 500, 3 ventes, total 5 commandes, 2 prestations, 55 000 Ar | `DayLiveServiceImplTest.getLive_activeDay_returnsIndicatorsOfTheThreeActivities` | ☐ |
| 8 bis | Journée non ouverte | Aucune journée aujourd'hui | `status` = `null`, tous les indicateurs à 0, flux vide | `DayLiveServiceImplTest.getLive_dayNotOpenedYet_hasNoStatusAndEmptyIndicators` | ☐ |
| 8 ter | Fuseau | 21 h 30 UTC (déjà le lendemain à Antananarivo) | la vue porte la date du lendemain et lit la bonne plage horaire | `DayLiveServiceImplTest.getLive_lateEveningUtc_isAlreadyTomorrowInAntananarivo` | ☐ |
| 9 | Port journée | `BusinessDayService.findToday()` | `null` sans journée ; la journée quel que soit son statut (ouverte ou close) | `BusinessDayServiceImplTest.findToday_noDayToday_returnsNull`, `findToday_dayAlreadyClosed_returnsTheDayWhateverItsStatus` | ☐ |
| 10 | Port livraison | Comptage des commandes du jour par statut | tous les statuts présents, ceux sans commande à 0 | `DeliveryDayServiceImplTest.countByStatus_someStatusesPresent_fillsTheOthersWithZero`, `countByStatus_noOrderToday_returnsZeroForEveryStatus` | ☐ |
| 10 bis | Port livraison | Dernières commandes du jour | page de 30 triée par dernière modification décroissante | `DeliveryDayServiceImplTest.getRecentOrders_limit_asksTheLatestUpdatedOrdersOfTheDay` | ☐ |
| 11 | Écran : affichage | Quand l'administrateur ouvre `/day/live` | statut de la journée, CA, ventes, tournée « 2 / 4 », prestations, tableau par canal et flux (VTE-0002 avant PRE-0001) | `day-live-page.spec.ts` « should show the indicators and the feed of the day » | ☐ |
| 11 bis | Écran : sans journée | La journée n'est pas ouverte | badge « Journée non ouverte » et flux vide expliqué | `day-live-page.spec.ts` « should show that the day is not opened yet » | ☐ |
| 12 | Écran : mise à jour automatique | 15 secondes après la première lecture, une vente de plus a eu lieu | l'écran affiche 26 000 Ar et 3 ventes sans action de l'utilisateur | `day-live-page.spec.ts` « should read the server again by itself, without manual reload » | ☐ |
| 12 bis | Écran : bouton Actualiser | L'administrateur clique « Actualiser » | relecture immédiate | `day-live-page.spec.ts` « should read the server at once when the administrator presses refresh » | ☐ |
| 12 ter | Écran : lecture | Appel `GET /api/v1/days/live` | méthode GET, aucune écriture | `day-live-service.spec.ts` « should read the live view of the day from the API » | ☐ |
| 13 | Menu | Entrée « Suivi du jour » | visible pour l'administrateur (`reports.read`), absente pour la vendeuse et le livreur | `menu.spec.ts` (assertions ajoutées) | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `DayLiveIT.getLive_withoutToken_returns401` | ☐ |
| Appel avec un mauvais rôle (vendeuse, livreur) | 403 `FORBIDDEN` | `DayLiveIT.getLive_sellerAndDeliveryDriver_return403` | ☐ |
| Champ interdit dans le corps (`role`, `id`) | sans objet : lecture seule, aucun corps, aucun paramètre | — | — |
| Données invalides, ressource introuvable, conflit | sans objet : aucune entrée, aucun identifiant | — | — |
| Relecture qui échoue (réseau, 5xx) | l'écran garde la dernière vue et affiche une alerte « les chiffres peuvent être en retard » ; la relecture suivante la retire | `day-live-page.spec.ts` « should keep the last view and warn when a refresh fails, then recover » | ☐ |
| Première lecture qui échoue | état d'erreur avec « Réessayer » | `day-live-page.spec.ts` « should offer a retry when the very first read fails » | ☐ |

## Cas limites et hors ligne

- **Journée non ouverte** : la vue reste lisible (`status` `null`, indicateurs à 0) ; elle ne lève pas le 409 de `requireOpenDay`.
- **Journée close** : `status` = `CLOSED`, les indicateurs de la journée restent affichés.
- **Jour = jour ouvré d'Antananarivo**, jamais la date UTC : vérifié à 21 h 30 UTC (8 ter). Les ventes sont comptées sur `business_date`, les commandes sur `scheduled_date`, les prestations sur la plage `[00:00, 24:00[` locale.
- **Seules les ventes validées comptent** dans le CA et le nombre de ventes ; annulées et contre-passées restent visibles dans le flux avec leur statut (3). Les commandes annulées sont exclues du dénominateur « livrées / à livrer ».
- **Flux borné** à 30 opérations (7, 7 bis, 7 ter) ; au-delà, l'historique complet reste sur les écrans de chaque module (ventes, livraisons, prestations).
- **Pas de chevauchement** : une relecture n'est lancée que si la précédente est terminée (`day-live-page.spec.ts` « should not stack requests while one is still running »). La relecture est suspendue quand l'onglet est masqué.
- **Hors ligne** : lecture seule, rien n'est mis en file (`Idempotency-Key` sans objet) ; la dernière vue reste affichée avec l'alerte (voir ci-dessus).
- **Base partagée entre classes de test** : `DayLiveIT` compare des différences avant / après ses propres opérations, ouvre la journée si besoin (201 ou 409 accepté, comme `ChannelDashboardIT`) et appelle `endAutomaticPromotions()` avant les ventes.

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : à lancer (CI ou local)
- Anomalies ouvertes : aucune connue ; la recette manuelle 🧪 (écran qui se met à jour pendant qu'une vente est saisie sur un autre poste) reste à passer par Njara.
