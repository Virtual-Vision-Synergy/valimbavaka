package org.vvs.valimbavaka.feature.sale.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.sale.dto.ChannelSalesRDto;
import org.vvs.valimbavaka.feature.sale.repository.ChannelSalesTotal;
import org.vvs.valimbavaka.feature.sale.repository.SaleQueryRepository;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@ExtendWith(MockitoExtension.class)
class ChannelDashboardServiceImplTest {

    /** 21:30 UTC on 30 November = 00:30 on 1 December in Madagascar (UTC+3). */
    private static final Clock JUST_AFTER_MIDNIGHT = Clock.fixed(Instant.parse("2026-11-30T21:30:00Z"), ZoneOffset.UTC);

    private static final LocalDate FIRST_OF_DECEMBER = LocalDate.of(2026, 12, 1);
    private static final UUID STORE = UUID.randomUUID();
    private static final UUID FACEBOOK = UUID.randomUUID();
    private static final UUID SALON = UUID.randomUUID();
    private static final UUID RETIRED = UUID.randomUUID();

    @Mock
    private SaleQueryRepository saleQueryRepository;

    @Mock
    private ReferentialService referentialService;

    private ChannelDashboardServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ChannelDashboardServiceImpl(saleQueryRepository, referentialService, JUST_AFTER_MIDNIGHT);
        var channels = List.of(
                new SalesChannelRDto(STORE, "STORE", "Boutique"),
                new SalesChannelRDto(FACEBOOK, "FACEBOOK", "Facebook"),
                new SalesChannelRDto(SALON, "SALON", "Salon"));
        lenient()
                .when(referentialService.getSalesChannels(any(PageQuery.class)))
                .thenReturn(new PageResponse<>(channels, 1, 100, 3, 1));
    }

    @Test
    void getDashboard_withoutDates_readsTheMonthToDateInMadagascarTime() {
        // Arrange
        when(saleQueryRepository.totalsByChannel(FIRST_OF_DECEMBER, FIRST_OF_DECEMBER, OperationStatus.VALIDATED))
                .thenReturn(List.of());

        // Act
        var result = sut.getDashboard(null, null);

        // Assert
        assertThat(result.from()).isEqualTo(FIRST_OF_DECEMBER);
        assertThat(result.to()).isEqualTo(FIRST_OF_DECEMBER);
    }

    @Test
    void getDashboard_threeChannels_computesTheShareOfTheTotalAndSortsByRevenue() {
        // Arrange
        var from = LocalDate.of(2026, 11, 1);
        var to = LocalDate.of(2026, 11, 30);
        when(saleQueryRepository.totalsByChannel(from, to, OperationStatus.VALIDATED))
                .thenReturn(List.of(
                        new ChannelSalesTotal(FACEBOOK, new BigDecimal("25000.00"), 2),
                        new ChannelSalesTotal(STORE, new BigDecimal("75000.00"), 5)));

        // Act
        var result = sut.getDashboard(from, to);

        // Assert
        assertThat(result.totalRevenue()).isEqualByComparingTo("100000");
        assertThat(result.totalSalesCount()).isEqualTo(7);
        assertThat(result.channels())
                .extracting(ChannelSalesRDto::salesChannelLabel)
                .containsExactly("Boutique", "Facebook", "Salon");
        assertThat(result.channels())
                .extracting(ChannelSalesRDto::revenueSharePercent)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("75.0"), new BigDecimal("25.0"), BigDecimal.ZERO);
    }

    @Test
    void getDashboard_noSaleAtAll_listsEveryActiveChannelAtZeroWithoutDividingByZero() {
        // Arrange
        var day = LocalDate.of(2026, 11, 2);
        when(saleQueryRepository.totalsByChannel(day, day, OperationStatus.VALIDATED))
                .thenReturn(List.of());

        // Act
        var result = sut.getDashboard(day, day);

        // Assert
        assertThat(result.totalRevenue()).isZero();
        assertThat(result.totalSalesCount()).isZero();
        assertThat(result.channels()).hasSize(3).allSatisfy(channel -> {
            assertThat(channel.revenue()).isZero();
            assertThat(channel.salesCount()).isZero();
            assertThat(channel.revenueSharePercent()).isEqualByComparingTo("0");
        });
    }

    @Test
    void getDashboard_oneThirdShare_roundsToOneDecimal() {
        // Arrange
        var day = LocalDate.of(2026, 11, 2);
        when(saleQueryRepository.totalsByChannel(day, day, OperationStatus.VALIDATED))
                .thenReturn(List.of(
                        new ChannelSalesTotal(STORE, new BigDecimal("2000.00"), 1),
                        new ChannelSalesTotal(FACEBOOK, new BigDecimal("1000.00"), 1)));

        // Act
        var result = sut.getDashboard(day, day);

        // Assert
        assertThat(result.channels().get(1).revenueSharePercent()).isEqualByComparingTo("33.3");
    }

    @Test
    void getDashboard_deactivatedChannelWithSales_staysListedWithoutLabel() {
        // Arrange
        var day = LocalDate.of(2026, 11, 2);
        when(saleQueryRepository.totalsByChannel(day, day, OperationStatus.VALIDATED))
                .thenReturn(List.of(new ChannelSalesTotal(RETIRED, new BigDecimal("500.00"), 1)));

        // Act
        var result = sut.getDashboard(day, day);

        // Assert
        assertThat(result.channels()).hasSize(4);
        assertThat(result.channels().get(0).salesChannelId()).isEqualTo(RETIRED);
        assertThat(result.channels().get(0).salesChannelLabel()).isNull();
    }

    @Test
    void getDashboard_fromAfterTo_throwsBusinessException() {
        // Arrange
        var from = LocalDate.of(2026, 11, 3);
        var to = LocalDate.of(2026, 11, 2);

        // Act + Assert
        assertThatThrownBy(() -> sut.getDashboard(from, to))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("date de début");
    }
}
