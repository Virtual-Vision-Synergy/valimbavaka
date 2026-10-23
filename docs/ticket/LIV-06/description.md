# LIV-06 · Suivi des livraisons par statut

> Ticket GitHub [#51](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/51) · Ouvert · Milestone : 09 · Commande & Livraison/Réception · 1 SP · Assigné(e) : Njara-Niaina

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux suivre les livraisons par statut afin de piloter l'activité du jour.

## Dépendances

- Bloqué par `LIV-01` (#46)

## Critères d'acceptation

- [ ] La liste est filtrable par statut (attente/en cours/livrée/retour/annulée).

## Détails

| | |
|---|---|
| **ID Ticket** | `LIV-06` |
| **Module** | Livraison |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [LIV-01](../LIV-01/description.md) · Création d'une commande à livrer (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/deliveries`
- `valimbavaka-ui/src/app/core/layout/driver-layout`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/delivery (à créer)`
