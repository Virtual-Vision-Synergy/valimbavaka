# VTE-02 · Poste de caisse avec calcul automatique

> Ticket GitHub [#63](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/63) · Ouvert · Milestone : 11 · Vente · 2 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux un poste de caisse avec panier et calcul automatique afin d'encaisser rapidement.

## Dépendances

- Bloqué par `VTE-01` (#62)

## Critères d'acceptation

- [ ] L'ajout d'un article met à jour en temps réel sous-total, remise et net à payer.

## Détails

| | |
|---|---|
| **ID Ticket** | `VTE-02` |
| **Module** | Ventes |
| **Story points** | 2 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- [PRO-02](../PRO-02/description.md) · Application automatique d'une promotion en caisse
- [VTE-03](../VTE-03/description.md) · Raccourcis clavier de caisse
- [VTE-04](../VTE-04/description.md) · Application d'un code promo
- [VTE-05](../VTE-05/description.md) · Choix du mode de règlement
- [VTE-06](../VTE-06/description.md) · Mise en attente d'un ticket
- [FAC-01](../FAC-01/description.md) · Génération automatique de facture
- [FAC-02](../FAC-02/description.md) · Régénération d'une facture existante

## Dépend de

- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/pos`
- `valimbavaka-ui/src/app/features/sales`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale (à créer)`
