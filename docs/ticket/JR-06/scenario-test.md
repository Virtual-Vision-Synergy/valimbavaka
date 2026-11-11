# Scénarios de test — JR-06 · Verrouillage de la clôture du jour

Statut : ☐ tests écrits, à lancer (CI ou local), 🧪 recette à passer · Ticket : [#93](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/93) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (permission `days.close`, JR-04) ; `SELLER` et `DELIVERY_DRIVER` pour le refus |
| Données de départ | Journée du jour ouverte (JR-01) ; un article fini reçu à l'entrepôt (200 pièces) puis 10 pièces transférées à la boutique : théorique 10 ; le comptage du soir (JR-05) enregistré pour toutes les lignes |
| Tickets à avoir déployés avant | JNL-01 (journal), JR-01 / JR-02 (journée, reprise de la veille), JR-04 (réconciliation, permission), JR-05 (comptage et motifs) |

Le ticket ajoute `POST /api/v1/days/closing/lock` (sans corps, `days.close`). Au verrouillage le serveur **relit** la réconciliation d'aujourd'hui, vérifie que **chaque ligne est comptée** (sinon 422) et que **chaque écart a son motif** sur le théorique d'aujourd'hui (sinon 422), remplace les lignes de `day_closing_counts` dont le théorique a bougé depuis le comptage (le théorique est ainsi figé au verrouillage), passe la journée à `CLOSED`, écrit **une ligne de journal** (écarts compris) et renvoie la réconciliation. Une fois la journée `CLOSED`, vente, annulation de vente (donc avoir FAC-04 et annulation de livraison LIV-05), livraison réussie et comptage sont refusés en 409. Aucune migration : le statut `CLOSED` existe depuis JR-01.

## Scénarios issus des critères d'acceptation

Critère unique : « Un message confirme l'enregistrement et l'écart est consigné au journal. » Il est découpé en scénarios numérotés 1, 1 bis, etc.

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Verrouillage confirmé | Toutes les lignes comptées, aucun écart ; POST `/api/v1/days/closing/lock` | 200, `status` `CLOSED`, `businessDayId` renseigné ; la journée est `CLOSED` en base ; `GET /api/v1/days/closing` reste lisible (statut `CLOSED`) | `DayClosingLockIT.lock_everyLineCounted_returns200WithTheClosedDayAndKeepsTheClosingReadable`, `DayClosingLockServiceImplTest.lock_everyLineCountedAndJustified_closesTheDayAndWritesOneJournalLine` | ☐ |
| 1 bis | Écran : message de confirmation | Comptage enregistré ; clic sur « Verrouiller la journée » puis confirmation | `POST /api/v1/days/closing/lock` ; bandeau « Journée verrouillée à 18:42 · consultation seule. » et toast « Journée verrouillée à 18:42 : la clôture est enregistrée au journal. » ; bouton et saisies désactivés | `day-close-lock.spec.ts` « should enable the lock once every count is saved and lock the day with the server » | ☐ |
| 2 | L'écart est consigné au journal | Théorique 10, compté 8, motif `LOSS`, puis verrouillage | une ligne `UPDATE` sur `BUSINESS_DAY` : « Clôture de la journée du 11/11/2026 : 2 ligne(s) comptée(s), 1 écart(s) (ART-0031 -2 LOSS) » | `DayClosingLockIT.lock_gapWithReason_namesTheGapInTheJournal`, `DayClosingLockServiceImplTest.lock_everyLineCountedAndJustified_closesTheDayAndWritesOneJournalLine` | ☐ |
| 2 bis | Une seule ligne de journal | Verrouillage sans écart | exactement une ligne « Clôture de la journée du … » de plus | `DayClosingLockIT.lock_everyLineCounted_writesOneJournalLineOnTheDay` | ☐ |
| 3 | Ligne non comptée refuse le verrouillage | Une ligne de la clôture sans comptage enregistré | 422 `VALIDATION_FAILED`, « Toutes les lignes doivent être comptées avant le verrouillage : ART-0032. » ; la journée reste `OPEN`, aucune ligne de journal | `DayClosingLockIT.lock_lineNotCounted_returns422AndTheDayStaysOpen`, `DayClosingLockServiceImplTest.lock_lineNotCounted_throwsBusinessExceptionAndClosesNothing` | ☐ |
| 3 bis | Message lisible | 12 lignes non comptées | 10 références puis « et 2 autre(s) » | `DayClosingLockServiceImplTest.lock_moreThanTenNotCounted_listsTenAndCountsTheOthers` | ☐ |
| 4 | Vente après le comptage : l'écart est relu | Compté 10 = théorique 10 (sans motif), puis vente de 2 : théorique 8 | 422 « Un motif est obligatoire pour chaque écart : ART-0031. » ; après un nouveau comptage (compté 8) le verrouillage passe | `DayClosingLockIT.lock_saleAfterTheCountLeavesAGapWithoutReason_returns422ThenPassesOnceRecounted`, `DayClosingLockServiceImplTest.lock_saleAfterTheCount_theNewGapWithoutReasonRefusesTheLock` | ☐ |
| 4 bis | Vente après le comptage, motif déjà donné | Compté 8 motif `BREAKAGE` pour un théorique de 10 ; une vente de 1 : théorique 9 | 200 ; la ligne est remplacée : théorique figé 9, motif gardé, `counted_at` = heure du verrouillage | `DayClosingLockServiceImplTest.lock_saleAfterTheCountWithTheReasonAlreadyGiven_freezesTheNewTheoretical` | ☐ |
| 4 ter | Vente qui supprime l'écart | Compté 8 motif `BREAKAGE` pour 10 ; deux ventes : théorique 8 | 200 ; théorique figé 8, motif effacé (contrainte `ck_day_closing_counts_reason_gap`) | `DayClosingLockIT.lock_saleAfterTheCountRemovesTheGap_freezesTheNewTheoreticalAndClearsTheReason`, `DayClosingLockServiceImplTest.lock_saleAfterTheCountRemovesTheGap_clearsTheReason` | ☐ |
| 4 quater | Comptages déjà à jour | Rien n'a bougé depuis le comptage | 200 ; aucune ligne de comptage réécrite | `DayClosingLockServiceImplTest.lock_countsAlreadyUpToDate_savesNothingNew` | ☐ |
| 4 quinquies | Journée sans aucune ligne | Réconciliation vide | 200, la journée est clôturée | `DayClosingLockServiceImplTest.lock_emptyReconciliation_closesTheDay` | ☐ |
| 5 | Plus aucune écriture sur la journée close : vente | Journée `CLOSED` ; vente d'un article en boutique | 409 `CONFLICT`, « La journée du … est déjà clôturée. » | `DayClosingLockIT.afterTheLock_aSaleIsRefusedWith409`, `BusinessDayServiceImplTest.requireOpenDay_closedDay_throwsConflictSayingItIsClosed` | ☐ |
| 5 bis | Plus aucune écriture : comptage | Journée `CLOSED` ; `PUT /api/v1/days/closing/counts` | 409 « … est déjà clôturée » ; les lignes figées ne changent pas | `DayClosingLockIT.afterTheLock_aCountIsRefusedWith409AndTheFrozenCountsStay` | ☐ |
| 5 ter | Annulation, avoir, annulation de livraison | Journée `CLOSED` ; annulation d'une vente du jour | 409 : ces trois parcours passent par `SaleService.cancel`, qui exige la journée ouverte (existants : VTE, FAC-04, LIV-05) | ⏭ déjà couvert : `SaleServiceImplTest` (annulation, journée close) et les tests FAC-04 / LIV-05 ; pas de nouveau test | ☐ |
| 6 | Le lendemain (JR-02) | Journée verrouillée ; le lendemain matin | la grille d'ouverture reprend la veille clôturée comme `previousDay` (ses quantités du matin) et l'ouverture de la nouvelle journée donne 201 | `DayClosingLockIT.lock_thenTheNextDay_opensWithTheClosedDayAsTheDayBefore` | ☐ |
| 7 | Entité et service de journée | Journée ouverte puis `closeDay()` ; journée close | statut `CLOSED` et sauvegarde ; 409 (« est déjà clôturée » ou « n'est pas ouverte ») sinon ; lecture sous verrou en écriture | `BusinessDayServiceImplTest.closeDay_openedToday_setsTheStatusToClosedAndSaves`, `closeDay_alreadyClosed_throwsConflictAndSavesNothing`, `closeDay_noDayToday_throwsConflict`, `lockOpenDay_openedToday_readsTheDayLockedInWriting`, `lockOpenDay_closedDay_throwsConflict` | ☐ |
| 8 | Écran : verrouillage bloqué | Comptage non enregistré (lignes sans comptage serveur) | bouton « Verrouiller la journée » désactivé, texte « … le comptage est enregistré. » | `day-close-lock.spec.ts` « should keep the lock disabled until the counts shown are the ones saved by the server » | ☐ |
| 8 bis | Écran : refus du serveur | Le serveur répond 422 « Un motif est obligatoire… » | message dans une alerte `role="alert"` sous le bouton ; journée toujours ouverte | `day-close-lock.spec.ts` « should show the refusal of the server when the lock is refused (422) » | ☐ |
| 8 ter | Écran : hors ligne | Réseau coupé : le verrouillage est mis en file | toast « Saisie enregistrée hors connexion » ; la page reste ouverte | `day-close-lock.spec.ts` « should leave the day open when the lock is only queued for the network » | ☐ |
| 8 quater | Écran : journée déjà clôturée | Chargement d'une réconciliation `CLOSED` | bandeau « Journée verrouillée · consultation seule. », pas de bouton, saisies désactivées | `day-close-lock.spec.ts` « should show a day already closed as read only, without the lock button » | ☐ |
| 9 | Data-access | `lock()` | `POST /api/v1/days/closing/lock` sans corps par la file hors ligne ; réponse rangée par colonne avec `status` ; `saved` vrai pour une ligne déjà comptée | `day-service.spec.ts` « verrouille la clôture par la file hors ligne, sans corps (POST /api/v1/days/closing/lock) », « range la réconciliation de la journée clôturée renvoyée par le verrouillage », « marque comme enregistrée une ligne que le serveur a déjà comptée (JR-06) » | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `DayClosingLockIT.lock_withoutToken_returns401` | ☐ |
| Appel avec un mauvais rôle (vendeuse, livreur) | 403 `FORBIDDEN` ; la journée reste `OPEN` | `DayClosingLockIT.lock_sellerAndDeliveryDriver_return403AndTheDayStaysOpen` | ☐ |
| Champ interdit dans le corps (`role`, `id`, `status`) | ignoré : l'endpoint ne lit aucun corps, le statut ne vient jamais du client | `DayClosingLockIT.lock_bodyWithRoleAndId_isIgnored` | ☐ |
| Données invalides | 422 `VALIDATION_FAILED` : ligne non comptée, écart sans motif (aucune donnée saisie par le client) | `DayClosingLockIT.lock_lineNotCounted_returns422AndTheDayStaysOpen`, `lock_saleAfterTheCountLeavesAGapWithoutReason_returns422ThenPassesOnceRecounted` | ☐ |
| Ressource introuvable | sans objet : l'endpoint agit sur la journée du jour et ne reçoit aucun identifiant ; une journée absente est un 409 | ⏭ sans objet | ☐ |
| Journée non ouverte | 409 `CONFLICT`, « La journée du … n'est pas ouverte. » | `DayClosingLockIT.lock_dayNotOpened_returns409`, `DayClosingLockServiceImplTest.lock_dayNotOpen_propagatesTheConflictAndReadsNothing`, `BusinessDayServiceImplTest.closeDay_noDayToday_throwsConflict` | ☐ |
| Journée déjà clôturée | 409 `CONFLICT`, « La journée du … est déjà clôturée. » | `DayClosingLockIT.lock_twiceInARow_secondReturns409`, `BusinessDayServiceImplTest.closeDay_alreadyClosed_throwsConflictAndSavesNothing` | ☐ |
| Deux verrouillages simultanés | l'un répond 200, l'autre 409 ; une seule ligne de journal ; la journée est `CLOSED` | `DayClosingLockIT.lock_twoSimultaneousRequests_oneClosesTheDayTheOtherGets409` | ☐ |
| Journal | une ligne `UPDATE` sur `BUSINESS_DAY` (id de la journée) par verrouillage réussi, aucune pour un refus | `DayClosingLockIT.lock_everyLineCounted_writesOneJournalLineOnTheDay`, `lock_lineNotCounted_returns422AndTheDayStaysOpen` | ☐ |
| Accessibilité de l'écran | inchangée : le spec axe de la page couvre l'écran chargé ; le bandeau (`role="status"`) et l'alerte (`role="alert"`) ajoutés ne portent que du texte | `day-close-page.spec.ts` « should have no detectable accessibility violation » | ☐ |

## Cas limites et hors ligne

- **Écart relu au verrouillage** : le théorique change quand une vente ou un mouvement est posté après le comptage ; le verrouillage compare le comptage enregistré au théorique d'**aujourd'hui** (scénarios 4, 4 bis, 4 ter). Un écart devenu nul efface le motif ; un écart apparu sans motif refuse (422) : l'administrateur recompte et motive.
- **Ligne dont toutes les composantes sont à zéro** : omise de la réconciliation (JR-04), donc non exigée au verrouillage.
- **Concurrence** : le verrouillage et l'enregistrement du comptage (JR-05) prennent d'abord le verrou en écriture de la ligne de la journée (`BusinessDayRepository.findByBusinessDateForUpdate`) : deux verrouillages passent l'un après l'autre (le second voit `CLOSED`, 409) et un comptage simultané attend la fin du verrouillage puis reçoit 409. Une vente simultanée n'est pas verrouillée : si elle est validée entre la relecture et le passage à `CLOSED`, son mouvement n'est pas dans le comptage figé (écart de théorique d'une unité de temps très courte, visible à la lecture suivante).
- **Hors ligne** : l'écriture passe par la file (`OutboxService.submit('POST', …)`), rejouée avec la même `Idempotency-Key` ; un rejeu après un verrouillage réussi reçoit la réponse mémorisée, sinon 409 « déjà clôturée ».
- **Pas de réouverture** : le ticket ne demande pas de rouvrir une journée close ; aucune route ne repasse en `OPEN` (les corrections passent par une contre-opération du lendemain, texte de l'écran).
- **Journée clôturée lisible** : `GET /api/v1/days/closing` répond 200 avec `status` `CLOSED` ; `GET /api/v1/days/live` (JR-03) idem.
- **Base partagée entre classes de test** : `DayClosingLockIT` remet la journée à `OPEN` et supprime les lignes de `day_closing_counts` après chaque test ; elle ne laisse jamais la journée du jour `CLOSED`. Elle crée une vendeuse (prénom « Zoé ») qu'elle désactive ensuite.
- **Saisies encore en démonstration** : la caisse et la liste de contrôle (livraisons soldées, tickets en attente, shootings) restent des données fictives, ne bloquent pas le verrouillage et gardent la route sous `underConstruction`.

## Tests existants

- Backend : `feature/dayclosing/controller/DayClosingLockIT.java` (nouveau), `feature/dayclosing/service/impl/DayClosingLockServiceImplTest.java` (nouveau), `feature/day/service/impl/BusinessDayServiceImplTest.java` (6 tests ajoutés), `feature/dayclosing/service/impl/DayClosingCountServiceImplTest.java` (constructeur : service de journée ajouté).
- Frontend : `features/day/pages/day-close-page/day-close-lock.spec.ts` (nouveau, 5 tests), `features/day/data-access/day-service.spec.ts` (3 tests ajoutés, fixture `saved` complétée).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Ouvrir `/day/close`, enregistrer le comptage avec un motif pour chaque écart | le bouton « Verrouiller la journée » devient actif et dit « Tout est vert » | 🧪 recette |
| R2 | Faire une vente au comptoir, revenir sur `/day/close` sans recharger puis verrouiller | refus 422 si un écart sans motif est apparu (message nommant l'article) ; sinon verrouillage | 🧪 recette |
| R3 | Verrouiller | toast et bandeau « Journée verrouillée à hh:mm », saisies désactivées | 🧪 recette |
| R4 | Tenter une vente au comptoir après le verrouillage | refus « La journée du … est déjà clôturée » | 🧪 recette |
| R5 | Consulter le journal | une ligne « Clôture de la journée du … » avec les écarts, signée par l'administrateur | 🧪 recette |
| R6 | Le lendemain, ouvrir la journée en reprenant les quantités de la veille | les quantités de la veille clôturée sont proposées (JR-02) | 🧪 recette |

## Résultat de l'exécution

- Date : à lancer (CI ou local) ; rien n'a été exécuté en local (Maven Central inaccessible depuis la machine de rédaction).
- Environnement : à compléter après la CI de la branche `feature/JR-06-verrouillage-cloture-jour`.
- Anomalies ouvertes : aucune connue. Hypothèses à valider par le PO : voir « Points d'attention » du guide. 6 étapes 🧪 restantes.
