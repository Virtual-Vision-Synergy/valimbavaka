package org.vvs.valimbavaka.feature.dashboard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.dashboard.dto.RevenueTrendPointRDto;
import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.domain.ReportPeriod;
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;
import org.vvs.valimbavaka.feature.report.service.ReportSalesService;

@ExtendWith(MockitoExtension.class)
class RevenueTrendServiceImplTest {

    /** 13:00 in Antananarivo (UTC+3), 2026-11-11. */
    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

    @Mock
    private ReportSalesService reportSalesService;

    @Test
    void getLastSevenDays_withSales_returnsOnePointPerDayOldestFirstEndingToday() {
        // Arrange
        var sut = serviceAt(NOW);
        stubSales(TODAY.minusDays(6), "1000", 1);
        stubSales(TODAY.minusDays(5), "0", 0);
        stubSales(TODAY.minusDays(4), "2500", 2);
        stubSales(TODAY.minusDays(3), "0", 0);
        stubSales(TODAY.minusDays(2), "0", 0);
        stubSales(TODAY.minusDays(1), "4000", 4);
        stubSales(TODAY, "15500", 3);

        // Act
        var trend = sut.getLastSevenDays();

        // Assert
        assertThat(trend.from()).isEqualTo(TODAY.minusDays(6));
        assertThat(trend.to()).isEqualTo(TODAY);
        assertThat(trend.points())
                .extracting(RevenueTrendPointRDto::date)
                .hasSize(7)
                .isSorted();
        assertThat(trend.points().getFirst().revenue()).isEqualByComparingTo("1000");
        assertThat(trend.points().get(1).revenue()).isEqualByComparingTo("0");
        assertThat(trend.points().get(1).salesCount()).isZero();
        assertThat(trend.points().getLast().date()).isEqualTo(TODAY);
        assertThat(trend.points().getLast().revenue()).isEqualByComparingTo("15500");
        assertThat(trend.points().getLast().salesCount()).isEqualTo(3);
    }

    @Test
    void getLastSevenDays_noSaleAtAll_returnsSevenPointsAtZero() {
        // Arrange
        var sut = serviceAt(NOW);
        when(reportSalesService.getSales(any(ReportFilter.class)))
                .thenReturn(new ReportSalesRDto(BigDecimal.ZERO, 0, BigDecimal.ZERO));

        // Act
        var trend = sut.getLastSevenDays();

        // Assert
        assertThat(trend.points()).hasSize(7);
        assertThat(trend.points()).allSatisfy(point -> {
            assertThat(point.revenue()).isEqualByComparingTo("0");
            assertThat(point.salesCount()).isZero();
        });
        verify(reportSalesService, times(7)).getSales(any(ReportFilter.class));
    }

    @Test
    void getLastSevenDays_lateEveningUtc_usesTheAntananarivoDay() {
        // Arrange: 21:30 UTC on 11 November is 00:30 on 12 November in Antananarivo.
        var sut = serviceAt(Instant.parse("2026-11-11T21:30:00Z"));
        when(reportSalesService.getSales(any(ReportFilter.class)))
                .thenReturn(new ReportSalesRDto(BigDecimal.ZERO, 0, BigDecimal.ZERO));

        // Act
        var trend = sut.getLastSevenDays();

        // Assert
        assertThat(trend.to()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(trend.from()).isEqualTo(LocalDate.of(2026, 11, 6));
    }

    @Test
    void getLastSevenDays_monthBoundary_crossesTheMonthWithoutGap() {
        // Arrange
        var sut = serviceAt(Instant.parse("2026-03-03T09:00:00Z"));
        when(reportSalesService.getSales(any(ReportFilter.class)))
                .thenReturn(new ReportSalesRDto(BigDecimal.ZERO, 0, BigDecimal.ZERO));

        // Act
        var trend = sut.getLastSevenDays();

        // Assert
        assertThat(trend.points())
                .extracting(RevenueTrendPointRDto::date)
                .containsExactly(
                        LocalDate.of(2026, 2, 25),
                        LocalDate.of(2026, 2, 26),
                        LocalDate.of(2026, 2, 27),
                        LocalDate.of(2026, 2, 28),
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 2),
                        LocalDate.of(2026, 3, 3));
    }

    private RevenueTrendServiceImpl serviceAt(Instant instant) {
        return new RevenueTrendServiceImpl(reportSalesService, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private void stubSales(LocalDate day, String revenue, long count) {
        var period = ReportPeriod.containing(ReportGranularity.DAY, day);
        when(reportSalesService.getSales(new ReportFilter(period, null, null)))
                .thenReturn(new ReportSalesRDto(new BigDecimal(revenue), count, BigDecimal.ZERO));
    }
}
