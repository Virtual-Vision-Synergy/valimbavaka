# COM-01 · Règles de commission par canal

> Ticket GitHub [#85](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/85) · Ouvert · Milestone : 14 · Commissions · 2 SP · Assigné(e) : ManaJudy · Labels : needs-info

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux définir des règles de commission par canal (taux, base de calcul) afin de cadrer la rémunération variable.

## Dépendances

- Bloqué par `AUTH-03` (#12)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-05` (#111)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Le formulaire couvre nom, canal, taux et base de calcul.

## Détails

| | |
|---|---|
| **ID Ticket** | `COM-01` |
| **Module** | Commissions |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

> Question client en attente : la commission est définie par utilisateur (AUTH-03), par canal (ici) et par service salon (SRV-01). Laquelle s'applique, et dans quel ordre ?

## Ce ticket débloque

- [COM-02](../COM-02/description.md) · Calcul automatique de la commission
- [COM-06](../COM-06/description.md) · Ajout d'une règle de commission

## Dépend de

- [AUTH-03](../AUTH-03/description.md) · Création et modification d'un utilisateur (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/commissions`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/commission (à créer)`
