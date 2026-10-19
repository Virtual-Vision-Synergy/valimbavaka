package org.vvs.valimbavaka.shared.excel.event;

/**
 * Publié après chaque export d'une méthode annotée {@code @ExcelExport}.
 * Les features l'écoutent (audit, journalisation) sans que le module les connaisse.
 */
public record ExcelExportedEvent(String target, String filename, Class<?> elementType) {
}
