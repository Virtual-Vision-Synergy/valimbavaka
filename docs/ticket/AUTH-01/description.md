# AUTH-01 · Connexion selon le rôle attribué

> Ticket GitHub [#10](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/10) · Fermé · Milestone : 01 · Authentification & socle technique · 2 SP · Assigné(e) : And-Matia · Labels : needs-info

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux me connecter avec mon compte afin que le système ouvre automatiquement l'interface correspondant au rôle qui m'a été attribué par l'administrateur.

## Dépendances

- Bloqué par `SYS-05` (#111)

## Critères d'acceptation

> Vérifiés le 2026-10-23 sur le code, les tests et la base : détail dans [scenario-test.md](scenario-test.md).

- [x] Le rôle appliqué à la connexion est exclusivement celui défini sur le compte par l'administrateur (AUTH-03) — l'utilisateur ne peut ni le choisir ni le modifier lui-même. — *✅ vérifié : rôle lu sur le compte, champ `role` du corps ignoré.*

<img width="1917" height="911" alt="Image" src="https://github.com/user-attachments/assets/9d470a86-24fd-4a87-ab18-c79440274b70" />

- [x] L'interface affichée après connexion correspond uniquement à ce rôle attribué. les menu affiché sont seulement celui de l'user(securité niveau UI + niveau requete) — *✅ vérifié : menu par rôle + 403 côté API.*

<img width="1919" height="910" alt="Image" src="https://github.com/user-attachments/assets/325fde84-2104-4e56-a103-922bdb773073" />

## Détails

| | |
|---|---|
| **ID Ticket** | `AUTH-01` |
| **Module** | Authentification |
| **Story points** | 2 |
| **Utilisateur concerné** | Tous |

> Question client en attente : connexion par email ou par identifiant/téléphone (toutes les vendeuses et livreurs ont-ils un email) ?

## Ce ticket débloque

- [AUTH-02](../AUTH-02/description.md) · Déconnexion utilisateur
- [AUTH-03](../AUTH-03/description.md) · Création et modification d'un utilisateur
- [JNL-01](../JNL-01/description.md) · Journal append-only des opérations
- [SYS-02](../SYS-02/description.md) · Sauvegarde automatique hors-ligne
- [SYS-01](../SYS-01/description.md) · Export PDF/Excel généralisé
- [SYS-04](../SYS-04/description.md) · Bascule thème clair/sombre
- [SYS-11](../SYS-11/description.md) · Session expirée avec des saisies en attente

## Dépend de

- [SYS-05](../SYS-05/description.md) · Rôles métier, référentiels et compte administrateur initial (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/core/auth`
- `valimbavaka-ui/src/app/core/http`
- `valimbavaka-ui/src/app/features/auth`
- `valimbavaka-ui/src/app/core/layout`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/auth`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security`
