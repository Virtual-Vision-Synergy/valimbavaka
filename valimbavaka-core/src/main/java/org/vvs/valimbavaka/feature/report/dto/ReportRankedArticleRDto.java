package org.vvs.valimbavaka.feature.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * One row of the ranking of the best-selling articles (RPT-05): quantity sold (3 decimals), turnover (sum of the line
 * totals, before the discount of the whole sale), gross margin (turnover minus purchase cost, {@code null} when the
 * cost of a line was unknown when it was sold) and share of the turnover of all the lines of the period, in percent
 * with one decimal.
 */
public record ReportRankedArticleRDto(
        UUID articleId,
        String code,
        String name,
        BigDecimal quantity,
        BigDecimal revenue,
        @Nullable BigDecimal margin,
        BigDecimal share) {}
