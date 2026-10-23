# VTE-08 · Tableau de bord des ventes par canal

> Ticket GitHub [#69](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/69) · Ouvert · Milestone : 11 · Vente · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux un tableau de bord des ventes par canal afin de comparer leur performance.

## Dépendances

- Bloqué par `VTE-01` (#62)
- Bloqué par `SYS-05` (#111)

## Critères d'acceptation

- [ ] Chaque canal affiche CA, nombre de ventes et part du CA total.

## Détails

| | |
|---|---|
| **ID Ticket** | `VTE-08` |
| **Module** | Ventes |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal (ouvert)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/pos`
- `valimbavaka-ui/src/app/features/sales`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale (à créer)`
