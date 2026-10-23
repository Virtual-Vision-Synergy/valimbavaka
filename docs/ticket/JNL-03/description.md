# JNL-03 · Accès au journal complet

> Ticket GitHub [#104](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/104) · Ouvert · Milestone : 17 · Finitions transverses · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux accéder au journal complet depuis la barre supérieure afin de l'auditer à tout moment.

## Dépendances

- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

- [ ] Le journal liste toutes les opérations du jour dans l'ordre chronologique.

## Détails

| | |
|---|---|
| **ID Ticket** | `JNL-03` |
| **Module** | Journal |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/journal`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/journal`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/operation`
