# TRF-02 · Génération automatique des transferts du matin

> Ticket GitHub [#55](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/55) · Ouvert · Milestone : 10 · Ouverture de journée · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux que l'ouverture de journée génère automatiquement les transferts du matin afin d'éviter une double saisie.

## Dépendances

- Bloqué par `JR-01` (#54)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Un transfert généré par l'ouverture apparaît immédiatement avec le statut Validé.

## Détails

| | |
|---|---|
| **ID Ticket** | `TRF-02` |
| **Module** | Transferts |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [JR-04](../JR-04/description.md) · Réconciliation automatique de clôture

## Dépend de

- [JR-01](../JR-01/description.md) · Répartition matinale du stock (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/transfers`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/transfer (à créer)`
