# RPT-04 · Répartition du CA par gamme

> Ticket GitHub [#101](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/101) · Ouvert · Milestone : 16 · Rapports & tableau de bord · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux la répartition du CA par gamme afin d'identifier mes gammes les plus performantes.

## Dépendances

- Bloqué par `ART-03` (#23)
- Bloqué par `RPT-01` (#98)
- Bloqué par `SYS-05` (#111)

## Critères d'acceptation

- [ ] La somme par gamme correspond au CA total de la période.

## Détails

| | |
|---|---|
| **ID Ticket** | `RPT-04` |
| **Module** | Rapports |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [ART-03](../ART-03/description.md) · Identification visuelle de la gamme (ouvert)
- [RPT-01](../RPT-01/description.md) · Rapports filtrables journaliers/mensuels/annuels (ouvert)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/reports`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/report (à créer)`
