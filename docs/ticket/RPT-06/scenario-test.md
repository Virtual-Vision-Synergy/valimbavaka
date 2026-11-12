# Scénarios de test — RPT-06 · Export des rapports en PDF/XLSX

Statut : ☐ rédigé le 2026-11-12, tests automatiques à lancer (CI ou local) · Ticket : [#103](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/103) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL ; tests frontend Vitest sur le comportement visible.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | Administrateur `ADMIN` (a `reports.read`, qui commande aussi l'export : pas de permission nouvelle) ; vendeuse et livreur refusés |
| Données de départ | Journée du jour ouverte, vendeuse, canaux Boutique et Facebook, articles à 8 500 Ar (coût d'achat 3 120,50 Ar) d'une gamme « Gamme RPT-06 » créée par le test puis désactivée. Exemple : une vente boutique de 3 savons et 1 huile = 25 500 + 8 500 = 34 000 Ar |
| Tickets à avoir déployés avant | RPT-01 à RPT-05 (`ReportService`, `ReportRDto`), SYS-01 (`ExcelExporter`, `PdfExporter`, `ExportService`) |

Les tests d'intégration partagent la base avec d'autres classes : tous les articles de `ReportExportIT` appartiennent à une gamme créée par la classe, et le filtre gamme les isole du reste de la base.

## Scénarios issus des critères d'acceptation

Critère : « Les boutons PDF et XLSX confirment la génération du fichier. »

| # | Cas | Étant donné / Quand / Alors | Résultat attendu | Test auto | OK |
|---|---|---|---|---|---|
| 1 | Bouton PDF : fichier et confirmation | Page `/reports` ouverte (mois courant, tous canaux, toutes gammes) ; clic sur « PDF » | `GET /api/v1/reports/export/pdf?granularity=MONTH&date=…` ; le fichier est téléchargé ; toast « Fichier rapport-mensuel-2026-11.pdf généré. » | `reports-export.spec.ts` « should download the PDF of the displayed report and confirm the file » | ☐ |
| 2 | Bouton XLSX : mêmes filtres que l'écran | Canal Facebook et gamme Botanika choisis ; clic sur « Exporter XLSX » | `GET /api/v1/reports/export` avec `salesChannelId=c-fb` et `rangeId=r-bot` ; fichier téléchargé | `reports-export.spec.ts` « should export the XLSX with the channel and the range currently selected », `report-service.spec.ts` « downloads the xlsx of the displayed report with the same criteria, then confirms it », « calls the PDF route » | ☐ |
| 3 | Échec : message et nouvel essai | Le serveur répond 403 | Aucun fichier ; toast d'erreur `export.forbidden` ; les boutons se réactivent | `reports-export.spec.ts` « should tell the user when the export fails and let her try again » | ☐ |
| 4 | XLSX : un onglet par bloc, chiffres de l'écran | Une vente : 3 savons + 1 huile (gamme du test) | Onglets « Rapport », « Synthèse », « Évolution », « Gammes », « Classement » ; CA 34 000 (égal à `sales.revenue` du JSON), 1 vente, panier 34 000 ; classement : savon (quantité 3, CA 25 500, marge 16 138,50, part 75,0) ; gamme à 100,0 ; une ligne d'évolution par point de la courbe | `ReportExportIT.exportXlsx_report_hasOneSheetPerBlockWithTheFiguresOfTheScreen` | ☐ |
| 5 | XLSX : onglet « Rapport » | Gamme du test et canal Facebook | Lignes « Rapport mensuel », « du 01/… au … », période précédente, libellé du canal, « Gamme RPT-06 » ; sans filtre : « Tous les canaux », « Toutes les gammes » | `ReportExportIT.exportXlsx_information_namesTheKindPeriodChannelAndRange`, `ReportExportLayoutTest.info_noFilter_saysAllChannelsAndAllRanges`, `ReportExportLayoutTest.info_day_printsTheDateAloneAndTheFilterLabels` | ☐ |
| 6 | PDF : titre, filtres, indicateurs, lignes | Une vente de 3 savons | PDF (`%PDF-`) contenant « Rapport mensuel », « Gamme : Gamme RPT-06 », « Chiffre d'affaires : 25 500,00 Ar », « Nombre de ventes : 1 », les blocs Évolution, Gamme, Classement, la marge « 16 138,50 » et la part « 100,0 » | `ReportExportIT.exportPdf_report_printsTheFiltersIndicatorsAndRows`, `ReportExportLayoutTest.pdfDetails_report_printsFiltersAndIndicatorsInFrench`, `ReportExportLayoutTest.pdfRows_report_chainsTheCurveTheRangesAndTheRanking` | ☐ |
| 7 | Même calcul que l'écran | L'export demande le rapport au service de l'écran | `ReportService.getReport` est appelé une fois avec les paramètres reçus ; aucun second calcul | `ReportExportServiceImplTest.exportXlsx_filters_readsTheReportOfTheScreenAndNamesTheFilters`, `ReportExportLayoutTest.indicators_report_copiesTheFiguresOfTheScreen` | ☐ |
| 8 | Noms de fichier | Jour 2026-11-12, mois 2026-03, année 2025 | `rapport-journalier-2026-11-12.xlsx`, `rapport-mensuel-2026-03.pdf`, `rapport-annuel-2025.pdf` (en-tête `Content-Disposition: attachment`) | `ReportExportIT.export_fileNames_followTheKindAndThePeriod`, `ReportExportLayoutTest.filename_eachGranularity_followsTheKindAndThePeriod` | ☐ |

## Sécurité et erreurs

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Appel sans jeton (les deux routes) | 401 `UNAUTHENTICATED` | `ReportExportIT.export_withoutToken_returns401` | ☐ |
| Vendeuse ou livreur (les deux routes) | 403 `FORBIDDEN` | `ReportExportIT.export_sellerAndDeliveryDriver_return403` | ☐ |
| `granularity` inconnue (`WEEK`) | 400 `MALFORMED_REQUEST` | `ReportExportIT.export_unknownGranularity_returns400` | ☐ |
| Période qui n'a pas commencé (année 2999) | 422 `VALIDATION_FAILED`, aucun fichier | `ReportExportIT.export_periodThatHasNotBegun_returns422WithoutFile`, `ReportExportServiceImplTest.exportXlsx_periodThatHasNotBegun_failsWithoutWritingAFile` | ☐ |

Sans objet : corps de requête (routes GET, pas de mass assignment), 404 et 409 (aucune ressource ni doublon).

## Cas limites et hors ligne

| Cas | Résultat attendu | Test auto | OK |
|---|---|---|---|
| Période sans vente (année 2000) | xlsx : onglets « Gammes » et « Classement » réduits à l'en-tête, 12 lignes d'évolution, CA 0 et variation vide | `ReportExportIT.exportXlsx_pastPeriodWithoutSale_hasHeaderRowsOnlyAndZeroIndicators` | ☐ |
| Période sans vente, PDF | PDF produit : « Chiffre d'affaires : 0,00 Ar », « Nombre de ventes : 0 (précédent : 0, n/d) » | `ReportExportIT.exportPdf_nothingSold_stillProducesTheDocument` | ☐ |
| Filtre canal | Seules les ventes du canal : CA 17 000 (huile ×2), un seul article classé | `ReportExportIT.exportXlsx_channelFilter_keepsOnlyThatChannel` | ☐ |
| Sans paramètre | Mois courant (heure de Madagascar), tous canaux, toutes gammes | `ReportExportIT.export_withoutParameters_isTheCurrentMonth` | ☐ |
| Pas de filtre : pas de recherche de libellé | Aucune requête de libellé de canal ou de gamme | `ReportExportServiceImplTest.exportPdf_noFilter_doesNotLookUpAnyLabel` | ☐ |
| Identifiant de canal inconnu | Le PDF imprime l'identifiant reçu ; le rapport est vide | `ReportExportServiceImplTest.exportPdf_unknownChannel_printsTheIdentifierReceived` | ☐ |
| Année : courbe par mois | Libellés « 11/2026 » dans le PDF | `ReportExportLayoutTest.pdfRows_year_labelsTheCurveByMonth` | ☐ |
| Marge inconnue | Cellule de marge vide (xlsx et PDF), jamais 0 | `ReportExportLayoutTest.pdfRows_report_chainsTheCurveTheRangesAndTheRanking` | ☐ |
| Hors ligne | Une lecture : pas de file d'écriture ; hors ligne, le téléchargement échoue et le toast `export.failed` s'affiche | _(pas de test dédié : comportement de `ExportService`, SYS-01)_ | ☐ |

## Recette à faire par Njara après le merge

- Ouvrir `/reports` en administrateur, choisir un mois avec des ventes et un canal ; cliquer « PDF » puis « Exporter XLSX » : deux fichiers `rapport-mensuel-AAAA-MM`, un toast « Fichier … généré. » chacun.
- Comparer les chiffres du fichier à l'écran (CA, ventes, panier, variations, gammes, classement).
- Se connecter en vendeuse : `/reports` reste inaccessible, et l'appel direct à `/api/v1/reports/export` donne 403.

## Résultat de l'exécution

- Date : à lancer (CI ou local)
- Environnement : à lancer (CI ou local)
- Anomalies ouvertes : aucune connue
