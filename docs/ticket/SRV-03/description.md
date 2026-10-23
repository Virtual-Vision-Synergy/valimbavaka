# SRV-03 · Historique des prestations du jour

> Ticket GitHub [#83](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/83) · Ouvert · Milestone : 07 · Services salon (catalogue + prestations) · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux consulter l'historique de mes prestations du jour afin de suivre mon activité.

## Dépendances

- Bloqué par `SRV-02` (#82)

## Critères d'acceptation

- [ ] Chaque prestation affiche heure, service, cliente et produits consommés.

## Détails

| | |
|---|---|
| **ID Ticket** | `SRV-03` |
| **Module** | Services salon |
| **Story points** | 1 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [SRV-02](../SRV-02/description.md) · Enregistrement d'une prestation (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/services`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/salonservice (à créer)`
