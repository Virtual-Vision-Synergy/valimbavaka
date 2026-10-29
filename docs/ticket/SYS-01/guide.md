# Guide d'intégration — SYS-01 · Export PDF/Excel généralisé

Statut : ✅ écrit le 2026-10-29 d'après le code de `shared/excel` — **tests non exécutés** (voir « Résultat de l'exécution » du scénario) · Ticket : [#105](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/105) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le module `shared/excel` sait transformer n'importe quelle liste en fichier **xlsx** (streaming, mémoire constante). Une feature n'écrit pas de contrôleur d'export : elle annote la méthode GET de sa liste avec `@ExcelExport` et les champs de son `XxxRDto` avec `@ExcelColumn`, et la route jumelle `<chemin>/export` apparaît avec les mêmes paramètres et la même permission. **Seul le XLSX existe** : aucun export PDF n'est livré (voir « Points d'attention »).

## Contrat exposé

### Endpoints backend

Il n'y a pas d'endpoint propre à SYS-01 : chaque route annotée `@ExcelExport` en crée un (`ExportAwareHandlerMapping.registerHandlerMethod`).

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `<chemin de la liste>/export` | celle de la méthode annotée (`@HasPermission` de la route d'origine) | mêmes paramètres que la liste (`PageQuery`, filtres) | 200 fichier `.xlsx` (`Content-Disposition: attachment`) | 401, 403, 422 (`size` > `maxRows`, ou plus de `maxRows` lignes) |

La route jumelle ne produit que `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` : le client doit l'appeler avec `Accept` xlsx ou sans préférence JSON. La route d'origine reste en JSON, paginée à 20 (max 100), même si le client demande du xlsx.

### Services et ports Java réutilisables

- `ExcelExporter` (`shared/excel/ExcelExporter.java:193`) : `export(Class<T>, String sheetName, Stream<T>, OutputStream)` pour un onglet, `export(OutputStream, Sheet<?>...)` (ligne 198) pour un classeur à plusieurs onglets, `ExcelExporter.sheet(type, nom, flux)` pour construire un onglet. Les flux ne sont pas fermés par l'exporteur.
- `@ExcelColumn` (`annotation/ExcelColumn.java`) sur un champ ou un composant de `record` : `header` (obligatoire), `order`, `expression` (SpEL en lecture seule, ex. `"role?.name"`), `format` Excel, `width`. Seuls les champs annotés sont exportés, ceux des classes parentes en premier.
- `@ExcelExport` (`annotation/ExcelExport.java`) sur une méthode GET : `target` (libellé repris dans l'événement), `filename`, `sheet` (31 caractères max). Par défaut, nom du fichier et de l'onglet = nom simple du DTO.
- Types de retour acceptés par `ExcelHttpMessageConverter.toRows` (`web/ExcelHttpMessageConverter.java:97`) : `Stream<Dto>` (écrit sans être collecté, puis fermé), `Page`, `PageResponse`, `Collection`, ou un objet seul.
- Cellules (`ExcelCellWriter`) : nombres, booléens, dates et instants (fuseau `vvs.excel.time-zone`, `Indian/Antananarivo` par défaut), énumérations par leur nom, le reste en texte.
- Événement `ExcelExportedEvent(target, filename, elementType)` publié après chaque export d'une méthode annotée : à écouter pour tracer l'export. **Aucune feature ne l'écoute encore.**

### Frontend

Aucun composant ni service partagé. Le client télécharge la route jumelle avec `HttpClient` en `responseType: 'blob'`, en reprenant les paramètres de la liste affichée ; le nom du fichier vient de l'en-tête `Content-Disposition`.

### Données

- Aucune table, aucune migration Liquibase.
- Journal : rien d'écrit par le module. L'export n'est tracé que si une feature écoute `ExcelExportedEvent`.
- Réglages `vvs.excel.*` (`config/ExcelProperties.java:71`) : `max-rows` (10 000, max 1 048 575), `column.max-width` (255), `column.padding` (2), `format.date` (`dd/mm/yyyy`), `format.datetime` (`dd/mm/yyyy hh:mm`), `time-zone`.

## Règles métier à respecter par les autres tickets

- **Mêmes droits que la liste** : la route jumelle réutilise la méthode Java, donc `@HasPermission`, filtres et validation. Une donnée sensible (ex. coût d'achat) ne doit pas porter `@ExcelColumn` si le rôle qui lit la liste ne doit pas la voir dans un fichier.
- **Seules les colonnes annotées sortent** : un champ sans `@ExcelColumn` n'est jamais exporté, même présent dans le JSON.
- **Taille de page** : sur la route d'export, `size` vaut `max-rows` par défaut et au maximum (`ExportAwareHandlerMapping.handleMatch`, ligne 194). Au-delà, 422 `VALIDATION_FAILED` ; plus de `max-rows` lignes dans un `Stream`, 422 (`ExcelRowLimitExceededException`) sans fichier partiel.
- **Pagination** : l'export d'une page ne contient que cette page (`page` base 1) ; pour tout exporter, laisser `page` par défaut.
- **Un nom de propriété inconnu dans `expression` fait échouer l'export** au lieu d'écrire des cellules vides ; un `null` dans le chemin donne une cellule vide.
- Une seule `WebMvcRegistrations` peut fournir le `RequestMappingHandlerMapping` : toute autre personnalisation du mapping se fait dans `ExcelExportWebConfig`.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/`)
- `ExcelExporter.java`, `ExcelSheetWriter.java`, `ExcelCellWriter.java`, `ExcelColumnDefinition.java`
- `annotation/ExcelColumn.java`, `annotation/ExcelExport.java`
- `config/ExcelExportWebConfig.java`, `config/ExcelProperties.java`
- `web/ExcelHttpMessageConverter.java`, `web/ExportAwareHandlerMapping.java`, `web/DeferredOutputStream.java`
- `event/ExcelExportedEvent.java`, `exception/ExcelRowLimitExceededException.java`

**Tests** (`valimbavaka-core/src/test/java/org/vvs/valimbavaka/shared/excel/`)
- `ExcelExporterTest.java`, `ExcelExportIT.java`

## Dépendances

**Ce ticket s'appuie sur** :
- [AUTH-01](../AUTH-01/description.md) · [guide](../AUTH-01/guide.md) (permissions `@HasPermission`)

**Ces tickets s'appuient sur lui** :
- [ART-08](../ART-08/description.md) · [guide](../ART-08/guide.md)
- [FAC-03](../FAC-03/description.md) · [guide](../FAC-03/guide.md)
- [FAC-06](../FAC-06/description.md) · [guide](../FAC-06/guide.md)
- [COM-05](../COM-05/description.md) · [guide](../COM-05/guide.md)
- [RPT-06](../RPT-06/description.md) · [guide](../RPT-06/guide.md)

## Comment l'utiliser depuis un autre ticket

```java
// DTO de sortie : on annote seulement ce qui doit sortir dans le fichier
public record ArticleRDto(
        @ExcelColumn(header = "Code", order = 1) String code,
        @ExcelColumn(header = "Prix", order = 2, format = "#,##0.00") BigDecimal price,
        UUID id) {}

// Contrôleur : une annotation suffit, la route GET /api/v1/articles/export existe ensuite
@GetMapping
@HasPermission("articles.read")
@ExcelExport(target = "articles", filename = "articles", sheet = "Articles")
PageResponse<ArticleRDto> list(@Valid PageQuery query) { ... }
```

Pour plusieurs onglets ou un fichier hors liste, injecter `ExcelExporter` et appeler `export(out, sheet(...), sheet(...))`.

## Points d'attention

- **Pas de PDF.** Le critère parle de « PDF et/ou XLSX » : seul XLSX est livré. FAC-03 (impression de facture) et RPT-06 (rapports en PDF) auront besoin du PDF ; décision à prendre avec ManaJudy et le client avant ces tickets.
- **Journal non branché** : aucune feature n'écoute `ExcelExportedEvent`. Tracer les exports est à la charge de la feature appelante (ou d'un ticket journal).
- **Aucune route réelle n'est annotée** : seul le contrôleur de démonstration de `ExcelExportIT` l'utilise. ART-08 sera le premier usage en production.
- **Flux JPA** : si une feature renvoie un `Stream` issu d'un repository, la transaction doit rester ouverte pendant l'écriture ; à vérifier au premier usage, aucun test ne le couvre.
- **`@ExcelColumn` sur un DTO partagé** : le même `XxxRDto` sert au JSON et à l'export ; ne pas annoter ce qui ne doit pas sortir en fichier.
- Les tests n'ont pas été exécutés : ce guide décrit le code lu, pas un résultat de CI.
