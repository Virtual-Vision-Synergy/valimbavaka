package org.vvs.valimbavaka.feature.report.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;
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
        verify(reportSalesService).getSales(captor.capture());
        assertThat(captor.getValue().period().from()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(captor.getValue().period().to()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(captor.getValue().salesChannelId()).isEqualTo(channel);
        assertThat(captor.getValue().rangeId()).isEqualTo(range);
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
}
