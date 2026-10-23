# STK-06 · Réception d'une livraison fournisseur (entrée en stock)

> Ticket GitHub [#113](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/113) · Ouvert · Milestone : 04 · Stock & transferts · 2 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux enregistrer la réception d'une livraison fournisseur (fournisseur, articles, quantités, coût) afin de faire entrer en stock les marchandises achetées.

## Dépendances

- Bloqué par `FRS-01` (#17)
- Bloqué par `STK-01` (#30)
- Bloqué par `JNL-02` (#15)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-05` (#111)
- Bloqué par `STK-05` (#34)

## Critères d'acceptation

- [ ] La réception génère un mouvement d'entrée en stock à l'emplacement choisi (entrepôt par défaut), tracé au journal.
- [ ] Une réception peut reprendre un bon de réapprovisionnement (STK-05) ou être saisie librement.
- [ ] Une réception validée ne se supprime pas : une erreur se corrige par un mouvement inverse.

## Détails

| | |
|---|---|
| **ID Ticket** | `STK-06` |
| **Module** | Stocks |
| **Story points** | 2 |
| **Utilisateur concerné** | Administrateur |

Ajouté lors de la revue du backlog du 07/10 : aucun ticket ne faisait entrer en stock les marchandises achetées aux fournisseurs.

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [FRS-01](../FRS-01/description.md) · Fiche fournisseur complète (fermé)
- [STK-01](../STK-01/description.md) · Suivi du stock par emplacement (ouvert)
- [JNL-02](../JNL-02/description.md) · Interdiction de suppression des opérations validées (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)
- [STK-05](../STK-05/description.md) · Génération d'un bon de réapprovisionnement (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/stocks`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/stock (à créer)`
