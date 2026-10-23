# VTE-07 · Consultation des ventes du jour

> Ticket GitHub [#68](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/68) · Ouvert · Milestone : 11 · Vente · 1 SP · Assigné(e) : Njara-Niaina

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux consulter mes ventes du jour afin de suivre mon activité.

## Dépendances

- Bloqué par `VTE-01` (#62)

## Critères d'acceptation

- [ ] Chaque vente affiche référence, articles, paiement, promo, total et accès facture.

## Détails

| | |
|---|---|
| **ID Ticket** | `VTE-07` |
| **Module** | Ventes |
| **Story points** | 1 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/pos`
- `valimbavaka-ui/src/app/features/sales`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale (à créer)`
