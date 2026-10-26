# JR-02 · Reprise des quantités de la veille

> Ticket GitHub [#56](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/56) · Ouvert · Milestone : 10 · Ouverture de journée · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux reprendre les quantités de la veille afin de gagner du temps à l'ouverture.

## Dépendances

- Bloqué par `JR-01` (#54)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Le bouton « Reprendre hier » applique les quantités du dernier jour ouvré.

## Détails

| | |
|---|---|
| **ID Ticket** | `JR-02` |
| **Module** | Cycle de journée |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [JR-01](../JR-01/description.md) · Répartition matinale du stock (ouvert)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/day`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/day (à créer)`
