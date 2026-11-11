package org.vvs.valimbavaka.feature.sale.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Article of a listed sale (VTE-07): name read from the article sheet, quantity and frozen line total. */
public record SaleSummaryLineRDto(
        UUID articleId, @Nullable String articleName, BigDecimal quantity, BigDecimal lineTotal) {}
