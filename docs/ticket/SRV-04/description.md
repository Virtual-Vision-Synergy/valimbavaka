# SRV-04 · Consommation salon dans la clôture

> Ticket GitHub [#84](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/84) · Ouvert · Milestone : 15 · Clôture de journée · 2 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux que la consommation salon alimente la clôture du jour afin de réconcilier le stock.

## Dépendances

- Bloqué par `JR-04` (#91)
- Bloqué par `SRV-02` (#82)

## Critères d'acceptation

- [ ] Le total consommé en salon correspond exactement à la valeur utilisée en clôture.

## Détails

| | |
|---|---|
| **ID Ticket** | `SRV-04` |
| **Module** | Services salon |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

> Déplacé dans la milestone « 15 · Clôture de journée » (revue du backlog du 07/10) : fait partie de la réconciliation de clôture JR-04.

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [JR-04](../JR-04/description.md) · Réconciliation automatique de clôture (ouvert)
- [SRV-02](../SRV-02/description.md) · Enregistrement d'une prestation (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/services`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/salonservice (à créer)`
