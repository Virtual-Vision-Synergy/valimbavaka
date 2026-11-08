/**
 * Export générique piloté par annotations : {@code @ExcelExport} sur une méthode GET ajoute les routes jumelles
 * {@code <chemin>/export} (xlsx) et {@code <chemin>/export/pdf} (PDF tabulaire), {@code @ExcelColumn} sur les
 * champs du DTO choisit les colonnes des deux formats.
 * Ne dépend d'aucune feature (convention §4.2).
 */
@NullMarked
package org.vvs.valimbavaka.shared.excel;

import org.jspecify.annotations.NullMarked;
