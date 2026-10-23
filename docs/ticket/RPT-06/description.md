# RPT-06 · Export des rapports en PDF/XLSX

> Ticket GitHub [#103](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/103) · Ouvert · Milestone : 16 · Rapports & tableau de bord · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux exporter chaque rapport en PDF/XLSX afin de le partager ou l'archiver.

## Dépendances

- Bloqué par `RPT-01` (#98)
- Bloqué par `SYS-01` (#105)

## Critères d'acceptation

- [ ] Les boutons PDF et XLSX confirment la génération du fichier.

## Détails

| | |
|---|---|
| **ID Ticket** | `RPT-06` |
| **Module** | Rapports |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [RPT-01](../RPT-01/description.md) · Rapports filtrables journaliers/mensuels/annuels (ouvert)
- [SYS-01](../SYS-01/description.md) · Export PDF/Excel généralisé (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/reports`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/report (à créer)`
