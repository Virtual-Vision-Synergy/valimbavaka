# LIV-04 · Signalement d'un échec de livraison

> Ticket GitHub [#49](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/49) · Ouvert · Milestone : 09 · Commande & Livraison/Réception · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que livreur, je veux signaler un échec avec motif obligatoire afin de déclencher le retour automatique en stock (réception du retour).

## Dépendances

- Bloqué par `LIV-02` (#47)
- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

- [ ] Le statut Échec exige un motif et remet les articles en stock retour.

## Détails

| | |
|---|---|
| **ID Ticket** | `LIV-04` |
| **Module** | Livraison |
| **Story points** | 2 |
| **Utilisateur concerné** | Livreur |

## Ce ticket débloque

- [LIV-05](../LIV-05/description.md) · Annulation d'une action récente
- [JR-04](../JR-04/description.md) · Réconciliation automatique de clôture

## Dépend de

- [LIV-02](../LIV-02/description.md) · Consultation de la tournée du jour (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/deliveries`
- `valimbavaka-ui/src/app/core/layout/driver-layout`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/delivery (à créer)`
