package org.vvs.valimbavaka.feature.report.dto;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/**
 * Variation in percent (one decimal, negative when it falls) of each sales indicator against the previous period
 * (RPT-02). An indicator is {@code null} when its previous value is 0: no percentage, never a division by zero.
 */
public record ReportVariationRDto(
        @Nullable BigDecimal revenue, @Nullable BigDecimal salesCount, @Nullable BigDecimal averageBasket) {}
