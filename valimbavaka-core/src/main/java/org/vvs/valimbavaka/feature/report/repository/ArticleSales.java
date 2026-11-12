package org.vvs.valimbavaka.feature.report.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Sales of one article over a period (RPT-05): quantity, turnover (line totals) and purchase cost of the goods sold.
 * {@code lines} is the number of sale lines, {@code costedLines} those whose unit cost was known when sold.
 */
public record ArticleSales(
        UUID articleId,
        String code,
        String name,
        @Nullable BigDecimal quantity,
        @Nullable BigDecimal revenue,
        @Nullable BigDecimal cost,
        long lines,
        long costedLines) {}
