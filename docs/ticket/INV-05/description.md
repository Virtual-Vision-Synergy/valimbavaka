# INV-05 · Historique des sessions d'inventaire

> Ticket GitHub [#81](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/81) · Ouvert · Milestone : 06 · Inventaire · 1 SP · Assigné(e) : Njara-Niaina

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux consulter l'historique des sessions d'inventaire afin de suivre la régularité des comptages.

## Dépendances

- Bloqué par `INV-04` (#80)

## Critères d'acceptation

- [ ] Chaque session passée est consultable avec statut et nombre d'écarts.

## Détails

| | |
|---|---|
| **ID Ticket** | `INV-05` |
| **Module** | Inventaire |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [INV-04](../INV-04/description.md) · Clôture d'une session d'inventaire (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/inventory`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/inventory (à créer)`
