# INV-04 · Clôture d'une session d'inventaire

> Ticket GitHub [#80](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/80) · Ouvert · Milestone : 06 · Inventaire · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux clôturer une session d'inventaire afin d'ajuster le stock théorique.

## Dépendances

- Bloqué par `INV-02` (#78)
- Bloqué par `JNL-02` (#15)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Après clôture, le stock système correspond aux quantités comptées.

## Détails

| | |
|---|---|
| **ID Ticket** | `INV-04` |
| **Module** | Inventaire |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [INV-05](../INV-05/description.md) · Historique des sessions d'inventaire

## Dépend de

- [INV-02](../INV-02/description.md) · Comptage physique avec calcul d'écart (ouvert)
- [JNL-02](../JNL-02/description.md) · Interdiction de suppression des opérations validées (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/inventory`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/inventory (à créer)`
