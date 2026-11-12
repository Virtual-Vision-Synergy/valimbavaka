package org.vvs.valimbavaka.feature.report.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.domain.ReportPeriod;
import org.vvs.valimbavaka.feature.report.domain.ReportVariation;
import org.vvs.valimbavaka.feature.report.dto.ReportComparisonRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportVariationRDto;
import org.vvs.valimbavaka.feature.report.service.ReportSalesService;
import org.vvs.valimbavaka.feature.report.service.ReportService;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    private final ReportSalesService reportSalesService;
    private final Clock clock;

    ReportServiceImpl(ReportSalesService reportSalesService, Clock clock) {
        this.reportSalesService = reportSalesService;
        this.clock = clock;
    }

    @Override
    public ReportRDto getReport(
            @Nullable ReportGranularity granularity,
            @Nullable LocalDate date,
            @Nullable UUID salesChannelId,
            @Nullable UUID rangeId) {
        var today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        var kind = granularity != null ? granularity : ReportGranularity.MONTH;
        var day = date != null ? date : today;
        var period = ReportPeriod.containing(kind, day);
        if (period.startsAfter(today)) {
            throw new BusinessException("Le rapport ne peut pas porter sur une période qui n'a pas commencé.");
        }
        var filter = new ReportFilter(period, salesChannelId, rangeId);
        var sales = reportSalesService.getSales(filter);
        var comparison = compare(sales, new ReportFilter(period.previous(), salesChannelId, rangeId));
        var trend = reportSalesService.getTrend(filter);
        var ranges = reportSalesService.getRanges(filter);
        return new ReportRDto(
                period.granularity(),
                period.from(),
                period.to(),
                salesChannelId,
                rangeId,
                sales,
                comparison,
                trend,
                ranges);
    }

    private ReportComparisonRDto compare(ReportSalesRDto current, ReportFilter previousFilter) {
        var previous = reportSalesService.getSales(previousFilter);
        var variation = new ReportVariationRDto(
                ReportVariation.percent(current.revenue(), previous.revenue()),
                ReportVariation.percent(current.salesCount(), previous.salesCount()),
                ReportVariation.percent(current.averageBasket(), previous.averageBasket()));
        return new ReportComparisonRDto(
                previousFilter.period().from(), previousFilter.period().to(), previous, variation);
    }
}
