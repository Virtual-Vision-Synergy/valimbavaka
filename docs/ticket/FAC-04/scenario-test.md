# Scénarios de test — FAC-04 · Émission d'un avoir motivé

Statut : ✅ tests automatiques verts en CI (11/11/2026) ; recette 🧪 à passer · Ticket : [#74](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/74) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | administrateur (ADMIN, émet), vendeuse (SELLER : lit l'avoir de sa vente, refusée à l'émission), livreur (DELIVERY_DRIVER, refusé) |
| Données de départ | journée ouverte, vendeuse avec canal et taux, article en stock ; une vente au comptoir encaissée (facture `FAC-2026-NNNN` émise par FAC-01). `StockApiTestSupport.endAutomaticPromotions()` avant chaque vente |
| Permission | `invoices.credit` (nouvelle, migration `2026_11_11-95-changelog.sql`) : ADMIN seulement |
| Tickets à avoir déployés avant | FAC-01, FAC-02, LIV-05, PRO-03, COM-02, JNL-01, JNL-02 |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | L'émission exige un motif | Vente de 2 savons (17 000 Ar) facturée. Admin : `POST /api/v1/invoices/{id}/credit-note` `{"reason":"Produit défectueux"}` | 201, avoir `AV-<année>-0001` copie figée de la facture, motif conservé | `CreditNoteIT.issueCreditNote_validInvoice_returns201WithTheFrozenContentOfTheInvoice` | ✅ auto |
| 1 | idem, motif vide, espaces seuls ou absent | corps `{}`, `""`, `"   "` | 422 `VALIDATION_FAILED`, rien ne change | `CreditNoteIT.issueCreditNote_missingOrBlankReason_returns422AndChangesNothing` | ✅ auto |
| 1 | idem, borne haute | motif de 500 caractères / 501 | 201 / 422 | `CreditNoteIT.issueCreditNote_reasonOfExactlyTheMaximumLength_returns201`, `CreditNoteIT.issueCreditNote_reasonOneCharacterTooLong_returns422AndChangesNothing` | ✅ auto |
| 1 | L'émission réintègre les articles en stock | Après l'avoir | facture `REVERSED`, vente `CANCELLED`, stock du comptoir remis (RETURN), commission annulée | `CreditNoteIT.issueCreditNote_validInvoice_reversesTheInvoiceCancelsTheSaleAndPutsTheArticlesBackInStock` | ✅ auto |
| 2 | Traçabilité (JNL-01) | Après l'avoir | journal `REVERSE` de la facture avec numéro d'avoir, motif ; annulation de la vente journalisée | `CreditNoteIT.issueCreditNote_validInvoice_tracesTheReversalAndTheCancellationInTheJournal` | ✅ auto |
| 3 | Numérotation sans trou | Deux factures créditées à la suite | `AV-…-0001` puis `AV-…-0002` | `CreditNoteIT.issueCreditNote_twoInvoices_getConsecutiveIncreasingNumbers` | ✅ auto |
| 4 | Vente annulée à chaud (annulation d'une livraison, LIV-05) | Livraison livrée puis annulée | facture `REVERSED`, avoir « Annulation de la vente VTE-… », stock restitué une seule fois | `DeliveryUndoIT.undo_deliveredOrder_reversesItsInvoiceWithACreditNoteInsteadOfLeavingItValidated`, `CreditNoteIT.cancelSale_saleWithAnInvoice_issuesItsCreditNoteAutomatically`, `CreditNoteIT.cancelSale_saleWithoutInvoice_cancelsWithoutCreditNote` | ✅ auto |
| 4 | Vente déjà annulée avant FAC-04 dont la facture restait `VALIDATED` | Avoir émis par l'admin | avoir émis, pas de second retour en stock | `CreditNoteIT.issueCreditNote_saleAlreadyCancelledBeforeFac04_issuesItWithoutPuttingTheArticlesBackTwice` | ✅ auto |
| 5 | Lecture de l'avoir | `GET /api/v1/invoices/{id}/credit-note` | admin et vendeuse de la vente : 200 ; collègue : 404 ; facture sans avoir : 404 | `CreditNoteIT.getCreditNote_afterTheIssue_isReadByTheAdministratorAndByTheSellerOfTheSale`, `CreditNoteIT.getCreditNote_sellerAskingForAColleagueInvoice_returns404`, `CreditNoteIT.getCreditNote_invoiceWithoutCreditNote_returns404` | ✅ auto |

Tests unitaires : `CreditNoteServiceImplTest` (émission, déjà contre-passée, inconnue, vente déjà annulée, journée close, `issueForCancelledSale`, lecture), `CreditNoteTest` (copie figée, lignes en lecture seule), `SaleCancelledListenerTest` (une erreur d'avoir annule l'annulation). Frontend : `credit-note-dialog.spec.ts` (motif blanc refusé, motif nettoyé envoyé, 409 affiché).

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `CreditNoteIT.issueCreditNote_withoutToken_returns401` | ✅ auto |
| Vendeuse ou livreur à l'émission | 403 `FORBIDDEN`, rien ne change | `CreditNoteIT.issueCreditNote_sellerOrDeliveryDriver_returns403AndChangesNothing` | ✅ auto |
| Champ interdit dans le corps (`id`, `number`, `total`…) | ignoré (mass assignment) | `CreditNoteIT.issueCreditNote_bodyWithForgedFields_isIgnored` | ✅ auto |
| Motif absent, vide, trop long | 422 `VALIDATION_FAILED` | voir ci-dessus | ☐ |
| Facture inconnue | 404 `NOT_FOUND` | `CreditNoteIT.issueCreditNote_unknownInvoice_returns404` | ✅ auto |
| Identifiant mal formé | 400 `MALFORMED_REQUEST` | `CreditNoteIT.issueCreditNote_malformedInvoiceId_returns400` | ✅ auto |
| Facture déjà créditée | 409 `CONFLICT`, aucun second numéro ni retour en stock | `CreditNoteIT.issueCreditNote_alreadyCredited_returns409AndUsesNoSecondNumberNorSecondRestock` | ✅ auto |
| Journée de la vente clôturée | 409 `CONFLICT`, tout est annulé (facture restée `VALIDATED`, numéro non consommé) | `CreditNoteIT.issueCreditNote_dayOfTheSaleClosed_returns409AndChangesNothing` | ✅ auto |
| Deux requêtes simultanées sur la même facture | un seul avoir, l'autre refusée | `CreditNoteIT.issueCreditNote_twoRequestsAtTheSameTime_issuesOneCreditNoteAndRejectsTheOther` | ✅ auto |
| Lecture sans jeton / livreur | 401 / 403 | `CreditNoteIT.getCreditNote_withoutTokenOrAsDeliveryDriver_returns401Or403` | ✅ auto |

## Cas limites et hors ligne

- Pas de file hors ligne : l'avoir est une opération légale qui exige la réponse du serveur (bouton « Avoir » du panneau facture, dialogue de motif ; le message d'erreur reste dans le dialogue).
- Bouton désactivé tant que la facture n'est pas émise (aperçu) ou déjà contre-passée.
- Recette 🧪 (Njara) : depuis `/sales`, ouvrir une vente, « Émettre un avoir… », saisir un motif ; vérifier badge « avoir », encadré avoir (numéro, date, motif), stock rendu, fr puis mg.

## Résultat de l'exécution

- Date : 11/11/2026
- Environnement : CI GitHub (ci.yml, conventions.yml, Qodana : 0 annotation) sur 72a8d47 ; backend 807 tests, 0 échec ; frontend Vitest vert ; rien lancé en local (consigne n°3)
- Anomalies ouvertes : aucune connue
