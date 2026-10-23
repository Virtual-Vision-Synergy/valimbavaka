# SYS-10 · Règle d'ordre de synchronisation en cas de blocage

> Ticket GitHub [#136](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/136) · Ouvert · Milestone : 01 · Authentification & socle technique · 1 SP · Assigné(e) : And-Matia, Kei-Shiro · Labels : needs-info

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux qu'une saisie impossible à envoyer ne bloque pas toutes les suivantes sans règle claire afin que mon travail reste synchronisé dans le bon ordre.

## Dépendances

- Bloqué par `SYS-02` (#16)

## Critères d'acceptation

- [ ] La règle est décidée avec l'équipe et écrite dans ce ticket : quelles saisies peuvent passer devant une saisie bloquée, et lesquelles doivent attendre (ordre strict pour le journal et les factures).
- [ ] Une saisie bloquée par un problème réseau n'empêche pas l'envoi des saisies indépendantes autorisées par cette règle.
- [ ] Les saisies qui dépendent les unes des autres sont toujours envoyées dans l'ordre de saisie.

## Détails

| | |
|---|---|
| **ID Ticket** | `SYS-10` |
| **Module** | Transverse |
| **Story points** | 1 |
| **Utilisateur concerné** | Tous |

> Décision d'équipe à prendre avant le développement. Constat de test (20/10/2026) : une seule saisie en erreur réseau arrête l'envoi de toutes celles qui la suivent dans la file.

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
