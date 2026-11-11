# Scénarios de test — FAC-06 · Export du livre des ventes

Statut : ✅ tests automatiques verts en CI (11/11/2026) ; recette 🧪 à passer · Ticket : [#76](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/76) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | administrateur (ADMIN, exporte), vendeuse (SELLER, refusée), livreur (DELIVERY_DRIVER, refusé) |
| Données de départ | journée ouverte, vendeuse avec canal et taux, article en stock ; une vente au comptoir encaissée (facture `FAC-2026-NNNN` par FAC-01), éventuellement contre-passée par un avoir (FAC-04). `StockApiTestSupport.endAutomaticPromotions()` avant chaque vente |
| Permission | `invoices.export` (nouvelle, migration `2026_11_11-97-changelog.sql`) : ADMIN seulement |
| Hypothèse comptable | la facture et son avoir sont **deux lignes** du livre ; l'avoir est en négatif ; la facture contre-passée garde son montant (somme des deux = 0) |
| Tickets à avoir déployés avant | FAC-01, FAC-04, SYS-01 |

## Scénarios issus des critères d'acceptation

La base de test est partagée : chaque test cherche (`search`) le numéro de sa propre facture et ne compte jamais les lignes d'une période entière.

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | L'export confirme la génération du fichier | Vente de 3 savons (25 500 Ar) facturée puis contre-passée. Admin : `GET /api/v1/invoices/sales-book/export?from=<jour>&to=<jour>&search=<n° facture>` | 200 `.xlsx` `livre-des-ventes.xlsx`, onglet « Livre des ventes », en-têtes fixes, ligne Facture (+25 500, « Contre-passée »), ligne Avoir (−25 500, facture d'origine, motif) | `SalesBookIT.exportSalesBook_byAdmin_returnsXlsxWithTheInvoiceAndTheNegativeCreditNote` | ✅ auto |
| 1 | idem, côté écran : confirmation | Admin ouvre « Livre des ventes » sur `/sales`, clique « Exporter en XLSX » | fichier téléchargé et toast « Fichier … généré. » ; le bouton redevient actif | `sales-book-export.spec.ts` : « exporte le xlsx de la période choisie et confirme la génération du fichier » | ✅ auto |
| 1 | idem, échec | Le serveur refuse (403) | toast d'échec, bouton réactivé | `sales-book-export.spec.ts` : « signale l'échec et rend la main pour un nouvel essai » | ✅ auto |
| 1 | idem, PDF | `GET …/sales-book/export/pdf` | 200 `application/pdf` (`%PDF`) | `SalesBookIT.exportSalesBookPdf_byAdmin_returnsAPdfFile` ; écran : « exporte le PDF par la route jumelle /export/pdf » | ✅ auto |
| 2 | Cohérence avec les avoirs (hypothèse) | Facture + avoir du même jour | liste JSON : 2 lignes, Facture `Contre-passée` puis Avoir en négatif (sous-total, total), `invoiceNumber`, `reason` | `SalesBookIT.getSalesBook_reversedInvoice_listsTheInvoiceAndItsCreditNoteInNegative` | ✅ auto |
| 2 | idem, solde | Somme des totaux des deux lignes | 0 | `SalesBookIT.getSalesBook_reversedInvoice_netTotalOfItsTwoLinesIsZero` | ✅ auto |
| 2 | Facture non contre-passée | Une facture `VALIDATED` | 1 ligne « Facture », « Validée », instantané (canal, vendeuse, règlement, montants), pas de motif | `SalesBookIT.getSalesBook_validInvoice_listsOneInvoiceLineWithItsSnapshot` | ✅ auto |
| 3 | Période | Jour avant / jour après la facture | 0 ligne (bornes incluses : le jour même la contient) | `SalesBookIT.getSalesBook_periodAfterTheInvoice_doesNotContainIt`, `SalesBookIT.getSalesBook_periodEndingTheDayBefore_doesNotContainIt` | ✅ auto |
| 4 | Liste bornée | Page de taille 1 sur facture + avoir | 2 pages, ordre stable Facture puis Avoir | `SalesBookIT.getSalesBook_pageSizeOne_servesTheTwoLinesOnTwoPagesInOrder` | ✅ auto |

