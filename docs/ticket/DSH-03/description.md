# DSH-03 · Alertes de stock sur le cockpit

> Ticket GitHub [#96](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/96) · Ouvert · Milestone : 16 · Rapports & tableau de bord · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux voir les alertes de stock directement sur le cockpit afin de réagir rapidement.

## Dépendances

- Bloqué par `STK-04` (#33)

## Critères d'acceptation

- [ ] Chaque article en alerte est cliquable vers sa fiche détaillée.

## Détails

| | |
|---|---|
| **ID Ticket** | `DSH-03` |
| **Module** | Tableau de bord |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [STK-04](../STK-04/description.md) · Alerte de seuil minimum (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/dashboard`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/dashboard (à créer)`
