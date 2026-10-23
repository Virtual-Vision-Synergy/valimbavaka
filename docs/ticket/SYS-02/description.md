# SYS-02 · Sauvegarde automatique hors-ligne

> Ticket GitHub [#16](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/16) · Fermé · Milestone : 01 · Authentification & socle technique · 2 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux que mes saisies soient sauvegardées automatiquement même hors connexion afin de ne rien perdre, dès la construction des premiers écrans.

## Dépendances

- Bloqué par `AUTH-01` (#10)

## Critères d'acceptation

> Vérifiés le 2026-10-23 sur le code, les tests et la base : détail dans [scenario-test.md](scenario-test.md).

- [x] Les saisies hors ligne sont mises en file et synchronisées au retour du réseau. — *✅ vérifié.*
- [x] L'architecture de synchronisation est posée avant le développement des modules métier pour éviter une reprise ultérieure. — *✅ vérifié : file, idempotence serveur, ADR 0001.*

## Détails

| | |
|---|---|
| **ID Ticket** | `SYS-02` |
| **Module** | Transverse |
| **Story points** | 2 |
| **Utilisateur concerné** | Tous |

## Ce ticket débloque

- [LIV-08](../LIV-08/description.md) · Fonctionnement hors ligne de la tournée
- [SYS-07](../SYS-07/description.md) · Brancher un premier formulaire sur la file hors ligne
- [SYS-08](../SYS-08/description.md) · Reprise automatique de la synchronisation
- [SYS-09](../SYS-09/description.md) · Suivi et gestion des saisies en échec
- [SYS-10](../SYS-10/description.md) · Règle d'ordre de synchronisation en cas de blocage
- [SYS-11](../SYS-11/description.md) · Session expirée avec des saisies en attente
- [SYS-12](../SYS-12/description.md) · Idempotence côté serveur des saisies renvoyées

## Dépend de

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
