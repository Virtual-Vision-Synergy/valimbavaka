# Scénarios de test — SYS-01 · Export PDF/Excel généralisé

Statut : ✅ tests exécutés en CI le 2026-11-08 (ajout du PDF et du service d'export front) · Ticket : [#105](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/105) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL. Frontend : Vitest.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | administrateur `admin.test@valimbavaka.mg` ; route de démonstration protégée par `excel-demo.unknown` pour le 403 |
| Données de départ | contrôleur de démonstration de `ExcelExportIT` : 150 produits (`Riz`, gamme `ÉPICERIE`, prix `2500`, mis en vente le 19/10/2026 ; `Savon` en ligne 2) ; contrôleur `PdfExportIT.XlsxOnlyController` (`/api/v1/pdf-demo`) |
| Horloge (tests unitaires PDF) | `Clock.fixed(2026-11-08T07:00:00Z)`, fuseau `Indian/Antananarivo` (10:00 locale) |
| Tickets à avoir déployés avant | AUTH-01 (fermé) |

## Scénarios issus des critères

Critère : « Chaque écran concerné propose un export PDF et/ou XLSX fonctionnel. » SYS-01 livre la brique (routes jumelles xlsx **et** PDF, `ExportService` front avec confirmation) ; chaque écran l'adopte dans son ticket (ART-08 livré en xlsx, FAC-03, FAC-06, COM-05, RPT-06).

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Route jumelle en xlsx | `GET /api/v1/excel-demo/products/export`, jeton admin | 200, `produits.xlsx`, onglet `Produits`, en-têtes `Libellé / Gamme / Prix / Mis en vente`, ligne 1 `Riz / ÉPICERIE / 2,500.00 / 19/10/2026`, 150 lignes | `ExcelExportIT.export_pagedRoute_returnsXlsxWithOrderedHeadersAndFormattedValues` | ✅ auto |
| 1b | Mêmes paramètres que la liste (xlsx) | `page=2&size=1` | une seule ligne, `Savon` | `ExcelExportIT.export_pagedRoute_appliesTheSameQueryParameters` | ✅ auto |
| 1c | Route en `Stream` | méthode renvoyant `Stream<Dto>` | chaque élément exporté, fichier `DemoProductRDto.xlsx` | `ExcelExportIT.export_streamRoute_exportsEveryElement` | ✅ auto |
| 1d | Colonnes ordonnées, parents d'abord | classe avec parent, `order` mélangés | ordre = `order`, colonnes du parent en tête à égalité | `ExcelExporterTest.export_classWithParent_sortsByOrderThenParentColumnsFirst` | ✅ auto |
| 1e | Record et expressions SpEL | composants de record, `category?.name` | valeurs lues correctement | `ExcelExporterTest.export_record_readsComponentsAndExpressions` | ✅ auto |
| 1f | Plusieurs onglets | deux `Sheet` | un onglet par `Sheet`, dans l'ordre | `ExcelExporterTest.export_severalSheets_writesOneTabPerSheetInOrder` | ✅ auto |
| 1g | Événement publié (xlsx) | export d'une route annotée | un seul `ExcelExportedEvent("PRODUCT", "produits.xlsx", …)` | `ExcelExportIT.export_annotatedRoute_publishesOneEvent` | ✅ auto |
| 1h | Route jumelle en PDF | `GET /api/v1/excel-demo/products/export/pdf`, jeton admin | 200 `application/pdf`, `produits.pdf`, titre `Produits`, en-têtes, `Riz`, `ÉPICERIE`, `2 500,00`, `19/10/2026`, `Produit 150` ; jamais `non exporté` (champ sans `@ExcelColumn`) | `PdfExportIT.exportPdf_pagedRoute_returnsPdfWithTitleHeadersAndFormattedValues` | ✅ auto |
| 1i | Mêmes paramètres que la liste (PDF) | `page=2&size=1` | `Savon` seul, pas `Riz` | `PdfExportIT.exportPdf_pagedRoute_appliesTheSameQueryParameters` | ✅ auto |
| 1j | Événement publié (PDF) | export PDF d'une route annotée | un seul `ExcelExportedEvent("PRODUCT", "produits.pdf", DemoProductRDto)` | `PdfExportIT.exportPdf_annotatedRoute_publishesOneEventWithPdfFilename` | ✅ auto |
| 1k | Mise en forme PDF | `Savon Ravo`, `2500` (format `#,##0.00`), 19/10/2026, `true`, `PAID`, `1234567` | `2 500,00`, `19/10/2026`, `Oui`, `PAID`, `1 234 567`, pied `Généré le 08/11/2026 10:00 · Page 1 / 1` | `PdfExporterTest.export_rows_writesTitleHeadersAndFrenchFormattedValues` | ✅ auto |
| 1l | Plusieurs pages | 200 lignes | en-tête répété sur chaque page, `Page x / n` sur chaque page | `PdfExporterTest.export_manyRows_repeatsHeaderOnEveryPageAndNumbersPages` | ✅ auto |
| 1m | Document avec en-tête et résumé (facture, rapport) | `Client : Rasoa Hanta` avant, `Total TTC : 25 000 Ar` après | lignes d'information avant le tableau, résumé après | `PdfExporterTest.export_detailsAndSummary_areWrittenAroundTheTable` | ✅ auto |
| 1n | PDF désactivé sur une route | `@ExcelExport(pdf = false)` | `/export/pdf` 404, `/export` 200 xlsx | `PdfExportIT.exportPdf_pdfDisabled_isNotRegisteredButXlsxIs` | ✅ auto |
| 1o | Écran : téléchargement xlsx confirmé | `download('/api/v1/sales', 'xlsx')`, serveur annonce `ventes.xlsx` | `GET /api/v1/sales/export`, fichier téléchargé, toast `export.done` « Fichier ventes.xlsx généré. » | `export-service.spec.ts` « télécharge le xlsx de la route /export et confirme… » | ✅ auto |
| 1p | Écran : PDF avec les critères affichés | `from=2026-11-01`, `to=2026-11-30`, URL avec `/` final | `GET /api/v1/sales/export/pdf?from=…&to=…` en `blob`, toast avec `export.pdf` faute d'en-tête | `export-service.spec.ts` « appelle /export/pdf avec les critères… » | ✅ auto |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton (xlsx) | `GET …/products/export` sans `Authorization` | 401 `UNAUTHENTICATED` | `ExcelExportIT.export_withoutToken_returns401` | ✅ auto |
| Sans jeton (PDF) | `GET …/products/export/pdf` sans `Authorization` | 401 | `PdfExportIT.exportPdf_withoutToken_returns401` | ✅ auto |
| Mauvais droit (xlsx) | route `@HasPermission("excel-demo.unknown")` | 403 `FORBIDDEN` | `ExcelExportIT.export_missingPermission_returns403` | ✅ auto |
| Mauvais droit (PDF) | `GET …/restricted/export/pdf` | 403 | `PdfExportIT.exportPdf_missingPermission_returns403` | ✅ auto |
| `size` au-dessus du plafond (xlsx) | `size=10001` | 422 `VALIDATION_FAILED` | `ExcelExportIT.export_sizeAboveMaxRows_returns422` | ✅ auto |
| `size` au-dessus du plafond (PDF) | `size=10001` | 422 `VALIDATION_FAILED` | `PdfExportIT.exportPdf_sizeAboveMaxRows_returns422` | ✅ auto |
| Flux trop long (xlsx) | 10 001 éléments | 422, aucun fichier, aucun événement | `ExcelExportIT.export_streamLongerThanMaxRows_returns422WithoutFile` | ✅ auto |
| Flux trop long (PDF) | 10 001 éléments | 422 ProblemDetail, pas de `Content-Disposition`, aucun événement | `PdfExportIT.exportPdf_streamLongerThanMaxRows_returns422WithoutFile` | ✅ auto |
| Route non annotée | `/plain/export` | 404 (pas de route jumelle) | `ExcelExportIT.export_notAnnotatedRoute_isNotRegistered` | ✅ auto |
| PDF « brut » d'une route non annotée | `GET /api/v1/pdf-demo/raw` (`byte[]`, `produces = application/pdf`) | 200, octets renvoyés tels quels (le convertisseur d'export ne s'en empare pas) | `PdfExportIT.rawPdfRoute_notAnnotated_isWrittenAsIs` | ✅ auto |
| Écran : 422 | serveur répond 422 | rien n'est téléchargé, toast `export.tooLarge` (danger), erreur relayée | `export-service.spec.ts` « signale un export trop volumineux (422)… » | ✅ auto |
| Écran : 403 | serveur répond 403 | toast `export.forbidden` | `export-service.spec.ts` « signale un refus du serveur (403) » | ✅ auto |
| Écran : réseau coupé | erreur réseau (statut 0) | toast `export.failed` | `export-service.spec.ts` « signale toute autre erreur… » | ✅ auto |
| Mass assignment, 404 métier, 409, journal | — | sans objet : lecture seule, routes générées ; journal non alimenté (voir guide) | — | ⏭ JNL |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Exactement `max-rows` (xlsx) | 5 lignes pour `max-rows = 5` | toutes écrites | `ExcelExporterTest.export_exactlyMaxRows_writesEveryRow` | ✅ auto |
| `max-rows` + 1 (xlsx) | 6 lignes | échec sans consommer le reste du flux, rien d'écrit | `ExcelExporterTest.export_moreThanMaxRows_failsWithoutConsumingTheRestOfTheStream` | ✅ auto |
| Exactement `max-rows` (PDF) | 200 lignes pour `max-rows = 200` | 200 lignes `Article 7` | `PdfExporterTest.export_exactlyMaxRows_writesEveryRow` | ✅ auto |
| `max-rows` + 1 (PDF) | 201 lignes | `ExcelRowLimitExceededException`, flux arrêté à 201, 0 octet écrit | `PdfExporterTest.export_moreThanMaxRows_failsWithoutWritingAnything` | ✅ auto |
| Liste vide (xlsx) | `Stream.empty()` | seule la ligne d'en-tête | `ExcelExporterTest.export_emptyStream_writesHeaderRowOnly` | ✅ auto |
| Liste vide (PDF) | `Stream.empty()` | en-tête + « Aucune ligne à exporter. » | `PdfExporterTest.export_emptyStream_writesHeaderAndEmptyMessage` | ✅ auto |
| Caractères hors police PDF | `Crème\tvisage → 🙂` | `Crème visage ? ?` (tabulation → espace, inimprimable → `?`), pas d'échec | `PdfExporterTest.export_unprintableCharacters_areReplacedInsteadOfFailing` | ✅ auto |
| Valeur très longue (PDF) | 600 × `x` | colonne plafonnée, texte tronqué par `…` | `PdfExporterTest.export_longValue_isTruncatedWithEllipsis` | ✅ auto |
| Valeur très longue (xlsx) | plusieurs centaines de caractères | colonne élargie jusqu'au plafond configuré | `ExcelExporterTest.export_longValue_widensColumnUpToConfiguredMaximum` | ✅ auto |
| Fuseau horaire (PDF) | `Instant` 2026-10-19T21:30Z | `20/10/2026 00:30` (Antananarivo, passage de minuit) | `PdfExporterTest.export_instant_isWrittenInConfiguredTimeZone` | ✅ auto |
| `null` dans un chemin SpEL | `category` nul avec `category.name` | cellule vide | `ExcelExporterTest.export_nullInExpressionPath_leavesCellEmpty` | ✅ auto |
| Propriété inconnue | `expression = "inconnu"` | échec, pas de cellules vides | `ExcelExporterTest.export_unknownProperty_failsInsteadOfWritingEmptyCells` | ✅ auto |
| Route d'origine inchangée | `size=101` sur la liste | 422 comme avant | `ExcelExportIT.originalRoute_sizeAbove100_isStillRejected` | ✅ auto |
| Route d'origine, `Accept: */*` | sans préférence | JSON, page de 20 | `ExcelExportIT.originalRoute_withAnyAccept_stillReturnsJsonPagedBy20` | ✅ auto |
| Route d'origine, `Accept` xlsx | demande xlsx sur la liste | 406, pas d'export | `ExcelExportIT.originalRoute_askingForXlsx_isNotExported` | ✅ auto |
| Route d'origine, `Accept` PDF | demande PDF sur la liste | 406, pas d'export | `PdfExportIT.originalRoute_askingForPdf_isNotExported` | ✅ auto |
| Flux JPA hors transaction | `Stream` d'un repository | 🔍 non couvert : aucune route réelle ne renvoie encore un `Stream` JPA (ART-08 renvoie une `Page`) | — | 🔍 code |

## Tests existants

- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/shared/excel/ExcelExporterTest.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/shared/excel/ExcelExportIT.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/shared/excel/PdfExporterTest.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/shared/excel/PdfExportIT.java`
- `valimbavaka-ui/src/app/shared/data-access/export-service.spec.ts`, `download-file.spec.ts`

## Recette manuelle (🧪)

| Cas | Étapes | Résultat attendu | Statut |
|---|---|---|---|
| Ouverture du xlsx | Articles → « Exporter » (ART-08), ouvrir dans Excel / LibreOffice | accents, dates `dd/mm/yyyy`, largeurs correctes | 🧪 recette |
| Ouverture du PDF | `GET /api/v1/articles/exportable/export/pdf` avec un jeton admin (aucun bouton PDF sur l'écran Articles pour l'instant), ouvrir et imprimer | A4 paysage lisible, en-tête répété, `Page x / n`, montants `2 500,00`, aucun coût d'achat | 🧪 recette |

## Résultat de l'exécution

- Date : 2026-11-08, CI GitHub (workflows « CI » et « Convention VVS ») sur `92905b4`, tous les jobs verts.
- Environnement : backend `mvn -B -ntp verify` (346 tests unitaires dont 9 `PdfExporterTest` et 9 `ExcelExporterTest`, 343 tests d'intégration dont 10 `PdfExportIT` et 12 `ExcelExportIT`, Testcontainers PostgreSQL) ; frontend `ng test` (62 fichiers, 374 tests, dont `export-service.spec.ts`) ; Spotless, Checkstyle, Prettier, ESLint verts.
- Anomalies ouvertes : 1. (faible) flux JPA (`Stream` d'un repository) sans test : à couvrir au premier usage ; 2. (faible) exports non tracés au journal (`ExcelExportedEvent` sans écouteur), à confier à un ticket JNL.
- Étapes 🧪 restantes : 2.
