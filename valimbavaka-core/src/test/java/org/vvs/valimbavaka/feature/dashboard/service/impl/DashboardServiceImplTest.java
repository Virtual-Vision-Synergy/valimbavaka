package org.vvs.valimbavaka.feature.dashboard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.day.domain.DayStatus;
import org.vvs.valimbavaka.feature.day.dto.BusinessDayRDto;
import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLineRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.ClosingLocationRDto;
import org.vvs.valimbavaka.feature.dayclosing.dto.DayClosingRDto;
import org.vvs.valimbavaka.feature.dayclosing.service.DayClosingService;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.service.DeliveryDayService;
import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.domain.ReportPeriod;
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;
import org.vvs.valimbavaka.feature.report.service.ReportSalesService;
import org.vvs.valimbavaka.feature.sale.dto.ChannelDashboardRDto;
import org.vvs.valimbavaka.feature.sale.dto.ChannelSalesRDto;
import org.vvs.valimbavaka.feature.sale.service.ChannelDashboardService;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    /** 13:00 in Antananarivo (UTC+3), 2026-11-11. */
    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);
    private static final UUID STORE = UUID.randomUUID();

    @Mock
    private BusinessDayService businessDayService;

    @Mock
    private ReportSalesService reportSalesService;

    @Mock
    private ChannelDashboardService channelDashboardService;

    @Mock
    private DeliveryDayService deliveryDayService;

    @Mock
    private DayClosingService dayClosingService;

    private DashboardServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = serviceAt(NOW);
    }

    @Test
    void getDashboard_openDay_composesTheFourIndicators() {
        // Arrange
        stubDay(DayStatus.OPEN);
        stubSales(TODAY, "15500", 3, "5166.67");
        stubSales(TODAY.minusDays(1), "10000", 2, "5000.00");
        stubChannels(TODAY, "15500", 3);
        var counts = new EnumMap<DeliveryStatus, Long>(DeliveryStatus.class);
        counts.put(DeliveryStatus.DELIVERED, 9L);
        counts.put(DeliveryStatus.ON_ROUND, 3L);
        counts.put(DeliveryStatus.TO_PREPARE, 2L);
        counts.put(DeliveryStatus.FAILED, 1L);
        counts.put(DeliveryStatus.CANCELLED, 4L);
        stubDeliveries(counts);
        var soap = UUID.randomUUID();
        stubClosing(location(line(soap, "10", "-4", null), line(UUID.randomUUID(), "5", "0", null)));

        // Act
        var dashboard = sut.getDashboard();

        // Assert
        assertThat(dashboard.businessDate()).isEqualTo(TODAY);
        assertThat(dashboard.status()).isEqualTo(DayStatus.OPEN);
        assertThat(dashboard.generatedAt()).isEqualTo(NOW);
        assertThat(dashboard.revenue().amount()).isEqualByComparingTo("15500");
        assertThat(dashboard.revenue().previousAmount()).isEqualByComparingTo("10000");
        assertThat(dashboard.revenue().changePercent()).isEqualByComparingTo("55.0");
        assertThat(dashboard.sales().count()).isEqualTo(3);
        assertThat(dashboard.sales().previousCount()).isEqualTo(2);
        assertThat(dashboard.sales().averageBasket()).isEqualByComparingTo("5166.67");
        assertThat(dashboard.sales().channels()).hasSize(1);
        assertThat(dashboard.deliveries().total()).isEqualTo(15);
        assertThat(dashboard.deliveries().delivered()).isEqualTo(9);
        assertThat(dashboard.deliveries().onRound()).isEqualTo(3);
        assertThat(dashboard.deliveries().toPrepare()).isEqualTo(2);
        assertThat(dashboard.deliveries().failed()).isEqualTo(1);
        assertThat(dashboard.deliveries().cancelled()).isEqualTo(4);
        assertThat(dashboard.stockGap().units()).isEqualByComparingTo("-4");
        assertThat(dashboard.stockGap().articles()).isEqualTo(1);
    }

    @Test
    void getDashboard_dayNotOpenedYet_hasNoStatusNorStockGapAndReadsNoClosing() {
        // Arrange
        when(businessDayService.findToday()).thenReturn(null);
        stubSales(TODAY, "0", 0, "0");
        stubSales(TODAY.minusDays(1), "0", 0, "0");
        stubChannels(TODAY, "0", 0);
        stubDeliveries(Map.of());

        // Act
        var dashboard = sut.getDashboard();

        // Assert
        assertThat(dashboard.status()).isNull();
        assertThat(dashboard.stockGap()).isNull();
        assertThat(dashboard.sales().count()).isZero();
        assertThat(dashboard.deliveries().total()).isZero();
        verifyNoInteractions(dayClosingService);
    }

    @Test
    void getDashboard_noRevenueTheDayBefore_hasNoChangePercent() {
        // Arrange
        stubDay(DayStatus.OPEN);
        stubSales(TODAY, "8500", 1, "8500.00");
        stubSales(TODAY.minusDays(1), "0", 0, "0");
        stubChannels(TODAY, "8500", 1);
        stubDeliveries(Map.of());
        stubClosing(location());

        // Act
        var dashboard = sut.getDashboard();

        // Assert
        assertThat(dashboard.revenue().previousAmount()).isEqualByComparingTo("0");
        assertThat(dashboard.revenue().changePercent()).isNull();
    }

    @Test
    void getDashboard_revenueDrop_hasNegativeChangePercentRoundedToOneDecimal() {
        // Arrange
        stubDay(DayStatus.OPEN);
        stubSales(TODAY, "6000", 1, "6000.00");
        stubSales(TODAY.minusDays(1), "9000", 2, "4500.00");
        stubChannels(TODAY, "6000", 1);
        stubDeliveries(Map.of());
        stubClosing(location());

        // Act
        var dashboard = sut.getDashboard();

        // Assert
        assertThat(dashboard.revenue().changePercent()).isEqualByComparingTo("-33.3");
    }

    @Test
    void getDashboard_countedLine_takesTheGapOfTheCountInsteadOfTheSystemStock() {
        // Arrange
        stubDay(DayStatus.OPEN);
        stubSales(TODAY, "0", 0, "0");
        stubSales(TODAY.minusDays(1), "0", 0, "0");
        stubChannels(TODAY, "0", 0);
        stubDeliveries(Map.of());
        var counted = line(UUID.randomUUID(), "8", "0", "5");
        var matching = line(UUID.randomUUID(), "3", "2", "3");
        stubClosing(location(counted, matching));

        // Act
        var dashboard = sut.getDashboard();

        // Assert
        assertThat(dashboard.stockGap().units()).isEqualByComparingTo("-3");
        assertThat(dashboard.stockGap().articles()).isEqualTo(1);
    }

    @Test
    void getDashboard_sameArticleShortInTwoLocations_sumsUnitsButCountsTheArticleOnce() {
        // Arrange
        stubDay(DayStatus.OPEN);
        stubSales(TODAY, "0", 0, "0");
        stubSales(TODAY.minusDays(1), "0", 0, "0");
        stubChannels(TODAY, "0", 0);
        stubDeliveries(Map.of());
        var soap = UUID.randomUUID();
        stubClosing(location(line(soap, "10", "-2", null)), location(line(soap, "6", "3", null)));

        // Act
        var dashboard = sut.getDashboard();

        // Assert
        assertThat(dashboard.stockGap().units()).isEqualByComparingTo("1");
        assertThat(dashboard.stockGap().articles()).isEqualTo(1);
    }

    @Test
    void getDashboard_justAfterMidnightUtcPlusThree_readsTheNextBusinessDay() {
        // Arrange
        var tomorrow = TODAY.plusDays(1);
        sut = serviceAt(Instant.parse("2026-11-11T21:30:00Z"));
        when(businessDayService.findToday()).thenReturn(null);
        stubSales(tomorrow, "0", 0, "0");
        stubSales(TODAY, "0", 0, "0");
        stubChannels(tomorrow, "0", 0);
        when(deliveryDayService.countByStatus(tomorrow)).thenReturn(allStatuses(Map.of()));

        // Act
        var dashboard = sut.getDashboard();

        // Assert
        assertThat(dashboard.businessDate()).isEqualTo(tomorrow);
    }

    private DashboardServiceImpl serviceAt(Instant instant) {
        return new DashboardServiceImpl(
                businessDayService,
                reportSalesService,
                channelDashboardService,
                deliveryDayService,
                dayClosingService,
                Clock.fixed(instant, ZoneOffset.UTC));
    }

    private void stubDay(DayStatus status) {
        var day = new BusinessDayRDto(UUID.randomUUID(), TODAY, status, List.of(), NOW, null);
        when(businessDayService.findToday()).thenReturn(day);
    }

    private void stubSales(LocalDate day, String revenue, long count, String basket) {
        var period = ReportPeriod.containing(ReportGranularity.DAY, day);
        when(reportSalesService.getSales(new ReportFilter(period, null, null)))
                .thenReturn(new ReportSalesRDto(new BigDecimal(revenue), count, new BigDecimal(basket)));
    }

    private void stubChannels(LocalDate day, String revenue, long count) {
        var channel = new ChannelSalesRDto(STORE, "Boutique", new BigDecimal(revenue), count, new BigDecimal("100.0"));
        when(channelDashboardService.getDashboard(day, day))
                .thenReturn(new ChannelDashboardRDto(day, day, new BigDecimal(revenue), count, List.of(channel)));
    }

    private void stubDeliveries(Map<DeliveryStatus, Long> counts) {
        when(deliveryDayService.countByStatus(TODAY)).thenReturn(allStatuses(counts));
    }

    /** Like the real service: every status is present, at zero when no order has it. */
    private static Map<DeliveryStatus, Long> allStatuses(Map<DeliveryStatus, Long> counts) {
        var complete = new EnumMap<DeliveryStatus, Long>(DeliveryStatus.class);
        for (var status : DeliveryStatus.values()) {
            complete.put(status, counts.getOrDefault(status, 0L));
        }
        return complete;
    }

    private void stubClosing(ClosingLocationRDto... locations) {
        when(dayClosingService.getClosing())
                .thenReturn(new DayClosingRDto(UUID.randomUUID(), TODAY, DayStatus.OPEN, List.of(locations)));
    }

    private static ClosingLocationRDto location(ClosingLineRDto... lines) {
        return new ClosingLocationRDto(UUID.randomUUID(), "STORE", "Boutique", List.of(lines));
    }

    private static ClosingLineRDto line(UUID article, String theoretical, String gap, String counted) {
        var zero = BigDecimal.ZERO;
        return new ClosingLineRDto(
                article,
                "SAV-001",
                "Savon",
                zero,
                zero,
                zero,
                zero,
                zero,
                zero,
                zero,
                new BigDecimal(theoretical),
                new BigDecimal(theoretical),
                new BigDecimal(gap),
                counted == null ? null : new BigDecimal(counted),
                null);
    }
}
