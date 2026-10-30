# STK-03 · Quantité et valeur du stock par emplacement

> Ticket GitHub [#32](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/32) · Ouvert · Milestone : 04 · Stock & transferts · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux voir la quantité et la valeur du stock par emplacement afin d'évaluer mon actif immobilisé.

## Dépendances

- Bloqué par `STK-01` (#30)
- Bloqué par `SYS-05` (#111)

## Critères d'acceptation

- [x] Chaque emplacement affiche sa quantité totale et sa valorisation en Ariary. — *✅ vérifié : `GET /api/v1/stocks/valuation` (administrateur) et cartes de `/stocks`, voir le [scénario](scenario-test.md)*

## Détails

| | |
|---|---|
| **ID Ticket** | `STK-03` |
| **Module** | Stocks |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (livré dans `develop`)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/stocks`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/stock` (détail dans le [guide](guide.md))
