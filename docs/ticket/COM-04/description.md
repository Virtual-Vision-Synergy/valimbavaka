# COM-04 · Suivi de commission et objectif

> Ticket GitHub [#88](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/88) · Ouvert · Milestone : 14 · Commissions · 1 SP · Assigné(e) : ManaJudy · Labels : needs-info

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux consulter ma commission du mois et mon objectif afin de suivre ma performance.

## Dépendances

- Bloqué par `COM-02` (#86)

## Critères d'acceptation

- [ ] Le détail par vente (base, taux, commission, statut) est affiché.

## Détails

| | |
|---|---|
| **ID Ticket** | `COM-04` |
| **Module** | Commissions |
| **Story points** | 1 |
| **Utilisateur concerné** | Vendeuse |

> Question client en attente : qui fixe l'objectif mensuel de la vendeuse, et où ?

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [COM-02](../COM-02/description.md) · Calcul automatique de la commission (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/commissions`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/commission (à créer)`
