# Scénarios de test — PRO-02 · Application automatique d'une promotion en caisse

Statut : ☐ tests écrits le 2026-11-11, à lancer (CI) · Ticket : [#59](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/59) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ☐ = test écrit, pas encore exécuté · 🔍 code = vérifié par lecture du code (aucun test) · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Classes de test :
- `PromotionPricingTest` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/promotion/domain/PromotionPricingTest.java`
- `PromotionServiceImplTest` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/promotion/service/impl/PromotionServiceImplTest.java`
- `SaleDiscountsTest` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/sale/service/impl/SaleDiscountsTest.java`
- `SaleServiceImplTest` = `…/feature/sale/service/impl/SaleServiceImplTest.java` · `SaleTest` = `…/feature/sale/domain/SaleTest.java`
- `SalePromotionIT` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/sale/controller/SalePromotionIT.java`
- `pos-pricing.spec.ts`, `pos-cart.spec.ts`, `pos-mapping.spec.ts` = `valimbavaka-ui/src/app/features/pos/data-access/`

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `SELLER` (vente et formulaire, `sales.create`), `ADMIN` (création des promotions et des articles) |
| Données de départ | journée ouverte ; article `Savon noir` à 8 500 Ar, stock boutique 30 ; une promotion **sans code** active (J → J+10, J = jour à Antananarivo) sur le canal `STORE` ; canaux `STORE`, `FACEBOOK`, `PHONE` |
| Migration | `2026_11_11-80-changelog.sql` : colonne `sales.promotion_id` (FK `promotions`, nullable) |
| Tickets à avoir déployés avant | PRO-01 et PRO-04 (promotions), VTE-01 (vente), VTE-02 (poste), VTE-03, VTE-05, VTE-06 (poste de caisse) : tous dans `develop` |

**Écart par rapport au ticket** : VTE-01 enregistre `discountAmount` à 0 et VTE-02 ne calcule une remise que sur une promotion codée en dur dans la maquette (`promotion: null` depuis VTE-01). PRO-02 livre donc : le calcul côté serveur (qui décide), la référence de la promotion sur la vente, la remise des promotions réelles dans le formulaire de vente et le même calcul au poste.

## Scénarios issus des critères d'acceptation

Critère : « La remise appliquée correspond à la règle de la promotion active concernée. »

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Pourcentage sur l'article visé (serveur) | 3 savons à 8 500 = 25 500 Ar, promotion `PERCENT` 15 % | `discountAmount` 3 825, `total` 21 675, `promotionId` = la promotion | `SalePromotionIT.createSale_automaticPercentPromotion_appliesItToTheTargetedArticle` | ☐ |
| 1b | Montant fixe retiré une seule fois | 3 savons, `AMOUNT` 2 000 Ar | `discountAmount` 2 000, `total` 23 500 | `SalePromotionIT.createSale_automaticAmountPromotion_takesTheAmountOffTheTotalOnce` | ☐ |
| 1c | N achetés, 1 offert | 7 savons, `BUY_X` 3 | 2 unités offertes : `discountAmount` 17 000 sur 59 500, `total` 42 500 | `SalePromotionIT.createSale_automaticBuyThreePromotion_givesOneFreeUnitPerGroupOfThree` | ☐ |
| 1d | Règle, arrondi et plafond (domaine) | 15 % de 2 × 8 500 + 1 × 12 000 ; 10 % de 3 × 335 (1 005 Ar) ; 12,5 % de 1,5 × 3 333,33 ; 100 % | 4 350 ; 101 (HALF_UP) ; 625 ; remise = total de la ligne | `PromotionPricingTest.discountOf_percentOnTheWholeCatalogue_takesTheRateOfEachLine`, `…_percentHalfAriary_roundsHalfUpToTheAriary`, `…_percentOnDecimalQuantity_usesTheLineTotalRoundedToTheCentime`, `…_percentHundred_neverExceedsTheLineTotal` | ☐ |
| 1e | Montant fixe et BUY_X (domaine) | `AMOUNT` 5 000 / 20 000 / 0,5 sur 17 000 ; `BUY_X` 3 avec 2, 3, 5, 6, 7, 3,5, 2,999 | 5 000 / 17 000 / 1 ; 0, 8 500, 8 500, 17 000, 17 000, 8 500, 0 | `PromotionPricingTest.discountOf_amount_isTakenOnceOffTheEligibleTotalAndCappedAtIt`, `…_buyThree_givesOneFreeUnitPerWholeGroupOfThree` | ☐ |
| 1f | Périmètre : gamme, articles, rien d'atteint | `RANGES` / `ARTICLES` ; article sans gamme ; panier vide | seules les lignes visées comptent ; 0 sinon | `PromotionPricingTest.discountOf_rangesScope_onlyCountsTheLinesOfTheTargetedRange`, `…_articlesScope_onlyCountsTheTargetedArticles`, `…_noLineReached_isZero`, `…_emptyBasket_isZero` | ☐ |
| 1g | Plusieurs promotions : la plus avantageuse, sans cumul | 10 % (2 550 Ar) et 1 000 Ar sur 25 500 Ar | 2 550, `promotionId` de la promotion à 10 % | `SalePromotionIT.createSale_twoAutomaticPromotions_appliesTheMostAdvantageousOneWithoutAccumulating`, `PromotionServiceImplTest.priceBasket_twoPromotionsReachTheBasket_keepsTheOneThatGivesTheLargestDiscount` | ☐ |
| 1h | Égalité de remise | deux `AMOUNT` 1 000 | la première de l'ordre du dépôt (fin la plus proche, puis nom) | `PromotionServiceImplTest.priceBasket_sameDiscount_keepsTheFirstOfTheRepositoryOrder` | ☐ |
| 1i | Canal : la promotion ne vise pas le canal de la vente | promotion `FACEBOOK` seule, vente `STORE` | `discountAmount` 0, pas de `promotionId`, `total` 25 500 | `SalePromotionIT.createSale_promotionOfAnotherChannel_givesNoDiscount`, `PromotionServiceImplTest.priceBasket_promotionOfAnotherChannel_isIgnored` | ☐ |
| 1j | La vente porte la remise décidée par le serveur | remise 2 550 de la promotion P sur 17 000 | `discountAmount` 2 550, `promotionId` = P, `total` 14 450 | `SaleServiceImplTest.createSale_automaticPromotion_recordsTheDiscountAndThePromotionDecidedByTheServer`, `SaleTest.record_withDiscount_subtractsItFromTheSubtotalAndKeepsThePromotion` | ☐ |
| 1k | Poste : même calcul à l'écran (pourcentage, montant, BUY_X) | mêmes exemples qu'en 1d et 1e | mêmes montants que le serveur | `pos-pricing.spec.ts` « arrondit la remise en pourcentage de chaque ligne à l'ariary (HALF_UP) », « retire un montant fixe une seule fois du ticket, borné au total des lignes visées », « offre une unité par groupe complet de N achetés (BUY_X) » | ☐ |
| 1l | Poste : canal et choix de la promotion | promotion Facebook seule, puis canal Facebook ; 15 % contre 1 000 Ar | sans effet sur la boutique, 1 275 Ar sur Facebook ; 15 % retenue | `pos-cart.spec.ts` « applique la promotion seulement sur les canaux qu'elle vise », « choisit la promotion la plus avantageuse quand plusieurs s'appliquent » | ☐ |
| 1m | Poste : ligne ciblée, montant fixe, BUY_X | fixture Octobre rose (15 % sur 2 articles) ; `AMOUNT` 2 000 ; `BUY_X` 2 | 3 825 + 1 875 ; remise sur le ticket, 0 sur les lignes ; 1 unité offerte | `pos-cart.spec.ts` « applique la remise de ligne de la promotion aux seuls articles ciblés », « retire un montant fixe une seule fois du ticket et ne l'affiche sur aucune ligne », « offre une unité par groupe de deux achetées avec une promotion BUY_X » | ☐ |
| 1n | Le formulaire de vente liste les promotions automatiques | promotion 15 % sur le savon, canal `STORE` | `GET /api/v1/sales/form` → `promotions[]` avec `kind`, `value`, `articleIds` (le savon), `channelIds` | `SalePromotionIT.getForm_automaticPromotion_isListedWithTheArticlesItReachesAndItsChannels`, `PromotionServiceImplTest.getAutomaticPromotions_readsTheAutomaticPromotionsActiveOnTheBusinessDay` | ☐ |
| 1o | Les canaux du serveur deviennent ceux du poste | promotion sur `STORE` et `DELIVERY` | `channels: ['SHOP']` (`DELIVERY` n'est pas un canal du poste) | `pos-mapping.spec.ts` « traduit les canaux des promotions en canaux du poste et écarte ceux que le poste ne vend pas » | ☐ |

## Sécurité et erreurs

Aucun endpoint nouveau : `POST /api/v1/sales` et `GET /api/v1/sales/form` (VTE-01, `sales.create`) gardent leurs contrôles, déjà testés dans `SaleIT`.

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `POST /api/v1/sales` | 401 `UNAUTHENTICATED` | `SaleIT.createSale_withoutToken_returns401` (VTE-01) | ☐ |
| Mauvais rôle | jeton livreur, vente et formulaire | 403 `FORBIDDEN` | `SaleIT.createSaleAndForm_deliveryDriver_return403` (VTE-01) | ☐ |
| Mass assignment | corps avec `"discountAmount":9999`, `"promotionId":"<uuid>"`, `"total":1` | champs ignorés : la remise vient du serveur (0 sans promotion), pas de `promotionId`, `total` 25 500 | `SalePromotionIT.createSale_discountAndPromotionSentByTheClient_areIgnored` | ☐ |
| Données invalides, 404, 409 | quantité ≤ 0, article inconnu, journée fermée | inchangés (VTE-01) : la remise n'est calculée qu'après les contrôles de la vente | `SaleIT` (VTE-01) | ☐ |
| Journal | vente remisée | la ligne `CREATE` / `SALE` de VTE-01 (total net) ; pas de ligne supplémentaire : aucune promotion n'est modifiée | `SaleIT.createSale_validSale_isTracedInTheJournalWithOneSaleMovementPerLine` | ☐ |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Promotion avec code | code `AB12CD34EF`, sans saisie | non appliquée (VTE-04 pour les codes) ; absente du formulaire | `SalePromotionIT.createSale_promotionWithACode_isNotAppliedAutomatically`, `SalePromotionIT.getForm_promotionWithACode_isNotListed` | ☐ |
| Période : pas encore commencée | début demain | aucune remise | `SalePromotionIT.createSale_promotionNotStartedYet_givesNoDiscount` | ☐ |
| Période : terminée hier / finit aujourd'hui | fin J-1 ; fin J | aucune remise ; remise appliquée (jours inclus) | `SalePromotionIT.createSale_promotionEndedYesterday_givesNoDiscount`, `SalePromotionIT.createSale_promotionEndingToday_stillApplies` | ☐ |
| Plafond d'utilisation atteint | `usage_limit` 5, `usage_count` 5 | aucune remise | `SalePromotionIT.createSale_usageLimitReached_givesNoDiscount` | ☐ |
| Remise supérieure au sous-total | remise 5 000 sur 1 000 Ar | plafonnée à 1 000, `total` 0, jamais négatif (contrainte `0 ≤ remise ≤ sous-total`) | `SaleTest.record_discountAboveTheSubtotal_capsItSoTheTotalIsNeverNegative` | ☐ |
| Remise nulle | promotion sans effet | pas de `promotionId` | `SaleTest.record_zeroDiscount_forgetsThePromotion`, `PromotionServiceImplTest.priceBasket_noPromotionReachesTheBasket_returnsNone` | ☐ |
| Panier vide | aucune ligne | `NONE`, aucune lecture des promotions | `PromotionServiceImplTest.priceBasket_emptyBasket_returnsNoneWithoutReadingThePromotions` | ☐ |
| Prix convenu (commande livrée, LIV-03) | ligne avec `unitPrice` imposé | hors promotion : prix déjà négocié ; seules les lignes au prix du catalogue entrent dans le panier | `SaleDiscountsTest.of_agreedPriceLine_isLeftOutOfTheBasket`, `SaleDiscountsTest.of_agreedAndCatalogueLines_onlyPricesTheCatalogueLine` | ☐ |
| Gamme de l'article transmise | article de la gamme R | le panier envoyé aux promotions porte `rangeId` = R | `SaleDiscountsTest.of_catalogueLine_pricesTheBasketWithTheRangeOfTheArticleAndKeepsTheDiscount` | ☐ |
| Passage de minuit à Antananarivo | 22:00 UTC = 01:00 le lendemain à Tana | jour métier = jour de Tana (`PromotionServiceImpl.today()`, déjà testé par PRO-04) | `PromotionServiceImplTest.getPromotions_activeFilter_searchesByStatusNameOnTheBusinessDayWithWhiteListedSort` (même horloge) | ☐ |
| Promotion modifiée entre l'ouverture du poste et l'encaissement | promotion terminée pendant la vente | le serveur ne remise pas ; le poste affichait une remise : toast d'écart de net de VTE-02 (`pos.toast.totalAdjusted`) | 🧪 recette | 🧪 |
| Hors ligne | vente mise en file avec la remise affichée | rejouée avec la même `Idempotency-Key` ; le serveur recalcule au rejeu (la remise du jour du rejeu fait foi) | 🔍 `SaleServiceImpl.record` (aucun test dédié) | 🔍 code |
| Compteur d'utilisation | vente remisée | `usage_count` non incrémenté ici : relève de PRO-03 (la vente garde `promotion_id`) | — | ⏭ hors périmètre (PRO-03) |
| Code promo saisi à la caisse | touche `F6` | hors périmètre (VTE-04) | — | ⏭ hors périmètre (VTE-04) |
| Autres tests mis à jour | ventes de `SaleIT` et `SaleQueryIT` | les promotions automatiques laissées par d'autres classes de test sont terminées avant (`StockApiTestSupport.endAutomaticPromotions`) pour que les totaux restent la somme des lignes | `SaleIT`, `SaleQueryIT` | ☐ |

## Recette manuelle (🧪)

| # | Parcours | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Créer une promotion `PERCENT` 15 % sans code sur la gamme Zezika, canal Boutique ; ouvrir `/pos`, ajouter un article Zezika | le bandeau « … appliquée automatiquement » apparaît, la remise de ligne et le net sont à jour | 🧪 |
| R2 | Encaisser ce ticket | aucun toast d'écart ; la vente porte la même remise (`GET /api/v1/sales/{id}` : `discountAmount`, `promotionId`) | 🧪 |
| R3 | Passer le canal du ticket sur Téléphone (promotion non ciblée) | la remise disparaît | 🧪 |

## Résultat de l'exécution

- Date : à lancer (CI ou local) ; rien n'a été exécuté au moment de la rédaction (2026-11-11).
- Environnement : CI GitHub (`ci.yml`, `conventions.yml`, Qodana).
- Anomalies ouvertes : aucune connue. À arbitrer par le PO : voir « Points d'attention » du [guide](guide.md).
- Étapes 🧪 restantes : 4 (R1 à R3 et promotion modifiée pendant la vente).
