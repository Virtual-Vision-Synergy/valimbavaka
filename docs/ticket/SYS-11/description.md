# SYS-11 · Session expirée avec des saisies en attente

> Ticket GitHub [#137](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/137) · Ouvert · Milestone : 01 · Authentification & socle technique · 1 SP · Assigné(e) : Kei-Shiro

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux que mes saisies en attente soient conservées si ma session expire pendant une coupure afin de les retrouver et de les envoyer après ma reconnexion.

## Dépendances

- Bloqué par `SYS-02` (#16)
- Bloqué par `AUTH-01` (#10)

## Critères d'acceptation

- [ ] Si la session expire pendant la coupure, les saisies en attente ne sont ni perdues ni envoyées sans authentification.
- [ ] Au retour du réseau, la session est renouvelée automatiquement, ou l'utilisateur est invité à se reconnecter, puis les saisies en attente sont synchronisées.
- [ ] Les saisies en attente ne sont envoyées qu'avec le compte qui les a créées (pas par un autre utilisateur qui se connecte sur le même poste).

## Détails

| | |
|---|---|
| **ID Ticket** | `SYS-11` |
| **Module** | Transverse |
| **Story points** | 1 |
| **Utilisateur concerné** | Tous |

> Non testé le 20/10/2026 : expiration de session hors ligne et partage d'un poste entre deux comptes.

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [SYS-02](../SYS-02/description.md) · Sauvegarde automatique hors-ligne (fermé)
- [AUTH-01](../AUTH-01/description.md) · Connexion selon le rôle attribué (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/core/offline`
- `valimbavaka-ui/src/app/features/sync`
- `valimbavaka-ui/src/app/core/http/session-interceptor.ts`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/idempotency`
- `docs/adr/0001-synchronisation-hors-ligne.md`
