# COM-06 · Ajout d'une règle de commission

> Ticket GitHub [#90](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/90) · Ouvert · Milestone : 14 · Commissions · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux ajouter une nouvelle règle de commission afin d'ajuster ma politique commerciale.

## Dépendances

- Bloqué par `COM-01` (#85)
- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

- [ ] Le formulaire enregistre la règle et la rend disponible pour le calcul.

## Détails

| | |
|---|---|
| **ID Ticket** | `COM-06` |
| **Module** | Commissions |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [COM-01](../COM-01/description.md) · Règles de commission par canal (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/commissions`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/commission (à créer)`
