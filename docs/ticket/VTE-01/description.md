# VTE-01 · Enregistrement d'une vente multicanal

> Ticket GitHub [#62](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/62) · Ouvert · Milestone : 11 · Vente · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que vendeuse, je veux enregistrer une vente multicanal (boutique/Facebook/téléphone) afin de tracer chaque transaction.

## Dépendances

- Bloqué par `ART-01` (#21)
- Bloqué par `AUTH-03` (#12)
- Bloqué par `JR-01` (#54)
- Bloqué par `STK-01` (#30)
- Bloqué par `JNL-02` (#15)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-05` (#111)

## Critères d'acceptation

- [ ] Le formulaire permet de choisir canal, vendeuse, paiement et lignes d'articles.

## Détails

| | |
|---|---|
| **ID Ticket** | `VTE-01` |
| **Module** | Ventes |
| **Story points** | 2 |
| **Utilisateur concerné** | Vendeuse |

## Ce ticket débloque

- [JR-03](../JR-03/description.md) · Suivi temps réel de la journée
- [VTE-02](../VTE-02/description.md) · Poste de caisse avec calcul automatique
- [VTE-07](../VTE-07/description.md) · Consultation des ventes du jour
- [VTE-08](../VTE-08/description.md) · Tableau de bord des ventes par canal
- [VTE-09](../VTE-09/description.md) · Détail d'une vente
- [FAC-02](../FAC-02/description.md) · Régénération d'une facture existante
- [COM-02](../COM-02/description.md) · Calcul automatique de la commission
- [JR-04](../JR-04/description.md) · Réconciliation automatique de clôture
- [DSH-02](../DSH-02/description.md) · Graphique du CA sur 7 jours
- [DSH-04](../DSH-04/description.md) · Répartition du CA par gamme et canal
- [RPT-01](../RPT-01/description.md) · Rapports filtrables journaliers/mensuels/annuels

## Dépend de

- [ART-01](../ART-01/description.md) · Fiche article complète (ouvert)
- [AUTH-03](../AUTH-03/description.md) · Création et modification d'un utilisateur (fermé)
- [JR-01](../JR-01/description.md) · Répartition matinale du stock (ouvert)
- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (ouvert)
- [JNL-02](../JNL-02/description.md) · Interdiction de suppression des opérations validées (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/pos`
- `valimbavaka-ui/src/app/features/sales`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/sale (à créer)`
