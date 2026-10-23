# COM-02 · Calcul automatique de la commission

> Ticket GitHub [#86](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/86) · Ouvert · Milestone : 14 · Commissions · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux que la commission soit calculée automatiquement selon la règle du canal afin d'éviter les erreurs manuelles.

## Dépendances

- Bloqué par `COM-01` (#85)
- Bloqué par `SRV-02` (#82)
- Bloqué par `VTE-01` (#62)
- Bloqué par `SYS-05` (#111)

## Critères d'acceptation

- [ ] Le montant affiché correspond au CA généré multiplié par le taux applicable.

## Détails

| | |
|---|---|
| **ID Ticket** | `COM-02` |
| **Module** | Commissions |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [VTE-09](../VTE-09/description.md) · Détail d'une vente
- [COM-03](../COM-03/description.md) · Validation des commissions
- [COM-04](../COM-04/description.md) · Suivi de commission et objectif
- [COM-05](../COM-05/description.md) · Recalcul et export des commissions

## Dépend de

- [COM-01](../COM-01/description.md) · Règles de commission par canal (ouvert)
- [SRV-02](../SRV-02/description.md) · Enregistrement d'une prestation (ouvert)
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal (ouvert)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/commissions`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/commission (à créer)`
