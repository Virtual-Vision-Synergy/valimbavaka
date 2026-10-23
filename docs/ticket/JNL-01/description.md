# JNL-01 · Journal append-only des opérations

> Ticket GitHub [#14](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/14) · Fermé · Milestone : 01 · Authentification & socle technique · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux qu'un journal append-only trace chaque opération afin de garantir la traçabilité complète dès le premier module livré.

## Dépendances

- Bloqué par `AUTH-01` (#10)
- Bloqué par `SYS-06` (#112)

## Critères d'acceptation

- [ ] Chaque opération métier génère automatiquement une ligne horodatée et nominative.
- [ ] Le mécanisme est disponible comme brique commune que tous les modules suivants devront appeler.

## Détails

| | |
|---|---|
| **ID Ticket** | `JNL-01` |
| **Module** | Journal |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [AUTH-03](../AUTH-03/description.md) · Création et modification d'un utilisateur
- [AUTH-04](../AUTH-04/description.md) · Activation / désactivation d'un utilisateur
- [JNL-02](../JNL-02/description.md) · Interdiction de suppression des opérations validées
- [FRS-01](../FRS-01/description.md) · Fiche fournisseur complète
- [FRS-02](../FRS-02/description.md) · Création / modification d'un fournisseur
- [ART-01](../ART-01/description.md) · Fiche article complète
- [ART-02](../ART-02/description.md) · Création / modification d'un article
- [ART-06](../ART-06/description.md) · Fiche article unifiée (360°)
- [ART-07](../ART-07/description.md) · Import d'articles en masse
- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement
- [STK-05](../STK-05/description.md) · Génération d'un bon de réapprovisionnement
- [TRF-03](../TRF-03/description.md) · Enregistrement d'un retour de stock
- [PRD-01](../PRD-01/description.md) · Création d'un lot de production
- [PRD-02](../PRD-02/description.md) · Cohérence stock lors de la validation d'un lot
- [SHO-01](../SHO-01/description.md) · Enregistrement d'une sortie shooting
- [SHO-02](../SHO-02/description.md) · Réintégration d'un produit shooting
- [LIV-01](../LIV-01/description.md) · Création d'une commande à livrer
- [LIV-03](../LIV-03/description.md) · Marquage rapide d'une livraison
- [LIV-04](../LIV-04/description.md) · Signalement d'un échec de livraison
- [LIV-05](../LIV-05/description.md) · Annulation d'une action récente
- [JR-01](../JR-01/description.md) · Répartition matinale du stock
- [TRF-02](../TRF-02/description.md) · Génération automatique des transferts du matin
- [PRO-01](../PRO-01/description.md) · Création d'une promotion
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal
- [FAC-01](../FAC-01/description.md) · Génération automatique de facture
- [FAC-02](../FAC-02/description.md) · Régénération d'une facture existante
- [FAC-04](../FAC-04/description.md) · Émission d'un avoir motivé
- [INV-01](../INV-01/description.md) · Ouverture d'une session d'inventaire
- [INV-02](../INV-02/description.md) · Comptage physique avec calcul d'écart
- [INV-04](../INV-04/description.md) · Clôture d'une session d'inventaire
- [SRV-02](../SRV-02/description.md) · Enregistrement d'une prestation
- [COM-01](../COM-01/description.md) · Règles de commission par canal
- [COM-03](../COM-03/description.md) · Validation des commissions
- [COM-05](../COM-05/description.md) · Recalcul et export des commissions
- [COM-06](../COM-06/description.md) · Ajout d'une règle de commission
- [JR-04](../JR-04/description.md) · Réconciliation automatique de clôture
- [JR-06](../JR-06/description.md) · Verrouillage de la clôture du jour
- [JNL-03](../JNL-03/description.md) · Accès au journal complet
- [STK-06](../STK-06/description.md) · Réception d'une livraison fournisseur (entrée en stock)
- [SYS-12](../SYS-12/description.md) · Idempotence côté serveur des saisies renvoyées

## Dépend de

- [AUTH-01](../AUTH-01/description.md) · Connexion selon le rôle attribué (fermé)
- [SYS-06](../SYS-06/description.md) · Audit : auteur des opérations en UUID (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/journal`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/journal`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/operation`
