# Guide d'intégration — SYS-01 · Export PDF/Excel généralisé

Statut : ✅ mis à jour le 2026-11-08 (ajout du PDF et du service d'export front) — tests verts en CI (voir « Résultat de l'exécution » du scénario) · Ticket : [#105](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/105) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

Le module `shared/excel` transforme n'importe quelle liste en fichier **xlsx** (streaming) ou en **PDF tabulaire** (A4 paysage). Une feature n'écrit pas de contrôleur d'export : elle annote la méthode GET de sa liste avec `@ExcelExport` et les champs de son `XxxRDto` avec `@ExcelColumn` ; les routes `<chemin>/export` (xlsx) et `<chemin>/export/pdf` apparaissent avec les mêmes paramètres et la même permission. Côté écran, `ExportService.download(url, 'xlsx' | 'pdf', params)` télécharge le fichier et confirme sa génération par un toast.

## Contrat exposé

### Endpoints backend

Il n'y a pas d'endpoint propre à SYS-01 : chaque route annotée `@ExcelExport` en crée deux (`ExportAwareHandlerMapping.registerHandlerMethod`, `web/ExportAwareHandlerMapping.java:39`).

| Méthode | URL | Permission | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `<chemin de la liste>/export` | celle de la méthode annotée (`@HasPermission` de la route d'origine) | mêmes paramètres que la liste (`PageQuery`, filtres) | 200 `.xlsx` (`Content-Disposition: attachment`) | 401, 403, 422 (`size` > `maxRows`, ou plus de `maxRows` lignes) |
| GET | `<chemin de la liste>/export/pdf` (sauf `@ExcelExport(pdf = false)`) | idem | idem | 200 `application/pdf`, `<filename>.pdf` | 401, 403, 422 |

Chaque route jumelle ne produit que son type : le client l'appelle avec `Accept` correspondant ou sans préférence. La route d'origine reste en JSON, paginée à 20 (max 100) ; lui demander du xlsx ou du PDF donne 406.

### Services et ports Java réutilisables

- `@ExcelExport` (`annotation/ExcelExport.java`) sur une méthode GET : `target` (repris dans l'événement), `filename` (sans extension ; `.xlsx` / `.pdf` ajouté, une extension `.xlsx` ou `.pdf` saisie est remplacée), `sheet` (onglet xlsx, 31 caractères), `title` (titre du PDF ; à défaut `sheet`, puis le nom du DTO), `pdf` (`true` par défaut).
- `@ExcelColumn` (`annotation/ExcelColumn.java`) : `header`, `order`, `expression` (SpEL lecture seule, ex. `"role?.name"`), `format`, `width`. **Les mêmes colonnes servent au xlsx et au PDF** ; seuls les champs annotés sortent.
- `ExcelExporter` (`ExcelExporter.java`) : `export(Class<T>, String sheetName, Stream<T>, OutputStream)`, ou `export(OutputStream, Sheet<?>...)` pour un classeur à plusieurs onglets.
- `PdfExporter` (`PdfExporter.java`) : `export(Class<T>, String title, Stream<T>, OutputStream)` (ligne 61) pour un tableau seul ; `export(Report<?>, OutputStream)` (ligne 65) pour un document complet. `PdfExporter.report(title, type, data)` (ligne 57) puis `withDetails(List<String>)` (lignes d'information sous le titre : client, période, filtres) et `withSummary(List<String>)` (lignes en gras après le tableau : totaux). Document construit en mémoire puis écrit d'un bloc : en cas d'erreur, aucun octet n'est envoyé.
- Mise en page PDF (`PdfTableWriter`, `PdfText`, `PdfCellFormatter`, package-privés) : Helvetica 8 pt, A4 paysage, en-tête de tableau gris répété sur chaque page, colonnes numériques alignées à droite, colonne plafonnée à 220 pt puis réduite à la largeur de la page, texte trop long tronqué par `…`, pied « Généré le jj/mm/aaaa hh:mm · Page x / n » (horloge `Clock`, fuseau `vvs.excel.time-zone`).
- Cellules PDF (`PdfCellFormatter.format`, ligne 43) : nombres à la française (`2 500,00`, espace des milliers, virgule décimale, arrondi au demi supérieur) avec le `format` de l'annotation s'il est numérique, sinon `#,##0` (entiers) ou `#,##0.##` ; dates `dd/MM/yyyy`, date-heure `dd/MM/yyyy HH:mm` (le `format` de date Excel est ignoré en PDF) ; booléens `Oui` / `Non` ; énumérations par leur nom.
- Types de retour acceptés par les deux routes (`ExportHttpMessageConverter.toRows`) : `Stream<Dto>` (écrit sans être collecté côté xlsx, puis fermé), `Page`, `PageResponse`, `Collection`, ou un objet seul.
- Événement `ExcelExportedEvent(target, filename, elementType)` publié après chaque export réussi (xlsx ou PDF, le format se lit dans l'extension de `filename`). **Aucune feature ne l'écoute encore.**

### Frontend

- `ExportService.download(listUrl, format, params?)` (`shared/data-access/export-service.ts:27`) : `GET <listUrl>/export` ou `<listUrl>/export/pdf` en `blob`, téléchargement (`downloadFile`), puis toast `export.done` « Fichier {{filename}} généré. ». Échec : toast danger `export.tooLarge` (422), `export.forbidden` (403) ou `export.failed`, et l'erreur est relayée à l'appelant. Émet le nom du fichier.
- `downloadFile` / `filenameOf` (`shared/data-access/download-file.ts`, ART-08) restent utilisables seuls.
- Clés de traduction `export.done`, `export.failed`, `export.tooLarge`, `export.forbidden` (fr, mg).

### Données

- Aucune table, aucune migration Liquibase, aucune permission nouvelle : les routes jumelles reprennent celle de la route d'origine.
- Dépendance Maven ajoutée : `org.apache.pdfbox:pdfbox` 3.0.6 (`pom.xml`).
- Réglages `vvs.excel.*` (`config/ExcelProperties.java`) : `max-rows` (10 000) vaut pour les deux formats ; `time-zone` sert aux dates du PDF ; `column.*` et `format.*` ne concernent que le xlsx.

## Règles métier à respecter par les autres tickets

- **Mêmes droits que la liste** : les routes jumelles réutilisent la méthode Java, donc `@HasPermission`, filtres et validation. Si l'export doit être plus restreint que la lecture, créer une méthode GET dédiée avec sa permission (modèle : `ArticleController.getExportableArticles`, route `/exportable`, ART-08).
- **Seules les colonnes annotées sortent**, dans les deux formats : ne jamais annoter une donnée sensible (coût d'achat…) qu'un rôle qui lit la liste ne doit pas voir dans un fichier.
- **Taille de page** : sur les routes d'export, `size` vaut `max-rows` par défaut et au maximum. Pour tout exporter, ne pas envoyer `page` ni `size` ; au-delà de 10 000 lignes, 422 sans fichier partiel.
- **Le convertisseur d'export n'agit que pour une méthode `@ExcelExport`** (`ExportHttpMessageConverter.canWrite`, ligne 74) : une route qui renvoie déjà un PDF (`byte[]`, `Resource`, `produces = application/pdf`) n'est pas touchée.
- **Un nom de propriété inconnu dans `expression` fait échouer l'export** ; un `null` dans le chemin donne une cellule vide.
- Une seule `WebMvcRegistrations` peut fournir le `RequestMappingHandlerMapping` : toute autre personnalisation du mapping se fait dans `ExcelExportWebConfig`.

### Mode d'emploi par ticket

| Ticket | Besoin | Comment exporter |
|---|---|---|
| **FAC-03** (imprimer / exporter / envoyer une facture) | un PDF par facture | Service de la feature : `pdfExporter.export(PdfExporter.report("Facture " + numero, InvoiceLineRDto.class, lignes).withDetails(List.of("Client : …", "Date : …", "Vendeuse : …")).withSummary(List.of("Total TTC : … Ar")), out)` ; contrôleur `GET /api/v1/invoices/{id}/pdf` avec `produces = application/pdf` renvoyant `byte[]` (ou `ResponseEntity<byte[]>` avec `Content-Disposition`). Écran : `HttpClient` en `blob` + `downloadFile`, puis toast de confirmation ; « imprimer » = ouvrir le même PDF. L'envoi (e-mail) n'est pas couvert par SYS-01. |
| **FAC-06** (livre des ventes en XLSX) | xlsx de la liste des ventes filtrée | `@ExcelExport(target = "sales-book", filename = "livre-des-ventes", sheet = "Ventes")` sur le GET de la liste, `@ExcelColumn` sur `SaleRDto` (ou un `SaleBookRDto` dédié). Écran : `exportService.download('/api/v1/sales', 'xlsx', params)` dans `exportBook()` de `sales-page` ; le toast confirme la génération. Mettre `pdf = false` si le PDF n'est pas voulu. |
| **COM-05** (exporter les commissions du mois) | xlsx (et PDF si utile) du relevé | `@ExcelExport(target = "commissions", filename = "commissions-2026-11", title = "Commissions de novembre 2026")` sur le GET du relevé, filtré par mois ; écran : `exportStatement()` de `commissions-page` appelle `exportService.download(url, 'xlsx', params)`. Le journal de l'export demande un écouteur de `ExcelExportedEvent` (voir Points d'attention). |
| **RPT-06** (chaque rapport en PDF/XLSX) | deux boutons par rapport | `@ExcelExport` sur chaque GET de rapport (DTO de lignes annoté) : les deux routes existent. Écran : `exportAs('PDF')` / `exportAs('XLSX')` de `reports-page` → `exportService.download(url, 'pdf' \| 'xlsx', params)` avec les filtres affichés. Pour un rapport à plusieurs blocs, une méthode de service peut appeler `ExcelExporter.export(out, sheet(...), sheet(...))` ou `PdfExporter.export(report(...).withDetails(filtres), out)`. |

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/shared/excel/`)
- `ExcelExporter.java`, `ExcelSheetWriter.java`, `ExcelCellWriter.java`, `ExcelColumnDefinition.java`
- `PdfExporter.java`, `PdfTableWriter.java`, `PdfText.java`, `PdfRows.java`, `PdfCellFormatter.java`
- `annotation/ExcelColumn.java`, `annotation/ExcelExport.java`
- `config/ExcelExportWebConfig.java`, `config/ExcelProperties.java`
- `web/ExportHttpMessageConverter.java`, `web/ExcelHttpMessageConverter.java`, `web/PdfHttpMessageConverter.java`, `web/ExportAwareHandlerMapping.java`, `web/DeferredOutputStream.java`
- `event/ExcelExportedEvent.java`, `exception/ExcelRowLimitExceededException.java`
- `valimbavaka-core/pom.xml` (PDFBox)

**Frontend**
- `valimbavaka-ui/src/app/shared/data-access/export-service.ts`, `download-file.ts`
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json` (bloc `export`)

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/shared/excel/ExcelExporterTest.java`, `ExcelExportIT.java`, `PdfExporterTest.java`, `PdfExportIT.java`
- `valimbavaka-ui/src/app/shared/data-access/export-service.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [AUTH-01](../AUTH-01/description.md) · [guide](../AUTH-01/guide.md) (permissions `@HasPermission`)

**Ces tickets s'appuient sur lui** :
- [ART-08](../ART-08/description.md) · [guide](../ART-08/guide.md) (livré en xlsx ; la route `/api/v1/articles/exportable/export/pdf` existe désormais aussi)
- [FAC-03](../FAC-03/description.md) · [guide](../FAC-03/guide.md)
- [FAC-06](../FAC-06/description.md) · [guide](../FAC-06/guide.md)
- [COM-05](../COM-05/description.md) · [guide](../COM-05/guide.md)
- [RPT-06](../RPT-06/description.md) · [guide](../RPT-06/guide.md)

## Comment l'utiliser depuis un autre ticket

```java
public record SaleBookRDto(
        @ExcelColumn(header = "N° facture", order = 1) String invoiceNumber,
        @ExcelColumn(header = "Montant TTC", order = 2, format = "#,##0.00") BigDecimal total,
        UUID id) {}

// GET /api/v1/sales (JSON), /api/v1/sales/export (xlsx) et /api/v1/sales/export/pdf (PDF)
@GetMapping
@HasPermission("sales.read")
@ExcelExport(target = "sales-book", filename = "livre-des-ventes", sheet = "Ventes", title = "Livre des ventes")
PageResponse<SaleBookRDto> list(@Valid PageQuery query) { ... }
```

```ts
protected exportBook(format: ExportFormat): void {
  this.exporting.set(true);
  this.exportService
    .download('/api/v1/sales', format, this.criteriaParams())
    .pipe(finalize(() => this.exporting.set(false)))
    .subscribe({ error: () => undefined }); // toasts déjà affichés par le service
}
```

## Points d'attention

- **PDF tabulaire, pas de maquette libre** : le PDF reprend les colonnes `@ExcelColumn`, avec titre, lignes d'information et résumé. Une facture au graphisme imposé (logo, cadre) dépasse SYS-01 : FAC-03 peut partir de `PdfExporter.report(...)` et, si le client exige une mise en page propre, l'écrire avec PDFBox dans sa feature.
- **Polices standard** (Helvetica, WinAnsi) : aucun fichier de police embarqué ; les caractères hors Latin-1 étendu (flèches, émojis, alphabets non latins) s'impriment `?`. Le français et le malgache passent.
- **Mémoire** : le PDF est construit en mémoire (jusqu'à 10 000 lignes, environ 290 pages) ; le xlsx reste en streaming.
- **Journal non branché** : `journal_entries` n'accepte que CREATE / UPDATE / DELETE ; tracer un export demande une action `EXPORT` (migration + `JournalAction`) et un écouteur de `ExcelExportedEvent`. Aucun ticket ne le porte : à créer côté JNL (COM-05 en dépend via JNL-01).
- **Pas de permission propre aux routes jumelles** : elles héritent de la route d'origine. Hypothèse retenue pour garder un seul mécanisme ; ART-08 contourne par une route `/exportable` dédiée, à reproduire si besoin.
- **Écran Articles** : la route PDF existe mais aucun bouton PDF n'a été ajouté à la page (choix de ne pas modifier ART-08 dans ce ticket).
- **Flux JPA** : une feature qui renvoie un `Stream` issu d'un repository doit garder la transaction ouverte pendant l'écriture ; aucun test ne le couvre encore.
- `@ExcelColumn` sur un DTO partagé : le même `XxxRDto` sert au JSON et aux fichiers ; ne pas annoter ce qui ne doit pas sortir.
