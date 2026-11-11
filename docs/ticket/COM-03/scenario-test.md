# Scénarios de test — COM-03 · Validation des commissions

Statut : ✅ rédigé · Ticket : [#87](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/87) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` : lecture par vendeuse et validation ; `SELLER` et `DELIVERY_DRIVER` pour les refus 403 |
| Données de départ | journée du jour ouverte (JR-01) ; un canal neuf par test, une vendeuse « Miora Rakoto » à 5,00 % (la base est partagée) ; chaque vente d'un article à 8 500 Ar rapporte 425 Ar de commission |
| Tickets à avoir déployés avant | COM-02 (table `commissions`, `SaleCancelledEvent`), JNL-01, FAC-04 (l'avoir annule la vente) ; migration `2026_11_11-60` |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Statut « à valider » affiché par vendeuse | 2 ventes de 8 500 Ar, rien de validé | `status = TO_VALIDATE`, 2 commissions et 850 Ar à valider, 0 validée | `CommissionValidationIT.getSellers_sellerWithComputedCommissions_isToValidateWithCountAndTotal` | ✅ auto |
| 2 | Validation : statut « validée » affiché | validation de la vendeuse sur la période | 200 avec `validatedCount = 2`, `validatedAmount = 850` ; `GET /sellers` : `VALIDATED`, 0 à valider, 2 validées ; la liste donne `status = VALIDATED` | `CommissionValidationIT.validate_pendingCommissions_returnsCountAndTotalAndSellerBecomesValidated` | ✅ auto |
| 3 | Écran : statut et totaux par vendeuse | une vendeuse à valider, une validée | pastille « À valider » + bouton « Valider » ; pastille « Validée » sans bouton ; montants et nombres | `commission-list-page.spec.ts` › should show the status to validate or validated of each seller, with the totals | ✅ auto |
| 4 | Écran : validation après confirmation | clic « Valider », dialogue, confirmation | rien n'est envoyé avant la confirmation ; `POST /validation` avec la vendeuse et le mois ; message de réussite, rechargement des deux listes | `commission-list-page.spec.ts` › should validate the seller of the month only after the confirmation, then reload | ✅ auto |
| 5 | Écran : annulation du dialogue | « Annuler » | aucun appel, dialogue fermé | `commission-list-page.spec.ts` › should send nothing when the confirmation is cancelled | ✅ auto |
| 6 | Journal | validation d'une commission de 425 Ar | une ligne `UPDATE`, `COMMISSION`, « Commission VTE-… · Miora Rakoto validée : 425.00 Ar » | `CommissionValidationIT.validate_validatedCommissions_areTracedInTheJournal` | ✅ auto |
| 7 | Annulées exclues des totaux et de la validation | vente annulée (comme par un avoir FAC-04) | la vendeuse sort des totaux ; validation : 404 | `CommissionValidationIT.validate_cancelledCommission_isNeitherCountedNorValidated` | ✅ auto |
| 8 | Une annulée, une conservée | 2 ventes dont 1 annulée | seule la conservée est validée (1 commission, 425 Ar) | `CommissionValidationIT.validate_oneSaleCancelledOneKept_validatesOnlyTheKeptOne` | ✅ auto |
| 9 | Vente annulée après validation (hypothèse prudente) | commission validée, puis `SaleService.cancel` | la commission passe à `CANCELLED`, garde `validated_at`, n'est plus comptée ; le journal ajoute « (déjà validée : versement à corriger) » | `CommissionValidationIT.cancel_saleWithAValidatedCommission_commissionIsCancelledKeepsItsDateAndJournalSaysSo` ; `CommissionTest.cancel_validatedCommission_becomesCancelledAndKeepsItsValidationDate` | ✅ auto |
| 10 | Règle de l'état | valider une commission `CALCULATED`, `VALIDATED` ou `CANCELLED` | seule `CALCULATED` est validable (`IllegalStateException` sinon) | `CommissionTest.validate_calculatedCommission_becomesValidatedWithItsDate` ; `CommissionTest.validate_alreadyValidatedOrCancelled_isRefused` | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | GET `/sellers`, POST `/validation` | 401 `UNAUTHENTICATED` | `CommissionValidationIT.commissionsValidation_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton vendeuse (ne valide pas ses commissions), jeton livreur | 403 `FORBIDDEN` sur les deux endpoints ; rien n'est validé | `CommissionValidationIT.commissionsValidation_sellerOrDeliveryDriver_returns403AndValidatesNothing` | ✅ auto |
| Mass assignment | corps avec `status: CANCELLED` et `amount: 1` | champs ignorés, 425 Ar validés | `CommissionValidationIT.validate_extraFieldsInTheBody_areIgnored` | ✅ auto |
| Données invalides | `from` après `to` ; `sellerId` absent | 422 `VALIDATION_FAILED` | `CommissionValidationIT.validate_fromAfterToOrMissingField_returns422` | ✅ auto |
| Période invalide, taille > 100 (liste) | `from` après `to` ; `size=101` | 422 `VALIDATION_FAILED` | `CommissionValidationIT.getSellers_fromAfterToOrSizeAbove100_returns422` | ✅ auto |
| Introuvable | vendeuse inconnue ; période sans commission | 404 `NOT_FOUND`, rien n'est validé | `CommissionValidationIT.validate_unknownSeller_returns404` ; `CommissionValidationIT.validate_periodWithoutTheCommissions_returns404AndValidatesNothing` | ✅ auto |
| Conflit | valider deux fois la même période | 409 `CONFLICT` : une commission validée est définitive | `CommissionValidationIT.validate_twice_returns409` | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Nouvelle vente après une validation | 1 vente validée, puis une 2e | la vendeuse repasse « À valider » ; la validation suivante ne prend que la nouvelle (1, 425 Ar) | `CommissionValidationIT.validate_newSaleAfterAValidation_validatesOnlyTheNewCommission` | ✅ auto |
| Erreur renvoyée par le serveur | 409 au clic (validée entre-temps) | message du serveur affiché, liste des vendeuses rechargée | `commission-list-page.spec.ts` › should show the server error and refresh when the commissions are already validated | ✅ auto |
| Changement de mois | mois précédent choisi | les vendeuses et les commissions sont rechargées avec les bornes du mois | `commission-list-page.spec.ts` › should reload the chosen month from its first page | ✅ auto |
| Appels HTTP du service | `sellers` et `validate` | `GET /sellers?page&size&from&to`, `POST /validation` avec le seul corps `sellerId`, `from`, `to` | `commission-service.spec.ts` (2 tests ajoutés) | ✅ auto |
| Transaction unique | une erreur pendant la validation | toutes les commissions de la période sont validées ou aucune (`@Transactional`) | — | 🔍 code |
| Hors ligne | validation sans connexion | non rejouable : action d'administration sur l'état du serveur, jamais mise en file hors ligne | — | 🔍 code |

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Admin : Administration › Commissions calculées, mois en cours avec des ventes de Miora | tableau « Commissions par vendeuse » : « À valider », montant et nombre | 🧪 |
| Cliquer « Valider », lire le dialogue, confirmer | message « n commission(s) de Miora validée(s) », pastille « Validée », bouton disparu | 🧪 |
| Annuler la vente d'une commission déjà validée (avoir) | commission annulée, totaux corrigés, mention au journal « déjà validée » | 🧪 |
| Accessibilité : bouton « Valider » nommé par vendeuse, dialogue au clavier | libellé lu avec le nom de la vendeuse, Échap annule | 🧪 |

## Résultat de l'exécution

- Date : 11/11/2026, commit `8e49d5d7` de la branche (après fusion de `develop`).
- Environnement : CI GitHub : `ci.yml` vert (backend 822 tests dont les 15 de `CommissionValidationIT`, 0 échec ; frontend 124 fichiers, 785 tests réussis), `conventions.yml` vert, `qodana_code_quality.yml` vert, 0 annotation sur « Qodana for JVM ».
- Anomalies ouvertes : aucune connue.
