# LIV-01 · Création d'une commande à livrer

> Ticket GitHub [#46](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/46) · Ouvert · Milestone : 09 · Commande & Livraison/Réception · 2 SP · Assigné(e) : Kei-Shiro · Labels : needs-info

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux créer une commande à livrer (client, adresse, livreur) afin de déclencher une tournée.

## Dépendances

- Bloqué par `ART-01` (#21)
- Bloqué par `AUTH-03` (#12)
- Bloqué par `STK-01` (#30)
- Bloqué par `JNL-02` (#15)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] La commande créée est ajoutée à la tournée du livreur assigné.

## Détails

| | |
|---|---|
| **ID Ticket** | `LIV-01` |
| **Module** | Livraison |
| **Story points** | 2 |
| **Utilisateur concerné** | Vendeuse |

> Question client en attente : une commande livrée devient-elle une vente avec facture (impact Facture, Commissions, Clôture) ?

## Ce ticket débloque

- [LIV-02](../LIV-02/description.md) · Consultation de la tournée du jour
- [LIV-06](../LIV-06/description.md) · Suivi des livraisons par statut

## Dépend de

- [ART-01](../ART-01/description.md) · Fiche article complète (ouvert)
- [AUTH-03](../AUTH-03/description.md) · Création et modification d'un utilisateur (fermé)
- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (ouvert)
- [JNL-02](../JNL-02/description.md) · Interdiction de suppression des opérations validées (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/deliveries`
- `valimbavaka-ui/src/app/core/layout/driver-layout`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/delivery (à créer)`
