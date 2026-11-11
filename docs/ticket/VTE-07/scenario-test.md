# Scénarios de test — VTE-07 · Consultation des ventes du jour

Statut : ✅ rédigé le 2026-11-08 · Ticket : [#68](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/68) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Vendeuse `SELLER` (lit **ses** ventes), administrateur `ADMIN` (lit toutes les ventes, filtre par vendeuse et canal), livreur `DELIVERY_DRIVER` (refusé, pas de `sales.read`) |
| Données de départ | Journée du 08/11/2026 **ouverte** (JR-01) ; article fini « Savon coco » (prix 8 500 Ar) avec 20 unités en Boutique (`STORE`) ; vendeuses actives Miora Rakoto et Lova Rabe (canal Boutique, taux 5 %) ; ventes saisies au poste (VTE-01) : Boutique / Espèces / 2 × Savon coco = 17 000 Ar, Facebook / MVola |
| Tickets à avoir déployés avant | VTE-01 (PR de `feature/VTE-01-enregistrement-vente-multicanal`, base de cette PR empilée), SYS-05 (canaux), AUTH-03 (vendeuses), ART-01 (fiches articles) |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Chaque vente affiche référence, articles, paiement, promo, total et accès facture. | Miora a vendu 2 × Savon coco en Boutique, Espèces ; elle appelle `GET /api/v1/sales` | 200, 1 vente : `number` `VTE-nnnn`, `createdAt`, `salesChannelLabel` « Boutique », `sellerName` « Miora Rakoto », `paymentMethod` `CASH`, `status` `VALIDATED`, `subtotal` 17 000, `discountAmount` 0, `total` 17 000, `lines[0]` : `articleName` « Savon coco… », `quantity` 2, `lineTotal` 17 000 ; coût d'achat absent | `SaleQueryIT.getSales_seller_listsHerSaleWithArticlesPaymentPromoAndTotal` | ✅ auto |
| 1b | idem (noms) | Page de 2 ventes de la même vendeuse, 2 articles | Lignes dans l'ordre de la page (`VTE-0042` puis `VTE-0041`), canal « Boutique », vendeuse « Miora Rakoto », « Savon coco » et « Huile de ricin » ; vendeuse et articles lus **une seule fois** par page | `SaleSummaryAssemblerTest.toRDtos_twoSalesOfOneSeller_keepsThePageOrderAndReadsEachNameOnce` | ✅ auto |
| 1c | idem (écran) | Ligne affichée au tableau | Référence `VTE-0042`, heure `HH:MM`, canal, vendeuse, articles « Savon coco ×2 · Huile de ricin ×1 », règlement « MVola », promo « −1 500 Ar » (sinon « — »), total | `sales-mapping.spec.ts` « construit la ligne du tableau : référence, canal, vendeuse, articles, règlement, promo et total », « résume les articles de la vente avec leur quantité » | ✅ auto |
| 1d | idem (accès facture) | Bouton « Voir » de la colonne Facture (`aria-label` « Afficher la facture de VTE-0042 ») | Panneau facture : aperçu construit depuis la vente (statut « En attente », numéro « À émettre », lignes, sous-total, remise, total) ; une vente contre-passée montre « Avoir » | `sales-mapping.spec.ts` « prépare l'aperçu de facture non émise depuis la vente », « montre l'avoir d'une vente contre-passée » | ✅ auto |
| 1e | « mes ventes » (user story vendeuse) | Lova (collègue) a une vente ; Miora appelle `GET /api/v1/sales?sellerId=<Lova>` | 200, 0 vente : le serveur impose l'id de la vendeuse connectée | `SaleQueryIT.getSales_seller_neverSeesTheSalesOfAColleagueEvenWhenAskingForThem`, `SaleQueryServiceImplTest.getSales_activeSeller_readsOnlyTheSalesCreditedToHer` | ✅ auto |
| 1f | Filtres (administrateur) | Miora : 1 vente Boutique + 1 vente Facebook ; l'admin filtre `sellerId`, puis `sellerId` + `salesChannelId` Facebook | 2 ventes, puis 1 (Facebook, MVola) | `SaleQueryIT.getSales_administrator_filtersOnTheSellerAndTheChannel`, `SaleQueryServiceImplTest.getSales_administrator_keepsTheRequestedDayChannelAndSeller` | ✅ auto |
| 1g | Filtres (écran) | Jour `2026-11-08`, canal Facebook | `GET /api/v1/sales?page=2&size=10&date=2026-11-08&salesChannelId=…` ; jour vide et « Tous les canaux » ne sont pas envoyés | `sales-service.spec.ts` « lit une page des ventes d'un jour et d'un canal », « n'envoie ni jour ni canal vides : le serveur prend aujourd'hui et tous les canaux » | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Appel sans jeton | `GET /api/v1/sales` sans `Authorization` | 401 `UNAUTHENTICATED` | `SaleQueryIT.getSales_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `SaleQueryIT.getSales_deliveryDriver_returns403` | ✅ auto |
| Vendeuse qui demande les ventes d'une collègue | `?sellerId=<Lova>` avec le jeton de Miora | filtre remplacé par son propre id (0 vente), pas de fuite | `SaleQueryIT.getSales_seller_neverSeesTheSalesOfAColleagueEvenWhenAskingForThem` | ✅ auto |
| Pagination invalide | `size=101` | 422 `VALIDATION_FAILED` | `SaleQueryIT.getSales_sizeBounds_accepts100AndRejects101` | ✅ auto |
| Date mal formée | `date=08-11-2026` | 400 `MALFORMED_REQUEST` | `SaleQueryIT.getSales_malformedDate_returns400` | ✅ auto |
| Mass assignment | — | sans objet : lecture seule (aucun corps) | — | ⏭ sans objet |
| Ressource introuvable | — | sans objet : une liste vide répond 200 (le détail d'une vente reste `GET /api/v1/sales/{id}`, VTE-01) | — | ⏭ VTE-01 |
| Conflit / doublon | — | sans objet : lecture seule | — | ⏭ sans objet |
| Journal | — | sans objet : une consultation n'écrit pas au journal | — | ⏭ sans objet |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test auto | OK |
|---|---|---|---|---|
| Jour par défaut, minuit à Madagascar | horloge 07/11/2026 21:30 UTC = 08/11 00:30 à Antananarivo, sans `date` | ventes du **08/11/2026** (fuseau `Indian/Antananarivo`, comme l'ouverture JR-01) | `SaleQueryServiceImplTest.getSales_withoutDate_readsTodayInMadagascarTimeAndSkipsTheLinesOfAnEmptyPage` | ✅ auto |
| Autre jour | `date=2000-01-01` | 200, `totalCount` 0, `items` vide | `SaleQueryIT.getSales_anotherDay_returnsAnEmptyPage` | ✅ auto |
| Page vide | aucune vente | une seule requête (pas de lecture des lignes) | `SaleQueryServiceImplTest.getSales_withoutDate_readsTodayInMadagascarTimeAndSkipsTheLinesOfAnEmptyPage`, `SaleSummaryAssemblerTest.toRDtos_emptyPage_returnsNoRow` | ✅ auto |
| Bornes de `size` | 100 ; 101 | 200 ; 422 | `SaleQueryIT.getSales_sizeBounds_accepts100AndRejects101` | ✅ auto |
| Dernière page | 2 ventes, `page=2&size=1` | `page` 2, `pageSize` 1, `totalCount` 2, `totalPages` 2, 1 vente | `SaleQueryIT.getSales_pageOfOne_returnsTheCounts`, `SaleQueryServiceImplTest.getSales_secondPage_returnsTheCountsAndTheRowsInThePageOrder` | ✅ auto |
| Tri par défaut et tri demandé | 3 ventes (1, 3 et 2 savons) | plus récente d'abord ; `sortBy=total&sortDirection=ASC` : la plus petite d'abord | `SaleQueryIT.getSales_mostRecentFirstByDefault_andSortOnTheTotalOnDemand`, `SaleQueryServiceImplTest.getSales_sortOnTheTotal_isKept` | ✅ auto |
| Tri hors liste blanche | `sortBy=unitCost` | 200, repli sur `createdAt` décroissant (pas d'erreur) | `SaleQueryIT.getSales_mostRecentFirstByDefault_andSortOnTheTotalOnDemand`, `SaleQueryServiceImplTest.getSales_sortOutsideTheWhitelist_fallsBackToTheMostRecentFirst` | ✅ auto |
| Vente sans vendeuse (port LIV-03) sur un canal désactivé depuis, article sans fiche | `sellerId` `null`, canal Téléphone désactivé | `sellerName` et `salesChannelLabel` nuls (écran : « Sans vendeuse », « — »), article affiché par son id ; la vente n'est pas perdue | `SaleSummaryAssemblerTest.toRDtos_saleWithoutSellerOnADeactivatedChannel_hasNoSellerNameNorChannelLabel`, `sales-mapping.spec.ts` « garde une vente sans vendeuse ni canal actif sans nom… », « garde l'identifiant d'un article dont la fiche est introuvable » | ✅ auto |
| Vente annulée ou contre-passée | statut `CANCELLED` / `REVERSED` | listée avec son badge (« Annulée », « Contre-passée ») : rien n'est caché | — (🧪 R3, aucune annulation n'existe encore : LIV-05 / FAC-04) | 🧪 |
| Jour local de l'écran | 08/11/2026 00:30 heure locale | champ Jour pré-rempli `2026-11-08` (jour local, pas UTC) | `sales-mapping.spec.ts` « donne le jour local au format yyyy-MM-dd » | ✅ auto |
| Hors ligne | réseau coupé | lecture seule : l'écran montre l'état d'erreur avec « Réessayer » (pas de file hors ligne pour une lecture) | — (🧪 R4) | 🧪 |

## Tests existants

- Backend : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/sale/controller/SaleQueryIT.java`, `feature/sale/service/impl/SaleQueryServiceImplTest.java`, `feature/sale/service/impl/SaleSummaryAssemblerTest.java`
- Frontend : `valimbavaka-ui/src/app/features/sales/data-access/sales-service.spec.ts`, `sales-mapping.spec.ts`

## Recette manuelle (🧪)

| # | Parcours | Attendu | OK |
|---|---|---|---|
| R1 | Vendeuse connectée, journée ouverte : 2 ventes au poste, puis `/sales` | Ses 2 ventes, la plus récente en haut, avec articles, règlement, promo « — » et total ; les ventes des collègues n'apparaissent pas | 🧪 |
| R2 | Administrateur : `/sales`, pastille « Facebook », puis changer le jour | Seules les ventes Facebook ; retour en page 1 à chaque changement de filtre | 🧪 |
| R3 | Cliquer « Voir » sur une ligne, puis « Imprimer » | Panneau facture (aperçu « À émettre ») et impression de l'aperçu ; lien `?sale=VTE-nnnn` partageable vers une vente de la page | 🧪 |
| R4 | Couper le réseau puis ouvrir `/sales` | État d'erreur et bouton « Réessayer » | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-11
- Environnement : CI GitHub sur le commit 7d667fb7 : `ci.yml` vert (backend 670 tests, 0 échec ; frontend 112 fichiers de test, tous verts), `conventions.yml` vert (Spotless, Checkstyle, Prettier, ESLint), `qodana_code_quality.yml` vert, « Qodana for JVM » sans annotation
- Tests VTE-07 : 19 backend (`SaleQueryIT` 10, `SaleQueryServiceImplTest` 6, `SaleSummaryAssemblerTest` 3) et 9 frontend (`sales-mapping.spec.ts` 7, `sales-service.spec.ts` 2), exécutés en CI, tous verts
- Anomalies ouvertes : aucune connue
- Étapes 🧪 restantes : 6
