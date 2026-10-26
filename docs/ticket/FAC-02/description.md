# FAC-02 · Régénération d'une facture existante

> Ticket GitHub [#72](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/72) · Ouvert · Milestone : 13 · Facture · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux régénérer/rééditer une facture à partir d'une vente déjà enregistrée en caisse afin de la réimprimer ou la renvoyer, sans jamais pouvoir créer une facture hors caisse.

## Dépendances

- Bloqué par `FAC-01` (#71)
- Bloqué par `VTE-01` (#62)
- Bloqué par `JNL-01` (#14)
- Bloqué par `VTE-02` (#63)

## Critères d'acceptation

- [ ] Une facture ne peut être générée qu'à partir d'une vente préalablement enregistrée et tracée dans le système (VTE-01/VTE-02) — aucune saisie libre de lignes indépendante d'une vente.
- [ ] Le document généré reprend exactement les lignes, montants et mode de paiement de la vente d'origine.

## Détails

| | |
|---|---|
| **ID Ticket** | `FAC-02` |
| **Module** | Factures |
| **Story points** | 1 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [FAC-01](../FAC-01/description.md) · Génération automatique de facture (ouvert)
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [VTE-02](../VTE-02/description.md) · Poste de caisse avec calcul automatique (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/sales (composants invoice-*)`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/invoice (à créer)`
