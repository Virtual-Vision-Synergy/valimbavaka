package org.vvs.valimbavaka.feature.report.service;

import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;

/**
 * Sales aggregate of a filtered period (RPT-01), the building block of the reports and of the dashboard: validated
 * sales only, by business day, summed in SQL. Call it with {@code filter.period().previous()} for the comparison
 * period (RPT-02).
 */
public interface ReportSalesService {

    /** Turnover, number of sales and average basket of the period, restricted to the channel and range if any. */
    ReportSalesRDto getSales(ReportFilter filter);
}
