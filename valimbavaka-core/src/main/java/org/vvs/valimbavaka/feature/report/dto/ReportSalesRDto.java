package org.vvs.valimbavaka.feature.report.dto;

import java.math.BigDecimal;

/**
 * Sales indicators of a period (RPT-01): turnover, number of validated sales and average basket (turnover / number
 * of sales, 0 when there is no sale). Shared by the later reports, which return it for other periods (RPT-02).
 */
public record ReportSalesRDto(BigDecimal revenue, long salesCount, BigDecimal averageBasket) {}
