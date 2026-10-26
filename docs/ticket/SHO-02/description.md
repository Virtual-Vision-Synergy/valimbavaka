# SHO-02 · Réintégration d'un produit shooting

> Ticket GitHub [#44](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/44) · Ouvert · Milestone : 08 · Shooting · 1 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux réintégrer un produit sorti pour shooting afin de remettre le stock à jour.

## Dépendances

- Bloqué par `SHO-01` (#43)
- Bloqué par `JNL-01` (#14)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Le statut passe à Retourné et le stock entrepôt augmente en conséquence.

## Détails

| | |
|---|---|
| **ID Ticket** | `SHO-02` |
| **Module** | Shooting |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [JR-04](../JR-04/description.md) · Réconciliation automatique de clôture

## Dépend de

- [SHO-01](../SHO-01/description.md) · Enregistrement d'une sortie shooting (ouvert)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/day`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/shooting (à créer)`
