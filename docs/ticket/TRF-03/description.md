# TRF-03 · Enregistrement d'un retour de stock

> Ticket GitHub [#36](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/36) · Ouvert · Milestone : 04 · Stock & transferts · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux enregistrer un retour de stock (ex. boutique → entrepôt) afin de refléter les articles non écoulés.

## Dépendances

- Bloqué par `STK-01` (#30)
- Bloqué par `JNL-02` (#15)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-05` (#111)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Un transfert de retour est identifiable par un statut dédié.

- [ ] Ce ticket pose le modèle de transfert (origine, destination, lignes, statut) réutilisé par TRF-01, TRF-04 et l'ouverture de journée (TRF-02, JR-01).

## Détails

| | |
|---|---|
| **ID Ticket** | `TRF-03` |
| **Module** | Transferts |
| **Story points** | 1 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- [TRF-01](../TRF-01/description.md) · Historique des transferts de stock
- [JR-01](../JR-01/description.md) · Répartition matinale du stock

## Dépend de

- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (ouvert)
- [JNL-02](../JNL-02/description.md) · Interdiction de suppression des opérations validées (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/transfers`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/transfer (à créer)`
