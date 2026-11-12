package org.vvs.valimbavaka.feature.report.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.domain.ReportPeriod;
import org.vvs.valimbavaka.feature.report.repository.DailyRevenue;
import org.vvs.valimbavaka.feature.report.repository.ReportSaleRepository;
import org.vvs.valimbavaka.feature.report.repository.SalesTotal;

@ExtendWith(MockitoExtension.class)
class ReportSalesServiceImplTest {

    private static final ReportPeriod NOVEMBER =
            ReportPeriod.containing(ReportGranularity.MONTH, LocalDate.of(2026, 11, 11));
    private static final LocalDate FIRST = LocalDate.of(2026, 11, 1);
    private static final LocalDate LAST = LocalDate.of(2026, 11, 30);
    private static final UUID STORE = UUID.randomUUID();
    private static final UUID BOTANIKA = UUID.randomUUID();

    @Mock
    private ReportSaleRepository reportSaleRepository;

    private ReportSalesServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ReportSalesServiceImpl(reportSaleRepository);
    }

    @Test
    void getSales_withoutRange_readsTheTotalsOfValidatedSalesOfThePeriod() {
        // Arrange
        when(reportSaleRepository.totals(FIRST, LAST, STORE)).thenReturn(new SalesTotal(new BigDecimal("30000.00"), 3));

        // Act
        var result = sut.getSales(new ReportFilter(NOVEMBER, STORE, null));

        // Assert
        assertThat(result.revenue()).isEqualByComparingTo("30000");
        assertThat(result.salesCount()).isEqualTo(3);
        assertThat(result.averageBasket()).isEqualByComparingTo("10000");
        verify(reportSaleRepository).totals(FIRST, LAST, STORE);
        verifyNoMoreInteractions(reportSaleRepository);
    }

    @Test
    void getSales_withRange_readsTheTotalsOfThatRange() {
        // Arrange
        when(reportSaleRepository.totalsByRange(FIRST, LAST, null, BOTANIKA))
                .thenReturn(new SalesTotal(new BigDecimal("17000.00"), 2));

        // Act
        var result = sut.getSales(new ReportFilter(NOVEMBER, null, BOTANIKA));

        // Assert
        assertThat(result.revenue()).isEqualByComparingTo("17000");
        assertThat(result.salesCount()).isEqualTo(2);
        verifyNoMoreInteractions(reportSaleRepository);
    }

    @Test
    void getSales_noSale_returnsZerosWithoutDividingByZero() {
        // Arrange
        when(reportSaleRepository.totals(FIRST, LAST, null)).thenReturn(new SalesTotal(null, 0));

        // Act
        var result = sut.getSales(new ReportFilter(NOVEMBER, null, null));

        // Assert
        assertThat(result.revenue()).isEqualByComparingTo("0");
        assertThat(result.salesCount()).isZero();
        assertThat(result.averageBasket()).isEqualByComparingTo("0");
    }

    @Test
    void getSales_basketNotRound_isRoundedToTheCent() {
        // Arrange
        when(reportSaleRepository.totals(FIRST, LAST, null)).thenReturn(new SalesTotal(new BigDecimal("10000.00"), 3));

        // Act
        var result = sut.getSales(new ReportFilter(NOVEMBER, null, null));

        // Assert
        assertThat(result.averageBasket()).isEqualByComparingTo("3333.33");
    }

    @Test
    void getTrend_month_hasOnePointPerDayAndZeroForDaysWithoutSale() {
        // Arrange
        var day3 = LocalDate.of(2026, 11, 3);
        var day20 = LocalDate.of(2026, 11, 20);
        when(reportSaleRepository.dailyRevenue(FIRST, LAST, STORE))
                .thenReturn(List.of(
                        new DailyRevenue(day3, new BigDecimal("8500.00")),
                        new DailyRevenue(day20, new BigDecimal("17000.00"))));

        // Act
        var result = sut.getTrend(new ReportFilter(NOVEMBER, STORE, null));

        // Assert
        assertThat(result).hasSize(30);
        assertThat(result.get(0).date()).isEqualTo(FIRST);
        assertThat(result.get(0).revenue()).isEqualByComparingTo("0");
        assertThat(result.get(2).date()).isEqualTo(day3);
        assertThat(result.get(2).revenue()).isEqualByComparingTo("8500");
        assertThat(result.get(19).revenue()).isEqualByComparingTo("17000");
        assertThat(result.get(29).date()).isEqualTo(LAST);
        verifyNoMoreInteractions(reportSaleRepository);
    }

    @Test
    void getTrend_year_sumsTheDaysOfEachMonthIntoOnePoint() {
        // Arrange
        var year = ReportPeriod.containing(ReportGranularity.YEAR, LocalDate.of(2026, 6, 1));
        when(reportSaleRepository.dailyRevenue(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null))
                .thenReturn(List.of(
                        new DailyRevenue(LocalDate.of(2026, 3, 2), new BigDecimal("1000.00")),
                        new DailyRevenue(LocalDate.of(2026, 3, 30), new BigDecimal("2500.50")),
                        new DailyRevenue(LocalDate.of(2026, 12, 31), new BigDecimal("700.00"))));

        // Act
        var result = sut.getTrend(new ReportFilter(year, null, null));

        // Assert
        assertThat(result).hasSize(12);
        assertThat(result.get(2).date()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(result.get(2).revenue()).isEqualByComparingTo("3500.50");
        assertThat(result.get(11).revenue()).isEqualByComparingTo("700");
        assertThat(result.get(0).revenue()).isEqualByComparingTo("0");
    }

    @Test
    void getTrend_day_isASinglePoint() {
        // Arrange
        var day = ReportPeriod.containing(ReportGranularity.DAY, LocalDate.of(2026, 11, 11));
        when(reportSaleRepository.dailyRevenue(day.from(), day.to(), null)).thenReturn(List.of());

        // Act
        var result = sut.getTrend(new ReportFilter(day, null, null));

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).revenue()).isEqualByComparingTo("0");
    }

    @Test
    void getTrend_withRange_readsTheDailyRevenueOfThatRange() {
        // Arrange
        when(reportSaleRepository.dailyRevenueByRange(FIRST, LAST, STORE, BOTANIKA))
                .thenReturn(List.of(new DailyRevenue(FIRST, new BigDecimal("4250.00"))));

        // Act
        var result = sut.getTrend(new ReportFilter(NOVEMBER, STORE, BOTANIKA));

        // Assert
        assertThat(result.get(0).revenue()).isEqualByComparingTo("4250");
        verify(reportSaleRepository).dailyRevenueByRange(FIRST, LAST, STORE, BOTANIKA);
        verifyNoMoreInteractions(reportSaleRepository);
    }
}
