# LIV-05 · Annulation d'une action récente

> Ticket GitHub [#50](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/50) · Ouvert · Milestone : 09 · Commande & Livraison/Réception · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que livreur, je veux annuler une action récente afin de corriger une erreur de saisie.

## Dépendances

- Bloqué par `LIV-03` (#48)
- Bloqué par `LIV-04` (#49)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] L'annulation restaure le statut précédent pendant un délai limité.

## Détails

| | |
|---|---|
| **ID Ticket** | `LIV-05` |
| **Module** | Livraison |
| **Story points** | 1 |
| **Utilisateur concerné** | Livreur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [LIV-03](../LIV-03/description.md) · Marquage rapide d'une livraison (ouvert)
- [LIV-04](../LIV-04/description.md) · Signalement d'un échec de livraison (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/deliveries`
- `valimbavaka-ui/src/app/core/layout/driver-layout`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/delivery (à créer)`
