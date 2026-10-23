# STK-01 · Suivi du stock par emplacement

> Ticket GitHub [#30](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/30) · Ouvert · Milestone : 04 · Stock & transferts · 2 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux suivre le stock de chaque article par emplacement (entrepôt/boutique/livraison/salon) afin de connaître sa localisation réelle.

## Dépendances

- Bloqué par `ART-01` (#21)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-05` (#111)

## Critères d'acceptation

- [ ] Le stock d'un article détaille la quantité par emplacement et le total.

- [ ] Le stock n'est jamais modifié directement : il est calculé à partir de mouvements typés (réception, transfert, vente, livraison, retour, salon, shooting, production, inventaire), chacun tracé au journal. C'est la base de la réconciliation de clôture (JR-04).

## Détails

| | |
|---|---|
| **ID Ticket** | `STK-01` |
| **Module** | Stocks |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [ART-06](../ART-06/description.md) · Fiche article unifiée (360°)
- [STK-02](../STK-02/description.md) · Jauge visuelle du stock
- [STK-03](../STK-03/description.md) · Quantité et valeur du stock par emplacement
- [STK-04](../STK-04/description.md) · Alerte de seuil minimum
- [TRF-03](../TRF-03/description.md) · Enregistrement d'un retour de stock
- [PRD-01](../PRD-01/description.md) · Création d'un lot de production
- [SHO-01](../SHO-01/description.md) · Enregistrement d'une sortie shooting
- [LIV-01](../LIV-01/description.md) · Création d'une commande à livrer
- [JR-01](../JR-01/description.md) · Répartition matinale du stock
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal
- [INV-01](../INV-01/description.md) · Ouverture d'une session d'inventaire
- [SRV-02](../SRV-02/description.md) · Enregistrement d'une prestation
- [STK-06](../STK-06/description.md) · Réception d'une livraison fournisseur (entrée en stock)

## Dépend de

- [ART-01](../ART-01/description.md) · Fiche article complète (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/stocks`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/stock (à créer)`
