# JR-03 · Suivi temps réel de la journée

> Ticket GitHub [#57](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/57) · Ouvert · Milestone : 15 · Clôture de journée · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux suivre en temps réel les ventes, la tournée et les prestations du jour afin de garder une vue d'ensemble.

## Dépendances

- Bloqué par `LIV-02` (#47)
- Bloqué par `SRV-02` (#82)
- Bloqué par `VTE-01` (#62)

## Critères d'acceptation

- [ ] Les indicateurs et le flux d'opérations se mettent à jour sans rechargement manuel.

## Détails

| | |
|---|---|
| **ID Ticket** | `JR-03` |
| **Module** | Cycle de journée |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

> Déplacé dans la milestone « 15 · Clôture de journée » (revue du backlog du 07/10) : le suivi temps réel affiche ventes, tournée et prestations : tous livrés après M9.

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [LIV-02](../LIV-02/description.md) · Consultation de la tournée du jour (ouvert)
- [SRV-02](../SRV-02/description.md) · Enregistrement d'une prestation (ouvert)
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/day`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/day (à créer)`
