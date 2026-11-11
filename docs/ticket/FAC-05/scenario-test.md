# Scénarios de test — FAC-05 · Suivi des statuts de facture

Statut : ✅ tests automatiques verts en CI (11/11/2026) ; recette 🧪 à passer · Ticket : [#75](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/75) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | administrateur (ADMIN, lit les totaux) ; vendeuse (SELLER) et livreur (DELIVERY_DRIVER), refusés |
| Données de départ | factures de statuts `VALIDATED`, `CANCELLED`, `REVERSED` (insérées directement sur un jour réservé au test, 2001-02-03, pour des totaux exacts sur la base partagée) ; une vente au comptoir de 3 articles à 8 500 Ar (25 500 Ar) puis son avoir. `StockApiTestSupport.endAutomaticPromotions()` avant la vente |
| Permission | `reports.read` (existante, ADMIN seulement) : aucune migration |
| Tickets à avoir déployés avant | FAC-01 (factures), FAC-04 (statut `REVERSED`, avoir) |

Statuts suivis : `VALIDATED` = payée (l'écran affiche « Payée »), `REVERSED` = contre-passée par un avoir (« Avoir »), `CANCELLED` = facture d'une vente annulée avant FAC-04 (« Annulée »). Il n'existe pas de facture « impayée » : la facture naît de l'encaissement (FAC-01).

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Les totaux consolidés reflètent la somme des factures par statut. | Le 03/02/2001 : 2 factures `VALIDATED` (1 000 + 2 500 Ar), 1 `REVERSED` (4 000 Ar), 1 `CANCELLED` (500 Ar). Admin : `GET /api/v1/invoices/status-totals?from=2001-02-03&to=2001-02-03` | 200 ; payée 2 / 3 500 ; annulée 1 / 500 ; avoir 1 / 4 000 ; total consolidé 4 factures / 8 000 Ar | `InvoiceStatusIT.getStatusTotals_invoicesOfEveryStatus_sumsCountAndAmountOfEachStatus` | ✅ auto |
| 1 | idem, total = somme des statuts (logique métier) | Le dépôt renvoie « avoir » et « payée » seulement | les trois statuts sont listés (annulée à 0), total = 3 factures / 7 500 Ar | `InvoiceStatusServiceImplTest.getStatusTotals_someStatusesPresent_listsEveryStatusAndTheGrandTotalIsTheirSum` | ✅ auto |
| 1 | idem, avec un vrai flux | Vente de 25 500 Ar encaissée, puis avoir émis par l'admin | le montant « payée » du jour baisse de 25 500 Ar, le montant « avoir » monte de 25 500 Ar | `InvoiceStatusIT.getStatusTotals_saleThenCreditNote_movesTheAmountFromPaidToCredited` | ✅ auto |
| 1 | Écran | Admin ouvre `/sales/statuses` | une ligne par statut (nombre, montant), pied de tableau et cartes avec le total consolidé | `invoice-status-page.spec.ts` : « should show the number of invoices of each status », « should show the consolidated total of the period » | ✅ auto |
| 1 | Appel HTTP du front | période du mois | `GET /api/v1/invoices/status-totals?from=&to=`, une date vide n'est pas envoyée | `invoice-status-service.spec.ts` : « lit les totaux par statut de la période demandée », « n'envoie pas de période vide… » | ✅ auto |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `InvoiceStatusIT.getStatusTotals_withoutToken_returns401` | ✅ auto |
| Vendeuse ou livreur | 403 `FORBIDDEN` | `InvoiceStatusIT.getStatusTotals_sellerAndDeliveryDriver_return403` | ✅ auto |
| Champ interdit dans le corps (`role`, `id`) | sans objet : lecture seule, aucun corps | n/a | n/a |
| Début après la fin | 422 `VALIDATION_FAILED` | `InvoiceStatusIT.getStatusTotals_fromAfterTo_returns422`, `InvoiceStatusServiceImplTest.getStatusTotals_fromAfterTo_throwsBusinessExceptionWithoutReading` | ✅ auto |
| Date mal formée (`01-11-2026`) | 400 `MALFORMED_REQUEST` | `InvoiceStatusIT.getStatusTotals_malformedDate_returns400` | ✅ auto |
| Ressource introuvable / conflit | sans objet : agrégat, jamais de 404 ni de 409 | n/a | n/a |

## Cas limites et hors ligne

- Bornes `from` et `to` incluses ; une facture de la veille ou du lendemain n'est pas comptée : `InvoiceStatusIT.getStatusTotals_periodBounds_areInclusiveAndOtherDaysAreLeftOut` ✅ auto.
- Période sans facture : les trois statuts à 0 et total à 0 Ar : `InvoiceStatusIT.getStatusTotals_periodWithoutInvoice_listsEveryStatusAtZero`, `InvoiceStatusServiceImplTest.getStatusTotals_noInvoice_returnsZeroEverywhere` ✅ auto.
- Sans dates : du 1er du mois au jour courant (fuseau Indian/Antananarivo, un instant 21:30 UTC tombe déjà le lendemain) : `InvoiceStatusIT.getStatusTotals_withoutDates_returnsTheCurrentMonth`, `InvoiceStatusServiceImplTest.getStatusTotals_noDates_usesTheMonthToDateOfTheShopDay` ✅ auto.
- Écran : une période à l'envers n'est pas envoyée, un message l'explique (même comportement que VTE-08).
- Hors ligne : lecture seule, rien à rejouer ; l'écran affiche l'état d'erreur avec « Réessayer ».
- Recette 🧪 (Njara) : depuis `/sales` (administrateur), bouton « Statuts des factures » ; encaisser une vente, vérifier « Payée » +1 ; émettre son avoir, vérifier « Payée » −1 et « Avoir » +1 ; vendeuse : le bouton n'apparaît pas et `/sales/statuses` est refusée ; fr puis mg.

## Résultat de l'exécution

- Date : 11/11/2026
- Environnement : CI GitHub (ci.yml, conventions.yml, Qodana : 0 annotation) sur e12cd928 ; backend 839 tests, 0 échec (dont `InvoiceStatusIT` 9 et `InvoiceStatusServiceImplTest` 4) ; frontend Vitest vert ; rien lancé en local (consigne n°3)
- Anomalies ouvertes : aucune connue
