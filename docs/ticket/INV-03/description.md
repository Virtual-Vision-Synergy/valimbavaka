# INV-03 · Motif d'écart par ligne

> Ticket GitHub [#79](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/79) · Ouvert · Milestone : 06 · Inventaire · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux qualifier un motif d'écart par ligne afin d'en comprendre l'origine.

## Dépendances

- Bloqué par `INV-02` (#78)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Un motif est sélectionnable pour toute ligne en écart.

## Détails

| | |
|---|---|
| **ID Ticket** | `INV-03` |
| **Module** | Inventaire |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [INV-02](../INV-02/description.md) · Comptage physique avec calcul d'écart (ouvert)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/inventory`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/inventory (à créer)`
