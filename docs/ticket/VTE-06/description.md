# VTE-06 · Mise en attente d'un ticket

> Ticket GitHub [#67](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/67) · Ouvert · Milestone : 11 · Vente · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux mettre un ticket en attente afin de le reprendre plus tard.

## Dépendances

- Bloqué par `VTE-02` (#63)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] L'action vide le panier sans enregistrer de vente.

## Détails

| | |
|---|---|
| **ID Ticket** | `VTE-06` |
| **Module** | Ventes |
| **Story points** | 1 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [VTE-02](../VTE-02/description.md) · Poste de caisse avec calcul automatique (ouvert)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/pos`
- `valimbavaka-ui/src/app/features/sales`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale (à créer)`
