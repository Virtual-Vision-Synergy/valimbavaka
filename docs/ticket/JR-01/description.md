# JR-01 · Répartition matinale du stock

> Ticket GitHub [#54](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/54) · Ouvert · Milestone : 10 · Ouverture de journée · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux répartir en un seul écran la sortie du matin vers boutique/livraison/salon afin de préparer la journée.

## Dépendances

- Bloqué par `STK-01` (#30)
- Bloqué par `TRF-03` (#36)
- Bloqué par `JNL-02` (#15)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] La validation génère un bon de transfert par destination et met à jour les stocks.

## Détails

| | |
|---|---|
| **ID Ticket** | `JR-01` |
| **Module** | Cycle de journée |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [TRF-02](../TRF-02/description.md) · Génération automatique des transferts du matin
- [JR-02](../JR-02/description.md) · Reprise des quantités de la veille
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal

## Dépend de

- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (ouvert)
- [TRF-03](../TRF-03/description.md) · Enregistrement d'un retour de stock (ouvert)
- [JNL-02](../JNL-02/description.md) · Interdiction de suppression des opérations validées (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/day`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/day (à créer)`
