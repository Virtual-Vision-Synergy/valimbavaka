package org.vvs.valimbavaka.feature.sale.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.sale.dto.ChannelDashboardRDto;
import org.vvs.valimbavaka.feature.sale.dto.ChannelSalesRDto;
import org.vvs.valimbavaka.feature.sale.repository.ChannelSalesTotal;
import org.vvs.valimbavaka.feature.sale.repository.SaleQueryRepository;
import org.vvs.valimbavaka.feature.sale.service.ChannelDashboardService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

@Service
@Transactional(readOnly = true)
public class ChannelDashboardServiceImpl implements ChannelDashboardService {

    private static final PageQuery ALL_CHANNELS = new PageQuery(1, 100, null, "code", null);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final Comparator<ChannelSalesRDto> BEST_REVENUE_FIRST = Comparator.comparing(
                    ChannelSalesRDto::revenue)
            .reversed()
            .thenComparing(ChannelSalesRDto::salesChannelLabel, Comparator.nullsLast(Comparator.naturalOrder()));

    private final SaleQueryRepository saleQueryRepository;
    private final ReferentialService referentialService;
    private final Clock clock;

    ChannelDashboardServiceImpl(
            SaleQueryRepository saleQueryRepository, ReferentialService referentialService, Clock clock) {
        this.saleQueryRepository = saleQueryRepository;
        this.referentialService = referentialService;
        this.clock = clock;
    }

    @Override
    public ChannelDashboardRDto getDashboard(@Nullable LocalDate from, @Nullable LocalDate to) {
        var last = to != null ? to : LocalDate.now(clock.withZone(SaleQueryServiceImpl.BUSINESS_ZONE));
        var first = from != null ? from : last.withDayOfMonth(1);
        if (first.isAfter(last)) {
            throw new BusinessException("La date de début doit précéder la date de fin.");
        }
        var totals = saleQueryRepository.totalsByChannel(first, last, OperationStatus.VALIDATED).stream()
                .collect(Collectors.toMap(ChannelSalesTotal::salesChannelId, Function.identity()));
        var totalRevenue =
                totals.values().stream().map(ChannelSalesTotal::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalCount = totals.values().stream()
                .mapToLong(ChannelSalesTotal::salesCount)
                .sum();
        return new ChannelDashboardRDto(first, last, totalRevenue, totalCount, channels(totals, totalRevenue));
    }

    /** Active channels (at zero when they sold nothing) and the channels deactivated since that did sell. */
    private List<ChannelSalesRDto> channels(Map<UUID, ChannelSalesTotal> totals, BigDecimal totalRevenue) {
        var labels = referentialService.getSalesChannels(ALL_CHANNELS).items().stream()
                .collect(Collectors.toMap(SalesChannelRDto::id, SalesChannelRDto::label));
        var ids = new LinkedHashSet<>(labels.keySet());
        ids.addAll(totals.keySet());
        return ids.stream()
                .map(id -> toRDto(id, labels.get(id), totals.get(id), totalRevenue))
                .sorted(BEST_REVENUE_FIRST)
                .toList();
    }

    private static ChannelSalesRDto toRDto(
            UUID id, @Nullable String label, @Nullable ChannelSalesTotal total, BigDecimal totalRevenue) {
        var revenue = total == null ? BigDecimal.ZERO : total.revenue();
        var count = total == null ? 0L : total.salesCount();
        return new ChannelSalesRDto(id, label, revenue, count, share(revenue, totalRevenue));
    }

    /** Share of the total turnover in percent, one decimal; 0 when the total is zero. */
    private static BigDecimal share(BigDecimal revenue, BigDecimal totalRevenue) {
        if (totalRevenue.signum() == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY);
        }
        return revenue.multiply(HUNDRED).divide(totalRevenue, 1, RoundingMode.HALF_UP);
    }
}
