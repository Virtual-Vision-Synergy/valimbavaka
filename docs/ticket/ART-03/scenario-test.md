# Scénarios de test — ART-03 · Identification visuelle de la gamme

Statut : ⚠ rédigé, tests non exécutés · Ticket : [#23](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/23) · [description](description.md) · [guide](guide.md)

Ticket frontend seul : aucun endpoint, aucune migration. La couleur d'une gamme est de la présentation, elle reste
côté `valimbavaka-ui` (variables CSS), sans champ ajouté au contrat `ProductRangeRDto` de SYS-05.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | tous (affichage seul) |
| Données de départ | un article fini par gamme (BOTANIKA, ZEZIKA, SOLIKA) et un article sans gamme |
| Tickets à avoir déployés avant | ART-01, SYS-05 |

## Scénarios issus des critères d'acceptation

| # | Critère d'acceptation | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Chaque gamme a un code couleur constant sur toutes les vues. | Étant donné la gamme `BOTANIKA`, quand `vvs-range-tag` l'affiche | l'élément porte `data-range="BOTANIKA"` et le libellé `BOTANIKA` | `RangeTag` · `should expose data-range and the label text for BOTANIKA` | ☐ |
| 2 | idem | Même chose pour `ZEZIKA` | `data-range="ZEZIKA"` et libellé `ZEZIKA` | `RangeTag` · `should expose data-range and the label text for ZEZIKA` | ☐ |
| 3 | idem | Même chose pour `SOLIKA` | `data-range="SOLIKA"` et libellé `SOLIKA` | `RangeTag` · `should expose data-range and the label text for SOLIKA` | ☐ |
| 4 | idem | Quand on compare liste articles, fiche, aperçu, formulaire, POS, stock, dashboard, rapports et recherche | une gamme a la même teinte partout, via `--range-color`, `--range-bg`, `--range-fg` | recette 🧪 (visuelle) | ☐ |
| 5 | idem | Quand on bascule en thème sombre | les trois gammes restent distinctes et lisibles (variantes de `dark-theme.css`) | recette 🧪 (visuelle) | ☐ |

## Sécurité et erreurs

Sans objet : aucun appel réseau, aucune écriture.

## Cas limites

- Article sans gamme (non `FINISHED`) : aucun attribut `data-range`, donc pas de teinte de gamme (fond et texte retombent sur le style par défaut de l'élément). À vérifier en recette 🧪 sur la fiche d'un article matière première.
- Code de gamme inconnu (nouvelle gamme ajoutée en base) : pas de règle `[data-range]`, donc aucune teinte et le libellé reste affiché. Ajouter la gamme dans `src/styles/range.css` et dans les tokens.
- La couleur n'est jamais seule : le libellé de la gamme est toujours rendu avec elle (testé).
- Mode hors ligne : sans effet, tout est local.

## Tests existants

- `valimbavaka-ui/src/app/shared/components/range-tag/range-tag.spec.ts` (nouveau, 3 cas).
- La non-régression des 8 consommateurs CSS n'est pas couverte par un test auto : elle passe par la recette 🧪 (#4 et #5).

## Recette manuelle (🧪)

À passer par Njara après le merge : ouvrir chaque vue ci-dessus avec les trois gammes, en thème clair puis sombre, et
vérifier que la teinte de chaque gamme est identique d'une vue à l'autre (étiquette, vignette, barre du CA, donut).

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : à lancer (`ng test` côté UI)
- Anomalies ouvertes : aucune connue ; tests non exécutés
