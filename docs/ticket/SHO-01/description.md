# SHO-01 · Enregistrement d'une sortie shooting

> Ticket GitHub [#43](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/43) · Ouvert · Milestone : 08 · Shooting · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux enregistrer une sortie shooting afin de ne pas confondre ces unités avec des pertes.

## Dépendances

- Bloqué par `STK-01` (#30)
- Bloqué par `JNL-02` (#15)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] La sortie diminue le stock vendable et le statut passe à Sorti.

## Détails

| | |
|---|---|
| **ID Ticket** | `SHO-01` |
| **Module** | Shooting |
| **Story points** | 1 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- [SHO-02](../SHO-02/description.md) · Réintégration d'un produit shooting
- [SHO-03](../SHO-03/description.md) · Suivi des sorties shooting en cours

## Dépend de

- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (ouvert)
- [JNL-02](../JNL-02/description.md) · Interdiction de suppression des opérations validées (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/day`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/shooting (à créer)`
