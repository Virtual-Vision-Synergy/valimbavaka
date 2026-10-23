# INV-01 · Ouverture d'une session d'inventaire

> Ticket GitHub [#77](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/77) · Ouvert · Milestone : 06 · Inventaire · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux ouvrir une session d'inventaire (emplacement, périmètre, responsable) afin d'organiser un comptage.

## Dépendances

- Bloqué par `STK-01` (#30)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-05` (#111)

## Critères d'acceptation

- [ ] Le formulaire d'ouverture requiert ces 4 informations.

## Détails

| | |
|---|---|
| **ID Ticket** | `INV-01` |
| **Module** | Inventaire |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [INV-02](../INV-02/description.md) · Comptage physique avec calcul d'écart

## Dépend de

- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/inventory`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/inventory (à créer)`
