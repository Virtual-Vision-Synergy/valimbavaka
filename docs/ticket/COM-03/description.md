# COM-03 · Validation des commissions

> Ticket GitHub [#87](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/87) · Ouvert · Milestone : 14 · Commissions · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux valider les commissions afin de sécuriser leur versement.

## Dépendances

- Bloqué par `COM-02` (#86)
- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

- [ ] Le statut à valider / validée est affiché par vendeuse.

## Détails

| | |
|---|---|
| **ID Ticket** | `COM-03` |
| **Module** | Commissions |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [COM-02](../COM-02/description.md) · Calcul automatique de la commission (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/commissions`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/commission (à créer)`
