package org.vvs.valimbavaka.feature.referential.dto;

import java.util.UUID;

/** {@code code} : valeur stable utilisée par le code ; {@code label} : libellé affiché. */
public record StockLocationRDto(UUID id, String code, String label) {
}
