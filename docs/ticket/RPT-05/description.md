# RPT-05 · Classement des produits les plus vendus

> Ticket GitHub [#102](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/102) · Ouvert · Milestone : 16 · Rapports & tableau de bord · 1 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux un classement des produits les plus vendus afin d'orienter mes réassorts.

## Dépendances

- Bloqué par `RPT-01` (#98)

## Critères d'acceptation

- [ ] Le tableau est trié par CA décroissant avec quantité, marge et % du CA.

## Détails

| | |
|---|---|
| **ID Ticket** | `RPT-05` |
| **Module** | Rapports |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [RPT-01](../RPT-01/description.md) · Rapports filtrables journaliers/mensuels/annuels (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/reports`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/report (à créer)`
