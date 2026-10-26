# INV-02 · Comptage physique avec calcul d'écart

> Ticket GitHub [#78](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/78) · Ouvert · Milestone : 06 · Inventaire · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux saisir le comptage physique avec calcul d'écart afin de comparer au stock théorique.

## Dépendances

- Bloqué par `INV-01` (#77)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] La saisie d'une quantité met à jour immédiatement l'écart affiché.

## Détails

| | |
|---|---|
| **ID Ticket** | `INV-02` |
| **Module** | Inventaire |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [INV-03](../INV-03/description.md) · Motif d'écart par ligne
- [INV-04](../INV-04/description.md) · Clôture d'une session d'inventaire

## Dépend de

- [INV-01](../INV-01/description.md) · Ouverture d'une session d'inventaire (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/inventory`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/inventory (à créer)`
