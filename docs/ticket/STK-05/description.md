# STK-05 · Génération d'un bon de réapprovisionnement

> Ticket GitHub [#34](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/34) · Ouvert · Milestone : 04 · Stock & transferts · 1 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux générer un bon de réapprovisionnement pré-rempli afin de gagner du temps.

## Dépendances

- Bloqué par `FRS-01` (#17)
- Bloqué par `STK-04` (#33)
- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

- [ ] L'action confirme la génération du bon à partir des articles sous seuil.

## Détails

| | |
|---|---|
| **ID Ticket** | `STK-05` |
| **Module** | Stocks |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [STK-06](../STK-06/description.md) · Réception d'une livraison fournisseur (entrée en stock)

## Dépend de

- [FRS-01](../FRS-01/description.md) · Fiche fournisseur complète (fermé)
- [STK-04](../STK-04/description.md) · Alerte de seuil minimum (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/stocks`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/stock (à créer)`
