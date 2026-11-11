package org.vvs.valimbavaka.feature.dashboard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.feature.dashboard.dto.ChannelShareRDto;
import org.vvs.valimbavaka.feature.dashboard.dto.RangeShareRDto;
import org.vvs.valimbavaka.feature.referential.dto.ProductRangeRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.domain.ReportPeriod;
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;
import org.vvs.valimbavaka.feature.report.service.ReportSalesService;
import org.vvs.valimbavaka.feature.sale.dto.ChannelDashboardRDto;
import org.vvs.valimbavaka.feature.sale.dto.ChannelSalesRDto;
import org.vvs.valimbavaka.feature.sale.service.ChannelDashboardService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class RevenueSplitServiceImplTest {

    /** 00:30 on 2026-11-12 in Antananarivo (UTC+3), still the 11th in UTC. */
    private static final Instant NOW = Instant.parse("2026-11-11T21:30:00Z");

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    private static final ProductRangeRDto BOTANIKA = new ProductRangeRDto(UUID.randomUUID(), "BOTANIKA", "Botanika");
    private static final ProductRangeRDto ZEZIKA = new ProductRangeRDto(UUID.randomUUID(), "ZEZIKA", "Zezika");
    private static final ProductRangeRDto SOLIKA = new ProductRangeRDto(UUID.randomUUID(), "SOLIKA", "Solika");

    @Mock
    private ReportSalesService reportSalesService;

    @Mock
    private ChannelDashboardService channelDashboardService;

    @Mock
    private ReferentialService referentialService;

    @Test
    void getToday_withSales_returnsRangesBestFirstWithSharesAddingUpToOneHundred() {
        // Arrange
        var sut = serviceAt(NOW);
        stubRanges(BOTANIKA, ZEZIKA, SOLIKA);
        stubRangeRevenue(BOTANIKA, "286000");
        stubRangeRevenue(ZEZIKA, "562000");
        stubRangeRevenue(SOLIKA, "436000");
        stubChannels();

        // Act
        var split = sut.getToday();

        // Assert
        assertThat(split.date()).isEqualTo(TODAY);
        assertThat(split.byRange()).extracting(RangeShareRDto::code).containsExactly("ZEZIKA", "SOLIKA", "BOTANIKA");
        assertThat(split.byRange())
                .extracting(RangeShareRDto::sharePercent)
                .containsExactly(new BigDecimal("43.8"), new BigDecimal("33.9"), new BigDecimal("22.3"));
        assertThat(split.byRange().getFirst().revenue()).isEqualByComparingTo("562000");
    }

    @Test
    void getToday_withSales_returnsChannelsInTheOrderOfTheChannelDashboardWithBalancedShares() {
        // Arrange
        var sut = serviceAt(NOW);
        stubRanges(BOTANIKA);
        stubRangeRevenue(BOTANIKA, "0");
        stubChannels(channel("Boutique", "100"), channel("Facebook", "100"), channel(null, "100"));

        // Act
        var split = sut.getToday();

        // Assert
        assertThat(split.byChannel()).extracting(ChannelShareRDto::label).containsExactly("Boutique", "Facebook", null);
        assertThat(split.byChannel())
                .extracting(ChannelShareRDto::sharePercent)
                .containsExactly(new BigDecimal("33.4"), new BigDecimal("33.3"), new BigDecimal("33.3"));
    }

    @Test
    void getToday_noSaleAtAll_returnsEveryRangeAndChannelAtZero() {
        // Arrange
        var sut = serviceAt(NOW);
        stubRanges(BOTANIKA, ZEZIKA);
        when(reportSalesService.getSales(any(ReportFilter.class)))
                .thenReturn(new ReportSalesRDto(BigDecimal.ZERO, 0, BigDecimal.ZERO));
        var store = new ChannelSalesRDto(UUID.randomUUID(), "Boutique", BigDecimal.ZERO, 0, BigDecimal.ZERO);
        stubChannels(store);

        // Act
        var split = sut.getToday();

        // Assert
        assertThat(split.byRange()).hasSize(2);
        assertThat(split.byRange()).allSatisfy(range -> {
            assertThat(range.revenue()).isEqualByComparingTo("0");
            assertThat(range.sharePercent()).isEqualByComparingTo("0.0");
        });
        assertThat(split.byChannel()).singleElement().satisfies(channel -> {
            assertThat(channel.revenue()).isEqualByComparingTo("0");
            assertThat(channel.sharePercent()).isEqualByComparingTo("0.0");
        });
    }

    @Test
    void getToday_noActiveRange_returnsAnEmptyRangeList() {
        // Arrange
        var sut = serviceAt(NOW);
        stubRanges();
        stubChannels();

        // Act
        var split = sut.getToday();

        // Assert
        assertThat(split.byRange()).isEmpty();
        assertThat(split.byChannel()).isEmpty();
    }

    @Test
    void getToday_lateEveningUtc_readsTheAntananarivoDay() {
        // Arrange
        var sut = serviceAt(NOW);
        stubRanges(BOTANIKA);
        stubRangeRevenue(BOTANIKA, "8500");
        stubChannels();

        // Act
        var split = sut.getToday();

        // Assert: the range was read for the 12th (stubbed for that day only), not for the 11th
        assertThat(split.date()).isEqualTo(TODAY);
        assertThat(split.byRange().getFirst().revenue()).isEqualByComparingTo("8500");
        assertThat(split.byRange().getFirst().sharePercent()).isEqualByComparingTo("100.0");
    }

    private RevenueSplitServiceImpl serviceAt(Instant instant) {
        return new RevenueSplitServiceImpl(
                reportSalesService, channelDashboardService, referentialService, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private void stubRanges(ProductRangeRDto... ranges) {
        var items = List.of(ranges);
        when(referentialService.getProductRanges(any(PageQuery.class)))
                .thenReturn(new PageResponse<>(items, 1, 100, items.size(), 1));
    }

    private void stubRangeRevenue(ProductRangeRDto range, String revenue) {
        var period = ReportPeriod.containing(ReportGranularity.DAY, TODAY);
        when(reportSalesService.getSales(new ReportFilter(period, null, range.id())))
                .thenReturn(new ReportSalesRDto(new BigDecimal(revenue), 1, BigDecimal.ZERO));
    }

    private static ChannelSalesRDto channel(@Nullable String label, String revenue) {
        return new ChannelSalesRDto(UUID.randomUUID(), label, new BigDecimal(revenue), 1, new BigDecimal("33.3"));
    }

    private void stubChannels(ChannelSalesRDto... channels) {
        when(channelDashboardService.getDashboard(TODAY, TODAY))
                .thenReturn(new ChannelDashboardRDto(TODAY, TODAY, BigDecimal.ZERO, 0, List.of(channels)));
    }
}
