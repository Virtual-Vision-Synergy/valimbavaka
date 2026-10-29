# Scénarios de test — ART-05 · Recherche et filtrage des articles

Statut : ☐ tests écrits, à lancer (CI ou local) · Ticket : [#25](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/25) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · 🔍 code = vérifié par lecture du code (aucun test) · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à vérifier à la main · ☐ = pas encore exécuté.

Ce ticket est **uniquement frontend** : la recherche et les filtres sont faits par `GET /api/v1/articles` (ART-01, déjà testé côté serveur). Les tests front sont dans `article-list-page.spec.ts` (`describe('ArticleListPage')`, désigné ci-dessous par « spec liste »).

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (liste + bouton « Nouvel article »), `SELLER` (liste seule, `articles.read`) |
| Données de départ | `Savon noir 100 g` (`ART-0031`, produit fini, gamme Botanika, actif), `Huile de ricin` (`ART-0032`, matière première, sans gamme, inactif) ; gammes du référentiel : Botanika, Zezika |
| Tickets à avoir déployés avant | ART-01 (API articles), SYS-05 (gammes) |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | La liste se met à jour dynamiquement selon la recherche et les filtres | Ouverture de la page sans critère | `GET /articles?page=1&size=10&sortBy=name`, sans `search`, `type` ni `rangeId` ; deux lignes affichées | spec liste « should load the first page sorted by name, without criteria, on opening » | ☐ à lancer |
| 1b | idem (recherche) | Saisie de `"  savon "`, puis 350 ms de pause | une requête avec `search=savon` (rognée) ; la liste ne garde que `Savon noir 100 g` | spec liste « should reload with the trimmed search once typing has paused » | ☐ à lancer |
| 1c | idem (recherche, bruit) | Saisie de `savon` puis `savon  ` | aucune nouvelle requête (seuls des espaces diffèrent) | spec liste « should not query again while the search only differs by spaces » | ☐ à lancer |
| 1d | idem (gamme) | Clic sur « Botanika » puis « Toutes gammes » | `rangeId=<id Botanika>` envoyé, puis retiré | spec liste « should reload with the chosen range id and drop it for all ranges » | ☐ à lancer |
| 1e | idem (type) | Choix de « Matière première » | `type=RAW_MATERIAL` envoyé ; seule `Huile de ricin` reste | spec liste « should reload with the chosen type and show only the returned articles » | ☐ à lancer |
| 1f | idem (combinaison) | Gamme Botanika + type `FINISHED` + recherche `savon` | une seule requête avec les trois critères | spec liste « should combine search, range and type in a single request » | ☐ à lancer |
| 1g | idem (pagination) | Page 2 sur 25 résultats, puis changement de type | la requête repart à `page=1` | spec liste « should go back to the first page when a criterion changes » | ☐ à lancer |
| 1h | idem (affichage) | Liste reçue | code `ART-0031`, état « Actif » / « Inactif » affichés ; stock affiché `—` | spec liste « should show the code and the active state of each article » | ☐ à lancer |
| 1i | Recherche côté serveur (nom, code, code-barres, casse ignorée) | `search` = nom, code ou code-barres | articles trouvés quelle que soit la casse | `ArticleRepositoryIT.search_byNameCodeOrBarcode_findsTheArticleWhateverTheCase` (ART-01) | ☐ à lancer |
| 1j | Filtres côté serveur | type + gamme + état combinés | seuls les articles correspondants | `ArticleRepositoryIT.search_withTypeRangeAndStatusFilters_returnsOnlyMatchingArticles` (ART-01) ; `ArticleSecurityIT.getArticles_byTypeAndSearch_returnsOnlyMatchingArticles` | ☐ à lancer |
| 1k | Recette manuelle | Taper `savon`, choisir une gamme, changer de type dans l'application réelle | la liste se met à jour sans recharger la page | — | 🧪 recette |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Appel sans jeton | `GET /api/v1/articles` | 401 `UNAUTHENTICATED` | `ArticleSecurityIT.getArticles_withoutToken_returns401` (ART-01) | ☐ à lancer |
| Mauvais rôle | jeton livreur | 403 `FORBIDDEN` | `ArticleSecurityIT.getArticles_withDeliveryDriverRole_returns403` (ART-01) | ☐ à lancer |
| Rôle vendeuse | jeton vendeuse | 200 ; pas de bouton « Nouvel article » | `ArticleSecurityIT.getArticles_withSellerRole_returns200` ; spec liste « should hide the new article button without the create permission » | ☐ à lancer |
| Bouton de création pour l'administrateur | jeton admin | lien « Nouvel article » présent | spec liste « should link the new article button to the article sheet for an administrator » | ☐ à lancer |
| Type inconnu ou `size` > 100 | `type=PACKAGING`, `size=101` | refusé (400 / 422) | `ArticleSecurityIT.getArticles_unknownTypeOrSizeAboveLimit_isRefused` (ART-01) | ☐ à lancer |
| Tri hors liste blanche | `sortBy=password` | tri par défaut | `ArticleSecurityIT.getArticles_sortOutsideTheWhiteList_fallsBackToDefaultSort` (ART-01) | ☐ à lancer |
| Échec de la liste (403/500) | réponse 403 | état d'erreur avec bouton « Réessayer » | spec liste « should offer a retry when the list cannot be loaded » | ☐ à lancer |
| Échec des gammes | `/product-ranges` répond 403 | la liste reste utilisable, sans pastille de gamme à choisir | spec liste « should keep the list usable when the ranges cannot be loaded » | ☐ à lancer |
| Mass assignment, 404, 409 | — | ⏭ sans objet : ticket en lecture seule | — | ⏭ hors périmètre |

## Cas limites et hors ligne

- **Aucun résultat** : état « vide » à la place du tableau — spec liste « should show the empty state when no article matches ».
- **Réponses qui se doublent** : un nouveau critère annule la requête précédente (`rxResource`) — spec liste « should ignore the answer of a request overtaken by a newer criterion ».
- **Frappe rapide** : une seule requête par pause de 300 ms (cas 1b, 1c).
- **Hors ligne** : la liste est une lecture, elle n'utilise pas la file `OutboxService` ; hors ligne, l'état d'erreur et « Réessayer » s'affichent (comportement de `ListState`).
- **Stock** : les colonnes « Stock total » et le bouton « Stock faible » restent vides ou désactivés (`—`) tant que [STK-01](../STK-01/description.md) n'expose pas le stock. ⏭ hors périmètre.
- **Ancienne valeur « Emballage »** : retirée du filtre de type (le contrat d'ART-01 ne connaît que MP, SF, produit fini).
- **Recherche sur `%` ou `_`** : traitée par le serveur (ART-01) ; non testée ici.

## Résultat de l'exécution

- Date : non exécuté (consigne : pas de `ng test` par l'auteur) ; `tsc --noEmit`, ESLint et Prettier passent sur les fichiers modifiés.
- Environnement : à lancer en local ou en CI (`ng test`).
- Anomalies ouvertes : aucune connue ; colonnes de stock à brancher par STK-01.
