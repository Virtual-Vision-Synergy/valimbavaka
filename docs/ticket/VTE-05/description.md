# VTE-05 · Choix du mode de règlement

> Ticket GitHub [#66](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/66) · Ouvert · Milestone : 11 · Vente · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux choisir le mode de règlement (espèces/MVola/carte) afin de finaliser l'encaissement.

## Dépendances

- Bloqué par `VTE-02` (#63)

## Critères d'acceptation

- [ ] Le mode choisi est enregistré avec la vente et la facture.

## Détails

| | |
|---|---|
| **ID Ticket** | `VTE-05` |
| **Module** | Ventes |
| **Story points** | 2 |
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
