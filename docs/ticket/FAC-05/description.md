# FAC-05 · Suivi des statuts de facture

> Ticket GitHub [#75](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/75) · Ouvert · Milestone : 13 · Facture · 1 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux suivre les statuts de facture (payée/impayée/avoir) afin de piloter ma trésorerie.

## Dépendances

- Bloqué par `FAC-01` (#71)

## Critères d'acceptation

- [ ] Les totaux consolidés reflètent la somme des factures par statut.

## Détails

| | |
|---|---|
| **ID Ticket** | `FAC-05` |
| **Module** | Factures |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [FAC-01](../FAC-01/description.md) · Génération automatique de facture (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/sales (composants invoice-*)`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/invoice (à créer)`
