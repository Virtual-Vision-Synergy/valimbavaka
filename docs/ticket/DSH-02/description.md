# DSH-02 · Graphique du CA sur 7 jours

> Ticket GitHub [#95](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/95) · Ouvert · Milestone : 16 · Rapports & tableau de bord · 1 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux un graphique du CA sur 7 jours afin de suivre la tendance récente.

## Dépendances

- Bloqué par `VTE-01` (#62)

## Critères d'acceptation

- [ ] Le graphique affiche une valeur par jour avec infobulle au survol.

## Détails

| | |
|---|---|
| **ID Ticket** | `DSH-02` |
| **Module** | Tableau de bord |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/dashboard`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/dashboard (à créer)`
