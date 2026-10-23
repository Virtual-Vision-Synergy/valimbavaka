# JR-05 · Motif obligatoire en cas d'écart

> Ticket GitHub [#92](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/92) · Ouvert · Milestone : 15 · Clôture de journée · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux qu'un motif soit obligatoire en cas d'écart de clôture afin de garder une traçabilité fiable.

## Dépendances

- Bloqué par `JR-04` (#91)

## Critères d'acceptation

- [ ] La clôture n'est validée qu'après sélection d'un motif si un écart existe.

## Détails

| | |
|---|---|
| **ID Ticket** | `JR-05` |
| **Module** | Cycle de journée |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [JR-06](../JR-06/description.md) · Verrouillage de la clôture du jour

## Dépend de

- [JR-04](../JR-04/description.md) · Réconciliation automatique de clôture (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/day`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/day (à créer)`
