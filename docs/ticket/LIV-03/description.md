# LIV-03 · Marquage rapide d'une livraison

> Ticket GitHub [#48](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/48) · Ouvert · Milestone : 09 · Commande & Livraison/Réception · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant que livreur, je veux marquer une livraison comme livrée en un geste afin d'aller vite sur le terrain.

## Dépendances

- Bloqué par `LIV-02` (#47)
- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

- [ ] Le statut passe à Livrée sans saisie complémentaire.

## Détails

| | |
|---|---|
| **ID Ticket** | `LIV-03` |
| **Module** | Livraison |
| **Story points** | 2 |
| **Utilisateur concerné** | Livreur |

## Ce ticket débloque

- [LIV-05](../LIV-05/description.md) · Annulation d'une action récente
- [LIV-07](../LIV-07/description.md) · Historique de statut d'une livraison
- [LIV-08](../LIV-08/description.md) · Fonctionnement hors ligne de la tournée

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
