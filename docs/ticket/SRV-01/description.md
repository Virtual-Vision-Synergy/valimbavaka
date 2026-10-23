# SRV-01 · Catalogue des services salon

> Ticket GitHub [#29](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/29) · Ouvert · Milestone : 07 · Services salon (catalogue + prestations) · 1 SP · Assigné(e) : Kei-Shiro · Labels : needs-info

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux définir un catalogue de services salon afin de cadrer l'offre de prestations.

## Dépendances

- Bloqué par `ART-01` (#21)

## Critères d'acceptation

- [ ] Le formulaire couvre nom, durée, prix, commission et produits habituellement consommés.

## Détails

| | |
|---|---|
| **ID Ticket** | `SRV-01` |
| **Module** | Services salon |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

> Déplacé dans la milestone « 07 · Services salon » (revue du backlog du 07/10) : fusion des milestones Services (catalogue et prestations).

> Question client en attente : la commission du service s'ajoute-t-elle ou remplace-t-elle celle de la vendeuse ?

## Ce ticket débloque

- [SRV-02](../SRV-02/description.md) · Enregistrement d'une prestation

## Dépend de

- [ART-01](../ART-01/description.md) · Fiche article complète (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/services`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/salonservice (à créer)`
