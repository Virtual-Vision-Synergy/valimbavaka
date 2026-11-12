package org.vvs.valimbavaka.feature.report.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ranking of the best-selling articles (RPT-05): at most {@code size} articles, biggest turnover first, and the
 * turnover of all the lines of the period and filters (the base of the shares, not the sum of the listed rows). The
 * list is empty (and the revenue 0) when nothing was sold.
 */
public record ReportRankingRDto(int size, BigDecimal revenue, List<ReportRankedArticleRDto> items) {}