Tests unitaires : `SalesBookServiceImplTest` (période à l'envers, 367 jours refusés sans lecture, 366 jours et 1 jour acceptés, tri par défaut avec départage, tri inconnu ignoré, recherche nettoyée). Frontend : `sales-book-export.spec.ts` (mois en cours par défaut, bonne route, période invalide, 366 / 367 jours, date vidée).

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton | 401 `UNAUTHENTICATED` | `SalesBookIT.getSalesBook_withoutToken_returns401` | ✅ auto |
| Vendeuse (liste et export) | 403 `FORBIDDEN` | `SalesBookIT.getSalesBook_withSellerRole_returns403`, `SalesBookIT.exportSalesBook_withSellerRole_returns403` | ✅ auto |
| Livreur | 403 | `SalesBookIT.exportSalesBook_withDeliveryDriverRole_returns403` | ✅ auto |
| Période absente | 400 `MALFORMED_REQUEST` | `SalesBookIT.exportSalesBook_withoutPeriod_returns400` | ✅ auto |
| Date mal formée | 400 `MALFORMED_REQUEST` | `SalesBookIT.getSalesBook_malformedDate_returns400` | ✅ auto |
| Début après fin | 422 `VALIDATION_FAILED` | `SalesBookIT.getSalesBook_fromAfterTo_returns422` | ✅ auto |
| Période de 367 jours | 422 | `SalesBookIT.exportSalesBook_periodOf367Days_returns422` | ✅ auto |
| Plus de 10 000 lignes demandées (`size`) | 422, aucun fichier partiel | `SalesBookIT.exportSalesBook_sizeAboveMaxRows_returns422` | ✅ auto |
| Champ de tri hors liste blanche | ignoré (tri par jour) | `SalesBookIT.getSalesBook_unknownSortField_isIgnored` | ✅ auto |
| Champ interdit dans le corps (`role`, `id`) | sans objet : lecture seule, aucun corps | n/a | n/a |
| Ressource introuvable / conflit | sans objet : une période sans ligne donne une page vide | n/a | n/a |

## Cas limites et hors ligne

- Période de 366 jours (acceptée), d'un seul jour (acceptée) : `SalesBookIT.getSalesBook_periodOf366Days_returns200`, `SalesBookIT.getSalesBook_oneDayPeriod_returns200`.
- Période sans ligne : export avec la ligne d'en-têtes seule (`SalesBookIT.exportSalesBook_noLineInThePeriod_returnsTheHeaderRowOnly`), liste vide (`SalesBookIT.getSalesBook_searchNoMatch_returnsAnEmptyPage`).
- Une facture annulée (`CANCELLED`) sans avoir garde sa ligne (le numéro reste visible) mais compte 0 : cas rare (FAC-04 émet l'avoir à l'annulation), non couvert par un test d'intégration ; à vérifier en recette sur la vue `sales_book_entries`.
- Un avoir est daté du jour comptable de la facture (copié à l'émission, la journée de la vente doit être ouverte) : le livre d'une période close ne change plus après coup.
- Hors ligne : l'export est une lecture déclenchée à l'écran, rien à mettre en file ni à rejouer (`Idempotency-Key` sans objet) ; hors connexion le téléchargement échoue avec le toast « Export impossible : réessayez ».
- Journal : l'export n'est pas tracé (pas d'action `EXPORT` dans le journal, voir le guide de SYS-01).

## Résultat de l'exécution

- Date : 11/11/2026
- Environnement : CI GitHub (ci.yml, conventions.yml, Qodana : 0 annotation) sur c040afc9 ; backend 870 tests, 0 échec (dont `SalesBookIT` 22 et `SalesBookServiceImplTest` 7) ; frontend Vitest vert ; rien lancé en local (consigne n°3)
- Anomalies ouvertes : aucune connue
