# DSH-01 · Cockpit journalier

> Ticket GitHub [#94](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/94) · Ouvert · Milestone : 16 · Rapports & tableau de bord · 2 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux un cockpit journalier (CA, ventes, livraisons, écart) afin d'avoir une vue synthétique de la journée.

## Dépendances

- Bloqué par `JR-04` (#91)

## Critères d'acceptation

- [ ] Les 4 indicateurs clés sont affichés sur un seul écran.

## Détails

| | |
|---|---|
| **ID Ticket** | `DSH-01` |
| **Module** | Tableau de bord |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [JR-04](../JR-04/description.md) · Réconciliation automatique de clôture (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/dashboard`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/dashboard (à créer)`
