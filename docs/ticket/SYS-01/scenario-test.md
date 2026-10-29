# Scénarios de test — SYS-01 · Export PDF/Excel généralisé

Statut : ☐ tests non exécutés (écrit le 2026-10-29 d'après les tests existants) · Ticket : [#105](https://github.com/Virtual-Vision-Synergy/valimbavaka/issues/105) · [description](description.md) · [guide](guide.md)

Convention (VVS V2.1) : tests backend `methode_scenario_resultatAttendu`, SUT nommé `sut`, Arrange / Act / Assert, Testcontainers PostgreSQL.

## Pré-requis

| Élément | Valeur |
|---|---|
| Rôle(s) utilisé(s) | administrateur `admin.test@valimbavaka.mg` ; un jeton sans la permission de la route pour le 403 |
| Données de départ | contrôleur de démonstration de `ExcelExportIT` : 150 produits (`Riz`, gamme `ÉPICERIE`, prix `2500`, mis en vente le 19/10/2026) |
| Tickets à avoir déployés avant | AUTH-01 (fermé) |

## Scénarios issus des critères

Critère : « Chaque écran concerné propose un export PDF et/ou XLSX fonctionnel. » Ce ticket livre la brique XLSX ; les écrans l'adoptent dans leurs tickets (ART-08, FAC-03, FAC-06, COM-05, RPT-06). Le PDF n'est pas livré.

| # | Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|---|
| 1 | Route jumelle en xlsx | `GET /api/v1/excel-demo/products/export` avec jeton admin | 200, `produits.xlsx`, onglet `Produits`, en-têtes `Libellé / Gamme / Prix / Mis en vente`, ligne 1 `Riz / ÉPICERIE / 2,500.00 / 19/10/2026`, 150 lignes | `ExcelExportIT.export_pagedRoute_returnsXlsxWithOrderedHeadersAndFormattedValues` | ☐ |
| 1b | Mêmes paramètres que la liste | filtre de la liste repris sur `/export` | le fichier ne contient que les lignes filtrées | `ExcelExportIT.export_pagedRoute_appliesTheSameQueryParameters` | ☐ |
| 1c | Route en `Stream` | méthode renvoyant `Stream<Dto>` | chaque élément exporté | `ExcelExportIT.export_streamRoute_exportsEveryElement` | ☐ |
| 1d | Colonnes ordonnées, parents d'abord | classe avec parent, `order` mélangés | ordre = `order`, colonnes du parent en tête à égalité | `ExcelExporterTest.export_classWithParent_sortsByOrderThenParentColumnsFirst` | ☐ |
| 1e | Record et expressions SpEL | composants de record, `role?.name` | valeurs lues correctement | `ExcelExporterTest.export_record_readsComponentsAndExpressions` | ☐ |
| 1f | Plusieurs onglets | deux `Sheet` | un onglet par `Sheet`, dans l'ordre | `ExcelExporterTest.export_severalSheets_writesOneTabPerSheetInOrder` | ☐ |
| 1g | Événement publié | export d'une route annotée | un seul `ExcelExportedEvent` | `ExcelExportIT.export_annotatedRoute_publishesOneEvent` | ☐ |
| 1h | PDF | — | ⏭ hors périmètre livré : à décider avant FAC-03 / RPT-06 | — | ⏭ |

## Sécurité et erreurs

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Sans jeton | `GET …/products/export` sans `Authorization` | 401 `UNAUTHENTICATED` | `ExcelExportIT.export_withoutToken_returns401` | ☐ |
| Mauvais rôle | jeton sans la permission de la route | 403 `FORBIDDEN` | `ExcelExportIT.export_missingPermission_returns403` | ☐ |
| `size` au-dessus du plafond | `size=10001` | 422 `VALIDATION_FAILED` | `ExcelExportIT.export_sizeAboveMaxRows_returns422` | ☐ |
| Flux trop long | plus de 10 000 éléments | 422, aucun fichier partiel | `ExcelExportIT.export_streamLongerThanMaxRows_returns422WithoutFile` | ☐ |
| Route non annotée | `/export` d'une route sans `@ExcelExport` | pas de route jumelle | `ExcelExportIT.export_notAnnotatedRoute_isNotRegistered` | ☐ |
| Mass assignment, 404, 409, journal | — | sans objet : lecture seule, route générée | — | ⏭ |

## Cas limites

| Cas | Exemple | Résultat attendu | Test | Statut |
|---|---|---|---|---|
| Exactement `max-rows` | 10 000 lignes | toutes écrites | `ExcelExporterTest.export_exactlyMaxRows_writesEveryRow` | ☐ |
| `max-rows` + 1 | 10 001 lignes | échec sans consommer le reste du flux | `ExcelExporterTest.export_moreThanMaxRows_failsWithoutConsumingTheRestOfTheStream` | ☐ |
| Valeur très longue | texte de plusieurs centaines de caractères | colonne élargie jusqu'au plafond configuré | `ExcelExporterTest.export_longValue_widensColumnUpToConfiguredMaximum` | ☐ |
| `null` dans un chemin SpEL | `role` nul avec `role.name` | cellule vide | `ExcelExporterTest.export_nullInExpressionPath_leavesCellEmpty` | ☐ |
| Propriété inconnue | `expression = "inconnu"` | échec, pas de cellules vides | `ExcelExporterTest.export_unknownProperty_failsInsteadOfWritingEmptyCells` | ☐ |
| Route d'origine inchangée | `size=101` sur la liste | 422 comme avant | `ExcelExportIT.originalRoute_sizeAbove100_isStillRejected` | ☐ |
| Route d'origine, `Accept: */*` | sans préférence | JSON, page de 20 | `ExcelExportIT.originalRoute_withAnyAccept_stillReturnsJsonPagedBy20` | ☐ |
| Route d'origine, `Accept` xlsx | demande xlsx sur la liste | pas d'export | `ExcelExportIT.originalRoute_askingForXlsx_isNotExported` | ☐ |
| Liste vide | aucune ligne | 🔍 fichier avec la seule ligne d'en-tête ; test manquant | — | 🔍 code |
| Flux JPA hors transaction | `Stream` d'un repository | 🔍 non vérifié ; test à écrire au premier usage (ART-08) | — | 🔍 code |

## Tests existants

- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/shared/excel/ExcelExporterTest.java`
- `valimbavaka-core/src/test/java/org/vvs/valimbavaka/shared/excel/ExcelExportIT.java`

## Recette manuelle (🧪)

| Cas | Étapes | Résultat attendu | Statut |
|---|---|---|---|
| Ouverture dans Excel / LibreOffice | ouvrir un fichier produit par la première route annotée (ART-08) | accents, dates `dd/mm/yyyy`, largeurs correctes | 🧪 recette |

## Résultat de l'exécution

- Date : à lancer (CI ou local, `mvn -B -ntp verify` dans `valimbavaka-core`).
- Environnement : —
- Anomalies ouvertes : 1. (moyenne) pas de PDF alors que le critère le cite, à arbitrer avant FAC-03 / RPT-06 ; 2. (faible) cas « liste vide » et « flux JPA » sans test.
- Étapes 🧪 restantes : 1.
