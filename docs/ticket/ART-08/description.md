# ART-08 · Export de la liste des articles

> Ticket GitHub [#28](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/28) · Ouvert · Milestone : 03 · Articles · 1 SP · Assigné(e) : And-Matia · Labels : needs-info

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux exporter la liste des articles afin de la partager ou l'archiver.

## Dépendances

- Bloqué par `ART-01` (#21)
- Bloqué par `SYS-01` (#105)

## Critères d'acceptation

- [ ] Format d'export à préciser avant développement.

## Détails

| | |
|---|---|
| **ID Ticket** | `ART-08` |
| **Module** | Produits |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [ART-01](../ART-01/description.md) · Fiche article complète (ouvert)
- [SYS-01](../SYS-01/description.md) · Export PDF/Excel généralisé (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/articles`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article (à créer)`
