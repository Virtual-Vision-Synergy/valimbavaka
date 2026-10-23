# Guide d'intégration — AUTH-01 · Connexion selon le rôle attribué

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ⚠ pré-rempli depuis le code au 2026-10-23 — à vérifier et compléter · Ticket : [#10](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/10) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

_(2 à 3 phrases : ce que la fonctionnalité fait et ce qu'elle offre aux autres.)_

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | public (limité en débit) | `LoginWDto` | `AuthRDto + cookie refresh` |
| POST | `/api/v1/auth/refresh` | cookie refresh | `-` | `AuthRDto (rotation)` |
| GET | `/api/v1/auth/me` | authentifié | `-` | `CurrentUserRDto` |

_Relevé dans les contrôleurs ; compléter erreurs possibles, filtres et exemples de corps._

### Services et ports Java réutilisables

- _(à compléter : interface de service, port `security`, événement publié, etc.)_

### Frontend

- Routes : _(à compléter)_
- Services / stores (`data-access`) : _(à compléter)_
- Composants réutilisables : _(à compléter)_

### Données

- Tables et migrations Liquibase : _(à compléter)_
- Écriture dans le journal (action, champs) : _(à compléter)_

## Règles métier à respecter par les autres tickets

- _(à compléter : statuts, états interdits, calculs, arrondis, droits par rôle)_


## Fichiers concernés

_Fichiers existants repérés dans les PR liées ; ajouter ceux modifiés ensuite._

_54 fichiers : résumé par dossier._

- `valimbavaka-core/src/main/java/org/vvs/…` (28 fichiers)
- `valimbavaka-ui/src/app/core/auth/auth-guard.spec.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/auth/auth-guard.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/auth/auth-models.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/auth/auth-service.spec.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/auth/auth-service.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/auth/auth-store.spec.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/auth/auth-store.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/auth/auth-testing.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/auth/permission-guard.spec.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/auth/permission-guard.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/http/auth-interceptor.spec.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/http/session-interceptor.spec.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/http/session-interceptor.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/layout/home-guard.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/layout/main-layout/…` (4 fichiers)
- `valimbavaka-ui/src/app/core/layout/menu.spec.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/core/layout/menu.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/auth/auth.routes.ts/…` (1 fichiers)
- `valimbavaka-ui/src/app/features/auth/pages/…` (5 fichiers)

## Dépendances

**Ce ticket s'appuie sur** :
- [SYS-05](../SYS-05/description.md) · [guide](../SYS-05/guide.md)

**Ces tickets s'appuient sur lui** :
- [AUTH-02](../AUTH-02/description.md) · [guide](../AUTH-02/guide.md)
- [AUTH-03](../AUTH-03/description.md) · [guide](../AUTH-03/guide.md)
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md)
- [SYS-02](../SYS-02/description.md) · [guide](../SYS-02/guide.md)
- [SYS-01](../SYS-01/description.md) · [guide](../SYS-01/guide.md)
- [SYS-04](../SYS-04/description.md) · [guide](../SYS-04/guide.md)
- [SYS-11](../SYS-11/description.md) · [guide](../SYS-11/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
