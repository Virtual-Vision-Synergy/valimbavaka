package org.vvs.valimbavaka.feature.delivery.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;

/** Day view of the deliveries (JR-03): counts per status and latest activity of one scheduled day. */
public interface DeliveryDayService {

    /** Number of orders scheduled on {@code date} in every status (zero for a status without order). */
    Map<DeliveryStatus, Long> countByStatus(LocalDate date);

    /** At most {@code limit} orders scheduled on {@code date}, the most recently updated first. */
    List<DeliveryOrderRDto> getRecentOrders(LocalDate date, int limit);
}
