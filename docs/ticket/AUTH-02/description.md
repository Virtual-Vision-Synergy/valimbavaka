# AUTH-02 · Déconnexion utilisateur

> Ticket GitHub [#11](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/11) · Fermé · Milestone : 01 · Authentification & socle technique · 1 SP · Assigné(e) : And-Matia

Dossier : [scénario de test](scenario-test.md) · [guide d'intégration](guide.md) · [index des tickets](../README.md)

## User story

En tant qu'utilisateur, je veux me déconnecter de mon poste afin de permettre à un autre utilisateur de se connecter avec son propre compte.

## Dépendances

- Bloqué par `AUTH-01` (#10)

## Critères d'acceptation

- [ ] Un bouton de déconnexion est visible en permanence dans la barre supérieure.

<img width="1911" height="912" alt="Image" src="https://github.com/user-attachments/assets/097368ea-e852-447b-9892-2e833778f192" />

- [ ] La déconnexion ramène à l'écran de connexion et exige une nouvelle authentification — aucun changement de rôle n'est possible sans reconnexion avec un autre compte.

<img width="1917" height="902" alt="Image" src="https://github.com/user-attachments/assets/0b699b20-5377-4938-959a-8cc643f05bec" />

## Détails

| | |
|---|---|
| **ID Ticket** | `AUTH-02` |
| **Module** | Authentification |
| **Story points** | 1 |
| **Utilisateur concerné** | Tous |

## Ce ticket débloque

- _(aucun ticket ne dépend de celui-ci)_

## Dépend de

- [AUTH-01](../AUTH-01/description.md) · Connexion selon le rôle attribué (fermé)

## Zone du code concernée

_Repérée dans l'arborescence au 2026-10-23 ; le traiteur confirme dans `guide.md`._

**Frontend (`valimbavaka-ui`)**
- `valimbavaka-ui/src/app/core/auth/auth-service.ts`
- `valimbavaka-ui/src/app/core/auth/auth-store.ts`

**Backend (`valimbavaka-core`)**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/auth/controller/AuthController.java`
