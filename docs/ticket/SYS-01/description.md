# SYS-01 · Export PDF/Excel généralisé

> Ticket GitHub [#105](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/105) · Ouvert · Milestone : 03 · Articles · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux un export PDF/Excel généralisé (rapports, factures, clôture, commissions) afin de répondre à l'exigence du cahier des charges.

## Dépendances

- Bloqué par `AUTH-01` (#10)

## Critères d'acceptation

- [ ] Chaque écran concerné propose un export PDF et/ou XLSX fonctionnel.

## Détails

| | |
|---|---|
| **ID Ticket** | `SYS-01` |
| **Module** | Transverse |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

> Déplacé dans la milestone « 03 · Articles » (revue du backlog du 07/10) : brique d'export commune, nécessaire dès ART-08 (même logique que JNL-01).

## Ce ticket débloque

- [ART-08](../ART-08/description.md) · Export de la liste des articles
- [FAC-03](../FAC-03/description.md) · Impression / export / envoi de facture
- [FAC-06](../FAC-06/description.md) · Export du livre des ventes
- [COM-05](../COM-05/description.md) · Recalcul et export des commissions
- [RPT-06](../RPT-06/description.md) · Export des rapports en PDF/XLSX

## Dépend de

- [AUTH-01](../AUTH-01/description.md) · Connexion selon le rôle attribué (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- _(aucun)_

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel`
