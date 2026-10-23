# LIV-07 · Historique de statut d'une livraison

> Ticket GitHub [#52](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/52) · Ouvert · Milestone : 09 · Commande & Livraison/Réception · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux consulter l'historique de statut d'une livraison afin de comprendre son parcours.

## Dépendances

- Bloqué par `LIV-03` (#48)
- Bloqué par `SYS-06` (#112)

## Critères d'acceptation

- [ ] Chaque changement de statut est horodaté et attribué à un auteur.

## Détails

| | |
|---|---|
| **ID Ticket** | `LIV-07` |
| **Module** | Livraison |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [LIV-03](../LIV-03/description.md) · Marquage rapide d'une livraison (ouvert)
- [SYS-06](../SYS-06/description.md) · Audit : auteur des opérations en UUID (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/deliveries`
- `valimbavaka-ui/src/app/core/layout/driver-layout`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/delivery (à créer)`
