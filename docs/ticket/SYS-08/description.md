# SYS-08 · Reprise automatique de la synchronisation

> Ticket GitHub [#134](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/134) · Ouvert · Milestone : 01 · Authentification & socle technique · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux que mes saisies en attente soient renvoyées automatiquement après un échec réseau afin de ne pas avoir à recharger la page ni à attendre un retour de connexion pour qu'elles partent.

## Dépendances

- Bloqué par `SYS-02` (#16)

## Critères d'acceptation

- [ ] Après une erreur réseau, la synchronisation est relancée automatiquement avec un délai croissant entre les tentatives, sans action de l'utilisateur.
- [ ] Le nombre de tentatives est limité ; au-delà, la saisie passe en échec et reste visible (voir `SYS-09`).
- [ ] Une saisie qui réussit à une relance n'est envoyée qu'une seule fois.

## Détails

| | |
|---|---|
| **ID Ticket** | `SYS-08` |
| **Module** | Transverse |
| **Story points** | 1 |
| **Utilisateur concerné** | Tous |

> Constat de test (20/10/2026) : après une erreur réseau, une saisie reste en attente et n'est pas renvoyée pendant au moins 15 secondes ; la relance n'a lieu qu'à un changement de connexion, une connexion ou un rechargement.

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [SYS-02](../SYS-02/description.md) · Sauvegarde automatique hors-ligne (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/core/offline`
- `valimbavaka-ui/src/app/features/sync`
- `valimbavaka-ui/src/app/core/http/session-interceptor.ts`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/idempotency`
- `docs/adr/0001-synchronisation-hors-ligne.md`
