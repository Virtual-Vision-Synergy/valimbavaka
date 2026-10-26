# FRS-02 · Création / modification d'un fournisseur

> Ticket GitHub [#18](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/18) · Ouvert · Milestone : 02 · Fournisseurs · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux créer/modifier un fournisseur afin de maintenir mon répertoire à jour.

## Dépendances

- Bloqué par `FRS-01` (#17)
- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

- [x] Le formulaire enregistre correctement les modifications. — *✅ vérifié : création et modification des 7 champs, version renvoyée, journal `CREATE` / `UPDATE` seulement si la fiche change (voir [scénario](scenario-test.md))*

## Détails

| | |
|---|---|
| **ID Ticket** | `FRS-02` |
| **Module** | Fournisseurs |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne

## Dépend de

- [FRS-01](../FRS-01/description.md) · Fiche fournisseur complète (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/suppliers`
- `valimbavaka-ui/src/app/features/supplier`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/supplier`
