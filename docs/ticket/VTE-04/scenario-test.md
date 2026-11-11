# Scénarios de test — VTE-04 · Application d'un code promo

Statut : ☐ tests à lancer (CI ou local) · Ticket : [#65](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/65) · [description](description.md) · [guide](guide.md)

Légende de la colonne « OK » : ☐ non exécuté · ✅ test automatique vert · 🧪 recette manuelle. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

Classes de test (backend, `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/`) :
- `SalePromoCodeIT` = `sale/controller/SalePromoCodeIT.java` (API de bout en bout, base partagée)
- `PromotionServiceImplTest` = `promotion/service/impl/PromotionServiceImplTest.java`
- `SaleDiscountsTest` = `sale/service/impl/SaleDiscountsTest.java`
- `SaleServiceImplTest` = `sale/service/impl/SaleServiceImplTest.java`

Specs frontend (`valimbavaka-ui/src/app/features/pos/`) :
- `pos-pricing.spec.ts`, `pos-cart.spec.ts`, `pos-mapping.spec.ts`, `pos-service.spec.ts` (dans `data-access/`)
- `pos-page-promo-code.spec.ts` (dans `pages/pos-page/`)

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `SELLER` (vente et vérification du code, permission `sales.create`), `DELIVERY_DRIVER` (refusé), `ADMIN` (ouvre la journée) |
| Données de départ | journée ouverte ; article « savon » à 8 500 Ar, stock boutique 60 ; promotion **avec code** `VT4…` de 20 % sur cet article, canal `STORE`, valable de J à J+10 (J = jour à Antananarivo) ; une vente de 3 × 8 500 = 25 500 Ar |
| Migration | aucune : `promotions.code` (PRO-01), `sales.promotion_id` (PRO-02) et le compteur (PRO-03) existent déjà |
| Tickets à avoir déployés avant | PRO-01, PRO-04, PRO-02, PRO-03, VTE-01, VTE-02, VTE-03 |

**Périmètre** : le code est vérifié et la remise calculée par le serveur ; le poste affiche la même remise à l'écran avec la même règle (`bestPromotion`). Un seul code par ticket, une seule promotion par vente, sans cumul : le code remplace l'automatique s'il donne au moins autant (égalité : le code).

## Scénarios issus des critères d'acceptation

Critère : « L'application du code réduit le net à payer et l'indique sur le ticket. »

| # | Cas | Exemple | Résultat attendu | Test | OK |
|---|---|---|---|---|---|
| 1a | Code valide, vente enregistrée | 3 × 8 500 Ar, code 20 % | `discountAmount` 5 100, `total` 20 400, `promotionId` = celui du code, compteur du code = 1 | `SalePromoCodeIT.createSale_validCode_appliesTheDiscountAndCountsTheUse` | ☐ |
| 1b | Code d'un montant fixe | code `AMOUNT` 2 000 Ar | `discountAmount` 2 000, `total` 23 500 | `SalePromoCodeIT.createSale_amountCode_takesTheAmountOffTheBasket` | ☐ |
| 1c | Code lu sans casse ni espaces | `"  vt4abcdef12 "` | même remise que le code en majuscules | `SalePromoCodeIT.createSale_codeInLowerCaseWithSpaces_isReadWithoutCaseAndSpaces`, `PromotionServiceImplTest.getPromotionByCode_codeInLowerCaseWithSpaces_looksUpTheUpperCaseCode` | ☐ |
| 1d | Code donne plus que l'automatique | automatique 10 %, code 20 % | le code gagne (5 100), l'automatique n'est pas comptée | `SalePromoCodeIT.createSale_codeGivesMoreThanTheAutomaticPromotion_theCodeWinsAndTheAutomaticOneIsNotUsed`, `PromotionServiceImplTest.priceBasket_codeGivesMoreThanTheAutomaticPromotion_returnsTheCode` | ☐ |
| 1e | Automatique donne plus que le code | automatique 30 %, code 20 % | l'automatique reste (7 650), le code n'est pas compté, pas de cumul | `SalePromoCodeIT.createSale_automaticPromotionGivesMoreThanTheCode_theAutomaticOneWinsAndTheCodeIsNotUsed`, `PromotionServiceImplTest.priceBasket_automaticPromotionGivesMoreThanTheCode_returnsTheAutomaticPromotion` | ☐ |
| 1f | Égalité | automatique 10 %, code 10 % | le code gagne | `PromotionServiceImplTest.priceBasket_codeAndAutomaticPromotionTie_returnsTheCode` | ☐ |
| 1g | Le serveur reçoit le code et décide | `SaleWDto.promoCode = " octo20 "` ; code vide `"   "` | le code est transmis au calcul de la remise ; un code blanc est ignoré | `SaleServiceImplTest.createSale_promoCode_isHandedToTheDiscountDecisionAndABlankCodeIsDropped`, `SalePromoCodeIT.createSale_blankCode_isASaleWithoutCode` | ☐ |
| 1h | Le calcul passe par le code | code `OCTO20` sur 2 × 8 500 | `SaleDiscounts` appelle `priceBasket(canal, code, panier)` et non la version automatique ; sans code, jamais | `SaleDiscountsTest.of_promoCode_pricesTheBasketWithTheCodeAndKeepsTheDiscount`, `SaleDiscountsTest.of_noPromoCode_neverAsksForACode` | ☐ |
| 1i | Le poste vérifie le code avant l'encaissement | `GET /api/v1/sales/promo-codes/{code}?salesChannelId=…` | renvoie la promotion, ses articles et ses canaux ; ne compte aucun usage | `SalePromoCodeIT.getPromoCode_validCode_returnsThePromotionWithTheArticlesItReaches`, `pos-service.spec.ts` « fait vérifier le code promo par le serveur » | ☐ |
| 1j | **Le net baisse à l'écran** | F6, saisie `octo20`, Entrée | net 8 500 → 6 800 ; le ticket affiche « Code Octobre appliquée avec le code OCTO20 » | `pos-page-promo-code.spec.ts` « applique le code validé par le serveur : le net baisse et le ticket indique le code », `pos-cart.spec.ts` « applique un code promo validé » | ☐ |
| 1k | Le poste suit la même règle que le serveur | code 20 % vs automatique 10 % / 30 % / 10 % | code / automatique / code (égalité) ; code sans effet ou d'un autre canal ignoré | `pos-pricing.spec.ts` (4 cas « code promo », VTE-04), `pos-cart.spec.ts` « n'applique pas le code quand la promotion automatique donne plus » | ☐ |
| 1l | L'encaissement envoie le code, jamais un montant | F12 après un code retenu | le corps de `POST /api/v1/sales` contient `promoCode`, pas de remise ni de total ; sans code, pas de champ | `pos-page-promo-code.spec.ts` « envoie le code retenu avec la vente, jamais un montant de remise », « n'envoie aucun code quand la vendeuse n'en a pas saisi », `pos-mapping.spec.ts` (`withPromoCode`) | ☐ |
| 1m | Retirer le code | clic sur « × » | le net revient au prix du catalogue ; le code est oublié au ticket suivant | `pos-cart.spec.ts` « retire le code promo », « remet les totaux à zéro au ticket suivant en gardant la vendeuse » | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | OK |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/sales/promo-codes/VT4ABCDEF` | 401 `UNAUTHENTICATED` | `SalePromoCodeIT.getPromoCode_withoutTokenOrWithADeliveryDriver_returns401Then403` | ☐ |
| Mauvais rôle | jeton `DELIVERY_DRIVER` | 403 `FORBIDDEN` | `SalePromoCodeIT.getPromoCode_withoutTokenOrWithADeliveryDriver_returns401Then403` | ☐ |
| Champ interdit dans le corps | `discountAmount: 9999`, `total: 1`, `promotionId` aléatoire | ignorés : remise 0, total 25 500, pas de promotion | `SalePromoCodeIT.createSale_discountAndPromotionSentByTheClient_areIgnored` | ☐ |
| Code mal formé | `"a!"` (moins de 3 caractères, symbole) | 422 `VALIDATION_FAILED`, aucune vente | `SalePromoCodeIT.createSale_malformedCode_returns422` | ☐ |
| Code inconnu | `VT4ZZZZZZZ` | 404 `NOT_FOUND`, aucune vente enregistrée (nombre de ventes inchangé) | `SalePromoCodeIT.createSale_unknownCode_returns404AndRecordsNothing`, `SalePromoCodeIT.getPromoCode_unknownExpiredOrOtherChannel_returns404Or422`, `PromotionServiceImplTest.getPromotionByCode_unknownCode_throwsNotFound`, `PromotionServiceImplTest.priceBasket_unknownCode_throwsNotFound` | ☐ |
| Code expiré | fin hier | 422 `VALIDATION_FAILED` « expiré », compteur inchangé | `SalePromoCodeIT.createSale_expiredCode_returns422`, `PromotionServiceImplTest.getPromotionByCode_periodEnded_throwsBusinessException` | ☐ |
| Code pas encore valable | début demain | 422 | `SalePromoCodeIT.createSale_codeNotStartedYet_returns422`, `PromotionServiceImplTest.getPromotionByCode_periodNotStarted_throwsBusinessException` | ☐ |
| Code d'un autre canal | code limité à `FACEBOOK`, vente `STORE` | 422 « ne s'applique pas à ce canal » | `SalePromoCodeIT.createSale_codeOfAnotherChannel_returns422`, `PromotionServiceImplTest.getPromotionByCode_otherChannel_throwsBusinessException` | ☐ |
| Plafond déjà atteint | plafond 1, `usage_count` 1 | 422 « plafond d'utilisation », compteur inchangé | `SalePromoCodeIT.createSale_codeAtItsUsageLimit_returns422`, `PromotionServiceImplTest.getPromotionByCode_usageLimitReached_throwsBusinessException` | ☐ |
| Code sans effet sur le panier | code ciblant un autre article ; `BUY_X` 3 avec 2 achetés ; panier vide ou lignes à prix convenu | 422 « ne donne aucune remise sur ce panier » | `SalePromoCodeIT.createSale_codeThatReachesNoArticleOfTheBasket_returns422`, `PromotionServiceImplTest.priceBasket_codeWithoutEffectOnTheBasket_throwsBusinessException`, `PromotionServiceImplTest.priceBasket_codeAndEmptyBasket_throwsBusinessException`, `SaleDiscountsTest.of_promoCodeOnAgreedPriceLinesOnly_isRefusedByTheServerBecauseTheBasketIsEmpty` | ☐ |
| Plafond atteint entre la vérification et l'enregistrement (concurrence) | deux ventes avec le dernier usage d'un code | 409 `CONFLICT` pour la perdante : même chemin que PRO-03 (`SaleLedger.post` → `recordUsage`) | ⏭ PRO-03 : `SaleUsageIT.createSale_severalSimultaneousSalesOnALimitedPromotion_neverGoPastTheLimit` | ☐ |
| Code refusé au poste | serveur répond 422 « expiré » | le net ne change pas, toast « Code promo refusé : … », aucun code retenu | `pos-page-promo-code.spec.ts` « refuse le code que le serveur rejette » | ☐ |
| Journal | vente avec code | aucune ligne nouvelle : la vente garde sa ligne `CREATE` / `SALE` (VTE-01), le montant tracé est le net | 🔍 code : `SaleLedger.post` inchangé | 🔍 |

## Cas limites et hors ligne

- **Dernier jour de validité** : un code qui se termine aujourd'hui (Antananarivo) reste valable (`PromotionServiceImplTest.getPromotionByCode_lastDayOfThePeriod_isStillValid`) ☐.
- **Compteur** : seul le code **retenu** compte un usage. Si l'automatique gagne, le compteur du code reste à 0 (`SalePromoCodeIT.createSale_automaticPromotionGivesMoreThanTheCode…`) ; si le code gagne, celui de l'automatique reste à 0 (`…createSale_codeGivesMoreThanTheAutomaticPromotion…`) ☐.
- **Annulation** : l'usage du code est rendu par `SaleLedger.unpost` (PRO-03, `SaleUsageIT.cancel_saleWithAPromotion_givesTheUseBack`) : ⏭ PRO-03.
- **Changement de canal après la saisie** : un code limité à la boutique n'est plus retenu si le ticket passe sur Téléphone ; il n'est pas envoyé (`pos-cart.spec.ts` « ne retient pas le code quand le canal du ticket change… ») ☐.
- **Hors ligne** : la vérification du code demande le réseau ; sans connexion, le toast d'erreur s'affiche et la vente se fait sans code (🧪). Une vente mise en file avec un code est rejouée plus tard : le serveur revérifie le code au rejeu et la vente est refusée (422) si le code a expiré entre-temps (hypothèse du ticket, 🧪).
- **Sécurité de la vérification** : l'accès est protégé (`sales.create`) ; la vérification d'un code ne modifie rien (lecture seule, aucun usage compté).
- **Recette 🧪 (Njara)** : au poste, boutique : ajouter 3 savons, F6, saisir un code valable, vérifier le net et le bandeau, encaisser ; puis un code expiré, un code d'un autre canal, un code à plafond atteint ; vérifier sur la fiche de la promotion que l'usage vaut 1 seulement pour la vente qui l'a retenu.

## Tests existants

- Backend : `SalePromoCodeIT` (17 tests), `PromotionServiceImplTest` (13 tests ajoutés), `SaleDiscountsTest` (3 ajoutés), `SaleServiceImplTest` (1 ajouté). Les ventes des autres classes ne sont pas touchées : `endAutomaticPromotions()` avant chaque test, puis fin des promotions à code `VT4…` après.
- Frontend : `pos-pricing.spec.ts` (4 ajoutés), `pos-cart.spec.ts` (5 ajoutés), `pos-mapping.spec.ts` (3 ajoutés), `pos-service.spec.ts` (1 ajouté), `pos-page-promo-code.spec.ts` (6).

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : à lancer (CI ou local)
- Anomalies ouvertes : à lancer (CI ou local)
