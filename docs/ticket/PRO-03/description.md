# PRO-03 · Statut et utilisation d'une promotion

> Ticket GitHub [#60](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/60) · Ouvert · Milestone : 12 · Promotions · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux suivre le statut et l'utilisation d'une promotion afin d'évaluer son efficacité.

## Dépendances

- Bloqué par `PRO-02` (#59)

## Critères d'acceptation

- [ ] Le statut se met à jour selon les dates et le compteur s'incrémente à chaque usage.

## Détails

| | |
|---|---|
| **ID Ticket** | `PRO-03` |
| **Module** | Promotions |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [PRO-02](../PRO-02/description.md) · Application automatique d'une promotion en caisse (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/promotions`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/promotion (à créer)`
