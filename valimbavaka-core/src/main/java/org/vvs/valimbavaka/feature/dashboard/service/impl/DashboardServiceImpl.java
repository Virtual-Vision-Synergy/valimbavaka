package org.vvs.valimbavaka.feature.dashboard.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.dashboard.dto.RevenuePointRDto;
import org.vvs.valimbavaka.feature.dashboard.dto.RevenueTrendRDto;
import org.vvs.valimbavaka.feature.dashboard.service.DashboardService;
import org.vvs.valimbavaka.feature.sale.service.SaleRevenueService;
import org.vvs.valimbavaka.feature.salonservice.service.ServiceRecordService;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    /** Time zone of the business days (same as the day opening, JR-01). */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    private static final int DAYS = 7;
    private static final int WEEKS = 4;

    private final SaleRevenueService saleRevenueService;
    private final ServiceRecordService serviceRecordService;
    private final Clock clock;

    DashboardServiceImpl(
            SaleRevenueService saleRevenueService, ServiceRecordService serviceRecordService, Clock clock) {
        this.saleRevenueService = saleRevenueService;
        this.serviceRecordService = serviceRecordService;
        this.clock = clock;
    }

    @Override
    public RevenueTrendRDto getRevenueTrend() {
        var today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        var firstMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(WEEKS - 1L);
        var revenue = revenuePerDay(firstMonday, today);
        var days = Stream.iterate(today.minusDays(DAYS - 1L), day -> day.plusDays(1))
                .limit(DAYS)
                .map(day -> new RevenuePointRDto(day, revenue.getOrDefault(day, BigDecimal.ZERO)))
                .toList();
        var weeks = Stream.iterate(firstMonday, monday -> monday.plusWeeks(1))
                .limit(WEEKS)
                .map(monday -> new RevenuePointRDto(monday, weekTotal(revenue, monday)))
                .toList();
        return new RevenueTrendRDto(today, days, weeks);
    }

    /** Validated sales by business date plus salon services by local date of performance, in {@code [from, to]}. */
    private Map<LocalDate, BigDecimal> revenuePerDay(LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> revenue = new HashMap<>();
        for (var day : saleRevenueService.getDailyRevenue(from, to)) {
            revenue.merge(day.businessDate(), day.revenue(), BigDecimal::add);
        }
        var start = from.atStartOfDay(BUSINESS_ZONE).toInstant();
        var end = to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        for (var service : serviceRecordService.getServiceRecordsBetween(start, end)) {
            var performedOn = LocalDate.ofInstant(service.performedAt(), BUSINESS_ZONE);
            revenue.merge(performedOn, service.price(), BigDecimal::add);
        }
        return revenue;
    }

    private static BigDecimal weekTotal(Map<LocalDate, BigDecimal> revenue, LocalDate monday) {
        return Stream.iterate(monday, day -> day.plusDays(1))
                .limit(DayOfWeek.values().length)
                .map(day -> revenue.getOrDefault(day, BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
