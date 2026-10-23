# JNL-02 · Interdiction de suppression des opérations validées

> Ticket GitHub [#15](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/15) · Fermé · Milestone : 01 · Authentification & socle technique · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux qu'aucune opération validée ne puisse être supprimée afin de préserver l'intégrité des données dès la conception du socle.

## Dépendances

- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

- [ ] Seules l'annulation à chaud ou la contre-passation (avoir) sont possibles.
- [ ] Aucune fonction de suppression définitive n'est exposée dans le socle technique.

## Détails

| | |
|---|---|
| **ID Ticket** | `JNL-02` |
| **Module** | Journal |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [AUTH-03](../AUTH-03/description.md) · Création et modification d'un utilisateur
- [TRF-03](../TRF-03/description.md) · Enregistrement d'un retour de stock
- [PRD-01](../PRD-01/description.md) · Création d'un lot de production
- [SHO-01](../SHO-01/description.md) · Enregistrement d'une sortie shooting
- [LIV-01](../LIV-01/description.md) · Création d'une commande à livrer
- [JR-01](../JR-01/description.md) · Répartition matinale du stock
- [VTE-01](../VTE-01/description.md) · Enregistrement d'une vente multicanal
- [FAC-04](../FAC-04/description.md) · Émission d'un avoir motivé
- [INV-04](../INV-04/description.md) · Clôture d'une session d'inventaire
- [SRV-02](../SRV-02/description.md) · Enregistrement d'une prestation
- [STK-06](../STK-06/description.md) · Réception d'une livraison fournisseur (entrée en stock)

## Dépend de

- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/journal`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/journal`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/operation`

## Échanges sur le ticket

**And-Matia** :

> Socle JNL-02 poussé sur la branche `(branche de travail)` (3 commits, sans PR pour l'instant).
>
> - `shared/operation` : `OperationStatus` (annulation à chaud / contre-passation uniquement depuis `VALIDATED`), `OperationRepository` (sans `delete*`, pas de `JpaRepository`) et `ReversibleOperationService` (`cancel` / `reverse`).
> - Journal : actions `CANCEL` et `REVERSE` (migration `2026_10_20-01`, rollback fourni).
> - ArchUnit : aucun `@DeleteMapping` hors `feature.user`, aucun repository de feature métier héritant de `CrudRepository`, aucune méthode `delete*`/`remove*` sur `OperationRepository`.
>
> **Règle pour les tickets d'opérations à venir** : leurs repositories étendent `OperationRepository` et leurs services implémentent `ReversibleOperationService` (dépendance sur JNL-02).
>
> Tests : ArchUnit et tests unitaires OK ; l'IT Testcontainers du journal n'a pas pu tourner ici (pas de Docker), à valider en CI.
