# Guide d'intégration — RPT-06 · Export des rapports en PDF/XLSX

Statut : ✅ rédigé le 2026-11-12 (tests à lancer, voir « Résultat de l'exécution » du scénario) · Ticket : [#103](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/103) · [description](description.md) · [scénario de test](scenario-test.md)

## En bref

L'administrateur télécharge le rapport affiché (même période, même date, même canal, même gamme) en **xlsx** (un onglet par bloc : rapport, synthèse, évolution, gammes, classement) ou en **PDF** (titre, filtres et indicateurs sous le titre, puis un seul tableau). Les chiffres sortent du même `ReportService.getReport` que l'écran : l'export ne recalcule rien. Les deux boutons de `/reports` appellent l'API et `ExportService` confirme chaque fichier par un toast ; plus aucun élément de `/reports` n'est en démonstration (le marqueur `underConstruction` est retiré).

## Contrat exposé

### Endpoints backend

| Méthode | URL | Permission (`@HasPermission`) | Entrée | Sortie | Erreurs |
|---|---|---|---|---|---|
| GET | `/api/v1/reports/export` | `reports.read` | mêmes paramètres que le rapport : `granularity` (`DAY`, `MONTH` par défaut, `YEAR`), `date` (`yyyy-MM-dd`, aujourd'hui par défaut), `salesChannelId`, `rangeId` | 200 `.xlsx` (`Content-Disposition: attachment`) | 400 `MALFORMED_REQUEST`, 401, 403, 422 (période qui n'a pas commencé) |
| GET | `/api/v1/reports/export/pdf` | `reports.read` | idem | 200 `application/pdf` | idem |

Noms de fichier : `rapport-journalier-AAAA-MM-JJ`, `rapport-mensuel-AAAA-MM`, `rapport-annuel-AAAA` (+ `.xlsx` ou `.pdf`). Contrôleur : `ReportController.java:88` (xlsx) et `:111` (PDF).

### Services et ports Java réutilisables

- `ReportExportService` (`feature/report/service`) : `exportXlsx(granularity, date, salesChannelId, rangeId)` et `exportPdf(...)` renvoient un `ReportFile(filename, Resource)`. Implémentation : `ReportExportServiceImpl` ; elle lit le rapport par `ReportService.getReport`, retrouve les libellés du canal et de la gamme filtrés (`ReportSaleRepository.salesChannelLabel`, `productRangeLabel`) puis délègue l'écriture à `ReportFileWriter`.
- Mise en page : `ReportExportLayout` (`service/impl`) fabrique le titre, le nom de fichier, les lignes d'information, les indicateurs et les lignes du PDF (`filename` ligne 46, `info` 51, `indicators` 61, `pdfDetails` 81, `pdfRows` 96) ; `ReportFileWriter` appelle `ExcelExporter.export(out, sheet(...)×5)` (ligne 35) et `PdfExporter.export(report(...).withDetails(...))` (ligne 51).
- Colonnes : `@ExcelColumn` posé sur `ReportTrendPointRDto`, `ReportRangeRDto` et `ReportRankedArticleRDto` (les identifiants restent hors des fichiers) et sur trois DTO propres à l'export : `ReportInfoRowRDto`, `ReportIndicatorRowRDto`, `ReportPdfRowRDto`.
- Aucun événement publié : les routes n'utilisent pas `@ExcelExport` (voir Points d'attention), donc pas de `ExcelExportedEvent`.

### Frontend

- Route : `/reports` (`reports.routes.ts`), sans marqueur `underConstruction`.
- `ReportService.export(query, format)` (`features/reports/data-access/report-service.ts`) : délègue à `ExportService.download('/api/v1/reports', 'xlsx' | 'pdf', params)` avec les paramètres du rapport affiché ; émet le nom du fichier.
- `ReportsPage.exportAs('pdf' | 'xlsx')` : désactive les deux boutons pendant le téléchargement, relaie les filtres courants (`toQuery(filter())`), laisse `ExportService` afficher « Fichier … généré. » ou le message d'échec (`export.forbidden`, `export.tooLarge`, `export.failed`). La clé `reports.exported` (toast de démonstration) est supprimée en fr et mg.

### Données

