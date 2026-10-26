# SYS-12 · Idempotence côté serveur des saisies renvoyées

> Ticket GitHub [#138](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/138) · Ouvert · Milestone : 01 · Authentification & socle technique · 1 SP · Assigné(e) : ManaJudy

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux qu'une saisie renvoyée plusieurs fois ne crée jamais de doublon afin que mes données et mon journal restent justes.

## Dépendances

- Bloqué par `SYS-02` (#16)
- Bloqué par `JNL-01` (#14)

## Critères d'acceptation

- [x] Le serveur reconnaît l'en-tête `Idempotency-Key` : une même clé envoyée plusieurs fois ne crée qu'un seul enregistrement et renvoie le même résultat. — *⚠ partiel : tests écrits, non exécutés ; filtre livré par SYS-02, tests ajoutés (cas 1 à 1g)*
- [x] Le journal append-only ne reçoit qu'une seule écriture par saisie, même en cas de renvoi. — *⚠ partiel : tests écrits, non exécutés (cas 2)*
- [x] Une clé déjà utilisée avec un contenu différent est refusée avec une erreur claire. — *⚠ partiel : écart corrigé (empreinte du corps, migration `2026_10_26-02`) ; tests écrits, non exécutés (cas 3 à 3e)*

## Détails

| | |
|---|---|
| **ID Ticket** | `SYS-12` |
| **Module** | Transverse |
| **Story points** | 1 |
| **Utilisateur concerné** | Tous |

> Constat de test (20/10/2026) : le client envoie bien l'en-tête `Idempotency-Key` (identifiant de la saisie) à chaque requête ; la prise en charge côté serveur n'a pas été vérifiée.

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [SYS-02](../SYS-02/description.md) · Sauvegarde automatique hors-ligne (fermé)
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/core/offline`
- `valimbavaka-ui/src/app/features/sync`
- `valimbavaka-ui/src/app/core/http/session-interceptor.ts`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/idempotency`
- `docs/adr/0001-synchronisation-hors-ligne.md`
