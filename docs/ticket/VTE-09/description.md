# VTE-09 · Détail d'une vente

> Ticket GitHub [#70](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/70) · Ouvert · Milestone : 14 · Commissions · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux consulter le détail d'une vente afin de vérifier ses lignes et sa commission.

## Dépendances

- Bloqué par `COM-02` (#86)
- Bloqué par `VTE-01` (#62)

## Critères d'acceptation

- [ ] Le détail inclut la règle de commission appliquée.

## Détails

| | |
|---|---|
| **ID Ticket** | `VTE-09` |
| **Module** | Ventes |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

> Déplacé dans la milestone « 14 · Commissions » (revue du backlog du 07/10) : affiche la commission appliquée : dépend de COM-02.

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [COM-02](../COM-02/description.md) · Calcul automatique de la commission (ouvert)
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/pos`
- `valimbavaka-ui/src/app/features/sales`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale (à créer)`
