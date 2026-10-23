# TRF-04 · Détail d'un transfert

> Ticket GitHub [#37](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/37) · Ouvert · Milestone : 04 · Stock & transferts · 1 SP · Assigné(e) : Njara-Niaina

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux consulter le détail d'un transfert afin de vérifier les articles et quantités concernés.

## Dépendances

- Bloqué par `TRF-01` (#35)

## Critères d'acceptation

- [ ] Le clic sur un transfert ouvre son détail complet.

## Détails

| | |
|---|---|
| **ID Ticket** | `TRF-04` |
| **Module** | Transferts |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [TRF-01](../TRF-01/description.md) · Historique des transferts de stock (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/transfers`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/transfer (à créer)`
