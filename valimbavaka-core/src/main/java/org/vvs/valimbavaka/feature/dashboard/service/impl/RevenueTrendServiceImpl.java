package org.vvs.valimbavaka.feature.dashboard.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.dashboard.dto.RevenueTrendPointRDto;
import org.vvs.valimbavaka.feature.dashboard.dto.RevenueTrendRDto;
import org.vvs.valimbavaka.feature.dashboard.service.RevenueTrendService;
import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.domain.ReportPeriod;
import org.vvs.valimbavaka.feature.report.service.ReportSalesService;

@Service
@Transactional(readOnly = true)
public class RevenueTrendServiceImpl implements RevenueTrendService {

    /** Number of points of the trend, today included. */
    static final int DAYS = 7;

    private final ReportSalesService reportSalesService;
    private final Clock clock;

    RevenueTrendServiceImpl(ReportSalesService reportSalesService, Clock clock) {
        this.reportSalesService = reportSalesService;
        this.clock = clock;
    }

    @Override
    public RevenueTrendRDto getLastSevenDays() {
        var today = LocalDate.now(clock.withZone(DashboardServiceImpl.BUSINESS_ZONE));
        var from = today.minusDays(DAYS - 1L);
        List<RevenueTrendPointRDto> points =
                IntStream.range(0, DAYS).mapToObj(offset -> point(from.plusDays(offset))).toList();
        return new RevenueTrendRDto(from, today, points);
    }

    private RevenueTrendPointRDto point(LocalDate day) {
        var period = ReportPeriod.containing(ReportGranularity.DAY, day);
        var sales = reportSalesService.getSales(new ReportFilter(period, null, null));
        return new RevenueTrendPointRDto(day, sales.revenue(), sales.salesCount());
    }
}
