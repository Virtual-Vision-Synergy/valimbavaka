package org.vvs.valimbavaka.feature.report.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.dto.ReportRangesRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportTrendPointRDto;
import org.vvs.valimbavaka.feature.report.service.ReportSalesService;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    /** 21:30 UTC on 30 November = 00:30 on 1 December in Madagascar (UTC+3). */
    private static final Clock JUST_AFTER_MIDNIGHT = Clock.fixed(Instant.parse("2026-11-30T21:30:00Z"), ZoneOffset.UTC);

    private static final ReportSalesRDto SALES =
            new ReportSalesRDto(new BigDecimal("1000.00"), 1, new BigDecimal("1000.00"));

    @Mock
    private ReportSalesService reportSalesService;

    private ReportServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ReportServiceImpl(reportSalesService, JUST_AFTER_MIDNIGHT);
    }

    @Test
    void getReport_withoutParameters_isTheCurrentMonthInMadagascarTime() {
        // Arrange
        when(reportSalesService.getSales(any(ReportFilter.class))).thenReturn(SALES);

        // Act
        var result = sut.getReport(null, null, null, null);

        // Assert
        assertThat(result.granularity()).isEqualTo(ReportGranularity.MONTH);
        assertThat(result.from()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(result.to()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(result.sales()).isEqualTo(SALES);
    }

    @Test
    void getReport_yearWithFilters_passesThePeriodAndFiltersToTheAggregate() {
        // Arrange
        var channel = UUID.randomUUID();
        var range = UUID.randomUUID();
        when(reportSalesService.getSales(any(ReportFilter.class))).thenReturn(SALES);

        // Act
        var result = sut.getReport(ReportGranularity.YEAR, LocalDate.of(2025, 6, 15), channel, range);

        // Assert
        var captor = ArgumentCaptor.forClass(ReportFilter.class);
        verify(reportSalesService, times(2)).getSales(captor.capture());
        var current = captor.getAllValues().get(0);
        assertThat(current.period().from()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(current.period().to()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(current.salesChannelId()).isEqualTo(channel);
        assertThat(current.rangeId()).isEqualTo(range);
        assertThat(result.salesChannelId()).isEqualTo(channel);
        assertThat(result.rangeId()).isEqualTo(range);
    }

    @Test
    void getReport_dayInTheFuture_throwsBusinessException() {
        // Act + Assert
        assertThatThrownBy(() -> sut.getReport(ReportGranularity.DAY, LocalDate.of(2026, 12, 2), null, null))
                .isInstanceOf(BusinessException.class);
        verify(reportSalesService, never()).getSales(any(ReportFilter.class));
    }

    @Test
    void getReport_monthAfterTheCurrentOne_throwsBusinessException() {
        // Act + Assert
        assertThatThrownBy(() -> sut.getReport(ReportGranularity.MONTH, LocalDate.of(2027, 1, 5), null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getReport_dayLaterInTheCurrentMonth_isAccepted() {
        // Arrange
        when(reportSalesService.getSales(any(ReportFilter.class))).thenReturn(SALES);

        // Act
        var result = sut.getReport(ReportGranularity.MONTH, LocalDate.of(2026, 12, 20), null, null);

        // Assert
        assertThat(result.from()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void getReport_month_comparesWithThePreviousMonthWithTheSameFilters() {
        // Arrange
        var channel = UUID.randomUUID();
        var range = UUID.randomUUID();
        when(reportSalesService.getSales(any(ReportFilter.class))).thenReturn(SALES);

        // Act
        var result = sut.getReport(ReportGranularity.MONTH, LocalDate.of(2026, 3, 10), channel, range);

        // Assert
        var captor = ArgumentCaptor.forClass(ReportFilter.class);
        verify(reportSalesService, times(2)).getSales(captor.capture());
        var previous = captor.getAllValues().get(1);
        assertThat(previous.period().from()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(previous.period().to()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(previous.salesChannelId()).isEqualTo(channel);
        assertThat(previous.rangeId()).isEqualTo(range);
        assertThat(result.comparison().from()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(result.comparison().to()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void getReport_risingIndicators_giveAPositivePercentageOfThePreviousValue() {
        // Arrange
        stubCurrentAndPrevious(
                new ReportSalesRDto(new BigDecimal("1200.00"), 6, new BigDecimal("200.00")),
                new ReportSalesRDto(new BigDecimal("1000.00"), 4, new BigDecimal("250.00")));

        // Act
        var result = sut.getReport(ReportGranularity.MONTH, LocalDate.of(2026, 11, 5), null, null);

        // Assert
        assertThat(result.comparison().sales().revenue()).isEqualByComparingTo("1000.00");
        assertThat(result.comparison().variation().revenue()).isEqualByComparingTo("20.0");
        assertThat(result.comparison().variation().salesCount()).isEqualByComparingTo("50.0");
        assertThat(result.comparison().variation().averageBasket()).isEqualByComparingTo("-20.0");
    }

    @Test
    void getReport_previousPeriodWithoutSale_hasNoPercentage() {
        // Arrange
        var empty = new ReportSalesRDto(new BigDecimal("0.00"), 0, new BigDecimal("0.00"));
        stubCurrentAndPrevious(SALES, empty);

        // Act
        var result = sut.getReport(ReportGranularity.MONTH, LocalDate.of(2026, 11, 5), null, null);

        // Assert
        assertThat(result.comparison().sales()).isEqualTo(empty);
        assertThat(result.comparison().variation().revenue()).isNull();
        assertThat(result.comparison().variation().salesCount()).isNull();
        assertThat(result.comparison().variation().averageBasket()).isNull();
    }

    @Test
    void getReport_bothPeriodsWithoutSale_hasNoPercentage() {
        // Arrange
        var empty = new ReportSalesRDto(new BigDecimal("0.00"), 0, new BigDecimal("0.00"));
        when(reportSalesService.getSales(any(ReportFilter.class))).thenReturn(empty);

        // Act
        var result = sut.getReport(ReportGranularity.MONTH, LocalDate.of(2026, 11, 5), null, null);

        // Assert
        assertThat(result.comparison().variation().revenue()).isNull();
    }

    @Test
    void getReport_salesStoppedAfterAPeriodWithSales_isMinusOneHundredPercent() {
        // Arrange
        var empty = new ReportSalesRDto(new BigDecimal("0.00"), 0, new BigDecimal("0.00"));
        stubCurrentAndPrevious(empty, SALES);

        // Act
        var result = sut.getReport(ReportGranularity.MONTH, LocalDate.of(2026, 11, 5), null, null);

        // Assert
        assertThat(result.comparison().variation().revenue()).isEqualByComparingTo("-100.0");
        assertThat(result.comparison().variation().salesCount()).isEqualByComparingTo("-100.0");
    }

    @Test
    void getReport_trend_isTheCurveOfThePeriodWithTheSameFilters() {
        // Arrange
        var channel = UUID.randomUUID();
        var range = UUID.randomUUID();
        var point = new ReportTrendPointRDto(LocalDate.of(2026, 3, 1), new BigDecimal("1000.00"));
        when(reportSalesService.getSales(any(ReportFilter.class))).thenReturn(SALES);
        when(reportSalesService.getTrend(any(ReportFilter.class))).thenReturn(List.of(point));

        // Act
        var result = sut.getReport(ReportGranularity.MONTH, LocalDate.of(2026, 3, 10), channel, range);

        // Assert
        var captor = ArgumentCaptor.forClass(ReportFilter.class);
        verify(reportSalesService).getTrend(captor.capture());
        assertThat(captor.getValue().period().from()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(captor.getValue().period().to()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(captor.getValue().salesChannelId()).isEqualTo(channel);
        assertThat(captor.getValue().rangeId()).isEqualTo(range);
        assertThat(result.trend()).containsExactly(point);
    }

    @Test
    void getReport_ranges_isTheSplitOfThePeriodWithTheSameFilters() {
        // Arrange
        var channel = UUID.randomUUID();
        var range = UUID.randomUUID();
        var ranges = new ReportRangesRDto(new BigDecimal("1000.00"), List.of());
        when(reportSalesService.getSales(any(ReportFilter.class))).thenReturn(SALES);
        when(reportSalesService.getRanges(any(ReportFilter.class))).thenReturn(ranges);

        // Act
        var result = sut.getReport(ReportGranularity.MONTH, LocalDate.of(2026, 3, 10), channel, range);

        // Assert
        var captor = ArgumentCaptor.forClass(ReportFilter.class);
        verify(reportSalesService).getRanges(captor.capture());
        assertThat(captor.getValue().period().from()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(captor.getValue().salesChannelId()).isEqualTo(channel);
        assertThat(result.ranges()).isSameAs(ranges);
    }

    @Test
    void getReport_dayInTheFuture_doesNotReadTheCurve() {
        // Act + Assert
        assertThatThrownBy(() -> sut.getReport(ReportGranularity.DAY, LocalDate.of(2026, 12, 2), null, null))
                .isInstanceOf(BusinessException.class);
        verify(reportSalesService, never()).getTrend(any(ReportFilter.class));
        verify(reportSalesService, never()).getRanges(any(ReportFilter.class));
    }

    private void stubCurrentAndPrevious(ReportSalesRDto current, ReportSalesRDto previous) {
        when(reportSalesService.getSales(any(ReportFilter.class))).thenReturn(current, previous);
    }
}
