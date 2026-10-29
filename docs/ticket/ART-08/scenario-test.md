# Scénarios de test — ART-08 · Export de la liste des articles

Statut : ☐ tests écrits, à lancer (CI ou local) · Ticket : [#28](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/28) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · 🔍 code = vérifié par lecture du code (aucun test) · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à vérifier à la main · ☐ = pas encore exécuté.

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | administrateur `admin.test@valimbavaka.mg` ; vendeuse et livreur créés par le test |
| Données de départ | un article produit fini « Article-<uuid> » (coût `3120.5`, prix `8500`, seuil `40`, unité `PIECE`) ; une matière première de même préfixe ; un article désactivé |
| Tickets à avoir déployés avant | ART-01 (fiche article), ART-05 (liste branchée), SYS-01 (brique `shared/excel`) |

Choix de format (le ticket ne le précisait pas, validé avec le demandeur) : **XLSX**, onglet « Articles », fichier `articles.xlsx`, colonnes Code, Nom, Code-barres, Type, Unité, Prix de vente, Seuil minimum, Actif. **Le coût d'achat n'est jamais exporté.** Permission `articles.export` : administrateur seulement.

## Scénarios issus des critères d'acceptation

Critère : « Format d'export à préciser avant développement » — précisé ci-dessus ; l'administrateur télécharge la liste filtrée au format XLSX.

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Export complet | `GET /api/v1/articles/exportable/export?search=<nom>` avec le jeton admin | 200, `Content-Disposition: attachment; filename="articles.xlsx"`, onglet « Articles », en-têtes exacts, ligne `ART-xxxx / <nom> / (vide) / FINISHED / PIECE / 8 500,00 / 40,00 / VRAI` ; aucune colonne « Coût » | `ArticleExportIT.exportArticles_byAdmin_returnsXlsxWithHeadersAndNoCostColumn` | ☐ |
| 1b | Filtre par type | un produit fini et une matière première de même préfixe, `type=RAW_MATERIAL` | une seule ligne de données : la matière première | `ArticleExportIT.exportArticles_filteredByType_containsOnlyMatchingArticles` | ☐ |
| 1c | Article désactivé | article passé à `active=false` par `PATCH …/status` | exporté, colonne « Actif » à FAUX | `ArticleExportIT.exportArticles_inactiveArticle_isExportedWithActiveFalse` | ☐ |
| 1d | Aperçu JSON des lignes | `GET /api/v1/articles/exportable?search=<nom>` | 200, `items[0].name` = nom, ni `cost` ni `id` | `ArticleExportIT.getExportableArticles_byAdmin_returnsJsonRowsWithoutCost` | ☐ |
| 1e | Écran : bouton visible de l'administrateur | page liste ouverte en ADMIN | bouton « Exporter » présent | `article-list-page.export.spec.ts` « télécharge le fichier annoncé par le serveur, sans page ni taille dans l'URL » | ☐ |
| 1f | Écran : téléchargement | clic sur « Exporter » | `GET …/exportable/export` sans `page` ni `size`, le navigateur télécharge le fichier, bouton de nouveau actif | `article-list-page.export.spec.ts` « télécharge le fichier annoncé par le serveur, sans page ni taille dans l'URL » | ☐ |
| 1g | Écran : critères de la liste | recherche « savon », type `FINISHED`, gamme `g1`, `active=false` | ces paramètres sont repris dans la requête d'export | `ArticleService` « télécharge l'export en xlsx avec les critères de la liste et sans pagination » | ☐ |
| 1h | Lecture du nom de fichier | `filename="articles.xlsx"`, `filename*=UTF-8''caf%C3%A9.xlsx`, sans en-tête | nom lu, décodé, ou nom par défaut | `download-file.spec.ts` (3 tests de `filenameOf`) | ☐ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET …/exportable/export` sans `Authorization` | 401 `UNAUTHENTICATED` | `ArticleExportIT.exportArticles_withoutToken_returns401` | ☐ |
| Vendeuse | jeton vendeuse (a `articles.read`, pas `articles.export`) | 403 `FORBIDDEN` sur l'export | `ArticleExportIT.exportArticles_withSellerRole_returns403` | ☐ |
| Vendeuse, aperçu JSON | idem sur `…/exportable` | 403 | `ArticleExportIT.getExportableArticles_withSellerRole_returns403` | ☐ |
| Livreur | jeton livreur | 403 | `ArticleExportIT.exportArticles_withDeliveryDriverRole_returns403` | ☐ |
| Type inconnu | `type=PACKAGING` | 400 `MALFORMED_REQUEST` | `ArticleExportIT.exportArticles_unknownType_returns400` | ☐ |
| `size` trop grand | `size=10001` | 422 `VALIDATION_FAILED` | `ArticleExportIT.exportArticles_sizeAboveMaxRows_returns422` | ☐ |
| La liste n'a pas de route d'export | `GET /api/v1/articles/export` | 4xx (lu comme un identifiant) | `ArticleExportIT.exportArticles_onListRoute_isNotAvailable` | ☐ |
| Échec côté écran | le serveur répond 500 | toast `articles.exportFailed`, aucun téléchargement, bouton de nouveau actif | `article-list-page.export.spec.ts` « prévient quand le serveur refuse ou échoue, et rend la main » | ☐ |
| Bouton absent pour la vendeuse | page ouverte en SELLER | pas de bouton « Exporter » (le serveur décide quand même) | `article-list-page.export.spec.ts` « n'affiche pas le bouton à la vendeuse » | ☐ |
| Mass assignment, 404, 409 | — | ⏭ sans objet : lecture seule, aucun corps | — | ⏭ |
| Journal | — | ⏭ lecture seule ; l'action `EXPORT` n'existe pas dans `journal_entries` (CHECK sur CREATE/UPDATE/DELETE), voir le guide | — | ⏭ |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Aucun résultat | `search=aucun-article-<uuid>` | fichier avec la seule ligne d'en-têtes | `ArticleExportIT.exportArticles_noMatch_returnsHeaderRowOnly` | ☐ |
| Critères vides | `exportList({ search: '' })` | aucun paramètre dans l'URL | `ArticleService` « n'envoie à l'export que les critères choisis » | ☐ |
| Plus de 10 000 articles | 10 001 lignes | 422 (`ExcelRowLimitExceededException`), à affiner par filtres | ⏭ couvert par SYS-01 (`ExcelExportIT.export_streamLongerThanMaxRows_returns422WithoutFile`) | ⏭ |
| Hors ligne | clic sans réseau | l'export n'est pas mis en file (lecture) : erreur, toast | 🔍 code : `ArticleService.exportList` passe par `HttpClient`, pas par `OutboxService` ; pas de test dédié | 🔍 code |
| Session expirée | jeton expiré pendant le clic | 401 → intercepteur d'authentification (AUTH-04) | ⏭ AUTH-04 | ⏭ |
| Tri | sans `sortBy` | trié par code croissant | 🔍 code : `ArticleServiceImpl.getExportableArticles` ; test à ajouter | 🔍 code |

## Tests existants

- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/article/controller/ArticleExportIT.java`
- `valimbavaka-ui/src/app/features/article/data-access/article-service.spec.ts`
- `valimbavaka-ui/src/app/features/articles/pages/article-list-page/article-list-page.export.spec.ts`
- `valimbavaka-ui/src/app/shared/data-access/download-file.spec.ts`
- Brique réutilisée : `valimbavaka-core/src/test/java/org/vvs/valimbavaka/shared/excel/` (SYS-01)

## Recette manuelle (🧪)

| Cas | Étapes | Résultat attendu | Statut |
|---|---|---|---|
| Ouverture du fichier | connecté en admin, filtrer « Botanika » + « Produit fini », cliquer « Exporter », ouvrir `articles.xlsx` dans Excel | accents corrects, prix `8 500,00`, pas de colonne coût, seulement les articles filtrés | 🧪 recette |
| Vendeuse | connectée en vendeuse, ouvrir la liste | pas de bouton « Exporter » | 🧪 recette |

## Résultat de l'exécution

- Date : à lancer (CI ou local : `mvn -B -ntp verify` dans `valimbavaka-core`, `npx ng test --watch=false` dans `valimbavaka-ui`).
- Environnement : le code backend compile (`test-compile` sans erreur) ; le frontend n'a pas pu être compilé ici (`node_modules` absent).
- Anomalies ouvertes : 1. (faible) l'export n'est pas tracé dans le journal, il faudrait une action `EXPORT` (migration du CHECK + `JournalAction`), à confier au ticket journal ; 2. (faible) tri par défaut sans test.
- Étapes 🧪 restantes : 2.
