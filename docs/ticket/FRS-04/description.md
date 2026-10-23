# FRS-04 · Délai de réapprovisionnement selon le type de fournisseur

> Ticket GitHub [#20](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/20) · Ouvert · Milestone : 04 · Stock & transferts · 1 SP · Assigné(e) : ManaJudy · Labels : needs-info

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux que le type de fournisseur influence le délai retenu dans les alertes de seuil afin d'anticiper le réapprovisionnement.

## Dépendances

- Bloqué par `FRS-01` (#17)
- Bloqué par `STK-04` (#33)

## Critères d'acceptation

- [ ] Règle de calcul du délai à préciser avant développement.

## Détails

| | |
|---|---|
| **ID Ticket** | `FRS-04` |
| **Module** | Fournisseurs |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

> Déplacé dans la milestone « 04 · Stock & transferts » (revue du backlog du 07/10) : dépend des alertes de seuil STK-04.

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [FRS-01](../FRS-01/description.md) · Fiche fournisseur complète (fermé)
- [STK-04](../STK-04/description.md) · Alerte de seuil minimum (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/suppliers`
- `valimbavaka-ui/src/app/features/supplier`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier`
