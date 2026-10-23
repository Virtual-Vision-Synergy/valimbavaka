# FAC-06 · Export du livre des ventes

> Ticket GitHub [#76](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/76) · Ouvert · Milestone : 13 · Facture · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux exporter le livre des ventes en XLSX afin de le transmettre à la comptabilité.

## Dépendances

- Bloqué par `FAC-01` (#71)
- Bloqué par `SYS-01` (#105)

## Critères d'acceptation

- [ ] L'export confirme la génération du fichier.

## Détails

| | |
|---|---|
| **ID Ticket** | `FAC-06` |
| **Module** | Factures |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [FAC-01](../FAC-01/description.md) · Génération automatique de facture (ouvert)
- [SYS-01](../SYS-01/description.md) · Export PDF/Excel généralisé (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/sales (composants invoice-*)`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/invoice (à créer)`
