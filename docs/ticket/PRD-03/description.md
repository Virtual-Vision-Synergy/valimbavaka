# PRD-03 · Statut d'un lot de production

> Ticket GitHub [#40](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/40) · Ouvert · Milestone : 05 · Production · 1 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux suivre le statut d'un lot (planifié/en cours/terminé) afin de gérer ma production.

## Dépendances

- Bloqué par `PRD-01` (#38)

## Critères d'acceptation

- [ ] Le statut est visible et modifiable jusqu'à Terminé.

## Détails

| | |
|---|---|
| **ID Ticket** | `PRD-03` |
| **Module** | Production |
| **Story points** | 1 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [PRD-01](../PRD-01/description.md) · Création d'un lot de production (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/production`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production (à créer)`
