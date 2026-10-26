# PRD-01 · Création d'un lot de production

> Ticket GitHub [#38](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/38) · Ouvert · Milestone : 05 · Production · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux créer un lot de production (produit obtenu + matières consommées) afin de tracer la transformation.

## Dépendances

- Bloqué par `ART-04` (#24)
- Bloqué par `STK-01` (#30)
- Bloqué par `JNL-02` (#15)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Le formulaire permet de sélectionner produit obtenu, quantité, vendeuse responsable et entrants.

## Détails

| | |
|---|---|
| **ID Ticket** | `PRD-01` |
| **Module** | Production |
| **Story points** | 2 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- [ART-06](../ART-06/description.md) · Fiche article unifiée (360°)
- [PRD-02](../PRD-02/description.md) · Cohérence stock lors de la validation d'un lot
- [PRD-03](../PRD-03/description.md) · Statut d'un lot de production

## Dépend de

- [ART-04](../ART-04/description.md) · Type d'article et usage en production (ouvert)
- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (ouvert)
- [JNL-02](../JNL-02/description.md) · Interdiction de suppression des opérations validées (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/production`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production (à créer)`
