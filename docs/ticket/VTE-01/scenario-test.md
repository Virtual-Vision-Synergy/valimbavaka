# Scénarios de test — VTE-01 · Enregistrement d'une vente multicanal

Statut : ☐ tests écrits, à lancer en CI · Ticket : [#62](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/62) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Classes : `SaleIT` (`valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/sale/controller/SaleIT.java`), `SaleTest` (`…/sale/domain/SaleTest.java`), `SalePolicyTest`, `SaleLedgerTest`, `SaleServiceImplTest` (`…/sale/service/impl/`) ; front `pos-mapping.spec.ts`, `pos-service.spec.ts` (`valimbavaka-ui/src/app/features/pos/data-access/`).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Vendeuse `SELLER` (saisit et est créditée), administrateur `ADMIN` (saisit pour une vendeuse), livreur `DELIVERY_DRIVER` (refusé) |
| Données de départ | Journée du jour **ouverte** (JR-01, `POST /api/v1/days`) ; article fini « Savon noir 100 g » `ART-0031`, prix 8 500 Ar, coût 3 120,50 Ar, gamme Botanika ; 10 unités en Boutique (`STORE`) ; canaux `STORE`, `FACEBOOK`, `PHONE`, `DELIVERY` ; vendeuse active Miora Rakoto (canal Boutique, taux 5 %) |
| Tickets à avoir déployés avant | ART-01, AUTH-03, JR-01 (branche de base, PR empilée), STK-01, JNL-01, JNL-02, SYS-05 |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Le formulaire permet de choisir canal, vendeuse, paiement et lignes d'articles. | Vendeuse connectée, `GET /api/v1/sales/form` | 200 : canaux actifs avec leur emplacement (`STORE` et `PHONE` → Boutique), vendeuses actives (prénom, nom, sans email), articles finis actifs avec prix 8 500 et stock Boutique 6 | `SaleIT.getForm_seller_returnsChannelsWithTheirLocationSellersAndSellableArticlesWithStock` | ☐ |
| 1b | idem | Poste de caisse : formulaire du serveur + gammes | Le poste affiche le stock de la Boutique, la vendeuse connectée est proposée d'office, les canaux Boutique / Facebook / Téléphone sont reliés à leur id serveur | `pos-mapping.spec.ts` « affiche le stock de la boutique, les vendeuses et les canaux du poste », `pos-service.spec.ts` « lit le formulaire de vente et les gammes pour préparer le poste » | ☐ |
| 1c | idem | Administrateur sur le poste | Aucune vendeuse proposée d'office : il doit la choisir (encaissement bloqué sinon) | `pos-mapping.spec.ts` « ne propose aucune vendeuse d'office à un administrateur » | ☐ |
| 1d | idem | Vente Boutique : 3 × Savon noir, Espèces, vendeuse Miora | 201 + `Location`, numéro `VTE-nnnn`, statut `VALIDATED`, prix unitaire 8 500, total ligne 25 500, sous-total 25 500, remise 0, total 25 500 ; stock Boutique 10 → 7 | `SaleIT.createSale_storeChannel_returns201AndTakesTheStockOutOfTheStore` | ☐ |
| 1e | idem | Vente Facebook, 2 unités, MVola | Le stock sort de la Boutique (5 → 3) | `SaleIT.createSale_facebookChannel_takesTheStockOutOfTheStore` | ☐ |
| 1f | idem (décision PO du 07/11/2026, LIV-03) | Vente canal Livraison, règlement `ON_DELIVERY`, saisie par l'administrateur | Le stock sort de l'emplacement Livraison (4 → 3) | `SaleIT.createSale_deliveryChannel_takesTheStockOutOfTheDeliveryLocation` | ☐ |
| 1g | idem | Corps envoyé par le poste | Canal, vendeuse, règlement et quantités, **aucun prix** | `pos-mapping.spec.ts` « envoie canal, vendeuse, règlement et quantités sans aucun prix », `pos-service.spec.ts` « enregistre la vente par la file hors ligne (POST /api/v1/sales) » | ☐ |
| 1h | Prix figés | Vente à 8 500, puis prix de l'article passé à 9 900 | La vente relue garde 8 500 ; le coût d'achat n'est pas exposé | `SaleIT.getSaleById_afterAPriceChange_keepsTheFrozenPrice` | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Appel sans jeton | `POST /api/v1/sales` sans `Authorization` | 401 `UNAUTHENTICATED` | `SaleIT.createSale_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton livreur sur `POST /api/v1/sales` et `GET /api/v1/sales/form` | 403 `FORBIDDEN` | `SaleIT.createSaleAndForm_deliveryDriver_return403` | ☐ |
| Mass assignment | `id`, `number":"VTE-9999"`, `status":"CANCELLED"`, `total":1`, `unitPrice":1` dans le corps | ignorés : id et numéro du serveur, `VALIDATED`, prix 8 500 | `SaleIT.createSale_serverFieldsInTheBody_areIgnored` | ☐ |
| Données invalides | aucune ligne, quantité `0`, `paymentMethod` absent | 422 `VALIDATION_FAILED` | `SaleIT.createSale_noLineOrZeroQuantityOrMissingPayment_returns422` | ☐ |
| Article désactivé | Savon noir désactivé | 422 | `SaleIT.createSale_inactiveArticle_returns422`, `SalePolicyTest.check_inactiveArticle_throwsBusinessException` | ☐ |
| Matière première | article `RAW_MATERIAL` | 422 « n'est pas vendable » | `SalePolicyTest.check_rawMaterial_throwsBusinessException` | ☐ |
| Même article deux fois | 1 + 2 Savon noir | 422 | `SaleIT.createSale_sameArticleTwice_returns422`, `SalePolicyTest.check_sameArticleTwice_throwsBusinessException` | ☐ |
| Vente créditée à un non-vendeur | `sellerId` = administrateur | 422 | `SaleIT.createSale_creditedToAnAdministrator_returns422`, `SalePolicyTest.check_inactiveSeller_throwsBusinessException` | ☐ |
| Canal inconnu | `salesChannelId` aléatoire | 422 | `SaleIT.createSale_unknownChannel_returns422` | ☐ |
| Article inconnu | `articleId` aléatoire | 404 `NOT_FOUND` | `SaleIT.createSale_unknownArticle_returns404` | ☐ |
| Vente inconnue | `GET /api/v1/sales/{id}` aléatoire | 404 `NOT_FOUND` | `SaleIT.getSaleById_unknownSale_returns404` | ☐ |
| Stock insuffisant | 2 demandés, 1 en Boutique | 409 `CONFLICT`, aucune vente, stock inchangé | `SaleIT.createSale_notEnoughStock_returns409AndRecordsNothing` | ☐ |
| Journée non ouverte | aucune journée du jour | 409 « La journée du … n'est pas ouverte. », stock inchangé | `SaleIT.createSale_dayNotOpened_returns409`, `SaleServiceImplTest.record_dayNotOpened_throwsConflictAndRecordsNothing` | ☐ |
| Journal | vente Carte de 2 × 8 500 | ligne `CREATE` / `SALE` « Vente VTE-nnnn · Boutique · 1 article(s) · 17000.00 Ar · Carte » ; un mouvement `SALE` de −2 référencant la vente | `SaleIT.createSale_validSale_isTracedInTheJournalWithOneSaleMovementPerLine`, `SaleLedgerTest.post_twoLines_takesTheStockOutOfTheChannelLocationTracesAndPublishes` | ☐ |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Arrondi d'une ligne | 1,5 kg × 3 333,33 Ar = 4 999,995 | 5 000,00 (HALF_UP, 2 décimales) | `SaleTest.record_decimalQuantity_roundsTheLineTotalHalfUpToTwoDecimals` | ☐ |
| Totaux et coût figé | 3 × 8 500 + 1 × 12 000 (coût 7 000) | lignes 25 500,00 et 12 000,00, total 37 500, coût 7 000 conservé | `SaleTest.record_twoLines_freezesThePricesAndAddsTheLineTotals` | ☐ |
| Prix courant / prix convenu (port LIV-03) | ligne sans prix ; ligne à 8 000 | 8 500 (fiche) ; 8 000 (commande) | `SalePolicyTest.check_finishedActiveArticle_pricesTheLineWithTheCurrentPriceAndCost`, `SalePolicyTest.check_agreedPrice_keepsThePriceOfTheCommand` | ☐ |
| Prix convenu à 3 décimales | 8 000,005 | 422 | `SalePolicyTest.check_agreedPriceWithThreeDecimals_throwsBusinessException` | ☐ |
| Bornes des lignes | 0 ligne ; 101 lignes | 422 (1 à 100) | `SalePolicyTest.check_noLine_throwsBusinessException`, `SalePolicyTest.check_hundredAndOneLines_throwsBusinessException` | ☐ |
| Quantité nulle | 0 | 422 | `SalePolicyTest.check_zeroQuantity_throwsBusinessException` | ☐ |
| Journée, emplacement, vendeuse | vente Facebook du 08/11/2026, carte | vente rattachée à la journée ouverte, emplacement Boutique, vendeuse et règlement conservés ; ligne envoyée sans prix convenu | `SaleServiceImplTest.createSale_openDay_recordsTheSaleInTheDayAndTakesTheStockOfTheChannelLocation` | ☐ |
| Gamme inconnue côté poste | article sans gamme connue | non proposé au catalogue | `pos-mapping.spec.ts` « écarte un article dont la gamme est inconnue » | ☐ |
| Hors ligne | encaissement sans réseau | saisie en file (`Idempotency-Key`), toast « Saisie enregistrée hors connexion » ; le serveur numérote au rejeu ; un rejeu avec la même clé ne crée pas de seconde vente (SYS-02) | `pos-service.spec.ts` « enregistre la vente par la file hors ligne (POST /api/v1/sales) » (envoi par l'outbox) | ☐ |
| Double F12 | deux appuis rapides | un seul envoi (`submitting`) | — (🧪 recette) | 🧪 |
| Deux caisses en même temps | deux ventes du dernier article | la seconde reçoit 409 (verrou `pg_advisory_xact_lock` de STK-01) | — (couvert par STK-01) | ⏭ STK-01 |

## Recette manuelle (🧪)

| # | Parcours | Attendu | OK |
|---|---|---|---|
| R1 | Journée ouverte, vendeuse connectée : `/pos`, ajouter 2 articles, canal Facebook, MVola, encaisser | Toast « Vente VTE-nnnn encaissée · … », stock Boutique diminué dans `/stocks` | 🧪 |
| R2 | Administrateur : `/pos` sans choisir de vendeuse, F12 | Toast « Choisissez la vendeuse avant d'encaisser » ; après choix, la vente est créditée à la vendeuse choisie | 🧪 |
| R3 | Journée non ouverte : encaisser | Toast « Vente refusée : La journée du … n'est pas ouverte. », panier conservé | 🧪 |
| R4 | Couper le réseau, encaisser, rétablir | Toast hors ligne, puis vente visible au journal après synchronisation | 🧪 |

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : CI GitHub (`ci.yml`, `conventions.yml`)
- Anomalies ouvertes : aucune connue à ce stade
- Étapes 🧪 restantes : 5
