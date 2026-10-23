# PRD-04 · Indicateurs de production

> Ticket GitHub [#41](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/41) · Ouvert · Milestone : 05 · Production · 1 SP · Assigné(e) : Njara-Niaina

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux voir les indicateurs de production (lots, unités, coût matière) afin de piloter l'activité.

## Dépendances

- Bloqué par `PRD-02` (#39)

## Critères d'acceptation

- [ ] Les 3 indicateurs sont affichés en tête de l'écran Production.

## Détails

| | |
|---|---|
| **ID Ticket** | `PRD-04` |
| **Module** | Production |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [PRD-02](../PRD-02/description.md) · Cohérence stock lors de la validation d'un lot (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/production`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production (à créer)`
