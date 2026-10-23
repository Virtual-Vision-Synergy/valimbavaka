# TRF-01 · Historique des transferts de stock

> Ticket GitHub [#35](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/35) · Ouvert · Milestone : 04 · Stock & transferts · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux consulter l'historique des transferts de stock afin de tracer les mouvements entre emplacements.

## Dépendances

- Bloqué par `TRF-03` (#36)

## Critères d'acceptation

- [ ] Chaque transfert affiche référence, date, origine, destination, statut, émetteur.

## Détails

| | |
|---|---|
| **ID Ticket** | `TRF-01` |
| **Module** | Transferts |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [TRF-04](../TRF-04/description.md) · Détail d'un transfert

## Dépend de

- [TRF-03](../TRF-03/description.md) · Enregistrement d'un retour de stock (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/transfers`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/transfer (à créer)`
