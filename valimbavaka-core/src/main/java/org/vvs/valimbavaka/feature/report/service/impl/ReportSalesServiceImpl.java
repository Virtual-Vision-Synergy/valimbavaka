package org.vvs.valimbavaka.feature.report.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;
import org.vvs.valimbavaka.feature.report.repository.ReportSaleRepository;
import org.vvs.valimbavaka.feature.report.repository.SalesTotal;
import org.vvs.valimbavaka.feature.report.service.ReportSalesService;

@Service
@Transactional(readOnly = true)
public class ReportSalesServiceImpl implements ReportSalesService {

    private static final int MONEY_SCALE = 2;

    private final ReportSaleRepository reportSaleRepository;

    ReportSalesServiceImpl(ReportSaleRepository reportSaleRepository) {
        this.reportSaleRepository = reportSaleRepository;
    }

    @Override
    public ReportSalesRDto getSales(ReportFilter filter) {
        var from = filter.period().from();
        var to = filter.period().to();
        var channelId = filter.salesChannelId();
        var rangeId = filter.rangeId();
        if (rangeId == null) {
            return toIndicators(reportSaleRepository.totals(from, to, channelId));
        }
        return toIndicators(reportSaleRepository.totalsByRange(from, to, channelId, rangeId));
    }

    private static ReportSalesRDto toIndicators(SalesTotal total) {
        var revenue = total.revenue() == null ? BigDecimal.ZERO : total.revenue();
        revenue = revenue.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        var basket = revenue;
        if (total.salesCount() > 0) {
            basket = revenue.divide(BigDecimal.valueOf(total.salesCount()), MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return new ReportSalesRDto(revenue, total.salesCount(), basket);
    }
}
