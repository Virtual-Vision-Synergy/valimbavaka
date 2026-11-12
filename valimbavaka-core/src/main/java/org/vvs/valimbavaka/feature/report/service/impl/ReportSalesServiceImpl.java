package org.vvs.valimbavaka.feature.report.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.report.domain.ReportFilter;
import org.vvs.valimbavaka.feature.report.domain.ReportShare;
import org.vvs.valimbavaka.feature.report.dto.ReportRangeRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportRangesRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportSalesRDto;
import org.vvs.valimbavaka.feature.report.dto.ReportTrendPointRDto;
import org.vvs.valimbavaka.feature.report.repository.DailyRevenue;
import org.vvs.valimbavaka.feature.report.repository.RangeRevenue;
import org.vvs.valimbavaka.feature.report.repository.ReportSaleRepository;
import org.vvs.valimbavaka.feature.report.repository.SalesTotal;
import org.vvs.valimbavaka.feature.report.service.ReportSalesService;

@Service
@Transactional(readOnly = true)
public class ReportSalesServiceImpl implements ReportSalesService {

    private static final int MONEY_SCALE = 2;

    private final ReportSaleRepository reportSaleRepository;

    ReportSalesServiceImpl(ReportSaleRepository reportSaleRepository) {
        this.reportSaleRepository = reportSaleRepository;
    }

    @Override
    public ReportSalesRDto getSales(ReportFilter filter) {
        var from = filter.period().from();
        var to = filter.period().to();
        var channelId = filter.salesChannelId();
        var rangeId = filter.rangeId();
        if (rangeId == null) {
            return toIndicators(reportSaleRepository.totals(from, to, channelId));
        }
        return toIndicators(reportSaleRepository.totalsByRange(from, to, channelId, rangeId));
    }

    @Override
    public List<ReportTrendPointRDto> getTrend(ReportFilter filter) {
        var period = filter.period();
        var revenueByPoint = new HashMap<LocalDate, BigDecimal>();
        for (var row : dailyRevenue(filter)) {
            revenueByPoint.merge(period.pointOf(row.day()), toMoney(row.revenue()), BigDecimal::add);
        }
        var empty = toMoney(null);
        return period.points().stream()
                .map(point -> new ReportTrendPointRDto(point, revenueByPoint.getOrDefault(point, empty)))
                .toList();
    }

    @Override
    public ReportRangesRDto getRanges(ReportFilter filter) {
        var period = filter.period();
        var rows = reportSaleRepository.revenueByRange(period.from(), period.to(), filter.salesChannelId());
        var revenues = rows.stream().map(row -> toMoney(row.revenue())).toList();
        var shares = ReportShare.distribute(revenues);
        var items = new ArrayList<ReportRangeRDto>();
        for (var index = 0; index < rows.size(); index++) {
            items.add(toRange(rows.get(index), revenues.get(index), shares.get(index)));
        }
        return new ReportRangesRDto(revenues.stream().reduce(toMoney(null), BigDecimal::add), items);
    }

    private static ReportRangeRDto toRange(RangeRevenue row, BigDecimal revenue, BigDecimal share) {
        return new ReportRangeRDto(row.rangeId(), row.code(), row.label(), revenue, share);
    }

    private List<DailyRevenue> dailyRevenue(ReportFilter filter) {
        var period = filter.period();
        if (filter.rangeId() == null) {
            return reportSaleRepository.dailyRevenue(period.from(), period.to(), filter.salesChannelId());
        }
        return reportSaleRepository.dailyRevenueByRange(
                period.from(), period.to(), filter.salesChannelId(), filter.rangeId());
    }

    private static BigDecimal toMoney(@Nullable BigDecimal amount) {
        var value = amount == null ? BigDecimal.ZERO : amount;
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static ReportSalesRDto toIndicators(SalesTotal total) {
        var revenue = toMoney(total.revenue());
        var basket = revenue;
        if (total.salesCount() > 0) {
            basket = revenue.divide(BigDecimal.valueOf(total.salesCount()), MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return new ReportSalesRDto(revenue, total.salesCount(), basket);
    }
}
