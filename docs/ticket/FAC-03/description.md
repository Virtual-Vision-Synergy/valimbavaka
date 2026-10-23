# FAC-03 · Impression / export / envoi de facture

> Ticket GitHub [#73](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/73) · Ouvert · Milestone : 13 · Facture · 2 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux imprimer/exporter/envoyer une facture afin de la remettre au client.

## Dépendances

- Bloqué par `FAC-01` (#71)
- Bloqué par `SYS-01` (#105)

## Critères d'acceptation

- [ ] Les 3 actions sont disponibles et confirment leur exécution.

## Détails

| | |
|---|---|
| **ID Ticket** | `FAC-03` |
| **Module** | Factures |
| **Story points** | 2 |
| **Utilisateur concerné** | Vendeuse |

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