- Tables et migrations Liquibase : aucune.
- Permission : `reports.read` (déjà créée, réservée à l'administrateur) commande aussi l'export. Décision prise d'après la description (« En tant qu'administrateur… ») : un export ne montre rien de plus que l'écran, une permission `reports.export` à la manière de `invoices.export` n'apporterait rien. Si le client veut séparer lecture et export, il suffira d'une migration `reports.export` et d'un `@HasPermission` différent sur les deux méthodes.
- Écriture dans le journal : aucune. SYS-01 n'a pas d'action `EXPORT` au journal (`journal_entries` n'accepte que CREATE / UPDATE / DELETE) : hors périmètre, à porter par un ticket JNL.

## Règles métier à respecter par les autres tickets

- **Mêmes chiffres que l'écran** : tout nouveau bloc du rapport s'ajoute à `ReportRDto` (donc à l'écran), puis à `ReportFileWriter.xlsx` (un onglet) et à `ReportExportLayout.pdfRows` (des lignes) ; ne jamais calculer un chiffre dans l'export.
- Seuls les champs annotés `@ExcelColumn` sortent dans l'onglet correspondant ; ne pas annoter ce qui ne doit pas sortir (un seul DTO sert au JSON et au fichier).
- Une période qui n'a pas commencé donne 422 sans fichier partiel (le rapport est lu avant d'écrire).
- Périodes : le jour, le mois ou l'année qui contient `date`, heure de Madagascar ; ventes `VALIDATED` seulement (règles de RPT-01).
- Montants en Ar au format `#,##0.00` ; parts et variations à une décimale ; une marge inconnue (`null`) est une cellule vide, jamais 0 ; une variation inconnue s'écrit `n/d` dans le PDF et reste vide dans le xlsx.

## Fichiers concernés

**Backend** (`valimbavaka-core/src/main/java/org/vvs/valimbavaka/feature/report/`)
- `controller/ReportController.java` (deux routes ajoutées)
- `service/ReportExportService.java`, `service/ReportFile.java`
- `service/impl/ReportExportServiceImpl.java`, `ReportExportLayout.java`, `ReportFileWriter.java`, `ReportLabels.java`
- `dto/ReportInfoRowRDto.java`, `ReportIndicatorRowRDto.java`, `ReportPdfRowRDto.java` (nouveaux) ; `ReportTrendPointRDto.java`, `ReportRangeRDto.java`, `ReportRankedArticleRDto.java` (colonnes annotées)
- `repository/ReportSaleRepository.java` (deux requêtes de libellé)

**Frontend** (`valimbavaka-ui/src/app/features/reports/`)
- `data-access/report-service.ts`, `pages/reports-page/reports-page.ts`, `reports-page.html`, `reports.routes.ts`
- `valimbavaka-ui/public/i18n/fr.json`, `mg.json` (clé de démonstration retirée)

**Tests**
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/feature/report/controller/ReportExportIT.java`
- `.../feature/report/service/impl/ReportExportLayoutTest.java`, `ReportExportServiceImplTest.java`
- `data-access/report-service.spec.ts`, `pages/reports-page/reports-export.spec.ts`

## Dépendances

**Ce ticket s'appuie sur** :
- [RPT-01](../RPT-01/description.md) · [guide](../RPT-01/guide.md) (à RPT-05 : `ReportService`, `ReportRDto` et ses blocs)
- [SYS-01](../SYS-01/description.md) · [guide](../SYS-01/guide.md) (`ExcelExporter`, `PdfExporter`, `ExportService`)

**Ces tickets s'appuient sur lui** :
- _(rien)_

## Comment l'utiliser depuis un autre ticket

```ts
// Écran : exporter le rapport avec les critères affichés
this.reportService.export(toQuery(this.filter()), 'pdf').subscribe({ error: () => undefined });
```

## Points d'attention

- **Pas de `@ExcelExport`** : l'annotation de SYS-01 exporte une liste tabulaire d'un seul type ; un rapport a plusieurs blocs. Les deux routes sont donc écrites à la main (`ResponseEntity<Resource>`, comme `GET /api/v1/invoices/{id}/pdf`), sans l'événement `ExcelExportedEvent` ni le plafond de 10 000 lignes (le classement est borné à 10 articles et la courbe à 31 points au plus).
- **PDF à un seul tableau** : `PdfExporter` n'écrit qu'un tableau par document. L'évolution, les gammes et le classement s'enchaînent donc dans la même table (colonnes Bloc, Libellé, Quantité, CA, Marge, Part) ; les filtres et les trois indicateurs avec leur comparaison sont en lignes d'information sous le titre. Le xlsx, lui, garde un onglet par bloc, avec des cellules numériques. Hypothèse retenue : le plus simple qui contient tous les chiffres de l'écran.
- **Courbe annuelle** : le xlsx garde la date du premier jour du mois (`01/03/2026`), le PDF écrit `03/2026`.
- **Polices du PDF** : Helvetica, Latin-1 : le français passe, pas les caractères hors Latin-1 (voir SYS-01).
- **Journal** : aucun enregistrement d'export (voir « Données »).
- **Hors ligne** : l'export est une lecture ; hors ligne, le téléchargement échoue avec le toast `export.failed`, sans file d'attente.
- Le test `ReportExportIT` crée une gamme et la désactive après chaque test ; il ne suppose rien sur le contenu de la base.
