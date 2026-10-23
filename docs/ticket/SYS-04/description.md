# SYS-04 · Bascule thème clair/sombre

> Ticket GitHub [#107](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/107) · Ouvert · Milestone : 17 · Finitions transverses · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux basculer entre thème clair et sombre afin d'adapter le confort visuel.

## Dépendances

- Bloqué par `AUTH-01` (#10)

## Critères d'acceptation

- [ ] Le changement de thème s'applique immédiatement à toute l'interface.

## Détails

| | |
|---|---|
| **ID Ticket** | `SYS-04` |
| **Module** | Transverse |
| **Story points** | 1 |
| **Utilisateur concerné** | Tous |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [AUTH-01](../AUTH-01/description.md) · Connexion selon le rôle attribué (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/core/layout/ui-preferences-service.ts`

**Backend (`valimbavaka-core`)**
- _(aucun)_
