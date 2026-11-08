# Scénarios de test — PRO-04 · Liste et détail des promotions

Statut : ✅ tests exécutés en CI le 2026-11-08 · Ticket : [#61](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/61) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Classes de test :
- `PromotionServiceImplTest` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/promotion/service/impl/PromotionServiceImplTest.java`
- `PromotionListIT` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/promotion/controller/PromotionListIT.java`
- `PromotionSecurityIT` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/promotion/controller/PromotionSecurityIT.java` (fiche, livrée par PRO-01)
- `promotion-service.spec.ts` = `valimbavaka-ui/src/app/features/promotions/data-access/`
- `promotion-detail.spec.ts` = `valimbavaka-ui/src/app/features/promotions/components/promotion-detail/`
- `promotion-list-page.spec.ts` = `valimbavaka-ui/src/app/features/promotions/pages/promotion-list-page/`

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (liste, fiche, création), `SELLER` (liste et fiche en lecture, permission `promotions.read` existante), `DELIVERY_DRIVER` (aucun accès) |
| Données de départ | promotions de PRO-01 ; pour les statuts, une programmée (J+1 → J+5), une active (J → J), une terminée (J-5 → J-1), J = jour à Antananarivo ; canaux `BOUTIQUE`, `FACEBOOK` ; gamme `ZEZIKA` |
| Migration | aucune (aucune table ni permission nouvelle) |
| Tickets à avoir déployés avant | PRO-01 (branche `feature/PRO-01-creation-promotion`, PR empilée) |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Le clic sur une promotion ouvre sa fiche détaillée | clic sur la carte « Rentrée » | `GET /api/v1/promotions/promo-2`, panneau latéral avec le titre « Rentrée », bouton de la carte `aria-pressed="true"` | `promotion-list-page.spec.ts` « ouvre la fiche détaillée au clic sur une promotion » |✅ auto |
| 1b | La fiche montre remise, statut, périmètre, canaux et utilisations en libellés | `PERCENT` 20, gamme Zezika, canal Facebook, 52 / 200, sans code | « −20 % », « Active », « Zezika », « Facebook » (pas « Boutique »), « 52 / 200 », « Aucun (appliquée automatiquement) » | `promotion-detail.spec.ts` « affiche la remise, le statut, la gamme et les canaux… » |✅ auto |
| 1c | Fiche d'une promotion à code sur tout le catalogue | code `BF25`, `ALL`, sans plafond, `SCHEDULED` | « BF25 », « Tout le catalogue », « illimité », « Programmée » | `promotion-detail.spec.ts` « affiche le code, tout le catalogue et une utilisation illimitée » |✅ auto |
| 1d | Vue d'ensemble : la liste vient du serveur (fin des données de démonstration) | 2 promotions renvoyées | 2 cartes dans l'ordre du serveur, statut renvoyé par le serveur | `promotion-list-page.spec.ts` « affiche les promotions renvoyées par le serveur avec leur statut » |✅ auto |
| 1e | Filtre par statut côté serveur | puce « Terminées » | requête `status=ENDED`, `page=1` ; seule la carte terminée | `promotion-list-page.spec.ts` « demande au serveur les seules promotions terminées… », `promotion-service.spec.ts` « liste les promotions paginées… » |✅ auto |
| 1f | Statut calculé en base sur le jour d'Antananarivo | programmée, active (J → J), terminée (J-1) | `status=SCHEDULED`, `ACTIVE`, `ENDED` → 1 promotion chacune, du bon statut ; sans filtre : 3, la programmée en tête (tri `startsOn` décroissant) | `PromotionListIT.getPromotions_statusFilter_returnsOnlyThePromotionsOfThatStatus` |✅ auto |
| 1g | Service : statut, jour métier et tri transmis au dépôt | horloge 2026-11-07T22:00Z (= 8 nov. à Tana), `ACTIVE`, recherche `"  rose "`, tri `name` | `search("rose", "ACTIVE", 2026-11-08, page 1 base 0, taille 10, tri name)` ; statut `ACTIVE` mappé | `PromotionServiceImplTest.getPromotions_activeFilter_searchesByStatusNameOnTheBusinessDayWithWhiteListedSort` |✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/promotions` | 401 `UNAUTHENTICATED` | `PromotionListIT.getPromotions_withoutToken_returns401` |✅ auto |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `PromotionListIT.getPromotions_byDeliveryDriver_returns403` |✅ auto |
| Lecture par la vendeuse | vendeuse (canal + taux de commission), recherche du code en minuscules | 200, la promotion avec son code en majuscules et son canal | `PromotionListIT.getPromotions_bySellerSearchingTheCodeInLowerCase_findsThePromotion` |✅ auto |
| Statut inconnu | `status=EXPIRED` | 400 `MALFORMED_REQUEST` | `PromotionListIT.getPromotions_unknownStatus_returns400` |✅ auto |
| Fiche introuvable | `GET /{uuid inconnu}` | 404 `NOT_FOUND` ; à l'écran, état d'erreur avec « Réessayer » | `PromotionSecurityIT.getPromotionById_unknownId_returns404`, `promotion-detail.spec.ts` « propose de réessayer quand la promotion est introuvable » |✅ auto |
| Mass assignment, 409, 422, journal | lecture seule | sans objet : aucun corps accepté, aucune écriture, pas de ligne de journal | — | ⏭ hors périmètre (écritures : PRO-01) |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| `size` au maximum / maximum + 1 | `size=100` / `size=101` | 200 / 422 `VALIDATION_FAILED` | `PromotionListIT.getPromotions_sizeAboveLimitOrSortOutsideWhiteList_isRefusedOrIgnored` |✅ auto |
| Tri hors liste blanche | `sortBy=code; drop table promotions` | 200, tri par défaut `startsOn` décroissant | `PromotionListIT.getPromotions_sizeAboveLimitOrSortOutsideWhiteList_isRefusedOrIgnored`, `PromotionServiceImplTest.getPromotions_noStatusAndSortOutsideWhiteList_searchesAllByStartDateDescending` |✅ auto |
| Dernière page | 3 promotions, `size=2`, `page=2` | `page` 2, `totalPages` 2, 1 élément | `PromotionListIT.getPromotions_secondPageOfTwo_returnsTheLastPromotion` |✅ auto |
| Liste vide | filtre sans résultat | état vide « Aucune promotion dans ce filtre » | `promotion-list-page.spec.ts` « affiche l’état vide… » |✅ auto |
| Sans statut | pas de paramètre `status` | toutes les promotions (`ANY_STATUS`) ; le paramètre n'est pas envoyé par l'écran | `PromotionServiceImplTest.getPromotions_noStatusAndSortOutsideWhiteList_searchesAllByStartDateDescending`, `promotion-service.spec.ts` « liste les promotions paginées… » |✅ auto |
| Bornes de période | début = fin = aujourd'hui ; fin = hier | `ACTIVE` ; `ENDED` | `PromotionListIT.getPromotions_statusFilter_returnsOnlyThePromotionsOfThatStatus` |✅ auto |
| Minuit à Antananarivo | 22:00 UTC = 01:00 le lendemain à Tana | filtre et statut calculés sur le jour de Tana | `PromotionServiceImplTest.getPromotions_activeFilter_searchesByStatusNameOnTheBusinessDayWithWhiteListedSort` |✅ auto |
| Recherche par code, casse | code `AB12…` cherché en minuscules | trouvé (recherche insensible à la casse sur nom et code) | `PromotionListIT.getPromotions_bySellerSearchingTheCodeInLowerCase_findsThePromotion` |✅ auto |
| Changement de filtre en page 3 | page 3 puis puce « Actives » | retour en page 1 (`linkedSignal`) | `promotion-list-page.spec.ts` « demande au serveur… » (vérifie `page=1`) |✅ auto |
| Une page de 100 promotions | `size=100` | cibles et canaux chargés par lots (`@BatchSize(100)`), pas 200 requêtes | 🔍 `Promotion.java` (pas de test de nombre de requêtes) |🔍 code |
| Hors ligne | réseau coupé | la liste et la fiche affichent l'état d'erreur avec « Réessayer » (lecture non mise en file) | 🧪 recette |🧪 recette |

## Tests existants

- Backend : `PromotionServiceImplTest` (+2 tests : `getPromotions_…`), `PromotionListIT` (7 tests).
- Frontend : `promotion-service.spec.ts` (+2 tests), `promotion-detail.spec.ts` (3 tests), `promotion-list-page.spec.ts` (4 tests). Données de test : `data-access/promotion-testing.ts`.

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Admin → Promotions : la liste affiche les promotions créées (plus de données de démonstration) | cartes avec statut, période, code, utilisations | 🧪 |
| R2 | Cliquer sur une carte (souris, puis Tab + Entrée) | fiche à droite (tiroir sous 1600 px, ancrée au-dessus sinon) ; Échap ou ✕ la ferme ; sur très grand écran, « Fermer la fiche » rend le formulaire | 🧪 |
| R3 | Puces Programmées / Actives / Terminées / Toutes | la liste et la pagination suivent le filtre | 🧪 |
| R4 | Créer une promotion depuis le formulaire | toast de succès et liste rechargée | 🧪 |
| R5 | Vendeuse : ouvrir Promotions et une fiche | lecture possible | 🧪 |

## Résultat de l'exécution

- Date : 2026-11-08, CI GitHub lancée sur la branche `feature/PRO-04-liste-detail-promotions` (workflows `ci.yml` et `conventions.yml`), tous les jobs verts.
- Backend (`mvn -B -ntp verify`) : 289 tests unitaires et 259 tests d'intégration, 0 échec ; dont `PromotionServiceImplTest` 7, `PromotionListIT` 7, `PromotionSecurityIT` 10.
- Frontend (`ng test`) : 55 fichiers de test verts ; dont `promotion-list-page.spec.ts` 4, `promotion-detail.spec.ts` 3, `promotion-service.spec.ts` 3, contrôle axe de la page `promotions` sans violation.
- Conventions : Spotless, Checkstyle (0 violation), Prettier, ESLint, gitleaks verts.
- Anomalies ouvertes :
  1. (faible) Le chargement par lots des cibles et canaux n'a pas de test du nombre de requêtes.
  2. (faible) La « remise accordée » des cartes reste « — » : elle relève de PRO-03.
- Étapes 🧪 restantes : 5 (+ le cas hors ligne).
