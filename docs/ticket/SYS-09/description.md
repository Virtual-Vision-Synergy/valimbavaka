# SYS-09 · Suivi et gestion des saisies en échec

> Ticket GitHub [#135](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/135) · Ouvert · Milestone : 01 · Authentification & socle technique · 2 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux voir mes saisies en attente ou en échec et pouvoir les renvoyer ou les abandonner afin de corriger un problème sans perdre mon travail.

## Dépendances

- Bloqué par `SYS-02` (#16)
- Bloqué par `SYS-07` (#133)

## Critères d'acceptation

- [ ] Un indicateur global affiche le nombre de saisies en attente et en échec.
- [ ] La liste des saisies en échec affiche le message d'erreur de chacune.
- [ ] L'utilisateur peut renvoyer une saisie en échec, ou l'abandonner après confirmation.

## Détails

| | |
|---|---|
| **ID Ticket** | `SYS-09` |
| **Module** | Transverse |
| **Story points** | 2 |
| **Utilisateur concerné** | Tous |

> Constat de test (20/10/2026) : le moteur marque une saisie rejetée par le serveur comme « FAILED » avec son message d'erreur et sait la renvoyer ou la supprimer, mais aucun écran ne l'affiche.

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [SYS-02](../SYS-02/description.md) · Sauvegarde automatique hors-ligne (fermé)
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne (ouvert)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/core/offline`
- `valimbavaka-ui/src/app/features/sync`
- `valimbavaka-ui/src/app/core/http/session-interceptor.ts`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/idempotency`
- `docs/adr/0001-synchronisation-hors-ligne.md`
