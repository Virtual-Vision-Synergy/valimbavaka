# ART-03 · Identification visuelle de la gamme

> Ticket GitHub [#23](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/23) · Ouvert · Milestone : 03 · Articles · 1 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux voir la gamme d'un produit identifiée visuellement afin de la reconnaître rapidement.

## Dépendances

- Bloqué par `ART-01` (#21)
- Bloqué par `SYS-05` (#111)

## Critères d'acceptation

- [ ] Chaque gamme a un code couleur constant sur toutes les vues.

## Détails

| | |
|---|---|
| **ID Ticket** | `ART-03` |
| **Module** | Produits |
| **Story points** | 1 |
| **Utilisateur concerné** | Tous |

## Ce ticket débloque

- [DSH-04](../DSH-04/description.md) · Répartition du CA par gamme et canal
- [RPT-04](../RPT-04/description.md) · Répartition du CA par gamme

## Dépend de

- [ART-01](../ART-01/description.md) · Fiche article complète (ouvert)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/articles`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article (à créer)`
