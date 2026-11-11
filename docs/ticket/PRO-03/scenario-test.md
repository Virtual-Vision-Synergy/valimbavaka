# Scénarios de test — PRO-03 · Statut et utilisation d'une promotion

Statut : ☐ tests écrits, à lancer (CI ou local) · Ticket : [#60](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/60) · [description](description.md) · [guide](guide.md)

Légende de la colonne « OK » : ☐ non exécuté · ✅ test automatique vert. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Classes de test :
- `SaleUsageIT` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/sale/controller/SaleUsageIT.java`
- `PromotionServiceImplTest` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/promotion/service/impl/PromotionServiceImplTest.java`
- `SaleLedgerTest` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/sale/service/impl/SaleLedgerTest.java`
- `PromotionListIT` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/promotion/controller/PromotionListIT.java` (statut, déjà livré par PRO-04)

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `SELLER` (vente), `ADMIN` (lecture de la fiche promotion, `promotions.read`) |
| Données de départ | journée ouverte ; article à 8 500 Ar, stock boutique 60 ; promotion automatique de 15 % sur cet article, canal `STORE`, valable de J à J+10 (J = jour à Antananarivo) |
| Migration | aucune : la colonne `promotions.usage_count` existe depuis PRO-01 |
| Tickets à avoir déployés avant | PRO-01, PRO-04, PRO-02 |

**Périmètre** : le statut (`SCHEDULED` / `ACTIVE` / `ENDED`) est déjà déduit des dates par PRO-01 et PRO-04 et n'est jamais stocké ; PRO-03 ne le recode pas, il le vérifie. PRO-03 livre le compteur : +1 quand une vente portant la promotion est enregistrée, −1 quand elle est annulée, jamais au-delà du plafond, même en concurrence.

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Le statut se met à jour selon les dates | Étant donné des promotions à venir, en cours, terminée hier, terminant aujourd'hui · Quand l'administrateur filtre ou lit la fiche · Alors le statut suit le jour d'Antananarivo (bornes incluses) | `SCHEDULED`, `ACTIVE`, `ENDED` ; une promotion finissant aujourd'hui est `ACTIVE` | `PromotionListIT.getPromotions_statusFilter_returnsOnlyThePromotionsOfThatStatus` (PRO-04), `SalePromotionIT.createSale_promotionEndingToday_stillApplies`, `createSale_promotionEndedYesterday_givesNoDiscount`, `createSale_promotionNotStartedYet_givesNoDiscount` (PRO-02) | ☐ |
| 2 | Le compteur s'incrémente à chaque usage | Étant donné la promotion de 15 %, compteur 0 · Quand une vente de 3 × 8 500 Ar est enregistrée (remise 3 825 Ar) · Alors `usage_count` passe à 1 et la fiche (`GET /api/v1/promotions/{id}`) renvoie `usageCount` = 1, `status` = `ACTIVE` | une vente = un usage (pas un usage par ligne) | `SaleUsageIT.createSale_promotionApplied_incrementsTheUsageCounterOnceAndTheSheetShowsIt`, `SaleLedgerTest.post_saleWithPromotion_countsOneUseOfThePromotion`, `PromotionServiceImplTest.recordUsage_limitNotReached_incrementsTheCounter` | ☐ |
| 3 | idem | Étant donné une promotion d'un autre canal · Quand une vente est enregistrée au comptoir · Alors aucune promotion n'est comptée | compteur inchangé | `SaleUsageIT.createSale_noPromotionApplies_leavesTheCounterAtZero`, `SaleLedgerTest.post_twoLines_takesTheStockOutOfTheChannelLocationTracesAndPublishes` (aucun appel au service de promotion) | ☐ |
| 4 | Plafond d'utilisation | Étant donné un plafond de 1 · Quand deux ventes se suivent · Alors la première a la remise (compteur 1), la seconde n'en a plus (compteur reste 1) | le plafond se déclenche réellement | `SaleUsageIT.createSale_lastUseOfALimitedPromotion_reachesTheLimitAndTheNextSaleHasNoDiscount` | ☐ |
| 5 | Annulation d'une vente | Étant donné une vente avec promotion (plafond 1, compteur 1) · Quand elle est annulée (`SaleService.cancel`) · Alors le compteur revient à 0 et la promotion s'applique de nouveau à la vente suivante | l'usage est rendu une seule fois ; une vente sans promotion ne touche aucun compteur | `SaleUsageIT.cancel_saleWithAPromotion_givesTheUseBack`, `SaleUsageIT.cancel_saleWithoutPromotion_leavesEveryCounterUntouched`, `SaleLedgerTest.unpost_saleWithPromotion_givesTheUseBack`, `SaleLedgerTest.unpost_saleWithoutPromotion_doesNotTouchAnyPromotion`, `PromotionServiceImplTest.releaseUsage_anyPromotion_decrementsTheCounter` | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton / mauvais rôle | sans objet : aucun endpoint nouveau (la fiche et la liste restent protégées par PRO-01 et PRO-04) | `PromotionSecurityIT`, `PromotionListIT.getPromotions_withoutToken_returns401` (déjà livrés) | ☐ |
| Champ interdit dans le corps (`promotionId`, `discountAmount`) | ignoré : le serveur seul décide de la promotion, donc du compteur | `SalePromotionIT.createSale_discountAndPromotionSentByTheClient_areIgnored` (PRO-02) | ☐ |
| Plafond atteint entre le calcul de la remise et l'enregistrement (concurrence) | 409 `CONFLICT`, la vente est annulée avec sa transaction (ni stock sorti, ni compteur) | `SaleUsageIT.createSale_severalSimultaneousSalesOnALimitedPromotion_neverGoPastTheLimit`, `PromotionServiceImplTest.recordUsage_limitReached_throwsConflict` | ☐ |
| Promotion introuvable au moment de compter | 404 `NOT_FOUND` | `PromotionServiceImplTest.recordUsage_unknownPromotion_throwsNotFound` | ☐ |

## Cas limites et hors ligne

- **Concurrence** : 6 ventes simultanées sur une promotion à plafond 2 : chaque réponse vaut 201 ou 409, le compteur final est ≤ 2 et égal au nombre de ventes `VALIDATED` portant la promotion (`SaleUsageIT.createSale_severalSimultaneousSalesOnALimitedPromotion_neverGoPastTheLimit`). Le compteur est incrémenté par un seul `UPDATE` conditionnel (`PromotionRepository.incrementUsage`).
- **Plancher** : l'annulation ne descend jamais le compteur sous 0 (`PromotionRepository.decrementUsage`, condition `usage_count > 0`) : 🔍 code.
- **Hors ligne** : une vente rejouée après coup est recalculée avec les promotions du jour du rejeu, puis compte son usage à ce moment-là ; si le plafond est atteint entre-temps, la remise n'est plus accordée (pas de 409, le calcul ne la retient plus) : 🔍 code. Le 409 ne survient que dans la fenêtre de concurrence ci-dessus.
- **Annulation après clôture** : refusée par `SaleService.cancel` (409, LIV-05) ; le compteur n'est donc jamais modifié dans ce cas : 🔍 code.
- **Pas de recalcul rétroactif** : une promotion modifiée ou terminée garde son compteur ; seul le statut change avec les dates.
- **Recette 🧪** : en tant qu'ADMIN, ouvrir la fiche d'une promotion à plafond, passer deux ventes au poste et vérifier « 2 / plafond » ; annuler une livraison faite par vente promotionnelle n'est pas possible (les ventes de livraison n'ont pas de remise).

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : à lancer (CI ou local)
- Anomalies ouvertes : aucune connue ; le front n'est pas modifié (la fiche affichait déjà `usageCount` / `usageLimit` et le statut)
