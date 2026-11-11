# Scénarios de test — JR-05 · Motif obligatoire en cas d'écart

Statut : ☐ tests écrits, à lancer (CI ou local), 🧪 recette à passer · Ticket : [#92](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/92) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (permission `days.close`, créée par JR-04) ; `SELLER` et `DELIVERY_DRIVER` pour le refus |
| Données de départ | Journée du jour ouverte (JR-01) ; un article fini reçu à l'entrepôt (200 pièces) puis 10 pièces transférées à la boutique : théorique 10, stock du système 10 |
| Tickets à avoir déployés avant | JR-04 (réconciliation, permission `days.close`), JR-01 (journée), JNL-01 (journal) |

Le ticket enregistre le **comptage du soir** (`PUT /api/v1/days/closing/counts`) : pour chaque ligne (emplacement, article), la quantité comptée ; si elle diffère du théorique calculé par le serveur, un **motif** (`BREAKAGE`, `LOSS`, `ENTRY_ERROR`, `SAMPLE`, `OTHER`) est obligatoire, sinon 422. L'écart est toujours calculé côté serveur ; le théorique est figé à l'enregistrement. Le verrouillage de la journée (JR-06) n'est pas livré ici.

## Scénarios issus des critères d'acceptation

Critère unique : « La clôture n'est validée qu'après sélection d'un motif si un écart existe. » La « validation » livrée ici est l'enregistrement du comptage (le verrouillage relève de JR-06, qui s'appuiera sur ces lignes). Il est découpé en scénarios.

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Écart sans motif refusé | Théorique 10, compté 8, aucun motif | 422 `VALIDATION_FAILED`, détail « Un motif est obligatoire pour chaque écart : ART-0031. » ; rien n'est enregistré | `DayClosingCountIT.saveCounts_gapWithoutReason_returns422AndSavesNothing`, `DayClosingCountServiceImplTest.saveCounts_gapWithoutReason_throwsBusinessExceptionNamingTheArticleAndSavesNothing` | ☐ |
| 1 bis | Écart avec motif accepté | Théorique 10, compté 8, motif `BREAKAGE` | 200 ; la ligne renvoie `counted` 8, `reason` `BREAKAGE`, `theoretical` 10 ; une ligne en base avec le théorique figé à 10 | `DayClosingCountIT.saveCounts_gapWithReason_returns200WithTheCountAndTheReasonOnTheLine`, `DayClosingCountServiceImplTest.saveCounts_gapWithReason_savesTheCountWithTheTheoreticalFrozenAndWritesTheJournal` | ☐ |
| 1 ter | Pas d'écart, pas de motif | Théorique 10, compté 10 (ou `10.000`), sans motif | 200 ; `reason` absent | `DayClosingCountIT.saveCounts_countEqualToTheTheoretical_needsNoReason`, `DayClosingCountServiceImplTest.saveCounts_countEqualToTheTheoretical_needsNoReasonAndIgnoresOneSent` | ☐ |
| 1 quater | Motif envoyé sans écart | Théorique 10, compté 10, motif `LOSS` | 200 ; le motif est ignoré (null en base) | `DayClosingCountIT.saveCounts_reasonSentWithoutGap_isIgnored`, `DayClosingCountTest.create_countWithoutGap_dropsTheReasonSent` | ☐ |
| 1 quinquies | Compté à zéro | Théorique 3, compté 0 | c'est un écart : 422 sans motif, 200 avec | `DayClosingCountIT.saveCounts_zeroCounted_isAGapThatNeedsAReason`, `DayClosingCountServiceImplTest.saveCounts_zeroCountedWhereStockWasExpected_isAGapThatNeedsAReason` | ☐ |
| 2 | Tout ou rien | Deux lignes : une exacte, une en écart sans motif | 422 ; aucune des deux n'est enregistrée | `DayClosingCountIT.saveCounts_validLineAndGapWithoutReason_savesNeitherOfThem`, `DayClosingCountServiceImplTest.saveCounts_oneGapWithoutReasonAmongValidLines_refusesTheWholeSave` | ☐ |
| 2 bis | Message lisible | 12 écarts sans motif | le détail liste les 10 premiers articles puis « et 2 autre(s) » | `DayClosingCountServiceImplTest.saveCounts_moreThanTenGapsWithoutReason_listsTenAndCountsTheOthers` | ☐ |
| 3 | Le client ne décide pas de l'écart | Corps avec `"theoretical":8,"gap":0,"id":"…"` pour un compté de 8 (théorique réel 10) | 400 `MALFORMED_REQUEST` (champs inconnus refusés par la désérialisation) ; rien n'est enregistré : le client ne peut ni fixer le théorique ni l'écart | `DayClosingCountIT.saveCounts_theoreticalGapAndIdSentByTheClient_areRefusedAndNothingIsSaved` | ☐ |
| 4 | Nouveau comptage | Compté 8 avec motif, puis recompté 10 | une seule ligne en base ; `counted` 10, motif effacé | `DayClosingCountIT.saveCounts_recount_replacesTheCountAndClearsTheReasonWhenTheGapIsGone`, `DayClosingCountServiceImplTest.saveCounts_recountOfASavedLine_replacesTheCountAndClearsTheReasonWithoutGap`, `DayClosingCountTest.recount_gapClosed_clearsTheReason`, `recount_differentCount_replacesTheValuesAndReturnsTrue` | ☐ |
| 4 bis | Rejeu du même comptage | Même requête envoyée deux fois (rejeu hors ligne) | 200 deux fois ; une seule ligne ; aucune nouvelle ligne de journal | `DayClosingCountIT.saveCounts_sameRequestSentTwice_leavesASingleCount`, `DayClosingCountServiceImplTest.saveCounts_sameCountSentAgain_savesNothingAndWritesNoJournalLine`, `DayClosingCountTest.recount_sameValuesWithAnotherScale_changesNothingAndReturnsFalse` | ☐ |
| 5 | Vente après le comptage | Compté 10 (sans écart), puis vente de 2 | la ligne garde `counted` 10 et affiche un théorique de 8 : l'écran signale un nouvel écart à motiver | `DayClosingCountIT.saveCounts_aSaleAfterTheCount_showsANewGapOnTheLineWhileTheSavedCountStays`, `DayClosingCountTest.recount_newTheoreticalWithTheSameCount_isAChange` | ☐ |
| 6 | La lecture rend le comptage | `GET /api/v1/days/closing` après l'enregistrement | `counted`, `reason` sur la ligne ; `businessDayId` sur la réponse ; sans comptage, `counted` et `reason` sont null | `DayClosingCountIT.getClosing_afterTheCount_returnsTheSavedCountsAndTheDayId`, `DayClosingServiceImplTest.getClosing_savedCount_isReturnedWithItsReasonOnTheLine`, `getClosing_noSavedCount_leavesCountedAndReasonEmpty` | ☐ |
| 7 | Entité | Compté 8 / théorique 10 avec motif ; compté 10 / théorique 10 avec motif | motif gardé avec écart, effacé sans écart | `DayClosingCountTest.create_countWithAGap_keepsTheReason`, `create_countWithoutGap_dropsTheReasonSent` | ☐ |
| 8 | Écran : enregistrement bloqué | Ligne « Savon noir » en écart (12 compté pour 14), sans motif | bouton « Enregistrer le comptage » désactivé, texte « 1 écart sans motif : choisissez un motif pour enregistrer. » ; aucun envoi | `day-close-page.spec.ts` « should keep the count disabled while a gap has no reason » | ☐ |
| 8 bis | Écran : enregistrement | Motif « Casse » choisi pour l'écart, clic sur le bouton | `PUT /api/v1/days/closing/counts` avec toutes les lignes (`locationId`, `articleId`, `counted`, `reason`) | `day-close-page.spec.ts` « should save every count with the reason of each gap once all gaps are explained », `day-service.spec.ts` « enregistre le comptage par la file hors ligne (PUT /api/v1/days/closing/counts) » | ☐ |
| 8 ter | Écran : pas de motif sans écart | Motif choisi puis comptage ramené au théorique (14) | le motif n'est pas envoyé (`null`) | `day-close-page.spec.ts` « should not send the reason of a line whose count matches the theoretical » | ☐ |
| 8 quater | Écran : refus du serveur | Le serveur répond 422 « Un motif est obligatoire… » | message affiché dans une alerte `role="alert"` sous le bouton | `day-close-page.spec.ts` « should show the refusal of the server when a gap has no reason (422) » | ☐ |
| 8 quinquies | Écran : comptage relu | Réponse du serveur avec `counted` 12 et motif `BREAKAGE` | la liste « Motif » affiche « Casse » après l'enregistrement | `day-close-page.spec.ts` « should show the saved count and reason returned by the server » | ☐ |
| 9 | Data-access | Réponse avec comptage et motif enregistrés ; réponse sans | comptage et motif repris du serveur ; sinon stock du système et aucun motif ; `locationId` conservé | `day-service.spec.ts` « reprend le comptage et le motif enregistrés par le serveur (JR-05) », « lit la réconciliation sur GET /api/v1/days/closing et la range par colonne (JR-04) », « range la réconciliation renvoyée après l’enregistrement du comptage » | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `DayClosingCountIT.saveCounts_withoutToken_returns401` | ☐ |
| Appel avec un mauvais rôle (vendeuse, livreur) | 403 `FORBIDDEN` ; rien n'est enregistré | `DayClosingCountIT.saveCounts_sellerAndDeliveryDriver_return403` | ☐ |
| Champ interdit dans le corps (`id`, `theoretical`, `gap`) | jamais pris en compte (mass assignment) : le DTO d'entrée n'a ni `id` ni `role`, et la désérialisation du projet refuse les champs inconnus (400 `MALFORMED_REQUEST`) | `DayClosingCountIT.saveCounts_theoreticalGapAndIdSentByTheClient_areRefusedAndNothingIsSaved` | ☐ |
| Données invalides | 422 `VALIDATION_FAILED` : liste vide, corps `{}`, compté `-1`, compté `1.2345` (4 décimales), compté absent | `DayClosingCountIT.saveCounts_invalidBody_returns422` | ☐ |
| Écart sans motif | 422 `VALIDATION_FAILED` | `DayClosingCountIT.saveCounts_gapWithoutReason_returns422AndSavesNothing` | ☐ |
| Même ligne envoyée deux fois dans la requête | 422 « Un article ne peut être compté qu'une fois par emplacement : ART-0031. » | `DayClosingCountIT.saveCounts_sameLineTwice_returns422`, `DayClosingCountServiceImplTest.saveCounts_sameLineTwice_throwsBusinessException` | ☐ |
| Motif inconnu (`FORGOTTEN`) | 400 `MALFORMED_REQUEST` | `DayClosingCountIT.saveCounts_unknownReason_returns400` | ☐ |
| Article ou emplacement hors de la clôture du jour | 404 `NOT_FOUND` : article inconnu, ou article d'un autre emplacement que celui indiqué | `DayClosingCountIT.saveCounts_unknownArticle_returns404`, `saveCounts_articleOfAnotherLocation_returns404`, `DayClosingCountServiceImplTest.saveCounts_lineNotInTodaysClosing_throwsNotFound` | ☐ |
| Journée du jour non ouverte | 409 `CONFLICT`, « La journée du … n'est pas ouverte. » | `DayClosingCountIT.saveCounts_dayNotOpened_returns409`, `DayClosingCountServiceImplTest.saveCounts_dayNotOpened_propagatesTheConflictOfTheReconciliation` | ☐ |
| Journée déjà clôturée | 409 `CONFLICT`, « La journée du 11/11/2026 est déjà clôturée. » (aucun code ne clôture encore : JR-06) | `DayClosingCountServiceImplTest.saveCounts_closedDay_throwsConflict` | ☐ |
| Deux enregistrements simultanés | 409 `CONFLICT` (contrainte d'unicité jour / emplacement / article, ou verrou optimiste `@Version`) via `GlobalExceptionHandler` | 🔍 code : `uq_day_closing_counts_line` (migration 21), `BaseEntity.version` ; pas de test de concurrence | — |
| Journal | une ligne `UPDATE` sur la ressource `BUSINESS_DAY` (id de la journée) : « Comptage de clôture du 11/11/2026 : 1 ligne(s) enregistrée(s), dont 1 écart(s) motivé(s) » ; rien si rien n'a changé | `DayClosingCountIT.saveCounts_gapWithReason_writesAJournalLineOnTheDay`, `DayClosingCountServiceImplTest.saveCounts_gapWithReason_savesTheCountWithTheTheoreticalFrozenAndWritesTheJournal`, `saveCounts_sameCountSentAgain_savesNothingAndWritesNoJournalLine` | ☐ |
| Accessibilité de l'écran | aucune violation axe (WCAG 2 A / AA), nouveau bloc « Comptage du soir » compris | `day-close-page.spec.ts` « should have no detectable accessibility violation » | ☐ |

## Cas limites et hors ligne

- **Bornes** : compté `0` accepté (c'est un écart si du stock était attendu) ; compté négatif refusé ; au plus 3 décimales et 11 chiffres entiers (`NUMERIC(14,3)`) ; au plus 1000 lignes par requête ; liste vide refusée.
- **Comparaison numérique** : `10`, `10.0` et `10.000` sont le même comptage (comparaison sans tenir compte de l'échelle) : pas d'écart, pas de nouvelle ligne de journal.
- **Écart décidé par le serveur** : le théorique est recalculé à chaque enregistrement à partir des mouvements de stock (JR-04) et figé dans la ligne (`theoretical_quantity`) ; une vente faite ensuite change le théorique affiché mais pas le comptage enregistré (scénario 5) : l'écran refuse alors d'enregistrer tant que le nouvel écart n'a pas de motif, et JR-06 devra relire l'écart au verrouillage.
- **Base** : la contrainte `ck_day_closing_counts_reason_gap` garantit qu'une ligne en écart a un motif et qu'une ligne exacte n'en a pas, même si le service était contourné.
- **Hors ligne** : l'écriture passe par la file (`OutboxService.submit('PUT', …)`) ; hors connexion, le comptage est mis en file (toast « Saisie enregistrée hors connexion ») et rejoué avec la même `Idempotency-Key` (SYS-02) ; un rejeu identique ne change rien (4 bis). Un refus 422 rejoué reste visible dans la file en échec.
- **Lignes non envoyées** : un enregistrement ne touche que les lignes envoyées ; l'écran envoie toutes les lignes affichées (comptage proposé = stock du système pour celles que l'administrateur n'a pas touchées). Compter toutes les lignes avant de verrouiller sera exigé par JR-06.
- **Concurrence** : deux administrateurs enregistrant en même temps la même ligne : la contrainte d'unicité ou le verrou optimiste renvoie 409, l'écran affiche le message et l'administrateur recharge.
- **Base partagée entre classes de test** : `DayClosingCountIT` travaille sur ses propres articles, ouvre la journée si besoin (201 ou 409 accepté) et **supprime les lignes de `day_closing_counts` après chaque test** : plusieurs autres classes font `delete from business_days`, que la clé étrangère de la table bloquerait sinon. Le test « journée non ouverte » décale toutes les journées de 400 jours puis les remet dans un `finally`. Aucune vente au comptoir n'est enregistrée (les ventes sont simulées par un mouvement de stock `SALE`), donc pas de promotion automatique ni de vendeuse à créer.

## Tests existants

- Backend : `feature/dayclosing/controller/DayClosingCountIT.java` (nouveau), `feature/dayclosing/service/impl/DayClosingCountServiceImplTest.java` (nouveau), `feature/dayclosing/domain/DayClosingCountTest.java` (nouveau), `feature/dayclosing/service/impl/DayClosingServiceImplTest.java` (2 tests ajoutés, constructeur adapté), `feature/dayclosing/controller/DayClosingIT.java` (inchangé).
- Frontend : `features/day/pages/day-close-page/day-close-page.spec.ts` (5 tests ajoutés), `features/day/data-access/day-service.spec.ts` (3 tests ajoutés, fixtures complétées).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Ouvrir `/day/close` avec un article en écart (ajustement d'inventaire ou comptage plus bas que le théorique) | la ligne est marquée « motif manquant », le bouton « Enregistrer le comptage » est grisé et dit combien de motifs manquent | 🧪 recette |
| R2 | Choisir un motif pour chaque écart puis « Enregistrer le comptage » | toast « Comptage enregistré », bouton de nouveau actif | 🧪 recette |
| R3 | Recharger la page | les comptages et motifs enregistrés sont repris tels quels | 🧪 recette |
| R4 | Couper le réseau puis enregistrer | message « Saisie enregistrée hors connexion » ; au retour du réseau, le comptage part et apparaît au rechargement | 🧪 recette |
| R5 | Consulter le journal | une ligne « Comptage de clôture du … » signée par l'administrateur | 🧪 recette |

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : tests écrits, non exécutés : `mvn` indisponible dans l'environnement de rédaction ; la CI GitHub Actions les lance (`ci.yml`, `conventions.yml`, Qodana).
- Anomalies ouvertes : aucune connue ; 5 étapes 🧪 restantes (recette manuelle par Njara).
