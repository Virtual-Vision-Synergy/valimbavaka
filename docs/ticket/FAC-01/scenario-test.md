# Scénarios de test — FAC-01 · Génération automatique de facture

Statut : ✅ rédigé le 2026-11-11 · Ticket : [#71](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/71) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Vendeuse `SELLER` (lit **les factures de ses ventes**), administrateur `ADMIN` (lit toutes les factures), livreur `DELIVERY_DRIVER` (refusé, pas de `sales.read`) |
| Données de départ | Journée du 08/11/2026 **ouverte** (JR-01) ; article fini « Savon coco » (prix 8 500 Ar) avec 10 unités en Boutique (`STORE`) ; vendeuse active Miora Rakoto (canal Boutique, taux 5 %) ; ventes enregistrées au poste (VTE-01) |
| Tickets à avoir déployés avant | VTE-01 (vente, `SaleRecordedEvent`), VTE-02 (poste de caisse), VTE-05 (mode de règlement), VTE-07 (écran `/sales`), JNL-01 (journal), LIV-03 (commande livrée = vente + facture, décision PO du 07/11/2026), LIV-05 (`SaleCancelledEvent`, non traité ici : FAC-04) |

Il n'y a **aucun endpoint d'écriture** : la facture n'est jamais créée par une requête, seulement par l'enregistrement d'une vente. Le mass assignment, le 422 et le 409 ne s'appliquent donc pas aux endpoints de facture (ils restent ceux de `POST /api/v1/sales`, VTE-01).

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Chaque encaissement produit une facture avec numéro unique croissant. | Miora encaisse 3 × Savon coco en Boutique, MVola, puis lit `GET /api/v1/invoices/sales/{saleId}` | 200, facture `FAC-2026-NNNN` (année de la journée), `saleNumber` `VTE-nnnn`, `status` `VALIDATED`, canal « Boutique », vendeuse « Miora Rakoto », `paymentMethod` `MVOLA`, émetteur « By Valimbavaka Cosmetics », sous-total, remise 0 et total 25 500, 1 ligne (« Savon coco », 8 500, 25 500) | `InvoiceIT.createSale_storeSale_issuesAnInvoiceWithTheFrozenContentOfTheSale` | ☐ |
| 1b | idem (numéro croissant) | Deux ventes de suite (Espèces puis Carte) | les deux factures ont des numéros consécutifs : `FAC-2026-0042` puis `FAC-2026-0043` | `InvoiceIT.createSale_twoSalesInARow_getConsecutiveIncreasingNumbers` | ☐ |
| 1c | idem (numéro unique en concurrence) | 6 ventes enregistrées au même instant par 6 appels parallèles | 6 numéros distincts et consécutifs (écart max − min = 5), aucun doublon | `InvoiceIT.createSale_sixSalesAtTheSameTime_getSixDistinctConsecutiveNumbers` | ☐ |
| 1d | idem (sans trou) | Transaction qui prend le n° 1 de l'année 2099 puis est annulée ; deux transactions ensuite | le n° 1 est rendu : `1` (annulée), `1`, puis `2` | `InvoiceIT.nextNumber_transactionRolledBack_givesTheNumberBack` | ☐ |
| 1e | idem (même transaction que la vente) | Vente refusée (409 : 2 demandés, 1 en stock) | aucune facture créée, compteur de l'année inchangé | `InvoiceIT.createSale_notEnoughStock_returns409AndIssuesNoInvoiceNorNumber`, `SaleInvoiceListenerTest.onSaleRecorded_invoiceFails_letsTheErrorRollBackTheSale` | ☐ |
| 1f | idem (numéro attribué par le serveur) | `issueForSale` pour une nouvelle vente, compteur à 42 | facture `FAC-2026-0042` (année de la journée, 4 chiffres minimum), journal `CREATE` | `InvoiceServiceImplTest.issueForSale_newSale_numbersTheInvoiceWithTheYearOfTheBusinessDayAndTracesIt` | ☐ |
| 1g | idem (une seule facture par vente) | L'écouteur reçoit deux fois le même événement | la facture existante est rendue, aucun numéro consommé, aucun doublon de journal | `InvoiceServiceImplTest.issueForSale_saleAlreadyInvoiced_returnsTheExistingInvoiceAndUsesNoNumber` | ☐ |
| 1h | « à l'encaissement » (écouteur) | `SaleRecordedEvent(saleId, "VTE-0042")` | `InvoiceService.issueForSale(saleId)` appelé | `SaleInvoiceListenerTest.onSaleRecorded_recordedSale_issuesItsInvoice` | ☐ |
| 1i | Mode de règlement repris (VTE-05) | Une vente par mode (`ORANGE_MONEY`) | la facture porte `paymentMethod` `ORANGE_MONEY` | `InvoiceIT.createSale_eachPaymentMethodOfTheSale_isReportedOnTheInvoice`, `InvoiceDraftsTest.of_saleOfTwoArticles_copiesNamesAndFrozenPricesOnTheLines` | ☐ |
| 1j | Facture d'une commande livrée (décision PO du 07/11/2026, LIV-03) | Le livreur marque « livrée » une commande de 2 × Savon coco saisie par l'administrateur | la vente canal Livraison a sa facture : `FAC-…`, `ON_DELIVERY`, sans vendeuse, total 17 000 | `DeliveryCompletionIT.markDelivered_ownOrder_issuesTheInvoiceOfTheDeliverySaleWithoutSeller`, `InvoiceIT.createSale_deliveryChannelEnteredByTheAdministrator_isInvoicedWithTheDeliveryChannel` | ☐ |
| 1k | Document figé (conformité) | Après la vente, l'article est renommé « Renommé » et son prix passe à 9 900 | la facture garde le nom et le prix d'origine (8 500, total 8 500) | `InvoiceIT.getInvoiceById_afterAnArticleRenameAndAPriceChange_keepsTheIssuedContent` | ☐ |
| 1l | Contenu de la facture (domaine) | Vente de 2 lignes (17 000 + 12 000), remise 1 500 | la facture copie les montants (29 000 / 1 500 / 27 500), l'ordre des lignes, vendeuse, règlement `MVOLA`, statut `VALIDATED` ; sans vendeuse ni NIF : `null` | `InvoiceTest.issue_saleWithTwoLines_copiesTheSnapshotAndKeepsTheLineOrder`, `InvoiceTest.issue_saleWithoutSeller_keepsNoSellerOnTheInvoice`, `InvoiceTest.getLines_afterIssue_isReadOnly` | ☐ |
| 1m | Contenu de la facture (noms lus à l'émission) | Canal « Boutique », vendeuse « Miora Rakoto », 2 articles, NIF `4001234567` ; puis vente sans vendeuse sur un canal désactivé, NIF `"  "` ; puis article inconnu du catalogue | noms et NIF copiés ; canal, vendeuse et NIF vides sans erreur ; libellé = identifiant de l'article | `InvoiceDraftsTest.of_saleOfTwoArticles_copiesNamesAndFrozenPricesOnTheLines`, `of_saleWithoutSellerOnADeactivatedChannelAndBlankNif_leavesThemEmpty`, `of_articleUnknownToTheCatalogue_keepsItsIdAsLabelInsteadOfFailing` | ☐ |
| 1n | Journal (JNL-01) | Vente à 17 000 Ar réglée par carte | ligne `INVOICE` / `CREATE` : `Facture FAC-2026-0042 · vente VTE-0042 · 17000.00 Ar · Carte` | `InvoiceIT.createSale_validSale_tracesTheInvoiceInTheJournal` | ☐ |
| 1o | Écran `/sales` : facture émise | « Voir » sur une vente qui a sa facture | le panneau affiche `FAC-2026-0042` (titre et document), la date `jj/mm/aaaa HH:MM`, les lignes figées, pas de mention « aperçu » | `invoice-panel.spec.ts` « affiche le numéro de la facture émise et les lignes figées à l'émission », `sales-mapping.spec.ts` « prépare le document de la facture émise… » | ☐ |
| 1p | Écran `/sales` : vente sans facture | « Voir » sur une vente antérieure à FAC-01 (404) | aperçu « À émettre » et note « Aucune facture n'a été émise… » | `invoice-panel.spec.ts` « montre l'aperçu « À émettre » et son explication quand la vente n'a pas de facture » | ☐ |
| 1q | Écran (statut) | Facture `VALIDATED`, puis `CANCELLED` / `REVERSED` | badge « Payée », puis « Avoir » | `sales-mapping.spec.ts` « montre l'avoir d'une facture dont la vente est %s » | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Appel sans jeton | `GET /api/v1/invoices/sales/{id}` et `GET /api/v1/invoices/{id}` sans `Authorization` | 401 `UNAUTHENTICATED` | `InvoiceIT.getInvoiceBySale_withoutToken_returns401`, `getInvoiceById_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `InvoiceIT.getInvoiceBySale_deliveryDriver_returns403`, `getInvoiceById_deliveryDriver_returns403` | ☐ |
| Facture d'une collègue | jeton de Lova, facture d'une vente de Miora | 404 `NOT_FOUND` (pas de fuite) ; l'administrateur la lit (200) | `InvoiceIT.getInvoiceBySale_sellerAskingForAColleagueInvoice_returns404ButAdministratorReadsIt`, `InvoiceServiceImplTest.getInvoiceBySaleId_activeSellerAskingForAColleagueInvoice_returns404`, `…_activeSellerAskingForHerOwnInvoice_returnsIt`, `…_activeSellerAskingForAnInvoiceWithoutSeller_returns404`, `getInvoiceById_administrator_readsAnyInvoice` | ☐ |
| Ressource introuvable | vente ou facture inconnue (UUID aléatoire) | 404 `NOT_FOUND` | `InvoiceIT.getInvoiceBySale_unknownSale_returns404`, `getInvoiceById_unknownInvoice_returns404`, `InvoiceServiceImplTest.getInvoiceById_unknownInvoice_returns404` | ☐ |
| Identifiant mal formé | `GET /api/v1/invoices/sales/pas-un-uuid` | 400 `MALFORMED_REQUEST` | `InvoiceIT.getInvoiceBySale_malformedId_returns400` | ☐ |
| Mass assignment | — | sans objet : aucun corps (lecture seule) ; la facture ne vient que de la vente | — | ⏭ sans objet |
| Données invalides (422) | — | sans objet : aucune saisie ; les 422 de la vente restent ceux de VTE-01 | — | ⏭ VTE-01 |
| Doublon / conflit (409) | — | sans objet : un second événement pour la même vente rend la facture existante (1g) ; la contrainte `uq_invoices_sale_id` l'interdit aussi en base | `InvoiceServiceImplTest.issueForSale_saleAlreadyInvoiced_…` | ⏭ 1g |
| Journal | vente enregistrée | `INVOICE` / `CREATE` écrit dans la transaction de la vente (1n) | `InvoiceIT.createSale_validSale_tracesTheInvoiceInTheJournal` | ☐ |
| Architecture | `sale` ne dépend pas de `invoice` ; pas de cycle entre features ; domaines indépendants | règles ArchUnit vertes | `ArchitectureTest` (`no_cycles_between_features`, `feature_domains_do_not_depend_on_each_other`, `repositories_are_used_by_service_implementations_only`) | ☐ |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Deux caisses encaissent en même temps | 6 ventes en parallèle | numéros distincts, consécutifs ; la ligne du compteur est verrouillée jusqu'à la fin de la transaction de chaque vente | `InvoiceIT.createSale_sixSalesAtTheSameTime_getSixDistinctConsecutiveNumbers` | ☐ |
| Vente annulée après coup (annulation à chaud, LIV-05) | `SaleCancelledEvent` | la facture reste `VALIDATED` (le numéro n'est jamais repris, un document émis ne disparaît pas) ; l'avoir est FAC-04 | — | ⏭ FAC-04 |
| Changement d'année | vente du 02/01/2027 | repart à `FAC-2027-0001` (compteur par année de la journée ouverte) | — | 🧪 recette |
| Vente sans vendeuse (port LIV-03) | commande livrée saisie par l'administrateur | facture sans vendeuse (`sellerName` `null`, affichée « Sans vendeuse ») ; invisible d'une vendeuse | `InvoiceTest.issue_saleWithoutSeller_keepsNoSellerOnTheInvoice`, `InvoiceServiceImplTest.getInvoiceBySaleId_activeSellerAskingForAnInvoiceWithoutSeller_returns404` | ☐ |
| NIF non configuré | `INVOICE_ISSUER_NIF` vide | `issuerNif` `null`, le document affiche « [à compléter] » | `InvoiceDraftsTest.of_saleWithoutSellerOnADeactivatedChannelAndBlankNif_leavesThemEmpty` | ☐ |
| Ventes existantes avant FAC-01 | vente de `develop` sans facture | 404 sur l'API ; l'écran montre l'aperçu et la note (1p) ; pas de rattrapage automatique | `invoice-panel.spec.ts` (1p) | ⏭ FAC-02 |
| Hors ligne | vente saisie hors connexion puis rejouée (`Idempotency-Key`) | la facture est créée au rejeu, avec la vente ; un rejeu identique renvoie la même vente (donc la même facture) | — | ⏭ VTE-01 / SYS-12 |

## Tests existants

- Backend : `feature/invoice/domain/InvoiceTest.java`, `feature/invoice/service/impl/InvoiceServiceImplTest.java`, `InvoiceDraftsTest.java`, `SaleInvoiceListenerTest.java`, `feature/invoice/controller/InvoiceIT.java`, `feature/delivery/controller/DeliveryCompletionIT.java` (1 test ajouté), `ArchitectureTest.java` (règles inchangées).
- Frontend : `features/sales/data-access/invoice-service.spec.ts`, `sales-mapping.spec.ts` (4 tests ajoutés), `features/sales/components/invoice-panel/invoice-panel.spec.ts`.

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Encaisser une vente au poste (`/pos`), puis ouvrir `/sales` et cliquer « Voir » | la facture `FAC-aaaa-nnnn` s'affiche avec la date, les articles, le règlement et le total ; le numéro suit celui de la facture précédente | 🧪 recette |
| Imprimer (bouton « Imprimer ») | la facture seule est imprimée (fond blanc), numéro compris | 🧪 recette |
| Se connecter en vendeuse et ouvrir la facture d'une vente d'une collègue (`/sales?sale=VTE-nnnn`) | aperçu « À émettre » avec la note (pas de fuite de la facture) | 🧪 recette |
| Marquer une commande « livrée » (`/round`) puis ouvrir sa vente | la facture existe, sans vendeuse, règlement « À la livraison » | 🧪 recette |

## Résultat de l'exécution

- Date : à lancer (CI ou local) ; aucun test n'a été exécuté au moment de la rédaction (Maven Central inaccessible dans l'environnement de rédaction).
- Environnement : à lancer (CI ou local).
- Anomalies ouvertes : aucune connue ; voir « Points d'attention » du [guide](guide.md) (ventes antérieures sans facture, facture non annulée par `SaleCancelledEvent`).
- Étapes 🧪 restantes : 5.
