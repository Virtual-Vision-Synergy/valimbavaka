# PRD-02 · Cohérence stock lors de la validation d'un lot

> Ticket GitHub [#39](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/39) · Ouvert · Milestone : 05 · Production · 2 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux que la validation d'un lot génère simultanément la sortie des matières et l'entrée du produit fini afin de garantir la cohérence du stock.

## Dépendances

- Bloqué par `PRD-01` (#38)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Les deux mouvements sont générés dans la même opération, jamais séparément.

## Détails

| | |
|---|---|
| **ID Ticket** | `PRD-02` |
| **Module** | Production |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [PRD-04](../PRD-04/description.md) · Indicateurs de production
- [PRD-05](../PRD-05/description.md) · Détail et coût de revient d'un lot

## Dépend de

- [PRD-01](../PRD-01/description.md) · Création d'un lot de production (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/production`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/production (à créer)`
