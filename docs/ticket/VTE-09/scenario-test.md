# Scénarios de test — VTE-09 · Détail d'une vente

Statut : ✅ tests exécutés en CI · Ticket : [#70](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/70) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | administrateur ; vendeuse « Miora Rakoto » (taux personnel 5 %) et sa collègue « Lova Rabe » ; livreur (403) |
| Données de départ | journée du jour ouverte ; article « Savon coco » à 8 500 Ar, 20 en stock Boutique ; canal de test `IT-xxxxxxxx` créé par le test ; règle de commission « Règle test VTE-09 » 4 % sur le CA, posée seulement dans le scénario 1 |
| Tickets à avoir déployés avant | VTE-01 (vente), VTE-07 (liste), FAC-01 (facture), COM-01 / COM-02 (règle et calcul de la commission) |

Ce que `/sales` affichait déjà (vérifié avant de coder) : la ligne de la vente (référence, heure, canal, vendeuse, articles, règlement, promo, total) et le panneau facture. L'écart livré : prix unitaires, nom de la promotion, numéro de facture, et surtout la commission avec la règle appliquée.

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Exemple | Résultat attendu | Test auto | Statut |
|---|---|---|---|---|---|
| 1 | Le détail inclut la règle de commission appliquée. | Règle du canal 4 % sur le CA ; Miora vend 3 × 8 500 Ar = 25 500 Ar ; l'administrateur lit `GET /api/v1/sales/{id}/detail` | `commission` : `rateSource` `SALES_CHANNEL`, `ruleId` et `ruleName` « Règle test VTE-09 », `base` `REVENUE`, `baseAmount` 25 500, `rate` 4, `amount` 1 020, `status` `CALCULATED` ; lignes nommées avec prix unitaire 8 500 ; facture `FAC-…` | `SaleDetailIT.getDetail_administratorWithChannelRule_returnsLinesInvoiceAndTheRuleApplied` | ✅ auto |
| 1b | idem (taux de la vendeuse) | Aucune règle sur le canal ; 2 × 8 500 = 17 000 Ar | `rateSource` `SELLER`, `ruleId` et `ruleName` absents, taux 5, montant 850 | `SaleDetailIT.getDetail_noRuleOnTheChannel_commissionComesFromTheSellerRateWithoutRuleName` | ✅ auto |
| 1c | idem (assemblage) | Vente à 25 500 Ar, promotion « Soldes de novembre » (code `NOV10`), facture `FAC-2026-0042`, règle « Boutique 4 % » | Canal « Boutique », vendeuse « Miora Rakoto », promotion, article « Savon coco » (`ART-0001`), facture et nom de la règle | `SaleDetailServiceImplTest.getDetail_administratorOnASaleWithPromotionInvoiceAndChannelRule_assemblesEverything` | ✅ auto |
| 1d | idem (blocs absents) | Vente sans promotion ni facture, taux de la vendeuse | `promotion`, `invoice` nuls ; aucun appel au service des promotions ni des règles | `SaleDetailServiceImplTest.getDetail_sellerRateWithoutPromotionInvoiceOrRule_leavesThoseBlocksNull` | ✅ auto |
| 1e | idem (vente sans vendeuse) | Vente créditée à personne (commande livrée, LIV-03) | `sellerId`, `sellerName` et `commission` nuls | `SaleDetailServiceImplTest.getDetail_saleCreditedToNobody_hasNoSellerAndNoCommission` | ✅ auto |
| 1f | idem (écran) | Vente sélectionnée dans `/sales` | Panneau « Détail de la vente » : lignes avec prix unitaire, promotion, numéro de facture ; bloc « Commission » : origine du taux (« Règle du canal · Boutique 4 % »), base, taux, montant, statut | `sale-detail-panel.spec.ts` « affiche les lignes avec leur prix unitaire, la promotion et le numéro de facture », « affiche la règle de commission appliquée : origine du taux, nom de la règle, base, taux et montant » | ✅ auto |
| 1g | idem (écran, taux de la vendeuse) | `rateSource` `SELLER`, pas de règle | « Taux de la vendeuse », sans nom de règle | `sale-detail-panel.spec.ts` « sans règle de canal, indique le taux de la vendeuse sans nom de règle » | ✅ auto |
| 1h | idem (écran, rien à montrer) | `commission`, `promotion`, `invoice` nuls | « Aucune commission… », « Aucune promotion », « Aucune facture émise » | `sale-detail-panel.spec.ts` « explique l'absence de commission, de promotion et de facture » | ✅ auto |
| 1i | idem (appel API) | `salesService.detail('vente-42')` | `GET /api/v1/sales/vente-42/detail` | `sales-service.spec.ts` « lit le détail d'une vente par son identifiant » | ✅ auto |
| 2 | Une vendeuse lit sa commission (user story VTE-07 / COM-04) | Miora lit sa propre vente de 8 500 Ar | 200, commission 425 Ar (5 %), facture | `SaleDetailIT.getDetail_seller_readsHerOwnSaleWithHerCommission` | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test auto | Statut |
|---|---|---|---|---|
| Appel sans jeton | `GET /api/v1/sales/{id}/detail` sans `Authorization` | 401 `UNAUTHENTICATED` | `SaleDetailIT.getDetail_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `SaleDetailIT.getDetail_deliveryDriver_returns403` | ✅ auto |
| Vente d'une collègue | vente créditée à Lova, lue avec le jeton de Miora | 404 `NOT_FOUND` (pas de fuite d'existence) ; l'administrateur la lit | `SaleDetailIT.getDetail_sellerAskingForAColleaguesSale_returns404`, `SaleDetailServiceImplTest.getDetail_sellerOnAColleaguesSale_throwsNotFoundWithoutReadingTheOtherBlocks` | ✅ auto |
| Vendeuse et vente créditée à personne | vente sans vendeuse lue par Miora | 404 | `SaleDetailServiceImplTest.getDetail_sellerOnASaleCreditedToNobody_throwsNotFound` | ✅ auto |
| Vente inconnue | UUID aléatoire | 404 `NOT_FOUND` | `SaleDetailIT.getDetail_unknownSale_returns404`, `SaleDetailServiceImplTest.getDetail_unknownSale_propagatesNotFound` | ✅ auto |
| Mass assignment, 422, 409 | — | sans objet : lecture seule, ni corps ni paramètre | — | ⏭ sans objet |
| Journal | — | sans objet : une consultation n'écrit pas au journal | — | ⏭ sans objet |
| Facture d'une collègue (port FAC) | `findInvoiceBySaleId` par une vendeuse | 404 comme `getInvoiceBySaleId` ; vente sans facture : vide | `InvoiceServiceImplTest.findInvoiceBySaleId_activeSellerAskingForAColleagueInvoice_returns404`, `findInvoiceBySaleId_saleWithoutInvoice_returnsEmpty`, `findInvoiceBySaleId_activeSellerAskingForHerOwnInvoice_returnsIt` | ✅ auto |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test auto | Statut |
|---|---|---|---|---|
| Canal désactivé depuis, fiche article introuvable | canal absent des canaux actifs, article inconnu | `salesChannelLabel`, `articleName` et `articleCode` nuls ; la vente reste lisible (écran : « — », identifiant de l'article) | `SaleDetailServiceImplTest.getDetail_channelDeactivatedAndArticleGone_hasNullLabelAndNullArticleName` | ✅ auto |
| Coût d'achat | — | jamais exposé (`unitCost` absent du JSON) | `SaleDetailIT.getDetail_administratorWithChannelRule_returnsLinesInvoiceAndTheRuleApplied` | ✅ auto |
| Vente annulée (LIV-05) | commission passée à `CANCELLED` | `status` de la commission `CANCELLED` affiché « Annulée avec la vente » | — (🧪 R3, aucune annulation n'est exposée à l'écran) | 🧪 |
| Vente d'un autre jour | vente du 10/11 sélectionnée puis jour changé | le panneau suit la vente sélectionnée de la page courante (le détail est lu par identifiant) | — (🧪 R2) | 🧪 |
| 404 à l'écran | vente refusée par le serveur | pas de panneau vide : état d'erreur avec « Réessayer » | `sale-detail-panel.spec.ts` « propose de réessayer quand le serveur refuse la vente (404 d'une vente de collègue) » | ✅ auto |
| Hors ligne | réseau coupé | lecture seule : état d'erreur et « Réessayer » (pas de file hors ligne pour une lecture) | — (🧪 R4) | 🧪 |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/saledetail/controller/SaleDetailIT.java` (7), `feature/saledetail/service/impl/SaleDetailServiceImplTest.java` (7), `feature/invoice/service/impl/InvoiceServiceImplTest.java` (3 tests ajoutés)
- Frontend : `valimbavaka-ui/src/app/features/sales/components/sale-detail-panel/sale-detail-panel.spec.ts` (5), `data-access/sales-service.spec.ts` (1 test ajouté)

## Recette manuelle (🧪)

| # | Parcours | Attendu | Statut |
|---|---|---|---|
| R1 | Administrateur : créer une règle de canal 4 % (`/commission-rules`), vendre 3 savons sur ce canal, ouvrir `/sales` puis « Voir » sur la vente | Panneau « Détail de la vente » au-dessus de la facture : « Règle du canal · <nom> », base « Chiffre d'affaires », 4 %, montant, « À valider » | 🧪 |
| R2 | Vendeuse : `/sales`, « Voir » sur l'une de ses ventes | Son détail et sa commission ; aucune vente de collègue dans la liste | 🧪 |
| R3 | Administrateur : valider la commission du mois (COM-03), rouvrir le détail | Statut « Validée » | 🧪 |
| R4 | Couper le réseau puis sélectionner une vente | État d'erreur et « Réessayer » dans le panneau | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-11
- Environnement : CI GitHub sur le commit 46340c07 : `ci.yml` vert (backend 918 tests, 0 échec ; frontend 134 fichiers de test, 843 tests verts), `conventions.yml` vert (Spotless, Checkstyle, Prettier, ESLint), `qodana_code_quality.yml` vert, « Qodana for JVM » sans annotation
- Tests VTE-09 : 17 backend (`SaleDetailIT` 7, `SaleDetailServiceImplTest` 7, `InvoiceServiceImplTest` 3 ajoutés) et 6 frontend (`sale-detail-panel.spec.ts` 5, `sales-service.spec.ts` 1 ajouté), tous verts
- Anomalies ouvertes : aucune connue
- Étapes 🧪 restantes : 4
