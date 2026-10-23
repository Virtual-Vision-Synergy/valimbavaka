# LIV-02 · Consultation de la tournée du jour

> Ticket GitHub [#47](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/47) · Ouvert · Milestone : 09 · Commande & Livraison/Réception · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que livreur, je veux consulter ma tournée du jour afin de connaître mes arrêts.

## Dépendances

- Bloqué par `LIV-01` (#46)

## Critères d'acceptation

- [ ] Chaque arrêt affiche client, adresse, téléphone et montant à encaisser.

## Détails

| | |
|---|---|
| **ID Ticket** | `LIV-02` |
| **Module** | Livraison |
| **Story points** | 2 |
| **Utilisateur concerné** | Livreur |

## Ce ticket débloque

- [LIV-03](../LIV-03/description.md) · Marquage rapide d'une livraison
- [LIV-04](../LIV-04/description.md) · Signalement d'un échec de livraison
- [JR-03](../JR-03/description.md) · Suivi temps réel de la journée

## Dépend de

- [LIV-01](../LIV-01/description.md) · Création d'une commande à livrer (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/deliveries`
- `valimbavaka-ui/src/app/core/layout/driver-layout`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/delivery (à créer)`
