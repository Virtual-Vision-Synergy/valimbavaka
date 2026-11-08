package org.vvs.valimbavaka.feature.dashboard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.dashboard.dto.RevenuePointRDto;
import org.vvs.valimbavaka.feature.sale.dto.DailySalesRevenueRDto;
import org.vvs.valimbavaka.feature.sale.service.SaleRevenueService;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordRDto;
import org.vvs.valimbavaka.feature.salonservice.service.ServiceRecordService;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    /** Sunday 8 November 2026, 10:00 in Antananarivo (UTC+3). */
    private static final Instant NOW = Instant.parse("2026-11-08T07:00:00Z");

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    /** Monday of the current week minus 3 weeks: first day of the 4-week window. */
    private static final LocalDate FIRST_MONDAY = LocalDate.of(2026, 10, 12);

    @Mock
    private SaleRevenueService saleRevenueService;

    @Mock
    private ServiceRecordService serviceRecordService;

    private DashboardServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new DashboardServiceImpl(saleRevenueService, serviceRecordService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void getRevenueTrend_noSaleNorService_returnsSevenDaysAndFourWeeksAtZero() {
        // Arrange
        when(saleRevenueService.getDailyRevenue(any(), any())).thenReturn(List.of());
        when(serviceRecordService.getServiceRecordsBetween(any(), any())).thenReturn(List.of());

        // Act
        var trend = sut.getRevenueTrend();

        // Assert
        assertThat(trend.today()).isEqualTo(TODAY);
        assertThat(trend.last7Days())
                .extracting(RevenuePointRDto::date)
                .containsExactly(
                        LocalDate.of(2026, 11, 2),
                        LocalDate.of(2026, 11, 3),
                        LocalDate.of(2026, 11, 4),
                        LocalDate.of(2026, 11, 5),
                        LocalDate.of(2026, 11, 6),
                        LocalDate.of(2026, 11, 7),
                        TODAY);
        assertThat(trend.last7Days()).extracting(RevenuePointRDto::amount).allMatch(amount -> amount.signum() == 0);
        assertThat(trend.last4Weeks()).extracting(RevenuePointRDto::amount).allMatch(amount -> amount.signum() == 0);
    }

    @Test
    void getRevenueTrend_salesAndServices_sumsBothSourcesPerDay() {
        // Arrange
        when(saleRevenueService.getDailyRevenue(FIRST_MONDAY, TODAY))
                .thenReturn(List.of(
                        new DailySalesRevenueRDto(LocalDate.of(2026, 11, 6), new BigDecimal("125000.00")),
                        new DailySalesRevenueRDto(TODAY, new BigDecimal("64000.00"))));
        when(serviceRecordService.getServiceRecordsBetween(any(), any()))
                .thenReturn(List.of(service("2026-11-08T06:15:00Z", "15000.00")));

        // Act
        var trend = sut.getRevenueTrend();

        // Assert
        assertThat(trend.last7Days().get(4).amount()).isEqualByComparingTo("125000.00");
        assertThat(trend.last7Days().get(5).amount()).isEqualByComparingTo("0");
        assertThat(trend.last7Days().get(6).amount()).isEqualByComparingTo("79000.00");
    }

    @Test
    void getRevenueTrend_serviceJustAfterLocalMidnight_countsOnItsLocalDay() {
        // Arrange: 21:30 UTC on the 7th is 00:30 on the 8th in Antananarivo
        when(saleRevenueService.getDailyRevenue(any(), any())).thenReturn(List.of());
        when(serviceRecordService.getServiceRecordsBetween(any(), any()))
                .thenReturn(List.of(service("2026-11-07T21:30:00Z", "20000.00")));

        // Act
        var trend = sut.getRevenueTrend();

        // Assert
        assertThat(trend.last7Days().get(5).amount()).isEqualByComparingTo("0");
        assertThat(trend.last7Days().get(6).amount()).isEqualByComparingTo("20000.00");
    }

    @Test
    void getRevenueTrend_lastFourWeeks_groupsMondayToSundayWithTheCurrentWeekLast() {
        // Arrange
        when(saleRevenueService.getDailyRevenue(FIRST_MONDAY, TODAY))
                .thenReturn(List.of(
                        new DailySalesRevenueRDto(FIRST_MONDAY, new BigDecimal("10000.00")),
                        new DailySalesRevenueRDto(LocalDate.of(2026, 10, 18), new BigDecimal("5000.00")),
                        new DailySalesRevenueRDto(LocalDate.of(2026, 10, 19), new BigDecimal("7000.00")),
                        new DailySalesRevenueRDto(TODAY, new BigDecimal("3000.00"))));
        when(serviceRecordService.getServiceRecordsBetween(any(), any())).thenReturn(List.of());

        // Act
        var trend = sut.getRevenueTrend();

        // Assert
        var currentMonday = LocalDate.of(2026, 11, 2);
        assertThat(trend.last4Weeks())
                .extracting(RevenuePointRDto::date)
                .containsExactly(FIRST_MONDAY, FIRST_MONDAY.plusWeeks(1), FIRST_MONDAY.plusWeeks(2), currentMonday);
        assertThat(trend.last4Weeks())
                .extracting(point -> point.amount().stripTrailingZeros().toPlainString())
                .containsExactly("15000", "7000", "0", "3000");
    }

    @Test
    void getRevenueTrend_readsServicesFromTheFirstLocalMondayToTheEndOfToday() {
        // Arrange
        when(saleRevenueService.getDailyRevenue(any(), any())).thenReturn(List.of());
        when(serviceRecordService.getServiceRecordsBetween(any(), any())).thenReturn(List.of());

        // Act
        sut.getRevenueTrend();

        // Assert: local midnights in Antananarivo (UTC+3)
        verify(saleRevenueService).getDailyRevenue(FIRST_MONDAY, TODAY);
        var from = Instant.parse("2026-10-11T21:00:00Z");
        var to = Instant.parse("2026-11-08T21:00:00Z");
        verify(serviceRecordService).getServiceRecordsBetween(from, to);
    }

    private static ServiceRecordRDto service(String performedAt, String price) {
        return new ServiceRecordRDto(
                UUID.randomUUID(),
                "PRE-0001",
                UUID.randomUUID(),
                "Soin du visage",
                45,
                new BigDecimal(price),
                null,
                UUID.randomUUID(),
                "Miora Rakoto",
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(),
                null,
                Instant.parse(performedAt));
    }
}
