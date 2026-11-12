package org.vvs.valimbavaka.feature.report.service;

import java.util.List;

import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.dto.ReportRangesRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportTrendPointRDto;

/**
 * Sales aggregate of a filtered period (RPT-01), the building block of the reports and of the dashboard: validated
 * sales only, by business day, summed in SQL. Call it with {@code filter.period().previous()} for the comparison
 * period (RPT-02).
 */
public interface ReportSalesService {

    /** Turnover, number of sales and average basket of the period, restricted to the channel and range if any. */
    ReportSalesRDto getSales(ReportFilter filter);

    /**
     * Turnover curve of the period (RPT-03): one point per business day (day or month period) or per month (year
     * period), same filters as {@link #getSales}; a point without sale is 0, so the series has no gap.
     */
    List<ReportTrendPointRDto> getTrend(ReportFilter filter);

    /**
     * Turnover split by product range (RPT-04), one SQL aggregate: the ranges that sold something, biggest first, with
     * their share in percent (one decimal, adding up to exactly 100.0). Only the period and the channel of the filter
     * apply: the range of the filter is ignored, since a single range would always be 100 %.
     */
    ReportRangesRDto getRanges(ReportFilter filter);
}
