package org.vvs.valimbavaka.feature.delivery.service.impl;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;
import org.vvs.valimbavaka.feature.delivery.mapper.DeliveryMapper;
import org.vvs.valimbavaka.feature.delivery.repository.DeliveryOrderRepository;
import org.vvs.valimbavaka.feature.delivery.repository.DeliveryStatusCount;
import org.vvs.valimbavaka.feature.delivery.service.DeliveryDayService;

@Service
@Transactional(readOnly = true)
public class DeliveryDayServiceImpl implements DeliveryDayService {

    private final DeliveryOrderRepository orderRepository;
    private final DeliveryMapper deliveryMapper;

    DeliveryDayServiceImpl(DeliveryOrderRepository orderRepository, DeliveryMapper deliveryMapper) {
        this.orderRepository = orderRepository;
        this.deliveryMapper = deliveryMapper;
    }

    @Override
    public Map<DeliveryStatus, Long> countByStatus(LocalDate date) {
        var counts = new EnumMap<DeliveryStatus, Long>(DeliveryStatus.class);
        for (var status : DeliveryStatus.values()) {
            counts.put(status, 0L);
        }
        for (DeliveryStatusCount count : orderRepository.countByStatus(date)) {
            counts.put(count.status(), count.total());
        }
        return counts;
    }

    @Override
    public List<DeliveryOrderRDto> getRecentOrders(LocalDate date, int limit) {
        var pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "updatedAt"));
        var orders = orderRepository.search(null, date, null, pageable);
        return orders.map(deliveryMapper::toRDto).getContent();
    }
}
