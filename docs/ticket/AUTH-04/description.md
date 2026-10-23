# AUTH-04 · Activation / désactivation d'un utilisateur

> Ticket GitHub [#13](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/13) · Fermé · Milestone : 01 · Authentification & socle technique · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'administrateur, je veux activer/désactiver un utilisateur afin de contrôler les accès actifs.

## Dépendances

- Bloqué par `AUTH-03` (#12)
- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

> Vérifiés le 2026-10-23 sur le code, les tests et la base : détail dans [scenario-test.md](scenario-test.md).

- [x] La liste des utilisateurs affiche un badge ACTIF/DÉSACTIVÉ. — *✅ vérifié.*

<img width="1917" height="905" alt="Image" src="https://github.com/user-attachments/assets/6f110801-feb0-459a-842c-69101da2e677" />

- [ ] Un utilisateur désactivé n'apparaît plus dans les listes de sélection opérationnelles.(Les get prend en compte que les user desactive ne soit plus dan la liste(login,vente,etc) — *⏭ à porter par les tickets qui créent les listes de sélection (VTE-01, LIV-01, COM-03).*

- [x] Un utilisateur désactivé ne peut plus se connecter. — *✅ vérifié (connexion et refresh).*

- [ ] Le dernier administrateur actif ne peut être ni désactivé ni privé de son rôle. — *❌ écart : garanti seulement par `PATCH /status` ; `PUT` et `DELETE` le contournent.*

## Détails

| | |
|---|---|
| **ID Ticket** | `AUTH-04` |
| **Module** | Authentification |
| **Story points** | 1 |
| **Utilisateur concerné** | Administrateur |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [AUTH-03](../AUTH-03/description.md) · Création et modification d'un utilisateur (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/features/user`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/user`
