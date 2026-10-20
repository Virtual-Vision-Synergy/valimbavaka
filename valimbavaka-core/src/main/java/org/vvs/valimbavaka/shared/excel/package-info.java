/**
 * Export Excel générique piloté par annotations : {@code @ExcelExport} sur une méthode GET ajoute la route
 * jumelle {@code <chemin>/export} (xlsx), {@code @ExcelColumn} sur les champs du DTO choisit les colonnes.
 * Ne dépend d'aucune feature (convention §4.2).
 */
@NullMarked
package org.vvs.valimbavaka.shared.excel;

import org.jspecify.annotations.NullMarked;
