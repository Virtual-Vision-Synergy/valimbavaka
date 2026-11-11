package org.vvs.valimbavaka.feature.dashboard.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.dashboard.dto.ChannelShareRDto;
import org.vvs.valimbavaka.feature.dashboard.dto.RangeShareRDto;
import org.vvs.valimbavaka.feature.dashboard.dto.RevenueSplitRDto;
import org.vvs.valimbavaka.feature.dashboard.service.RevenueSplitService;
import org.vvs.valimbavaka.feature.referential.dto.ProductRangeRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.domain.ReportGranularity;
import org.vvs.valimbavaka.feature.report.domain.ReportPeriod;
import org.vvs.valimbavaka.feature.report.service.ReportSalesService;
import org.vvs.valimbavaka.feature.sale.dto.ChannelSalesRDto;
import org.vvs.valimbavaka.feature.sale.service.ChannelDashboardService;
import org.vvs.valimbavaka.shared.dto.PageQuery;

@Service
@Transactional(readOnly = true)
public class RevenueSplitServiceImpl implements RevenueSplitService {

    private static final PageQuery ALL_RANGES = new PageQuery(1, 100, null, "code", null);

    private final ReportSalesService reportSalesService;
    private final ChannelDashboardService channelDashboardService;
    private final ReferentialService referentialService;
    private final Clock clock;

    RevenueSplitServiceImpl(
            ReportSalesService reportSalesService,
            ChannelDashboardService channelDashboardService,
            ReferentialService referentialService,
            Clock clock) {
        this.reportSalesService = reportSalesService;
        this.channelDashboardService = channelDashboardService;
        this.referentialService = referentialService;
        this.clock = clock;
    }

    @Override
    public RevenueSplitRDto getToday() {
        var today = LocalDate.now(clock.withZone(DashboardServiceImpl.BUSINESS_ZONE));
        return new RevenueSplitRDto(today, byRange(today), byChannel(today));
    }

    /** One read of the sales report per active range (a handful), restricted to that range. */
    private List<RangeShareRDto> byRange(LocalDate day) {
        var period = ReportPeriod.containing(ReportGranularity.DAY, day);
        var ranges = referentialService.getProductRanges(ALL_RANGES).items();
        var revenues = ranges.stream().map(range -> revenueOf(period, range.id())).toList();
        var shares = PercentageShares.of(revenues);
        return IntStream.range(0, ranges.size())
                .mapToObj(i -> toRange(ranges.get(i), revenues.get(i), shares.get(i)))
                .sorted(Comparator.comparing(RangeShareRDto::revenue)
                        .reversed()
                        .thenComparing(RangeShareRDto::label))
                .toList();
    }

    private BigDecimal revenueOf(ReportPeriod period, UUID rangeId) {
        return reportSalesService.getSales(new ReportFilter(period, null, rangeId)).revenue();
    }

    private static RangeShareRDto toRange(ProductRangeRDto range, BigDecimal revenue, BigDecimal share) {
        return new RangeShareRDto(range.id(), range.code(), range.label(), revenue, share);
    }

    /** Channels of the day (the channel dashboard keeps its order: best turnover first), shares re-balanced to 100. */
    private List<ChannelShareRDto> byChannel(LocalDate day) {
        var channels = channelDashboardService.getDashboard(day, day).channels();
        var revenues = channels.stream().map(ChannelSalesRDto::revenue).toList();
        var shares = PercentageShares.of(revenues);
        return IntStream.range(0, channels.size())
                .mapToObj(i -> toChannel(channels.get(i), shares.get(i)))
                .toList();
    }

    private static ChannelShareRDto toChannel(ChannelSalesRDto channel, BigDecimal share) {
        return new ChannelShareRDto(channel.salesChannelId(), channel.salesChannelLabel(), channel.revenue(), share);
    }
}
