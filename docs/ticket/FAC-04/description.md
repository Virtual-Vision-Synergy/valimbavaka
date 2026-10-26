# FAC-04 · Émission d'un avoir motivé

> Ticket GitHub [#74](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/74) · Ouvert · Milestone : 13 · Facture · 2 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux émettre un avoir motivé afin de contre-passer une facture sans la supprimer.

## Dépendances

- Bloqué par `FAC-01` (#71)
- Bloqué par `JNL-02` (#15)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] L'émission exige un motif et réintègre les articles en stock.

## Détails

| | |
|---|---|
| **ID Ticket** | `FAC-04` |
| **Module** | Factures |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [FAC-01](../FAC-01/description.md) · Génération automatique de facture (ouvert)
- [JNL-02](../JNL-02/description.md) · Interdiction de suppression des opérations validées (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/sales (composants invoice-*)`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/invoice (à créer)`
