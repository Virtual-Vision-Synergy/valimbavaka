# Scénarios de test — ART-06 · Fiche article unifiée (360°)

Statut : ✅ tests exécutés en CI le 2026-11-08 · Ticket : [#26](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/26) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · ✅ base = vérifié en base après migrations · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

Classes de test :
- `ArticleOverviewIT` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/articleoverview/controller/ArticleOverviewIT.java`
- `ArticleOverviewServiceImplTest` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/articleoverview/service/impl/ArticleOverviewServiceImplTest.java`
- `PromotionServiceImplTest` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/promotion/service/impl/PromotionServiceImplTest.java`
- `ArchitectureTest` = `valimbavaka-core/src/test/java/org/vvs/valimbavaka/ArchitectureTest.java` (aucun cycle entre features, `@HasPermission`, service en lecture seule)
- `article-sheet.spec.ts` = `valimbavaka-ui/src/app/features/articles/data-access/`
- `article-detail-page.spec.ts` = `valimbavaka-ui/src/app/features/articles/pages/article-detail-page/`

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (fiche + bouton « Modifier »), `SELLER` (fiche en lecture, `articles.read`), `DELIVERY_DRIVER` (aucun accès) |
| Données de départ | un produit fini « Savon noir 100 g » (ART-0031, gamme Zezika, coût 3 000 Ar, prix 8 000 Ar, seuil 40) ; une matière première « Huile de coco » (kg) avec 10 kg reçus en entrepôt ; un lot validé LOT-00012 : 12 savons produits avec 3 kg d'huile ; un transfert de 2 savons entrepôt → boutique ; promotions actives sur l'article, sur sa gamme, sur tout le catalogue, une terminée et une sur un autre article |
| Migration | aucune (lecture seule, permission `articles.read` existante) |
| Tickets à avoir déployés avant | ART-01, STK-01, PRD-01 à PRD-04 (dans `develop`), PRO-01 et PRO-04 (PR empilées : branche de base `feature/PRO-04-liste-detail-promotions`) |

## Scénarios issus des critères d'acceptation

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Les 4 blocs sont visibles sans navigation supplémentaire | ouverture de `/articles/article-soap` | un seul `GET /api/v1/articles/article-soap/overview` (+ gammes) ; titres « Stock par emplacement », « Lots de production », « Promotions », « Journal de l'article » sur la même page | `article-detail-page.spec.ts` « should show the four blocks of the sheet without further navigation » | ✅ auto |
| 1b | Le serveur assemble les 4 blocs en un appel (produit fini, vendeuse) | savon : 12 produits, 2 transférés en boutique | `stock.total` 12, entrepôt 10, boutique 2 ; 1 lot `LOT-…` rôle `OUTPUT`, quantité 12, `COMPLETED` ; promotions « article », « gamme », « catalogue » présentes, « terminee » et « autre » absentes ; 3 mouvements, le plus récent `TRANSFER` +2, le plus ancien `PRODUCTION` | `ArticleOverviewIT.getOverview_finishedProductBySeller_showsStockBatchPromotionsAndMovements` | ✅ auto |
| 1c | Matière première : lots où elle entre, aucune promotion | huile : 10 kg reçus, 3 kg consommés par le lot ; une promotion `ALL` active | `stock.total` 7 ; `activePromotions` vide ; lot rôle `INPUT`, quantité 3, `outputArticleId` = savon ; 2 mouvements | `ArticleOverviewIT.getOverview_rawMaterial_showsTheConsumedQuantityAndNoPromotion` | ✅ auto |
| 1d | Service : un appel par bloc, 10 éléments au plus | savon Botanika | `getActivePromotionsFor(savon, Botanika)`, `getRecentBatchesOf(savon, 10)`, `getRecentMovements(savon, 10)` ; lot `OUTPUT` 30, code du produit `ART-0001` | `ArticleOverviewServiceImplTest.getOverview_finishedProduct_assemblesTheFourBlocksWithTheTenMostRecentItems` | ✅ auto |
| 1e | Service : matière première consommée par un lot à 2 entrants | lot 30 savons avec base 1,5 et huile 0,45 | rôle `INPUT`, quantité 0,45 (celle de l'huile, pas la somme), nom du produit obtenu ; promotions jamais lues | `ArticleOverviewServiceImplTest.getOverview_rawMaterial_skipsPromotionsAndGivesTheConsumedQuantity` | ✅ auto |
| 1f | Promotions qui visent l'article : article, gamme et catalogue, jour d'Antananarivo | horloge 2026-11-07T22:00Z (= 8 nov. à Tana) | dépôt interrogé avec `{article, gamme}`, `ALL`, 2026-11-08 ; statut `ACTIVE` ; sans gamme : `{article}` seul | `PromotionServiceImplTest.getActivePromotionsFor_articleWithRange_looksForTheArticleItsRangeAndTheCatalogueOnTheBusinessDay`, `PromotionServiceImplTest.getActivePromotionsFor_articleWithoutRange_looksForTheArticleOnly` | ✅ auto |
| 1g | Écran : marge, valeur au coût, statut de stock, libellés | coût 3 000, prix 8 000, total 12, seuil 40 | marge 63 %, valeur 36 000 Ar, « Stock faible », gamme Zezika, emplacement du mouvement résolu (« Boutique ») | `article-sheet.spec.ts` « calcule la marge, la valeur au coût et le statut… » | ✅ auto |
| 1h | Écran : lot où l'article est un entrant, blocs vides | huile (kg), 0,45 consommé, aucune promotion ni mouvement | « Consommé : 0.45 kg », « ART-0031 · Savon noir 100 g », « Aucune promotion active ne vise cet article. », « Aucun mouvement de stock pour cet article. » | `article-detail-page.spec.ts` « should show an input batch with the product it makes and empty blocks as messages » | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET /api/v1/articles/{uuid}/overview` | 401 `UNAUTHENTICATED` | `ArticleOverviewIT.getOverview_withoutToken_returns401` | ✅ auto |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `ArticleOverviewIT.getOverview_byDeliveryDriver_returns403` | ✅ auto |
| Article inconnu | UUID aléatoire | 404 `NOT_FOUND` ; côté service, aucun autre bloc n'est lu | `ArticleOverviewIT.getOverview_unknownArticle_returns404`, `ArticleOverviewServiceImplTest.getOverview_unknownArticle_throwsNotFoundWithoutReadingTheOtherBlocks` | ✅ auto |
| Article inconnu à l'écran | réponse 404 | état d'erreur avec « Réessayer », pas de titre | `article-detail-page.spec.ts` « should show the error state when the article is unknown » | ✅ auto |
| Bouton « Modifier » réservé à `articles.update` | administrateur / vendeuse | lien `/articles/{id}/edit` pour l'administrateur, absent pour la vendeuse | `article-detail-page.spec.ts` « should link the edit button… », « should hide the edit button… » | ✅ auto |
| Endpoint protégé et en lecture seule | règles ArchUnit | `@HasPermission("articles.read")`, service `@Transactional(readOnly = true)`, pas de cycle entre la nouvelle feature et `article`, `stock`, `promotion`, `production` | `ArchitectureTest` | ✅ auto |
| Mass assignment, 409, 422, journal | — | sans objet : aucune écriture, aucun corps accepté, aucune ligne de journal | — | ⏭ hors périmètre (lecture seule) |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Article sans aucun historique | matière première neuve | 4 emplacements à 0, `productionBatches` et `recentMovements` vides (pas d'erreur) | `ArticleOverviewIT.getOverview_articleWithoutHistory_returnsEmptyBlocks` | ✅ auto |
| Promotion terminée hier | `endsOn` = J-1 | absente | `ArticleOverviewIT.getOverview_finishedProductBySeller_…` | ✅ auto |
| Promotion d'un autre article | `ARTICLES` sur un autre savon | absente | `ArticleOverviewIT.getOverview_finishedProductBySeller_…` | ✅ auto |
| Promotion « tout le catalogue » sur une matière première | `ALL` active | absente : seul un produit fini se vend | `ArticleOverviewIT.getOverview_rawMaterial_…`, `ArticleOverviewServiceImplTest.getOverview_rawMaterial_…` | ✅ auto |
| Plus de 10 lots ou mouvements | 11e lot | seuls les 10 plus récents (limite passée au service : 10) ; la liste complète reste sur `/production` et `/stocks` | `ArticleOverviewServiceImplTest.getOverview_finishedProduct_…` (limite 10 vérifiée par les appels) | ✅ auto |
| Stock nul ou négatif, au seuil | total -1 / 39,5 / 40 pour un seuil 40 | « Rupture » / « Stock faible » / « Disponible » | `article-sheet.spec.ts` « suit la règle de la liste… » | ✅ auto |
| Article sans prix ni coût | matière première | marge et valeur affichées « — » | `article-sheet.spec.ts` « laisse marge et valeur vides… » | ✅ auto |
| Article désactivé | `active = false` | fiche lisible, badge « Désactivé » | — (lecture du gabarit) | 🧪 recette |
| Hors ligne | perte de réseau à l'ouverture | état d'erreur avec « Réessayer » (lecture seule, pas de file hors ligne) | — | 🧪 recette |

## Tests existants

- Backend : `ArticleOverviewIT`, `ArticleOverviewServiceImplTest`, `PromotionServiceImplTest` (2 nouveaux tests), `ArchitectureTest`.
- Frontend : `article-sheet.spec.ts`, `article-detail-page.spec.ts` (réécrit : la fiche ne lit plus les données de démonstration).

## Recette manuelle (🧪)

| # | Étape | Résultat attendu | Statut |
|---|---|---|---|
| R1 | Ouvrir la fiche d'un savon réel depuis la recherche globale (SYS-03) ou une alerte du cockpit (DSH-03) | identité, stock par emplacement, lots, promotions et journal sur un seul écran, sans clic supplémentaire | 🧪 recette |
| R2 | Ouvrir la fiche d'une matière première consommée par un lot | lot affiché « Consommé : … kg · ART-… · <produit> » ; bloc Promotions vide avec son message | 🧪 recette |
| R3 | Désactiver un article (ART-01) puis rouvrir sa fiche | fiche lisible, badge « Désactivé » | 🧪 recette |
| R4 | Couper le réseau puis ouvrir une fiche | état d'erreur, « Réessayer » recharge la fiche une fois le réseau revenu | 🧪 recette |

## Résultat de l'exécution

- Date : 2026-11-08, CI GitHub (workflows « CI » run 42419802843 et « Convention VVS » run 42419805106, commit `78f4acf6`), tous les jobs verts.
- Backend (`mvn -B -ntp verify`) : 422 tests, 0 échec ; dont `ArticleOverviewIT` 6/6, `ArticleOverviewServiceImplTest` 3/3, `PromotionServiceImplTest` 9/9 (2 nouveaux), `ArchitectureTest` 16/16. Spotless et Checkstyle : 0 violation.
- Frontend (`ng test`) : 73 fichiers, 432 tests, 0 échec ; dont `article-detail-page.spec.ts` 5/5 et `article-sheet.spec.ts` 3/3. Prettier et ESLint verts.
- Anomalies : aucune.
- Étapes 🧪 restantes : 4 (R1 à R4) + 2 cas limites.
