# LIV-08 · Fonctionnement hors ligne de la tournée

> Ticket GitHub [#53](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/53) · Ouvert · Milestone : 09 · Commande & Livraison/Réception · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que livreur, je veux que ma tournée fonctionne hors ligne afin de ne pas dépendre du réseau sur le terrain.

## Dépendances

- Bloqué par `LIV-03` (#48)
- Bloqué par `SYS-02` (#16)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Les actions hors connexion se synchronisent automatiquement au retour du réseau (s'appuie sur SYS-02).

## Détails

| | |
|---|---|
| **ID Ticket** | `LIV-08` |
| **Module** | Livraison |
| **Story points** | 1 |
| **Utilisateur concerné** | Livreur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [LIV-03](../LIV-03/description.md) · Marquage rapide d'une livraison (ouvert)
- [SYS-02](../SYS-02/description.md) · Sauvegarde automatique hors-ligne (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/deliveries`
- `valimbavaka-ui/src/app/core/layout/driver-layout`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/delivery (à créer)`
