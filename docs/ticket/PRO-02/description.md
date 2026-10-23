# PRO-02 · Application automatique d'une promotion en caisse

> Ticket GitHub [#59](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/59) · Ouvert · Milestone : 12 · Promotions · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux qu'une promotion active s'applique automatiquement en caisse afin d'éviter les erreurs de remise.

## Dépendances

- Bloqué par `PRO-01` (#58)
- Bloqué par `VTE-02` (#63)

## Critères d'acceptation

- [ ] La remise appliquée correspond à la règle de la promotion active concernée.

## Détails

| | |
|---|---|
| **ID Ticket** | `PRO-02` |
| **Module** | Promotions |
| **Story points** | 2 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- [PRO-03](../PRO-03/description.md) · Statut et utilisation d'une promotion

## Dépend de

- [PRO-01](../PRO-01/description.md) · Création d'une promotion (ouvert)
- [VTE-02](../VTE-02/description.md) · Poste de caisse avec calcul automatique (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/promotions`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/promotion (à créer)`
