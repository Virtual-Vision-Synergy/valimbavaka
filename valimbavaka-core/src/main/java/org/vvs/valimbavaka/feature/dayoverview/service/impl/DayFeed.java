package org.vvs.valimbavaka.feature.dayoverview.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.vvs.valimbavaka.feature.dayoverview.domain.DayFeedType;
import org.vvs.valimbavaka.feature.dayoverview.dto.DayFeedItemRDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleSummaryRDto;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordRDto;

/** Builds the feed of the day: the operations of the three activities merged, most recent first. */
final class DayFeed {

    private DayFeed() {}

    static List<DayFeedItemRDto> merge(
            List<SaleSummaryRDto> sales,
            List<DeliveryOrderRDto> deliveries,
            List<ServiceRecordRDto> services,
            int limit) {
        var items = new ArrayList<DayFeedItemRDto>();
        for (var sale : sales) {
            items.add(fromSale(sale));
        }
        for (var order : deliveries) {
            items.add(fromDelivery(order));
        }
        for (var performed : services) {
            items.add(fromService(performed));
        }
        items.sort(Comparator.comparing(DayFeedItemRDto::occurredAt).reversed());
        return List.copyOf(items.subList(0, Math.min(limit, items.size())));
    }

    private static DayFeedItemRDto fromSale(SaleSummaryRDto sale) {
        return new DayFeedItemRDto(
                DayFeedType.SALE,
                sale.id(),
                sale.number(),
                sale.salesChannelLabel(),
                sale.total(),
                sale.status().name(),
                sale.sellerName(),
                sale.createdAt());
    }

    private static DayFeedItemRDto fromDelivery(DeliveryOrderRDto order) {
        return new DayFeedItemRDto(
                DayFeedType.DELIVERY,
                order.id(),
                order.number(),
                order.customerName(),
                order.amount(),
                order.status().name(),
                null,
                order.updatedAt());
    }

    private static DayFeedItemRDto fromService(ServiceRecordRDto performed) {
        return new DayFeedItemRDto(
                DayFeedType.SERVICE,
                performed.id(),
                performed.number(),
                performed.serviceName(),
                performed.price(),
                null,
                performed.sellerName(),
                performed.performedAt());
    }
}
