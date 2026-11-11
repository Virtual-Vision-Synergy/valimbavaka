package org.vvs.valimbavaka.feature.dashboard.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.dashboard.dto.DashboardRDto;
import org.vvs.valimbavaka.feature.dashboard.dto.DeliveryKpiRDto;
import org.vvs.valimbavaka.feature.dashboard.dto.RevenueKpiRDto;
import org.vvs.valimbavaka.feature.dashboard.dto.SalesKpiRDto;
import org.vvs.valimbavaka.feature.dashboard.dto.StockGapKpiRDto;
import org.vvs.valimbavaka.feature.dashboard.service.DashboardService;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLineRDto;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingService;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.service.DeliveryDayService;
import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.domain.ReportPeriod;
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;
import org.vvs.valimbavaka.feature.report.service.ReportSalesService;
import org.vvs.valimbavaka.feature.sale.service.ChannelDashboardService;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BusinessDayService businessDayService;
    private final ReportSalesService reportSalesService;
    private final ChannelDashboardService channelDashboardService;
    private final DeliveryDayService deliveryDayService;
    private final DayClosingService dayClosingService;
    private final Clock clock;

    DashboardServiceImpl(
            BusinessDayService businessDayService,
            ReportSalesService reportSalesService,
            ChannelDashboardService channelDashboardService,
            DeliveryDayService deliveryDayService,
            DayClosingService dayClosingService,
            Clock clock) {
        this.businessDayService = businessDayService;
        this.reportSalesService = reportSalesService;
        this.channelDashboardService = channelDashboardService;
        this.deliveryDayService = deliveryDayService;
        this.dayClosingService = dayClosingService;
        this.clock = clock;
    }

    @Override
    public DashboardRDto getDashboard() {
        var now = clock.instant();
        var date = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        var day = businessDayService.findToday();
        var period = ReportPeriod.containing(ReportGranularity.DAY, date);
        var today = reportSalesService.getSales(new ReportFilter(period, null, null));
        var yesterday = reportSalesService.getSales(new ReportFilter(period.previous(), null, null));
        var channels = channelDashboardService.getDashboard(date, date).channels();
        return new DashboardRDto(
                date,
                day == null ? null : day.status(),
                now,
                revenue(today, yesterday),
                new SalesKpiRDto(today.salesCount(), yesterday.salesCount(), today.averageBasket(), channels),
                deliveries(date),
                day == null ? null : stockGap());
    }

    private static RevenueKpiRDto revenue(ReportSalesRDto today, ReportSalesRDto yesterday) {
        var change = changePercent(today.revenue(), yesterday.revenue());
        return new RevenueKpiRDto(today.revenue(), yesterday.revenue(), change);
    }

    private DeliveryKpiRDto deliveries(LocalDate date) {
        var counts = deliveryDayService.countByStatus(date);
        long delivered = counts.getOrDefault(DeliveryStatus.DELIVERED, 0L);
        long onRound = counts.getOrDefault(DeliveryStatus.ON_ROUND, 0L);
        long toPrepare = counts.getOrDefault(DeliveryStatus.TO_PREPARE, 0L);
        long failed = counts.getOrDefault(DeliveryStatus.FAILED, 0L);
        long cancelled = counts.getOrDefault(DeliveryStatus.CANCELLED, 0L);
        return new DeliveryKpiRDto(
                delivered + onRound + toPrepare + failed, delivered, onRound, toPrepare, failed, cancelled);
    }

    /** Signed sum of the line gaps of the closing reconciliation and number of articles concerned. */
    private StockGapKpiRDto stockGap() {
        var units = BigDecimal.ZERO;
        var articles = new HashSet<UUID>();
        for (var location : dayClosingService.getClosing().locations()) {
            for (var line : location.lines()) {
                var gap = gapOf(line);
                if (gap.signum() != 0) {
                    units = units.add(gap);
                    articles.add(line.articleId());
                }
            }
        }
        return new StockGapKpiRDto(units, articles.size());
    }

    /** The gap on the evening count when there is one (JR-05), on the system stock before. */
    private static BigDecimal gapOf(ClosingLineRDto line) {
        var counted = line.counted();
        return counted == null ? line.gap() : counted.subtract(line.theoretical());
    }

    private static @Nullable BigDecimal changePercent(BigDecimal amount, BigDecimal previous) {
        if (previous.signum() == 0) {
            return null;
        }
        return amount.subtract(previous).multiply(HUNDRED).divide(previous, 1, RoundingMode.HALF_UP);
    }
}
