# FRS-01 · Fiche fournisseur complète

> Ticket GitHub [#17](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/17) · Fermé · Milestone : 02 · Fournisseurs · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux une fiche fournisseur complète (type, contact, articles fournis) afin de suivre mes approvisionnements.

## Dépendances

- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

> Vérifiés le 2026-10-23 sur le code, les tests et la base : détail dans [scenario-test.md](scenario-test.md).

- [x] Tous les champs sont visibles et modifiables. — *✅ vérifié ; le bloc « articles fournis » est rempli depuis ART-01 (2026-10-26, tests à lancer).*

## Détails

| | |
|---|---|
| **ID Ticket** | `FRS-01` |
| **Module** | Fournisseurs |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

> Le bloc « articles fournis » de la fiche est rempli depuis [ART-01](../ART-01/description.md) (c'est l'article qui référence son fournisseur par `supplierId`).

## Ce ticket débloque

- [FRS-02](../FRS-02/description.md) · Création / modification d'un fournisseur
- [FRS-03](../FRS-03/description.md) · Filtrage des fournisseurs par type
- [FRS-04](../FRS-04/description.md) · Délai de réapprovisionnement selon le type de fournisseur
- [ART-01](../ART-01/description.md) · Fiche article complète
- [STK-05](../STK-05/description.md) · Génération d'un bon de réapprovisionnement
- [STK-06](../STK-06/description.md) · Réception d'une livraison fournisseur (entrée en stock)

## Dépend de

- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/suppliers`
- `valimbavaka-ui/src/app/features/supplier`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier`
