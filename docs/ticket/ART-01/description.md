# ART-01 · Fiche article complète

> Ticket GitHub [#21](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/21) · Ouvert · Milestone : 03 · Articles · 2 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux définir une fiche article complète (code, gamme, type, unité, coût, prix, seuil) afin de gérer mon catalogue.

## Dépendances

- Bloqué par `FRS-01` (#17)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-05` (#111)

## Critères d'acceptation

- [ ] Tous les champs sont visibles et modifiables sur la fiche.
- [ ] Le type d'article (MP/SF/Fini) est correctement enregistré.

## Détails

| | |
|---|---|
| **ID Ticket** | `ART-01` |
| **Module** | Produits |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [ART-02](../ART-02/description.md) · Création / modification d'un article
- [ART-03](../ART-03/description.md) · Identification visuelle de la gamme
- [ART-04](../ART-04/description.md) · Type d'article et usage en production
- [ART-05](../ART-05/description.md) · Recherche et filtrage des articles
- [ART-07](../ART-07/description.md) · Import d'articles en masse
- [ART-08](../ART-08/description.md) · Export de la liste des articles
- [SRV-01](../SRV-01/description.md) · Catalogue des services salon
- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement
- [LIV-01](../LIV-01/description.md) · Création d'une commande à livrer
- [PRO-01](../PRO-01/description.md) · Création d'une promotion
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal
- [SYS-03](../SYS-03/description.md) · Recherche globale (palette de commandes)

## Dépend de

- [FRS-01](../FRS-01/description.md) · Fiche fournisseur complète (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/articles`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/article (à créer)`
