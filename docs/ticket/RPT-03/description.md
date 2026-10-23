# RPT-03 · Graphique d'évolution du CA

> Ticket GitHub [#100](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/100) · Ouvert · Milestone : 16 · Rapports & tableau de bord · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux un graphique d'évolution du CA afin de visualiser la tendance sur la période.

## Dépendances

- Bloqué par `RPT-01` (#98)

## Critères d'acceptation

- [ ] Le graphique reflète les valeurs journalières de la période choisie.

## Détails

| | |
|---|---|
| **ID Ticket** | `RPT-03` |
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
