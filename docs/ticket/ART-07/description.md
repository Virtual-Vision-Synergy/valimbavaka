# ART-07 · Import d'articles en masse

> Ticket GitHub [#27](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/27) · Ouvert · Milestone : 03 · Articles · 1 SP · Assigné(e) : ManaJudy · Labels : needs-info

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux importer des articles en masse afin de gagner du temps de saisie.

## Dépendances

- Bloqué par `ART-01` (#21)
- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

_Format et comportement précisés avec le porteur de projet le 2026-10-29 (le label `needs-info` peut être retiré)._

- [ ] 1. L'administrateur importe un fichier CSV (UTF-8, séparateur `;`, 1 000 lignes au maximum) ; les colonnes sont `code;name;barcode;type;rangeId;unit;cost;price;threshold;supplierId`.
- [ ] 2. Le code de l'article vient du serveur (colonne `code` vide) ou du fichier (`ART-0500`) ; un code du fichier ne fait jamais entrer la séquence en collision.
- [ ] 3. Une ligne dont le code ou le code-barres existe déjà (ou est répété dans le fichier) est rejetée et signalée en conflit, sans rien modifier.
- [ ] 4. L'administrateur choisit ensuite d'écraser les articles en conflit : seules les lignes en conflit sont rejouées en mode `OVERWRITE`.
- [ ] 5. L'import est partiel : les lignes valides sont créées, les autres sont listées avec leur motif (numéro de ligne, article en conflit).
- [ ] 6. Chaque article créé ou remplacé est tracé dans le journal (une ligne `CREATE` ou `UPDATE` par article).
- [ ] 7. L'import n'est possible qu'en ligne et seul l'administrateur peut le lancer (`articles.import`).

## Détails

| | |
|---|---|
| **ID Ticket** | `ART-07` |
| **Module** | Produits |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [ART-01](../ART-01/description.md) · Fiche article complète (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/articles`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article (à créer)`
