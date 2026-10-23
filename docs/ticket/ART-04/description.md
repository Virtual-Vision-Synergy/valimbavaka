# ART-04 · Type d'article et usage en production

> Ticket GitHub [#24](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/24) · Ouvert · Milestone : 03 · Articles · 2 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux que le type d'article conditionne son usage en production afin de distinguer matières et produits finis.

## Dépendances

- Bloqué par `ART-01` (#21)

## Critères d'acceptation

- [ ] Les MP/SF apparaissent comme entrants possibles en production.
- [ ] Les produits finis apparaissent comme sortants possibles.

## Détails

| | |
|---|---|
| **ID Ticket** | `ART-04` |
| **Module** | Produits |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [PRD-01](../PRD-01/description.md) · Création d'un lot de production

## Dépend de

- [ART-01](../ART-01/description.md) · Fiche article complète (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/articles`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article (à créer)`
