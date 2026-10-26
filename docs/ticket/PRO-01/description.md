# PRO-01 · Création d'une promotion

> Ticket GitHub [#58](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/58) · Ouvert · Milestone : 12 · Promotions · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux créer une promotion (type, périmètre, canaux, période) afin d'animer mes ventes.

## Dépendances

- Bloqué par `ART-01` (#21)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-05` (#111)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Le formulaire couvre l'ensemble des champs et enregistre la promotion.

## Détails

| | |
|---|---|
| **ID Ticket** | `PRO-01` |
| **Module** | Promotions |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [ART-06](../ART-06/description.md) · Fiche article unifiée (360°)
- [PRO-02](../PRO-02/description.md) · Application automatique d'une promotion en caisse
- [PRO-04](../PRO-04/description.md) · Liste et détail des promotions
- [VTE-04](../VTE-04/description.md) · Application d'un code promo

## Dépend de

- [ART-01](../ART-01/description.md) · Fiche article complète (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/promotions`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/promotion (à créer)`
