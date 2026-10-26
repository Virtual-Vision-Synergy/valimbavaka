# FRS-03 · Filtrage des fournisseurs par type

> Ticket GitHub [#19](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/19) · Ouvert · Milestone : 02 · Fournisseurs · 1 SP · Assigné(e) : Njara-Niaina

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux filtrer les fournisseurs par type (local/étranger) afin de les retrouver rapidement.

## Dépendances

- Bloqué par `FRS-01` (#17)

## Critères d'acceptation

- [ ] La liste se met à jour selon le filtre et la recherche. — *⚠ implémenté, tests écrits : à cocher quand la CI est verte (voir [scénario](scenario-test.md))*

## Détails

| | |
|---|---|
| **ID Ticket** | `FRS-03` |
| **Module** | Fournisseurs |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [FRS-01](../FRS-01/description.md) · Fiche fournisseur complète (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/suppliers`
- `valimbavaka-ui/src/app/features/supplier`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier`
