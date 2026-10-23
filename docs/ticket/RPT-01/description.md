# RPT-01 · Rapports filtrables journaliers/mensuels/annuels

> Ticket GitHub [#98](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/98) · Ouvert · Milestone : 16 · Rapports & tableau de bord · 2 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux des rapports journaliers/mensuels/annuels filtrables afin d'analyser l'activité sous tous les angles.

## Dépendances

- Bloqué par `VTE-01` (#62)
- Bloqué par `SYS-05` (#111)

## Critères d'acceptation

- [ ] Le rapport se met à jour selon la période et les filtres gamme/canal.

## Détails

| | |
|---|---|
| **ID Ticket** | `RPT-01` |
| **Module** | Rapports |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [RPT-02](../RPT-02/description.md) · Indicateurs comparés à la période précédente
- [RPT-03](../RPT-03/description.md) · Graphique d'évolution du CA
- [RPT-04](../RPT-04/description.md) · Répartition du CA par gamme
- [RPT-05](../RPT-05/description.md) · Classement des produits les plus vendus
- [RPT-06](../RPT-06/description.md) · Export des rapports en PDF/XLSX

## Dépend de

- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal (ouvert)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/reports`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/report (à créer)`
