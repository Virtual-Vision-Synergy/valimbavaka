# Guide d'intégration — ART-03 · Identification visuelle de la gamme

Statut : ✅ écrit depuis le code, tests non exécutés · [description](description.md) · [scénario](scenario-test.md)

## En bref

Le code couleur d'une gamme est défini une seule fois, dans `valimbavaka-ui/src/styles/range.css`. Tout élément qui
porte `data-range="<code de la gamme>"` reçoit trois variables CSS que les composants consomment. Aucun changement
côté backend ni base.

## Contrat exposé

### Endpoints backend

Aucun. La couleur ne passe pas par l'API : `ProductRangeRDto` (SYS-05) reste `id`, `code`, `label`.

### Services et ports Java réutilisables

Aucun.

### Frontend

- `src/styles/range.css` : pour `BOTANIKA`, `ZEZIKA` et `SOLIKA`, définit `--range-color` (aplat, trait),
  `--range-bg` (fond teinté) et `--range-fg` (texte). Importé dans `src/styles.css` après `dark-theme.css`.
- Les teintes elles-mêmes restent dans `src/styles/tokens.css` (clair) et `src/styles/dark-theme.css` (sombre).
- Composant `vvs-range-tag` (`shared/components/range-tag/range-tag.ts`) : étiquette gamme avec libellé.

### Données

Aucune. Le `code` de la gamme vient de `GET /api/v1/product-ranges` (SYS-05) ou de l'article.

## Règles métier à respecter par les autres tickets

- Ne jamais écrire une couleur de gamme en dur dans un composant : poser `[attr.data-range]="code"` sur l'élément
  et lire `--range-color`, `--range-bg` ou `--range-fg`.
- La couleur ne remplace pas le texte : afficher aussi le libellé de la gamme.
- Une gamme n'existe que pour les articles `FINISHED` : pas de `data-range` sinon.

## Fichiers concernés

- `valimbavaka-ui/src/styles/range.css` (nouveau) et `valimbavaka-ui/src/styles.css`.
- Consommateurs migrés : `range-tag.css`, `article-thumb.css`, `article-preview.css`, `article-form-page.css`,
  `pos-catalog.css`, `revenue-split-card.css` (`.bar span`), `range-donut.css` (`.slice`).
- Test : `shared/components/range-tag/range-tag.spec.ts`.

## Dépendances

- ART-01 (ouvert au moment de la rédaction) : fournit `rangeId` et la fiche article.
- SYS-05 : fournit les codes de gamme via `GET /api/v1/product-ranges`.

## Comment l'utiliser depuis un autre ticket

Poser `data-range` avec le code de la gamme sur l'élément, puis utiliser `background: var(--range-bg)` et
`color: var(--range-fg)` (ou `var(--range-color)` pour un aplat). C'est le cas de DSH-04 et RPT-04.

## Points d'attention

- Nouvelle gamme en base : ajouter ses trois tokens (clair et sombre) et une règle dans `range.css`, sinon elle
  s'affiche sans teinte (le libellé reste visible).
- Les variables sont héritées : un enfant d'un élément `data-range` les reçoit (cas de `.visual` dans la tuile).
- Aucun test automatique ne vérifie le rendu des couleurs : passer par la recette 🧪.
