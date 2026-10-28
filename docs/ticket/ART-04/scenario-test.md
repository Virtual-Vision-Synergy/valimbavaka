# Scénarios de test — ART-04 · Type d'article et usage en production

Statut : ☐ tests écrits, à lancer (CI ou local) · Ticket : [#24](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/24) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · 🔍 code = vérifié par lecture du code (aucun test) · ❌ écart = comportement différent de l'attendu · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à faire à la main · ☐ à lancer = test écrit, pas encore exécuté. Détail : [CONSIGNE.md](../CONSIGNE.md#4-remplir-le-scénario-de-test).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (accès), `SELLER` (lecture autorisée, `articles.read`), `DELIVERY_DRIVER` (refusé) |
| Données de départ | un catalogue préfixé `Prod-xxxxxxxx-` : `…MP` (matière première), `…SF` (semi-fini), `…PF` (produit fini, gamme existante), `…MP-inactif` (matière première désactivée) |
| Tickets à avoir déployés avant | ART-01 (fiche article), AUTH-01 |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Les MP/SF apparaissent comme entrants possibles | Catalogue ci-dessus ; `GET /api/v1/articles/production?usage=INPUT&search=Prod-xxxxxxxx-` | 200, `totalCount = 2` : `…MP` et `…SF` ; ni `…PF` ni `…MP-inactif` | `ArticleProductionIT.getProductionArticles_inputUsage_listsActiveRawMaterialsAndSemiFinishedOnly` | ☐ à lancer |
| 1b | idem (règle) | `ProductionUsage.INPUT` | accepte `RAW_MATERIAL` et `SEMI_FINISHED`, refuse `FINISHED` | `ProductionUsageTest.accepts_rawMaterialAndSemiFinished_areInputsOnly` | ☐ à lancer |
| 1c | idem (requête) | `getProductionArticles(search=" huile ", INPUT)` | le dépôt reçoit `"huile"` et les types `{RAW_MATERIAL, SEMI_FINISHED}` | `ArticleProductionServiceTest.getProductionArticles_input_searchesActiveRawMaterialsAndSemiFinished` | ☐ à lancer |
| 1d | idem (contrôle serveur pour PRD-01) | matière première active demandée comme entrant | fiche renvoyée | `ArticleProductionServiceTest.requireProductionArticle_activeRawMaterialAsInput_returnsTheArticle` | ☐ à lancer |
| 1e | idem (front) | `listForProduction('INPUT', {search: 'huile'})` | `GET /api/v1/articles/production?usage=INPUT&search=huile`, sans `type` envoyé | `article-service.spec.ts` « demande au serveur les entrants de production (MP et SF) avec la recherche » | ☐ à lancer |
| 2 | Les produits finis apparaissent comme sortants possibles | Catalogue ci-dessus ; `usage=OUTPUT` | 200, `totalCount = 1` : `…PF`, `type = FINISHED` | `ArticleProductionIT.getProductionArticles_outputUsage_listsActiveFinishedProductsOnly` | ☐ à lancer |
| 2b | idem (règle) | `ProductionUsage.OUTPUT` | accepte `FINISHED` seulement | `ProductionUsageTest.accepts_finishedProduct_isOutputOnly` | ☐ à lancer |
| 2c | idem (requête) | `getProductionArticles(OUTPUT)` sans recherche | le dépôt reçoit `""` et `{FINISHED}` | `ArticleProductionServiceTest.getProductionArticles_output_searchesActiveFinishedProductsOnly` | ☐ à lancer |
| 2d | idem (contrôle serveur) | produit fini actif demandé comme sortant | fiche renvoyée | `ArticleProductionServiceTest.requireProductionArticle_activeFinishedProductAsOutput_returnsTheArticle` | ☐ à lancer |
| 2e | idem (front) | `listForProduction('OUTPUT', …)` | `usage=OUTPUT`, page reçue | `article-service.spec.ts` « demande au serveur les sortants de production (produits finis) » | ☐ à lancer |
| 3 | Chaque type a un et un seul usage | pour les 3 types | exactement un usage accepte chaque type (un nouveau type casse le test tant qu'il n'est pas classé) | `ProductionUsageTest.types_everyArticleType_hasExactlyOneUsage` | ☐ à lancer |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Liste sans jeton | `GET …/production?usage=INPUT` | 401 `UNAUTHENTICATED` | `ArticleProductionIT.getProductionArticles_withoutToken_returns401` | ☐ à lancer |
| Rôle sans `articles.read` | jeton livreur | 403 `FORBIDDEN` | `ArticleProductionIT.getProductionArticles_withDeliveryDriverRole_returns403` | ☐ à lancer |
| Rôle avec `articles.read` | jeton vendeuse, `usage=OUTPUT` | 200 | `ArticleProductionIT.getProductionArticles_withSellerRole_returns200` | ☐ à lancer |
| Usage inconnu | `usage=PACKAGING` | 400 `MALFORMED_REQUEST` | `ArticleProductionIT.getProductionArticles_unknownUsage_returns400` | ☐ à lancer |
| Usage absent | `GET …/production` | 400 `MALFORMED_REQUEST` | `ArticleProductionIT.getProductionArticles_missingUsage_returns400` | ☐ à lancer |
| Produit fini comme entrant (PRD-01) | `requireProductionArticle(id, INPUT)` | 422 `VALIDATION_FAILED` « Seules les matières premières et les semi-finis peuvent entrer en production. » | `ArticleProductionServiceTest.requireProductionArticle_finishedProductAsInput_throwsValidationFailed` | ☐ à lancer |
| Semi-fini comme sortant (PRD-01) | `requireProductionArticle(id, OUTPUT)` | 422 « Seuls les produits finis peuvent sortir de production. » | `ArticleProductionServiceTest.requireProductionArticle_semiFinishedAsOutput_throwsValidationFailed` | ☐ à lancer |
| Article désactivé (PRD-01) | MP `ART-0042` désactivée | 422 « Article désactivé, inutilisable en production : ART-0042 » | `ArticleProductionServiceTest.requireProductionArticle_inactiveRawMaterial_throwsValidationFailed` | ☐ à lancer |
| Article introuvable (PRD-01) | identifiant inconnu | 404 `NOT_FOUND` | `ArticleProductionServiceTest.requireProductionArticle_unknownId_throwsNotFound` | ☐ à lancer |
| Mass assignment, 409, journal | — | sans objet : lecture seule, aucune écriture ni trace | — | ⏭ hors périmètre |

## Cas limites et hors ligne

| Cas | Résultat attendu | Test | Statut |
|---|---|---|---|
| Aucun article ne correspond | 200, `items = []`, `totalCount = 0` | `ArticleProductionIT.getProductionArticles_noMatchingArticle_returnsEmptyPage` | ☐ à lancer |
| Article désactivé | absent des deux listes | couvert par les lignes 1 et 2 (`…MP-inactif`) | ☐ à lancer |
| Pagination / taille | `page` base 1, `size` ≤ 100, tri par nom croissant par défaut, tri en liste blanche (`createdAt`, `code`, `name`, `type`, `price`) | 🔍 code : `PageQuery` partagé, `ArticleServiceImpl.getProductionArticles` | 🔍 code |
| Hors ligne | lecture seule : pas de file `Idempotency-Key` ; l'erreur réseau remonte à l'appelant (PRD-01) | — | ⏭ PRD-01 |
| Écran de lot de production | les sélecteurs entrants/sortants utilisent `listForProduction` | — | ⏭ PRD-01 |

## Recette manuelle (🧪)

1. Connecté en administrateur, créer une MP, un SF et un produit fini, puis désactiver une deuxième MP.
2. Appeler `GET /api/v1/articles/production?usage=INPUT` (Swagger) : seuls la MP active et le SF apparaissent.
3. Appeler avec `usage=OUTPUT` : seul le produit fini apparaît.
4. Avec un compte livreur : 403.

## Résultat de l'exécution

- Date : à lancer (CI ou local) — aucun test exécuté par le traiteur (`mvn verify`, `ng test`).
- Environnement : —
- Anomalies ouvertes : aucune connue. Le formatage Spotless n'a pas été appliqué en local (Maven absent du poste) : lancer `mvn spotless:apply` si la CI le signale.
