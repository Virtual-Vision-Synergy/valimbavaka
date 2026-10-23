# PRD-05 · Détail et coût de revient d'un lot

> Ticket GitHub [#42](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/42) · Ouvert · Milestone : 05 · Production · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux consulter le détail d'un lot (entrants, coût de revient) afin de vérifier sa rentabilité.

## Dépendances

- Bloqué par `PRD-02` (#39)

## Critères d'acceptation

- [ ] Le coût de revient unitaire affiché est calculé à partir des entrants.

## Détails

| | |
|---|---|
| **ID Ticket** | `PRD-05` |
| **Module** | Production |
| **Story points** | 1 |
| **Utilisateur concerné** | Vendeuse |

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
