# Guide d'intégration — VTE-04 · Application d'un code promo

Statut : ✅ rédigé le 2026-11-11 sur le code livré, tests verts en CI · Ticket : [#65](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/65) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La vendeuse tape un code promo au poste (bouton « + Ajouter » ou **F6**) ; le serveur le vérifie (existence, période, canal, plafond) et renvoie la promotion avec les articles qu'elle atteint, le poste recalcule alors le net à l'écran et l'indique sur le ticket (« … appliquée avec le code OCTO20 »). À l'encaissement, `POST /api/v1/sales` reçoit seulement le **code** (`promoCode`) : le **serveur décide** de la remise, la compare à la meilleure promotion automatique du canal et garde la plus avantageuse, sans cumul. Le compteur d'usage est celui de PRO-03 (`SaleLedger.post`), sans rien d'autre à appeler.

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| `GET` | `/api/v1/sales/promo-codes/{code}?salesChannelId=<uuid>` | `sales.create` | code en chemin (sans casse ni espaces autour), canal de la vente en paramètre | `SalePromotionRDto` (`id`, `name`, `kind`, `value`, `articleIds`, `channelIds`), même forme que `promotions[]` du formulaire (PRO-02) | 401, 403, **404** code inconnu, **422** pas encore valable / expiré / autre canal / plafond atteint |
| `POST` | `/api/v1/sales` | `sales.create` | `SaleWDto` + **nouveau champ optionnel `promoCode`** (3 à 12 lettres ou chiffres, espaces tolérés, vide = pas de code) | `SaleRDto` inchangé (`discountAmount`, `promotionId`, `total`) | s'ajoutent : **404** code inconnu, **422** code refusé (mêmes causes + « aucune remise sur ce panier »), 409 si le plafond est atteint par une autre vente au même instant (PRO-03) |

`SaleWDto` n'accepte toujours ni remise, ni total, ni promotion : seul le code est une demande, vérifiée par le serveur. `SaleRDto` ne change pas (aucun constructeur à modifier).

### Services et ports Java réutilisables

- `PromotionService.getPromotionByCode(String code, UUID salesChannelId)` (`PromotionServiceImpl.java:124`) : lit le code sans casse ni espaces, 404 s'il est inconnu, 422 (`BusinessException`) s'il n'est pas valable aujourd'hui (jour d'Antananarivo, bornes incluses), n'est pas ouvert au canal ou a atteint son plafond (`requireUsable`, ligne 175). Lecture seule : ne compte aucun usage.
- `PromotionService.priceBasket(UUID salesChannelId, String code, List<PromotionBasketLine> lines)` (`PromotionServiceImpl.java:129`) : vérifie le code comme ci-dessus, calcule sa remise avec `PromotionPricing.discountOf` (règle de PRO-02, non dupliquée), refuse un code sans effet (422), puis compare à `priceBasket(salesChannelId, lines)` : l'automatique ne l'emporte que si elle donne **strictement** plus ; à égalité le code gagne. Renvoie un `PromotionDiscount` (promotion retenue, montant).
- `PromotionRepository.findByCode(String)` (`PromotionRepository.java:32`) : les codes sont stockés en majuscules.
- `SaleCommand.promoCode()` (`SaleCommand.java:24`) : 5e composant, `null` sans code. Un constructeur à 4 arguments est conservé : `DeliverySale` et les tests existants ne changent pas.
- `SaleDiscounts.of` (`SaleDiscounts.java:46`) appelle la version « code » quand `promoCode` n'est pas nul, même si toutes les lignes sont à prix convenu (le serveur répond alors 422 « aucune remise »). `SaleServiceImpl.createSale` (ligne 85) transmet le code et abandonne un code blanc.
- `SaleFormAssembler.promoCode` (`SaleFormAssembler.java:106`) : traduit la promotion du code en `SalePromotionRDto` avec les articles vendables qu'elle atteint (même règle `reaches` que le formulaire).

### Frontend (`valimbavaka-ui/src/app/features/pos/`)

- `data-access/pos-service.ts` : `checkPromoCode(code, salesChannelId)` (l.40) appelle l'endpoint ci-dessus.
- `data-access/pos-pricing.ts` : `bestPromotion(promotions, channel, lines, codePromotion = null)` (l.109) : même règle que le serveur (le code remplace l'automatique s'il donne au moins autant).
- `data-access/pos-cart.ts` : `applyCode(code, promotion)` (l.134), `removeCode()` (l.140), signaux `promoCode` (code saisi), `appliedCode` (code qui donne réellement la remise, `null` sinon) et `promoEntry` (saisie ouverte). `clear()` oublie le code.
- `data-access/pos-mapping.ts` : `toPosPromotion` (l.88) traduit les canaux du serveur en canaux du poste ; `withPromoCode(body, code)` (l.134) ajoute le code au corps de la vente.
- `components/pos-ticket` : champ de saisie (`#pos-promo-code`, libellé pour lecteur d'écran, Échap annule), pastille « Code OCTO20 » avec bouton de retrait, bandeau « … appliquée avec le code … » ; sortie `promoCodeSubmit`. `pages/pos-page/pos-page.ts` : `openPromoEntry` (F6, l.121) et `applyPromoCode` (l.129) ; `checkout` n'envoie le code que s'il est **retenu** (`appliedCode`).
- Clés de traduction `pos.ticket.promoCode*` et `pos.toast.promoCode*` (fr et mg).

### Données

- Aucune migration : `promotions.code` (PRO-01, unique), `sales.promotion_id` (PRO-02), `usage_count` (PRO-03).
- Journal : aucune ligne nouvelle ; la vente garde sa ligne `CREATE` / `SALE` (VTE-01), le montant tracé est le net.

## Règles métier à respecter par les autres tickets

- **Une promotion par vente, sans cumul** : le code et l'automatique sont comparés, la plus avantageuse gagne, le code gagne l'égalité. Seule la promotion retenue compte un usage (`sales.promotion_id` → `SaleLedger.post`).
- **Le serveur fait foi** : un montant ou une promotion envoyés par le client sont ignorés ; le code est revérifié à l'enregistrement (période, canal, plafond), le poste ne fait qu'afficher la même règle.
- **Un code qui n'a aucun effet est refusé** (422), pas ignoré : cela évite une vente enregistrée en croyant que le client a eu sa remise.
- **Les lignes à prix convenu** (livraison, LIV-03) ne participent pas au panier ; un code sur une vente qui n'a que de telles lignes est refusé.
- Si la règle de calcul change, la changer des deux côtés (`PromotionPricing` et `pos-pricing.ts`).

## Fichiers concernés

- Backend, promotion : `feature/promotion/repository/PromotionRepository.java`, `service/PromotionService.java`, `service/impl/PromotionServiceImpl.java`.
- Backend, vente : `feature/sale/controller/SaleController.java`, `domain/SaleCommand.java`, `dto/SaleWDto.java`, `service/SaleService.java`, `service/impl/{SaleServiceImpl,SaleDiscounts,SaleFormAssembler}.java`.
- Frontend : `features/pos/data-access/{pos-pricing,pos-cart,pos-mapping,pos-models,pos-service}.ts`, `components/pos-ticket/{pos-ticket.ts,pos-ticket.html,pos-ticket.css}`, `pages/pos-page/{pos-page.ts,pos-page.html}`, `public/i18n/{fr,mg}.json`.
- Tests : voir le [scénario](scenario-test.md) (`SalePromoCodeIT`, `PromotionServiceImplTest`, `SaleDiscountsTest`, `SaleServiceImplTest`, `pos-pricing.spec.ts`, `pos-cart.spec.ts`, `pos-mapping.spec.ts`, `pos-service.spec.ts`, `pos-page-promo-code.spec.ts`).

## Dépendances

**Ce ticket s'appuie sur** :
- [PRO-01](../PRO-01/description.md) · [guide](../PRO-01/guide.md) : promotions avec code (3 à 12 caractères, majuscules, unique)
- [PRO-02](../PRO-02/description.md) · [guide](../PRO-02/guide.md) : `priceBasket`, `PromotionPricing`, `sales.promotion_id`, front `pos-pricing`
- [PRO-03](../PRO-03/description.md) · [guide](../PRO-03/guide.md) : compteur d'usage et plafond sûr en concurrence
- [VTE-02](../VTE-02/description.md) · [guide](../VTE-02/guide.md) : poste de caisse, calcul au poste
- [VTE-03](../VTE-03/description.md) · [guide](../VTE-03/guide.md) : raccourci F6

**Ces tickets s'appuient sur lui** : aucun.

## Comment l'utiliser depuis un autre ticket

```java
// Vente par une autre feature avec un code : le serveur vérifie et décide.
saleService.record(new SaleCommand(channelId, sellerId, PaymentMethod.CASH, lines, "OCTO20"));
```

## Points d'attention

- **Hypothèses non validées par le PO** : (1) un seul code par ticket ; (2) à égalité avec l'automatique, le code gagne, et si l'automatique donne strictement plus la vente garde l'automatique (le code n'est alors pas compté, le poste affiche « Code … sans effet sur ce ticket ») ; (3) un code sans effet sur le panier est refusé en 422 ; (4) code inconnu = 404, tout autre refus = 422 avec un message en français affiché tel quel à la caissière.
- **Hors ligne** : la vérification du code demande le réseau ; sans connexion le code n'est pas appliqué. Une vente mise en file avec un code est revérifiée au rejeu et refusée (422) si le code a expiré ou atteint son plafond entre-temps ; elle reste alors en erreur dans la file (SYS-07) pour que la caissière la traite. Un rejeu « sans le code » n'est pas fait automatiquement : c'est un choix prudent, à revoir si le PO préfère accorder la vente sans remise.
- **Plafond** : entre la vérification au poste et l'enregistrement, un autre ticket peut prendre le dernier usage : 409 (PRO-03), la caissière relance. Le code n'est pas réservé à la saisie.
- **Limite du poste** : l'endpoint ne renvoie que les articles vendables (finis, actifs, avec prix) ; un article hors de cette liste n'a de toute façon pas de ligne au poste.
- **Pas de limite par client ni de sécurité anti-devinette** : un code est vérifiable par toute personne connectée avec `sales.create` ; pas de limitation de tentatives (à décider si les codes deviennent sensibles).
- **Test d'intégration** : `SalePromoCodeIT` partage la base ; il termine les promotions automatiques avant et après, et celles dont le code commence par `VT4` à la fin de chaque test. Tout nouveau test de vente doit appeler `StockApiTestSupport.endAutomaticPromotions()`.
- **Traduction malgache** (`mg.json`) rédigée sans relecture par un locuteur : à faire valider.
