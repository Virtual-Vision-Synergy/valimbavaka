# Guide d'intégration — SYS-03 · Recherche globale (palette de commandes)

Statut : ✅ écrit le 2026-11-08 — **tests à lancer** (voir « Résultat de l'exécution » du scénario) · Ticket : [#106](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/106) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

La palette de commandes (Ctrl K / Cmd K, ou le champ de recherche de la barre du haut) n'utilise plus de données de démonstration. À chaque frappe, elle filtre les pages du menu et les actions de création permises à l'utilisateur ; 300 ms après la dernière frappe, elle demande à `GET /api/v1/articles` (ART-01) une page de 5 articles correspondant au nom, au code ou au code-barres. Aucun endpoint ni aucune migration n'est ajouté.

## Contrat exposé

### Endpoints backend

Aucun nouvel endpoint. Endpoint consommé :

| Méthode | URL | Permission | Paramètres envoyés | Sortie | Erreurs gérées |
|---|---|---|---|---|---|
| GET | `/api/v1/articles` | `articles.read` | `page=1`, `size=5`, `sortBy=name`, `sortDirection=ASC`, `search=<saisie rognée, en minuscules>` | `PageResponse<ArticleRDto>` | toute erreur (réseau, 401, 403, 5xx) : le groupe Articles est vide et un message le signale ; pages et créations restent utilisables |

### Services et ports Java réutilisables

- Aucun.

### Frontend

- Routes : aucune nouvelle. Le composant est monté par `core/layout/top-bar/top-bar.html` (`@defer`, ouverture au clic ou Ctrl K).
- `core/layout/search-dialog/search-index.ts` (fonctions pures, testables sans Angular) :
  - `buildGroups(rawTerm, ctx, articles)` (`search-index.ts:159`) : groupes `articles`, `actions`, `goto` dans cet ordre, groupes vides omis, rien sous 2 caractères ;
  - `normalizeTerm` (`:69`) : rognage + minuscules, chaîne vide sous `MIN_TERM_LENGTH` (2) ;
  - `highlightParts` (`:79`) : découpe pour surligner sans `innerHTML` ;
  - `CREATE_ACTIONS` (`:46`) : actions de création (libellé i18n, route, permission).
- `core/layout/search-dialog/search-dialog.ts` : temporisation (`SEARCH_DEBOUNCE_MS`, `:32`) puis `rxResource` sur `ArticleService.list` (`:68`, `features/article/data-access/article-service.ts`) ; aucun appel sans saisie ou sans `articles.read`.
- Traductions : namespace `search.*` de `public/i18n/fr.json` et `mg.json` (clés ajoutées : `loading`, `articlesUnavailable`, `action.createSupplier`, `action.createUser` ; supprimées : `type.*`, `group.records`).

### Données

- Tables et migrations Liquibase : aucune.
- Journal : aucune écriture (lecture seule).

## Règles métier à respecter par les autres tickets

- **Ajouter une page** : une entrée dans `MENU_SECTIONS` (`core/layout/menu.ts`) apparaît automatiquement dans « Aller à », filtrée par sa permission et cherchée sur son libellé traduit.
- **Ajouter une création** : une ligne dans `CREATE_ACTIONS` avec la permission d'écriture du backend (`xxx.create`), une clé `search.action.*` en français **et** en malgache (le test `translations.spec.ts` l'exige). Le guard de la route cible reste la vraie barrière ; le serveur décide.
- **Articles** : la palette propose aussi les articles désactivés (elle sert à retrouver une fiche). Un écran de sélection (vente, production) doit, lui, filtrer `active=true` (règle d'ART-01).
- Les résultats articles mènent à `/articles/<id>`, comme la liste d'ART-05 : la fiche de détail deviendra réelle avec [ART-06](../ART-06/description.md) sans toucher à la palette.

## Fichiers concernés

**Frontend** (`valimbavaka-ui/`)
- `src/app/core/layout/search-dialog/search-index.ts`
- `src/app/core/layout/search-dialog/search-dialog.ts`, `search-dialog.html`, `search-dialog.css`
- `public/i18n/fr.json`, `public/i18n/mg.json`

**Tests**
- `src/app/core/layout/search-dialog/search-index.spec.ts`
- `src/app/core/layout/search-dialog/search-dialog.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** : [ART-01](../ART-01/guide.md) (`GET /api/v1/articles`, `ArticleService`, `ArticleRDto`), [ART-05](../ART-05/guide.md) (même recherche serveur et même lien `/articles/<id>`), [AUTH-01](../AUTH-01/guide.md) (permissions `AuthStore.hasPermission`).

**Ces tickets s'appuient sur lui** : aucun.

## Comment l'utiliser depuis un autre ticket

```ts
// search-index.ts : rendre une nouvelle création cherchable dans la palette
{ id: 'new-promotion', labelKey: 'search.action.createPromotion', route: '/promotions/new', permission: 'promotions.create' },
```

## Points d'attention

- **Hypothèse (ticket sans décision métier)** : le critère cite « navigation, création et articles » ; les résultats de démonstration « ventes, livraisons, fournisseurs » de la maquette ont été retirés plutôt que gardés en dur (aucune API de recherche multi-ressource). Ajouter fournisseurs, ventes ou livraisons = un nouveau ticket, avec un appel par ressource ou un endpoint de recherche dédié.
- **Hypothèse** : l'action « Nouvelle vente avec « article » » de la maquette est devenue « Nouvelle vente » (`/pos`, `sales.read`, la permission du menu Caisse) : la caisse ne sait pas encore recevoir un article présélectionné. À reprendre quand la caisse le permettra (VTE-01).
- **Hypothèse** : les créations sont désormais filtrées sur leur libellé (« créer », « nouvelle », « fournisseur »…) au lieu d'être toujours affichées, pour que le critère « filtre en temps réel… création » soit tenu.
- Pas de pastille de gamme sur les résultats articles : il faudrait charger le référentiel des gammes (SYS-05) à chaque ouverture ; le type d'article (« Produit fini », « Matière première », « Semi-fini ») est affiché à la place.
- Les traductions malgaches ajoutées (`search.loading`, `search.articlesUnavailable`, `search.action.createSupplier`, `search.action.createUser`, textes d'aide) sont à faire relire.
- Marqueurs de maquette retirés : étiquette « SYS-03 » de l'en-tête et pastille « en construction » du pied de la palette.
- `<dialog>` : si `showModal`/`close` n'existent pas (jsdom en test), la palette passe par l'attribut `open` et émet `closed` elle-même (`search-dialog.ts:105`, `:141`), comme `ConfirmDialog`.
- Le surlignage met désormais les morceaux non surlignés dans un `<span>` : sans cela, les espaces du gabarit autour de l'interpolation pouvaient couper un mot en deux (« fourn isseur »).
