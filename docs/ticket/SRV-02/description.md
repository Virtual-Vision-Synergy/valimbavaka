# SRV-02 · Enregistrement d'une prestation

> Ticket GitHub [#82](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/82) · Ouvert · Milestone : 07 · Services salon (catalogue + prestations) · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux enregistrer une prestation avec ses produits consommés afin que le stock salon soit à jour.

## Dépendances

- Bloqué par `SRV-01` (#29)
- Bloqué par `STK-01` (#30)
- Bloqué par `JNL-02` (#15)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] L'enregistrement déduit automatiquement le stock salon des produits consommés.

## Détails

| | |
|---|---|
| **ID Ticket** | `SRV-02` |
| **Module** | Services salon |
| **Story points** | 2 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- [JR-03](../JR-03/description.md) · Suivi temps réel de la journée
- [SRV-03](../SRV-03/description.md) · Historique des prestations du jour
- [SRV-04](../SRV-04/description.md) · Consommation salon dans la clôture
- [COM-02](../COM-02/description.md) · Calcul automatique de la commission
- [JR-04](../JR-04/description.md) · Réconciliation automatique de clôture

## Dépend de

- [SRV-01](../SRV-01/description.md) · Catalogue des services salon (ouvert)
- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (ouvert)
- [JNL-02](../JNL-02/description.md) · Interdiction de suppression des opérations validées (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/services`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/salonservice (à créer)`
