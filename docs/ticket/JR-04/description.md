# JR-04 · Réconciliation automatique de clôture

> Ticket GitHub [#91](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/91) · Ouvert · Milestone : 15 · Clôture de journée · 2 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux une réconciliation automatique de clôture (transféré/vendu/livré/retour/salon/shooting) afin de fiabiliser le stock chaque soir.

## Dépendances

- Bloqué par `LIV-04` (#49)
- Bloqué par `SHO-02` (#44)
- Bloqué par `SRV-02` (#82)
- Bloqué par `TRF-02` (#55)
- Bloqué par `VTE-01` (#62)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Chaque article affiche les 6 composantes et l'écart théorique/compté.

## Détails

| | |
|---|---|
| **ID Ticket** | `JR-04` |
| **Module** | Cycle de journée |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [SRV-04](../SRV-04/description.md) · Consommation salon dans la clôture
- [JR-05](../JR-05/description.md) · Motif obligatoire en cas d'écart
- [DSH-01](../DSH-01/description.md) · Cockpit journalier

## Dépend de

- [LIV-04](../LIV-04/description.md) · Signalement d'un échec de livraison (ouvert)
- [SHO-02](../SHO-02/description.md) · Réintégration d'un produit shooting (ouvert)
- [SRV-02](../SRV-02/description.md) · Enregistrement d'une prestation (ouvert)
- [TRF-02](../TRF-02/description.md) · Génération automatique des transferts du matin (ouvert)
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/day`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/day (à créer)`
