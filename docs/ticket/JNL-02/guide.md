# Guide d'intégration — JNL-02 · Interdiction de suppression des opérations validées

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ⚠ pré-rempli depuis le code au 2026-10-23 — à vérifier et compléter · Ticket : [#15](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/15) · [description](description.md) · [scénario de test](scenario-test.md)

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

- ⚠ Règle pour les tickets d'opérations à venir (voir commentaire du ticket) : leurs repositories étendent `OperationRepository` (pas de `delete*`, pas de `JpaRepository`) et leurs services passent par `ReversibleOperationService` (`cancel` / `reverse`).

## Fichiers concernés

_Fichiers existants repérés dans les PR liées ; ajouter ceux modifiés ensuite._

**Backend**
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/journal/controller/JournalController.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/journal/domain/JournalAction.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/journal/domain/JournalEntry.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/journal/dto/JournalEntryRDto.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/journal/mapper/JournalMapper.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/journal/repository/JournalEntryRepository.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/journal/service/JournalService.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/journal/service/impl/JournalServiceImpl.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/operation/OperationRepository.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/operation/OperationStatus.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/operation/ReversibleOperationService.java`

**Frontend**
- `valimbavaka-ui/src/app/features/journal/data-access/journal-models.ts`
- `valimbavaka-ui/src/app/features/journal/data-access/journal-service.ts`
- `valimbavaka-ui/src/app/features/journal/journal.routes.ts`
- `valimbavaka-ui/src/app/features/journal/pages/journal-list-page/journal-list-page.css`
- `valimbavaka-ui/src/app/features/journal/pages/journal-list-page/journal-list-page.html`
- `valimbavaka-ui/src/app/features/journal/pages/journal-list-page/journal-list-page.ts`

**Tests**
- `valimbavaka-ui/src/app/features/journal/data-access/journal-service.spec.ts`
- `valimbavaka-ui/src/app/features/journal/pages/journal-list-page/journal-list-page.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [JNL-01](../JNL-01/description.md) · [guide](../JNL-01/guide.md)

**Ces tickets s'appuient sur lui** :
- [AUTH-03](../AUTH-03/description.md) · [guide](../AUTH-03/guide.md)
- [TRF-03](../TRF-03/description.md) · [guide](../TRF-03/guide.md)
- [PRD-01](../PRD-01/description.md) · [guide](../PRD-01/guide.md)
- [SHO-01](../SHO-01/description.md) · [guide](../SHO-01/guide.md)
- [LIV-01](../LIV-01/description.md) · [guide](../LIV-01/guide.md)
- [JR-01](../JR-01/description.md) · [guide](../JR-01/guide.md)
- [VTE-01](../VTE-01/description.md) · [guide](../VTE-01/guide.md)
- [FAC-04](../FAC-04/description.md) · [guide](../FAC-04/guide.md)
- [INV-04](../INV-04/description.md) · [guide](../INV-04/guide.md)
- [SRV-02](../SRV-02/description.md) · [guide](../SRV-02/guide.md)
- [STK-06](../STK-06/description.md) · [guide](../STK-06/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
