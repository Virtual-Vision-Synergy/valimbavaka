# SHO-03 · Suivi des sorties shooting en cours

> Ticket GitHub [#45](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/45) · Ouvert · Milestone : 08 · Shooting · 1 SP · Assigné(e) : Njara-Niaina

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux suivre les sorties shooting en cours et leur valeur immobilisée afin de garder le contrôle.

## Dépendances

- Bloqué par `SHO-01` (#43)

## Critères d'acceptation

- [ ] Les 3 indicateurs sont affichés en tête de l'écran Shooting.

## Détails

| | |
|---|---|
| **ID Ticket** | `SHO-03` |
| **Module** | Shooting |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [SHO-01](../SHO-01/description.md) · Enregistrement d'une sortie shooting (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/day`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/shooting (à créer)`
