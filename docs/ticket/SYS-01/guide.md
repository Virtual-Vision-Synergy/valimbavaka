# Guide d'intégration — SYS-01 · Export PDF/Excel généralisé

> **À compléter par le traiteur du ticket une fois le scénario de test rédigé et l'implémentation faite.** Ce guide est lu par les autres tickets pour utiliser cette fonctionnalité sans relire le code : quoi appeler, quels fichiers sont exposés, quel est le contrat.

Statut : ⚠ pré-rempli depuis le code au 2026-10-23 — à vérifier et compléter · Ticket : [#105](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/105) · [description](description.md) · [scénario de test](scenario-test.md)

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
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/ExcelCellWriter.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/ExcelColumnDefinition.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/ExcelExporter.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/ExcelSheetWriter.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/annotation/ExcelColumn.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/annotation/ExcelExport.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/config/ExcelExportWebConfig.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/config/ExcelProperties.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/event/ExcelExportedEvent.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/exception/ExcelRowLimitExceededException.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/web/DeferredOutputStream.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/web/ExcelHttpMessageConverter.java`
- `valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/web/ExportAwareHandlerMapping.java`

## Dépendances

**Ce ticket s'appuie sur** :
- [AUTH-01](../AUTH-01/description.md) · [guide](../AUTH-01/guide.md)

**Ces tickets s'appuient sur lui** :
- [ART-08](../ART-08/description.md) · [guide](../ART-08/guide.md)
- [FAC-03](../FAC-03/description.md) · [guide](../FAC-03/guide.md)
- [FAC-06](../FAC-06/description.md) · [guide](../FAC-06/guide.md)
- [COM-05](../COM-05/description.md) · [guide](../COM-05/guide.md)
- [RPT-06](../RPT-06/description.md) · [guide](../RPT-06/guide.md)

## Comment l'utiliser depuis un autre ticket

```text
(exemple d'appel ou d'injection à compléter)
```

## Points d'attention

- _(à compléter : pièges, limites connues, dette technique, décisions en attente du client)_
