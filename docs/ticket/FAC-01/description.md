# FAC-01 · Génération automatique de facture

> Ticket GitHub [#71](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/71) · Ouvert · Milestone : 13 · Facture · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux qu'une facture numérotée soit générée automatiquement à l'encaissement afin de respecter la conformité comptable.

## Dépendances

- Bloqué par `VTE-02` (#63)
- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

- [ ] Chaque encaissement produit une facture avec numéro unique croissant.

## Détails

| | |
|---|---|
| **ID Ticket** | `FAC-01` |
| **Module** | Factures |
| **Story points** | 2 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- [FAC-02](../FAC-02/description.md) · Régénération d'une facture existante
- [FAC-03](../FAC-03/description.md) · Impression / export / envoi de facture
- [FAC-04](../FAC-04/description.md) · Émission d'un avoir motivé
- [FAC-05](../FAC-05/description.md) · Suivi des statuts de facture
- [FAC-06](../FAC-06/description.md) · Export du livre des ventes

## Dépend de

- [VTE-02](../VTE-02/description.md) · Poste de caisse avec calcul automatique (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/sales (composants invoice-*)`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/invoice (à créer)`
