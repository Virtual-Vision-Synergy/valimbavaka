# Scénarios de test — SYS-03 · Recherche globale (palette de commandes)

Statut : ☐ tests écrits, à lancer (CI ou local) · Ticket : [#106](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/106) · [description](description.md) · [guide](guide.md)

Légende de la colonne « Statut » : ✅ auto = test automatique vert · 🔍 code = vérifié par lecture du code (aucun test) · ⏭ hors périmètre = renvoyé à un autre ticket · 🧪 recette = à vérifier à la main · ☐ = pas encore exécuté.

Ce ticket est **uniquement frontend** : les articles viennent de `GET /api/v1/articles?search=` (ART-01, déjà testé côté serveur), les pages du menu (`core/layout/menu.ts`) et les créations sont filtrées dans le navigateur. Tests front : `search-index.spec.ts` (`describe('search index')`, « spec index ») et `search-dialog.spec.ts` (`describe('SearchDialog')`, « spec palette »), dans `valimbavaka-ui/src/app/core/layout/search-dialog/`.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | `ADMIN` (articles, toutes les créations, toutes les pages) ; `SELLER` (articles, « Nouvelle vente », ses pages) ; `DELIVERY_DRIVER` (ni articles ni création) |
| Données de départ | `Savon noir 100 g` (`ART-0031`, produit fini, 12 000 Ar, code-barres `6201234567890`) ; `Beurre de karité brut` (`ART-0045`, matière première, sans prix) |
| Tickets à avoir déployés avant | ART-01 (API articles) ; ART-05 (liste des articles, même recherche serveur) |

## Scénarios issus des critères d'acceptation

| # | Critère | Étant donné / Quand / Alors | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | La palette filtre en temps réel navigation, création et articles | Admin, Ctrl K, saisie `fourn` | tout de suite, sans appel serveur : « Créer un fournisseur » puis « Catalogue › Fournisseurs » | spec palette « should filter pages and creations at each keystroke, before any server call » | ☐ à lancer |
| 1b | idem (articles) | Saisie `  savon `, pause de 350 ms | une seule requête `GET /api/v1/articles?page=1&size=5&sortBy=name&search=savon` ; résultat « Savon noir 100 g » avec `Savon` surligné, statut « 1 résultat(s) » | spec palette « should ask the server for one page of articles once typing has paused » | ☐ à lancer |
| 1c | idem (contenu d'un résultat article) | Article produit fini à 12 000 Ar | titre = nom, ligne secondaire `ART-0031 · 12 000 Ar`, type « Produit fini » à droite, lien `/articles/<id>` | spec index `buildGroups_articleTerm_listsServerArticlesWithCodeAndPrice` | ☐ à lancer |
| 1d | idem (article sans prix) | Matière première `ART-0045` | ligne secondaire `ART-0045` seule, type « Matière première » | spec index `buildGroups_articleWithoutPrice_showsCodeOnly` | ☐ à lancer |
| 1e | idem (code, code-barres) | Saisie `art-0031` puis `62012345` | l'article reste proposé (même recherche que le serveur : nom, code, code-barres) | spec index `buildGroups_codeOrBarcodeTerm_keepsTheArticle` | ☐ à lancer |
| 1f | idem (frappe plus rapide que le serveur) | Page reçue pour `savon`, saisie devenue `savon n` | les articles qui ne correspondent plus disparaissent sans attendre la réponse suivante | spec index `buildGroups_staleServerPage_dropsArticlesThatNoLongerMatch` | ☐ à lancer |
| 1g | idem (création) | Admin, saisie `créer un f` | seul « Créer un fournisseur » (`/suppliers/new`) | spec index `buildGroups_creationTerm_filtersCreateActionsByLabel` | ☐ à lancer |
| 1h | idem (navigation) | Saisie `fourn` | admin : « Catalogue › Fournisseurs » ; vendeuse : rien (pas `suppliers.read`) | spec index `buildGroups_navigationTerm_listsAllowedPagesOnly` | ☐ à lancer |
| 1i | idem (ordre) | Saisie `article` | groupes dans l'ordre Articles, Créer, Aller à | spec index `buildGroups_mixedTerm_ordersArticlesThenActionsThenPages` | ☐ à lancer |
| 1j | idem (ouvrir au clavier) | Résultat article actif, touche Entrée | navigation vers `/articles/<id>` et fermeture de la palette | spec palette « should open the chosen article with Enter » | ☐ à lancer |
| 1k | idem (flèches) | Saisie `fourn`, ↓ puis Entrée | navigation vers `/suppliers` (2ᵉ résultat) | spec palette « should move the active result with the arrow keys » | ☐ à lancer |
| 1l | idem (surlignage) | `highlightParts('Savon noir', 'avo')` | `S` / `avo` (surligné) / `n noir`, sans `innerHTML` | spec index `highlightParts_matchInside_splitsAroundTheMatch`, `highlightParts_noMatch_returnsWholeText` | ☐ à lancer |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Appel sans jeton | `GET /api/v1/articles` | 401 `UNAUTHENTICATED` | `ArticleSecurityIT.getArticles_withoutToken_returns401` (ART-01) | ☐ à lancer |
| Mauvais rôle côté serveur | jeton livreur sur `GET /api/v1/articles` | 403 `FORBIDDEN` | `ArticleSecurityIT.getArticles_withDeliveryDriverRole_returns403` (ART-01) | ☐ à lancer |
| Mauvais rôle côté palette | livreur, saisie `savon` | aucun appel aux articles, « Aucun résultat pour « savon ». » | spec palette « should not search articles for a delivery driver » ; spec index `buildGroups_withoutArticlesRead_hidesArticles` | ☐ à lancer |
| Créations réservées | vendeuse, saisie `créer` | aucune création proposée (`articles.create`, `suppliers.create`, `users.create` absents) | spec palette « should offer a seller no administrator creation » ; spec index `buildGroups_sellerTypingCreer_offersNoAdminCreation` | ☐ à lancer |
| Échec de l'appel articles | erreur réseau ou 5xx | pages et créations restent listées ; statut « Articles indisponibles pour le moment… » | spec palette « should keep pages and actions when the articles cannot be read » | ☐ à lancer |
| Mass assignment, 404, 409, 422 | — | sans objet : la palette ne fait qu'une lecture paginée (`size=5`) | — | ⏭ hors périmètre |

## Cas limites et hors ligne

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Saisie trop courte | ` s ` (1 caractère après rognage) | aucune requête, aucun résultat, aide « Saisissez au moins 2 caractères… » | spec palette « should not call the server below two characters » ; spec index `normalizeTerm_oneCharacterAfterTrim_returnsEmpty`, `buildGroups_shortTerm_returnsNothing` | ☐ à lancer |
| Borne exacte | ` SA ` | cherché comme `sa` (2 caractères, minuscules) | spec index `normalizeTerm_oneCharacterAfterTrim_returnsEmpty` | ☐ à lancer |
| Espaces autour | `  savon ` | `search=savon` | cas 1b | ☐ à lancer |
| Frappe rapide | `s`, `sa`, `sav`… en moins de 300 ms | une seule requête après la pause ; une saisie identique (après rognage) ne relance pas | cas 1b (`debounceTime` + `distinctUntilChanged` dans `search-dialog.ts`) | ☐ à lancer |
| Réponses qui se doublent | requête `sav` lente, puis `savon` | la requête précédente est annulée (`rxResource`) et la liste suit la saisie courante | cas 1f | ☐ à lancer |
| Plus de 5 articles | `savon` correspond à 12 articles | 5 au plus (une page `size=5`, triée par nom) ; la liste complète reste dans « Catalogue › Articles » | cas 1b | ☐ à lancer |
| Article désactivé | `ART-0045` inactif | proposé quand même (la palette sert à retrouver une fiche, pas à vendre) | 🔍 code `search-dialog.ts` (aucun filtre `active`) | 🔍 code |
| Caractères `%` ou `_` | `10%` | traités par le serveur (ART-01) puis refiltrés localement sur le texte | — | ⏭ hors périmètre (ART-01) |
| Hors ligne | réseau coupé | appel articles en erreur : statut « Articles indisponibles… », pages et créations utilisables | cas « Échec de l'appel articles » | ☐ à lancer |
| Fermeture | bouton Échap, voile, choix d'un résultat | la palette émet `closed` ; la barre du haut la retire et rend le focus au bouton de recherche (`top-bar.ts`, `closeSearch`) | spec palette « should emit closed when the close button is pressed » | ☐ à lancer |

## Recette manuelle (🧪)

| Étape | Résultat attendu | Statut |
|---|---|---|
| Ctrl K (ou Cmd K) depuis n'importe quelle page, saisir `sav` | palette ouverte, focus dans le champ, articles affichés après une courte pause | 🧪 recette |
| Ouvrir un article depuis la palette | fiche `/articles/<id>` (détail encore sur données de démonstration tant qu'[ART-06](../ART-06/description.md) n'est pas livré) | 🧪 recette |
| Lecteur d'écran (NVDA) : flèches dans la liste | l'option active est annoncée (`aria-activedescendant`), le nombre de résultats aussi (`role="status"`) | 🧪 recette |
| Mobile (≤ 640 px) | palette lisible, sans débordement | 🧪 recette |

## Résultat de l'exécution

- Date : à lancer (CI ou local).
- Environnement : `tsc --noEmit` (app et spec), ESLint et Prettier passent sur les fichiers modifiés ; `ng test` à lancer par la CI.
- Anomalies ouvertes : aucune connue. Étapes 🧪 restantes : 4.
