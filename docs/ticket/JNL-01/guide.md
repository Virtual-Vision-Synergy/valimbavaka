# Guide d'intégration — JNL-01 · Journal append-only des opérations

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ⚠ pré-rempli depuis le code au 2026-10-23 — à vérifier et compléter · Ticket : [#14](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/14) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

_(2 à 3 phrases : ce que la fonctionnalité fait et ce qu'elle offre aux autres.)_

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission | Entrée | Sortie |
|---|---|---|---|---|
| GET | `/api/v1/journal` | journal.read | `PageQuery + filtres` | `PageResponse<JournalEntryRDto>` |

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
- [AUTH-01](../AUTH-01/description.md) · [guide](../AUTH-01/guide.md)
- [SYS-06](../SYS-06/description.md) · [guide](../SYS-06/guide.md)

**Ces tickets s'appuient sur lui** :
- [AUTH-03](../AUTH-03/description.md) · [guide](../AUTH-03/guide.md)
- [AUTH-04](../AUTH-04/description.md) · [guide](../AUTH-04/guide.md)
- [JNL-02](../JNL-02/description.md) · [guide](../JNL-02/guide.md)
- [FRS-01](../FRS-01/description.md) · [guide](../FRS-01/guide.md)
- [FRS-02](../FRS-02/description.md) · [guide](../FRS-02/guide.md)
- [ART-01](../ART-01/description.md) · [guide](../ART-01/guide.md)
- [ART-02](../ART-02/description.md) · [guide](../ART-02/guide.md)
- [ART-06](../ART-06/description.md) · [guide](../ART-06/guide.md)
- [ART-07](../ART-07/description.md) · [guide](../ART-07/guide.md)
- [STK-01](../STK-01/description.md) · [guide](../STK-01/guide.md)
- [STK-05](../STK-05/description.md) · [guide](../STK-05/guide.md)
- [TRF-03](../TRF-03/description.md) · [guide](../TRF-03/guide.md)
- [PRD-01](../PRD-01/description.md) · [guide](../PRD-01/guide.md)
- [PRD-02](../PRD-02/description.md) · [guide](../PRD-02/guide.md)
- [SHO-01](../SHO-01/description.md) · [guide](../SHO-01/guide.md)
- [SHO-02](../SHO-02/description.md) · [guide](../SHO-02/guide.md)
- [LIV-01](../LIV-01/description.md) · [guide](../LIV-01/guide.md)
- [LIV-03](../LIV-03/description.md) · [guide](../LIV-03/guide.md)
- [LIV-04](../LIV-04/description.md) · [guide](../LIV-04/guide.md)
- [LIV-05](../LIV-05/description.md) · [guide](../LIV-05/guide.md)
- [JR-01](../JR-01/description.md) · [guide](../JR-01/guide.md)
- [TRF-02](../TRF-02/description.md) · [guide](../TRF-02/guide.md)
- [PRO-01](../PRO-01/description.md) · [guide](../PRO-01/guide.md)
- [VTE-01](../VTE-01/description.md) · [guide](../VTE-01/guide.md)
- [FAC-01](../FAC-01/description.md) · [guide](../FAC-01/guide.md)
- [FAC-02](../FAC-02/description.md) · [guide](../FAC-02/guide.md)
- [FAC-04](../FAC-04/description.md) · [guide](../FAC-04/guide.md)
- [INV-01](../INV-01/description.md) · [guide](../INV-01/guide.md)
- [INV-02](../INV-02/description.md) · [guide](../INV-02/guide.md)
- [INV-04](../INV-04/description.md) · [guide](../INV-04/guide.md)
- [SRV-02](../SRV-02/description.md) · [guide](../SRV-02/guide.md)
- [COM-01](../COM-01/description.md) · [guide](../COM-01/guide.md)
- [COM-03](../COM-03/description.md) · [guide](../COM-03/guide.md)
- [COM-05](../COM-05/description.md) · [guide](../COM-05/guide.md)
- [COM-06](../COM-06/description.md) · [guide](../COM-06/guide.md)
- [JR-04](../JR-04/description.md) · [guide](../JR-04/guide.md)
- [JR-06](../JR-06/description.md) · [guide](../JR-06/guide.md)
- [JNL-03](../JNL-03/description.md) · [guide](../JNL-03/guide.md)
- [STK-06](../STK-06/description.md) · [guide](../STK-06/guide.md)
- [SYS-12](../SYS-12/description.md) · [guide](../SYS-12/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
