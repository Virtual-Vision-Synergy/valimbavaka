# ART-07 · Import d'articles en masse

> Ticket GitHub [#27](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/27) · Ouvert · Milestone : 03 · Articles · 1 SP · Assigné(e) : ManaJudy · Labels : needs-info

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux importer des articles en masse afin de gagner du temps de saisie.

## Dépendances

- Bloqué par `ART-01` (#21)
- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

- [ ] Format et comportement à préciser avec le porteur de projet avant développement.

## Détails

| | |
|---|---|
| **ID Ticket** | `ART-07` |
| **Module** | Produits |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [ART-01](../ART-01/description.md) · Fiche article complète (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/articles`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article (à créer)`
