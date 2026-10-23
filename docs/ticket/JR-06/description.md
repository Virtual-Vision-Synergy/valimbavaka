# JR-06 · Verrouillage de la clôture du jour

> Ticket GitHub [#93](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/93) · Ouvert · Milestone : 15 · Clôture de journée · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux verrouiller la clôture du jour afin de figer la journée dans le journal.

## Dépendances

- Bloqué par `JNL-01` (#14)
- Bloqué par `JR-05` (#92)

## Critères d'acceptation

- [ ] Un message confirme l'enregistrement et l'écart est consigné au journal.

## Détails

| | |
|---|---|
| **ID Ticket** | `JR-06` |
| **Module** | Cycle de journée |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [JR-05](../JR-05/description.md) · Motif obligatoire en cas d'écart (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/day`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/day (à créer)`
