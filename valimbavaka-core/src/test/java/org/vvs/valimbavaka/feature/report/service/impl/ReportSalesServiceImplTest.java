package org.vvs.valimbavaka.feature.report.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.domain.ReportPeriod;
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
}
