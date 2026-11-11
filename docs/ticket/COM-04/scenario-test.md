# Scénarios de test — COM-04 · Suivi de commission et objectif

Statut : ✅ rédigé · Ticket : [#88](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/88) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Décision PO du 07/11/2026 : l'objectif mensuel de la vendeuse est un champ de sa fiche utilisateur, modifiable par l'administrateur seulement. Hypothèse prise : l'objectif est un chiffre d'affaires mensuel en ariary, comparé au CA des ventes et prestations commissionnées du mois (annulées exclues).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `SELLER` : ses commissions et son objectif ; `ADMIN` : fixe l'objectif (fiche utilisateur) et consulte toute vendeuse ; `DELIVERY_DRIVER` pour le refus 403 |
| Données de départ | journée du jour ouverte (JR-01) ; un canal neuf par test, deux vendeuses « Miora Rakoto » et « Hanta Rakoto » à 5,00 % (la base est partagée) ; chaque vente d'un article à 8 500 Ar rapporte 425 Ar de commission ; objectif de 20 000 Ar |
| Tickets à avoir déployés avant | COM-02 (table `commissions`), COM-03 (statut `VALIDATED`), AUTH-03 (fiche utilisateur) ; migration `2026_11_11-61` |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Détail par vente : base, taux, commission, statut | une vente validée, une vente à valider (8 500 Ar, 5 %) | chaque ligne porte `baseAmount = 8500`, `rate = 5`, `amount = 425` et son `status` (`VALIDATED`, `CALCULATED`) | `CommissionFollowUpIT.getLines_sellerWithSales_returnsBaseRateAmountAndStatusOfEachSale` | ☐ |
| 1b | Écran : détail par vente | 3 lignes (calculée, validée à 4,5 %, annulée) | numéro, base, taux, commission et pastille de statut (Calculée / Validée / Annulée) par ligne | `commissions-page.spec.ts` › should show the base, the rate, the commission and the status of each sale | ☐ |
| 1c | Vente annulée visible mais non comptée | vente annulée par un avoir | listée avec `CANCELLED` ; totaux : 0 commission | `CommissionFollowUpIT.getLines_cancelledSale_isListedWithItsStatusButNotCounted` | ☐ |
| 2 | Commission du mois et objectif | 2 ventes de 8 500 Ar, objectif 20 000 Ar | 2 commissions, CA 17 000, commission 850, à valider 850, objectif 20 000, avancement 85 % | `CommissionFollowUpIT.getFollowUp_sellerWithSales_returnsTotalsOfTheMonthAndProgressTowardsTheTarget` | ☐ |
| 2b | Part validée et à valider | une vente validée puis une nouvelle | validée 425, à valider 425, total 850 | `CommissionFollowUpIT.getFollowUp_validatedCommissions_areSplitFromThoseToValidate` | ☐ |
| 2c | Annulées exclues des totaux (COM-03) | 1 vente gardée, 1 annulée | 1 commission, CA 8 500, commission 425 | `CommissionFollowUpIT.getFollowUp_cancelledSale_isNotCounted` | ☐ |
| 2d | Écran : commission, objectif et anneau | objectif 20 000, avancement 85 | montant de la commission, objectif affiché, anneau étiqueté 85 % | `commissions-page.spec.ts` › should show the commission of the month and the progress towards the target | ☐ |
| 3 | Objectif fixé sur la fiche par l'administrateur | `PUT /users/{id}` avec `monthlyTarget = 2500000.50` par l'administrateur | 200, objectif renvoyé et relu par `GET /users/{id}` ; retiré avec `null` | `SellerTargetIT.updateUser_byAdministrator_setsChangesAndClearsTheTargetAndTracesIt` | ☐ |
| 3b | Objectif à la création | `POST /users` vendeuse avec `monthlyTarget = 1500000` | 201, objectif renvoyé et stocké | `SellerTargetIT.createUser_sellerWithTarget_returnsAndStoresTheTarget` | ☐ |
| 3c | Objectif facultatif | `POST /users` vendeuse sans objectif | 201, pas d'objectif ; le suivi renvoie `monthlyTarget` et `targetProgressPercent` nuls | `SellerTargetIT.createUser_sellerWithoutTarget_hasNoTarget` ; `CommissionFollowUpIT.getFollowUp_sellerWithoutTarget_hasNullTargetAndProgress` | ☐ |
| 3d | Écran sans objectif | `monthlyTarget = null` | pas d'anneau, message « aucun objectif fixé par l'administrateur » | `commissions-page.spec.ts` › should say that no target is set instead of showing a progress ring | ☐ |
| 4 | Journal du changement d'objectif | objectif : aucun → 1 000 000 → 2 500 000,50 → aucun | 3 lignes `UPDATE` `USER` « … · objectif mensuel : ancien → nouveau Ar » ; un objectif inchangé n'ajoute rien | `SellerTargetIT.updateUser_byAdministrator_setsChangesAndClearsTheTargetAndTracesIt` ; `SellerTargetIT.updateUser_sameTarget_isNotTracedAgain` | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | GET `/follow-up`, `/follow-up/lines` ; PUT `/users/{id}` | 401 `UNAUTHENTICATED` | `CommissionFollowUpIT.followUp_withoutToken_returns401` ; `SellerTargetIT.updateUser_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton livreur sur le suivi | 403 `FORBIDDEN` | `CommissionFollowUpIT.followUp_deliveryDriver_returns403` | ☐ |
| La vendeuse ne s'augmente pas l'objectif | jeton vendeuse, `PUT /users/{son id}` avec `monthlyTarget = 1` | 403 `FORBIDDEN` ; l'objectif en base reste 1 000 000 (aucun DTO d'entrée de la vendeuse ne porte l'objectif) | `SellerTargetIT.updateUser_sellerOnHerOwnProfile_returns403AndTheTargetStaysUnchanged` | ☐ |
| Le serveur impose l'identifiant (comme VTE-07) | la vendeuse envoie `sellerId` d'une collègue (objectif 99 999, 2 ventes) | elle reçoit ses propres chiffres (1 vente, pas d'objectif) ; les lignes sont les siennes | `CommissionFollowUpIT.getFollowUp_sellerNamingAnotherSeller_stillGetsHerOwnFigures` ; `CommissionFollowUpServiceImplTest.getFollowUp_seller_readsHerOwnFiguresWhateverSellerIdIsRequested` | ☐ |
| L'administrateur voit tout | `sellerId` de Hanta, objectif 10 000 | ses chiffres : 425 Ar, avancement 85 % | `CommissionFollowUpIT.getFollowUp_administratorNamingASeller_readsHerFigures` | ☐ |
| Administrateur sans vendeuse | aucun `sellerId` | 422 `VALIDATION_FAILED` (suivi et lignes) | `CommissionFollowUpIT.getFollowUp_administratorWithoutSellerId_returns422` ; `CommissionFollowUpServiceImplTest.getFollowUp_administratorWithoutSellerId_throwsBusinessException` | ☐ |
| Vendeuse inconnue ou compte non vendeuse | `sellerId` aléatoire ; id de l'administrateur | 404 `NOT_FOUND` | `CommissionFollowUpIT.getFollowUp_administratorWithUnknownOrNonSellerId_returns404` ; `CommissionFollowUpServiceImplTest.getFollowUp_administratorNamingANonSeller_throwsNotFound` | ☐ |
| Objectif invalide | `0`, `-5`, `100.123` | 422 `VALIDATION_FAILED` | `SellerTargetIT.updateUser_invalidTarget_returns422` | ☐ |
| Objectif sur un non-vendeur | l'administrateur reçoit `monthlyTarget = 1000000` | 422 (réservé aux vendeuses) | `SellerTargetIT.updateUser_targetOnAnAdministrator_returns422` | ☐ |
| Mois mal formé | `month=2026-13`, `month=novembre` | 400 | `CommissionFollowUpIT.getFollowUp_malformedMonth_returns400` | ☐ |
| Taille de page | `size=101` | 422 `VALIDATION_FAILED` | `CommissionFollowUpIT.getLines_pagingAndSort_pageOfOneAndSizeAbove100` | ☐ |
| Mass assignment | l'objectif n'est accepté que par `CreateUserWDto` et `UpdateUserWDto` (`users.create` / `users.update`, administrateur) ; le suivi n'a aucune écriture | voir ligne « La vendeuse ne s'augmente pas l'objectif » | `SellerTargetIT.updateUser_sellerOnHerOwnProfile_returns403AndTheTargetStaysUnchanged` | ☐ |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Aucune vente ce mois | vendeuse avec objectif 20 000 | totaux à 0, avancement 0 % | `CommissionFollowUpIT.getFollowUp_sellerWithoutSaleThisMonth_returnsZeroTotals` ; `CommissionFollowUpServiceImplTest.getFollowUp_noTargetAndNoSale_hasZeroTotalsAndNullProgress` | ☐ |
| Écran : mois vide | aucune commission | état vide « Aucune commission ce mois-ci » | `commissions-page.spec.ts` › should show an empty state when there is no commission this month | ☐ |
| Dépassement de l'objectif | CA 8 500 pour un objectif 5 000 | avancement 170 % (non plafonné côté API) ; l'anneau s'arrête à 100 %, le texte garde 170 % | `CommissionFollowUpIT.getFollowUp_revenueAboveTheTarget_progressExceeds100Percent` ; `commissions-page.spec.ts` › should let the ring stop at 100 % while the text keeps the real progress | ☐ |
| Arrondi de l'avancement | CA 1 000, objectif 3 000 | 33,3 % (une décimale, arrondi HALF_UP) | `CommissionFollowUpServiceImplTest.getFollowUp_targetSet_progressIsRevenueOverTargetRoundedToOneDecimal` | ☐ |
| Mois précédent | `month` = mois précédent | les ventes du mois en cours n'y figurent pas ; bornes `AAAA-MM-01` à fin de mois | `CommissionFollowUpIT.getFollowUp_otherMonth_leavesTheSalesOfTheCurrentMonthOut` | ☐ |
| Mois par défaut | pas de `month` | mois en cours à Madagascar (`Indian/Antananarivo`) | `CommissionFollowUpServiceImplTest.getFollowUp_noMonth_usesTheCurrentMonthOfTheClock` | ☐ |
| Pagination du détail | `size=1` trié par `amount`, 2 ventes | 1 ligne, 2 au total, 2 pages | `CommissionFollowUpIT.getLines_pagingAndSort_pageOfOneAndSizeAbove100` | ☐ |
| Écran : changement de mois | autre mois choisi | nouvel appel avec ce `month` | `commissions-page.spec.ts` › should ask again when another month is chosen | ☐ |
| Écran : vendeuse | la page ne nomme jamais de vendeuse | aucun `sellerId` envoyé, même si l'URL en porte un | `commissions-page.spec.ts` › should ask the server for the month of a seller without naming any seller | ☐ |
| Écran : administrateur | sans `?sellerId=` ; avec `?sellerId=u1` | « Choisissez une vendeuse », aucun appel ; sinon `sellerId=u1` envoyé | `commissions-page.spec.ts` › should ask an administrator to pick a seller and call nothing meanwhile ; should send the seller named by the administrator | ☐ |
| Appels HTTP du service | `followUp`, `followUpLines` | `GET /follow-up?month[&sellerId]`, `GET /follow-up/lines?page&size&month` | `commission-service.spec.ts` (3 tests ajoutés) | ☐ |
| Calcul des mois et jours restants | 30 novembre ; passage d'année | 1 jour restant ; `2025-12` pour `2026-01` − 1 mois | `commission-months.spec.ts` (3 tests) | ☐ |
| Objectif modifié en cours de mois | l'objectif change le 15 | le suivi utilise l'objectif courant de la fiche (pas d'historique) | — | 🔍 code (`CommissionFollowUpServiceImpl.getFollowUp`) |
| Hors ligne | consultation sans connexion | lecture seule : l'écran affiche l'erreur et « Réessayer » ; rien n'est mis en file | — | 🔍 code (`commissions-page.html`) |
| Hors périmètre | recalcul, export du relevé | bouton « Exporter mon relevé » toujours en démonstration | — | ⏭ COM-05 |

## Tests existants

- Backend : `feature/commission/controller/CommissionFollowUpIT.java`, `feature/user/controller/SellerTargetIT.java`, `feature/commission/service/impl/CommissionFollowUpServiceImplTest.java` ; constructeurs adaptés dans `UserServiceImplTest` (`CreateUserWDto`, `UpdateUserWDto`) et dans les tests qui construisent un `UserRDto`.
- Frontend : `commissions-page.spec.ts`, `commission-service.spec.ts`, `commission-months.spec.ts` ; `user-list-page.spec.ts` (nouveau champ `monthlyTarget`) ; `demo-pages.a11y.spec.ts` charge toujours la page.

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Admin : Utilisateurs › fiche d'une vendeuse, champ « Objectif mensuel de ventes (Ar) », enregistrer | objectif enregistré ; ligne « objectif mensuel » dans le journal | 🧪 |
| Vendeuse : Mes commissions, mois en cours | commission, anneau d'avancement, jours restants, détail par vente avec statut | 🧪 |
| Vendeuse : vérifier qu'aucun champ ne permet de modifier l'objectif | aucun | 🧪 |
| Admin : Commissions calculées › bouton « Suivi » d'une vendeuse | page « Mes commissions » de cette vendeuse, mois repris | 🧪 |
| Accessibilité : anneau (rôle image, libellé), tableau, sélecteur de mois au clavier | libellés lus, focus visible | 🧪 |

## Résultat de l'exécution

- Date : _(à compléter après la CI)_
- Environnement : tests automatiques à lancer (CI ou local), aucun lancé pour l'instant.
- Anomalies ouvertes : aucune connue.
