# STK-02 · Jauge visuelle du stock

> Ticket GitHub [#31](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/31) · Ouvert · Milestone : 04 · Stock & transferts · 1 SP · Assigné(e) : Njara-Niaina

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux une jauge visuelle de répartition du stock afin de le visualiser d'un coup d'œil.

## Dépendances

- Bloqué par `STK-01` (#30)

## Critères d'acceptation

- [ ] Le survol de la jauge affiche le détail chiffré par emplacement.

## Détails

| | |
|---|---|
| **ID Ticket** | `STK-02` |
| **Module** | Stocks |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/stocks`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/stock (à créer)`
