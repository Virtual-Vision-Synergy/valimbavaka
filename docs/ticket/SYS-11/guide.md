# Guide d'intégration — SYS-11 · Session expirée avec des saisies en attente

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ⚠ pré-rempli depuis le code au 2026-10-23 — à vérifier et compléter · Ticket : [#137](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/137) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

_(2 à 3 phrases : ce que la fonctionnalité fait et ce qu'elle offre aux autres.)_

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée (`XxxWDto`) | Sortie (`XxxRDto`) | Erreurs |
|---|---|---|---|---|---|
| _(à compléter)_ | | | | | |

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

**Backend**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/idempotency/IdempotencyFilter.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/security/idempotency/IdempotencyStore.java`

**Frontend**
- `valimbavaka-ui/src/app/core/http/session-interceptor.ts`
- `valimbavaka-ui/src/app/core/offline/connectivity-service.ts`
- `valimbavaka-ui/src/app/core/offline/outbox-models.ts`
- `valimbavaka-ui/src/app/core/offline/outbox-service.ts`
- `valimbavaka-ui/src/app/core/offline/outbox-storage.ts`
- `valimbavaka-ui/src/app/core/offline/sync-status.css`
- `valimbavaka-ui/src/app/core/offline/sync-status.html`
- `valimbavaka-ui/src/app/core/offline/sync-status.ts`
- `valimbavaka-ui/src/app/features/sync/pages/sync-page/sync-page.css`
- `valimbavaka-ui/src/app/features/sync/pages/sync-page/sync-page.html`
- `valimbavaka-ui/src/app/features/sync/pages/sync-page/sync-page.ts`
- `valimbavaka-ui/src/app/features/sync/sync.routes.ts`

**Tests**
- `valimbavaka-ui/src/app/core/offline/connectivity-service.spec.ts`
- `valimbavaka-ui/src/app/core/offline/outbox-service.spec.ts`
- `valimbavaka-ui/src/app/core/offline/sync-status.a11y.spec.ts`
- `valimbavaka-ui/src/app/core/offline/sync-status.spec.ts`
- `valimbavaka-ui/src/app/features/sync/pages/sync-page/sync-page.spec.ts`

**Autres**
- `docs/adr/0001-synchronisation-hors-ligne.md`

## Dépendances

**Ce ticket s'appuie sur** :
- [SYS-02](../SYS-02/description.md) · [guide](../SYS-02/guide.md)
- [AUTH-01](../AUTH-01/description.md) · [guide](../AUTH-01/guide.md)

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
