# SYS-07 · Brancher un premier formulaire sur la file hors ligne

> Ticket GitHub [#133](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/133) · Ouvert · Milestone : 01 · Authentification & socle technique · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux que la saisie d'un formulaire réel soit conservée et synchronisée automatiquement quand je perds la connexion afin de voir concrètement qu'aucune de mes saisies n'est perdue.

## Dépendances

- Bloqué par `SYS-02` (#16)
- Bloqué par `FRS-02` (#18)

## Critères d'acceptation

- [ ] Le formulaire de référence (création / modification d'un fournisseur, FRS-02) enregistre via la file hors ligne de SYS-02 et non par un appel direct à l'API.
- [ ] Hors connexion, la saisie est conservée localement, reste présente après rechargement de la page, et un indicateur visible affiche son statut (en attente / synchronisé / échec).
- [ ] Au retour du réseau, la saisie est synchronisée automatiquement et n'existe qu'une seule fois en base (aucun doublon, même si la synchronisation est relancée).
- [ ] Le principe d'intégration est documenté en quelques lignes pour être reproduit par les modules métier suivants.

## Détails

| | |
|---|---|
| **ID Ticket** | `SYS-07` |
| **Module** | Transverse |
| **Story points** | 2 |
| **Utilisateur concerné** | Tous |

> Constat de test (20/10/2026) : le moteur de file d'attente (IndexedDB `vvs-outbox`, ordre d'arrivée, en-tête `Idempotency-Key`, statuts PENDING/FAILED) fonctionne, mais aucun écran ne l'utilise encore. Le formulaire « Nouvel utilisateur » envoie directement à l'API.

## Ce ticket débloque

- [SYS-09](../SYS-09/description.md) · Suivi et gestion des saisies en échec
- [ART-07](../ART-07/description.md) · Import d'articles en masse
- [STK-05](../STK-05/description.md) · Génération d'un bon de réapprovisionnement
- [TRF-03](../TRF-03/description.md) · Enregistrement d'un retour de stock
- [STK-06](../STK-06/description.md) · Réception d'une livraison fournisseur (entrée en stock)
- [PRD-01](../PRD-01/description.md) · Création d'un lot de production
- [PRD-03](../PRD-03/description.md) · Statut d'un lot de production
- [INV-01](../INV-01/description.md) · Ouverture d'une session d'inventaire
- [INV-02](../INV-02/description.md) · Comptage physique avec calcul d'écart
- [INV-03](../INV-03/description.md) · Motif d'écart par ligne
- [INV-04](../INV-04/description.md) · Clôture d'une session d'inventaire
- [SRV-02](../SRV-02/description.md) · Enregistrement d'une prestation
- [SHO-01](../SHO-01/description.md) · Enregistrement d'une sortie shooting
- [SHO-02](../SHO-02/description.md) · Réintégration d'un produit shooting
- [LIV-01](../LIV-01/description.md) · Création d'une commande à livrer
- [LIV-03](../LIV-03/description.md) · Marquage rapide d'une livraison
- [LIV-04](../LIV-04/description.md) · Signalement d'un échec de livraison
- [LIV-05](../LIV-05/description.md) · Annulation d'une action récente
- [LIV-08](../LIV-08/description.md) · Fonctionnement hors ligne de la tournée
- [JR-01](../JR-01/description.md) · Répartition matinale du stock
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal
- [PRO-01](../PRO-01/description.md) · Création d'une promotion
- [FAC-04](../FAC-04/description.md) · Émission d'un avoir motivé
- [COM-03](../COM-03/description.md) · Validation des commissions
- [COM-05](../COM-05/description.md) · Recalcul et export des commissions
- [COM-06](../COM-06/description.md) · Ajout d'une règle de commission
- [JR-05](../JR-05/description.md) · Motif obligatoire en cas d'écart
- [JR-06](../JR-06/description.md) · Verrouillage de la clôture du jour
- [COM-01](../COM-01/description.md) · Règles de commission par canal
- [SRV-01](../SRV-01/description.md) · Catalogue des services salon

## Dépend de

- [SYS-02](../SYS-02/description.md) · Sauvegarde automatique hors-ligne (fermé)
- [FRS-02](../FRS-02/description.md) · Création / modification d'un fournisseur (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/core/offline`
- `valimbavaka-ui/src/app/features/sync`
- `valimbavaka-ui/src/app/core/http/session-interceptor.ts`
- `valimbavaka-ui/src/app/features/suppliers`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/idempotency`
- `docs/adr/0001-synchronisation-hors-ligne.md`
