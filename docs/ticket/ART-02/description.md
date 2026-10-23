# ART-02 · Création / modification d'un article

> Ticket GitHub [#22](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/22) · Ouvert · Milestone : 03 · Articles · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux créer/modifier un article afin de maintenir mon catalogue à jour.

## Dépendances

- Bloqué par `ART-01` (#21)
- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

- [ ] Le formulaire pré-remplit les valeurs existantes en modification.
- [ ] L'enregistrement met à jour la fiche article.

## Détails

| | |
|---|---|
| **ID Ticket** | `ART-02` |
| **Module** | Produits |
| **Story points** | 2 |
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
