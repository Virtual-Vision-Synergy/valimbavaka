# VTE-04 · Application d'un code promo

> Ticket GitHub [#65](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/65) · Ouvert · Milestone : 12 · Promotions · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux appliquer un code promo au panier afin de faire bénéficier le client d'une remise.

## Dépendances

- Bloqué par `PRO-01` (#58)
- Bloqué par `VTE-02` (#63)

## Critères d'acceptation

- [ ] L'application du code réduit le net à payer et l'indique sur le ticket.

## Détails

| | |
|---|---|
| **ID Ticket** | `VTE-04` |
| **Module** | Ventes |
| **Story points** | 1 |
| **Utilisateur concerné** | Vendeuse |

> Déplacé dans la milestone « 12 · Promotions » (revue du backlog du 07/10) : le code promo a besoin des promotions (PRO-01) : rejoint la milestone Promotions, désormais après Vente.

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [PRO-01](../PRO-01/description.md) · Création d'une promotion (ouvert)
- [VTE-02](../VTE-02/description.md) · Poste de caisse avec calcul automatique (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/pos`
- `valimbavaka-ui/src/app/features/sales`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale (à créer)`
