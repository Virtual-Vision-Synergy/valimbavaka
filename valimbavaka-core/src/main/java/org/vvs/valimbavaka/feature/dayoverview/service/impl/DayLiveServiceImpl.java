package org.vvs.valimbavaka.feature.dayoverview.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.day.service.BusinessDayService;
import org.vvs.valimbavaka.feature.dayoverview.dto.DayDeliveriesRDto;
import org.vvs.valimbavaka.feature.dayoverview.dto.DayLiveRDto;
import org.vvs.valimbavaka.feature.dayoverview.dto.DaySalesRDto;
import org.vvs.valimbavaka.feature.dayoverview.dto.DayServicesRDto;
import org.vvs.valimbavaka.feature.dayoverview.service.DayLiveService;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.service.DeliveryDayService;
import org.vvs.valimbavaka.feature.sale.domain.SaleFilter;
import org.vvs.valimbavaka.feature.sale.service.ChannelDashboardService;
import org.vvs.valimbavaka.feature.sale.service.SaleQueryService;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordRDto;
import org.vvs.valimbavaka.feature.salonservice.service.ServiceRecordService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class DayLiveServiceImpl implements DayLiveService {

    /** Operations kept in the feed, and read from each activity before the merge. */
    static final int FEED_LIMIT = 30;

    static final ZoneId BUSINESS_ZONE = ZoneId.of("Indian/Antananarivo");

    private static final PageQuery LATEST_SALES = new PageQuery(1, FEED_LIMIT, null, "createdAt", SortDirection.DESC);

    private final BusinessDayService businessDayService;
    private final ChannelDashboardService channelDashboardService;
    private final SaleQueryService saleQueryService;
    private final DeliveryDayService deliveryDayService;
    private final ServiceRecordService serviceRecordService;
    private final Clock clock;

    DayLiveServiceImpl(
            BusinessDayService businessDayService,
            ChannelDashboardService channelDashboardService,
            SaleQueryService saleQueryService,
            DeliveryDayService deliveryDayService,
            ServiceRecordService serviceRecordService,
            Clock clock) {
        this.businessDayService = businessDayService;
        this.channelDashboardService = channelDashboardService;
        this.saleQueryService = saleQueryService;
        this.deliveryDayService = deliveryDayService;
        this.serviceRecordService = serviceRecordService;
        this.clock = clock;
    }

    @Override
    public DayLiveRDto getLive() {
        var now = clock.instant();
        var date = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        var day = businessDayService.findToday();
        var dashboard = channelDashboardService.getDashboard(date, date);
        var salesPage = saleQueryService.getSales(LATEST_SALES, new SaleFilter(date, null, null));
        var deliveries = deliveryDayService.getRecentOrders(date, FEED_LIMIT);
        var records = serviceRecordService.getServiceRecordsBetween(
                date.atStartOfDay(BUSINESS_ZONE).toInstant(),
                date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant());
        return new DayLiveRDto(
                date,
                day == null ? null : day.status(),
                now,
                new DaySalesRDto(dashboard.totalRevenue(), dashboard.totalSalesCount(), dashboard.channels()),
                deliveryIndicators(date),
                serviceIndicators(records),
                DayFeed.merge(salesPage.items(), deliveries, records, FEED_LIMIT));
    }

    private DayDeliveriesRDto deliveryIndicators(LocalDate date) {
        var counts = deliveryDayService.countByStatus(date);
        long total = 0;
        for (var count : counts.values()) {
            total += count;
        }
        return new DayDeliveriesRDto(
                total,
                counts.get(DeliveryStatus.TO_PREPARE),
                counts.get(DeliveryStatus.ON_ROUND),
                counts.get(DeliveryStatus.DELIVERED),
                counts.get(DeliveryStatus.FAILED),
                counts.get(DeliveryStatus.CANCELLED));
    }

    private static DayServicesRDto serviceIndicators(List<ServiceRecordRDto> records) {
        var revenue = BigDecimal.ZERO;
        for (var performed : records) {
            revenue = revenue.add(performed.price());
        }
        return new DayServicesRDto(records.size(), revenue);
    }
}
