# ART-06 · Fiche article unifiée (360°)

> Ticket GitHub [#26](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/26) · Ouvert · Milestone : 16 · Rapports & tableau de bord · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux une fiche article unifiée (stock, lots, promos, journal) afin d'avoir une vue à 360°.

## Dépendances

- Bloqué par `JNL-01` (#14)
- Bloqué par `PRD-01` (#38)
- Bloqué par `PRO-01` (#58)
- Bloqué par `STK-01` (#30)

## Critères d'acceptation

- [ ] Les 4 blocs d'information sont visibles sans navigation supplémentaire.

## Détails

| | |
|---|---|
| **ID Ticket** | `ART-06` |
| **Module** | Produits |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

> Déplacé dans la milestone « 16 · Rapports & tableau de bord » (revue du backlog du 07/10) : la fiche 360° affiche stock, lots, promos et journal : modules livrés bien plus tard.

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [PRD-01](../PRD-01/description.md) · Création d'un lot de production (ouvert)
- [PRO-01](../PRO-01/description.md) · Création d'une promotion (ouvert)
- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/articles`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article (à créer)`
