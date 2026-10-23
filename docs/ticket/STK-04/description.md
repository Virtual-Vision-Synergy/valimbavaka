# STK-04 · Alerte de seuil minimum

> Ticket GitHub [#33](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/33) · Ouvert · Milestone : 04 · Stock & transferts · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux être alerté des articles sous leur seuil minimum afin d'anticiper une rupture.

## Dépendances

- Bloqué par `STK-01` (#30)

## Critères d'acceptation

- [ ] La liste affiche stock actuel, seuil, manque et quantité suggérée à commander.

## Détails

| | |
|---|---|
| **ID Ticket** | `STK-04` |
| **Module** | Stocks |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [FRS-04](../FRS-04/description.md) · Délai de réapprovisionnement selon le type de fournisseur
- [STK-05](../STK-05/description.md) · Génération d'un bon de réapprovisionnement
- [DSH-03](../DSH-03/description.md) · Alertes de stock sur le cockpit

## Dépend de

- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/stocks`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/stock (à créer)`
