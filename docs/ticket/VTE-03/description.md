# VTE-03 · Raccourcis clavier de caisse

> Ticket GitHub [#64](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/64) · Ouvert · Milestone : 11 · Vente · 1 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux des raccourcis clavier de caisse afin d'accélérer ma saisie.

## Dépendances

- Bloqué par `VTE-02` (#63)

## Critères d'acceptation

- [ ] Les touches définies déclenchent encaissement, recherche et ajout d'article.

## Détails

| | |
|---|---|
| **ID Ticket** | `VTE-03` |
| **Module** | Ventes |
| **Story points** | 1 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [VTE-02](../VTE-02/description.md) · Poste de caisse avec calcul automatique (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/pos`
- `valimbavaka-ui/src/app/features/sales`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale (à créer)`
