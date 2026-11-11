package org.vvs.valimbavaka.feature.delivery.service.impl;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrderFilter;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryDriverRDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryEventRDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderWDto;
import org.vvs.valimbavaka.feature.delivery.mapper.DeliveryMapper;
import org.vvs.valimbavaka.feature.delivery.repository.DeliveryOrderRepository;
import org.vvs.valimbavaka.feature.delivery.service.DeliveryOrderService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class DeliveryOrderServiceImpl implements DeliveryOrderService {

    static final String RESOURCE_TYPE = "DELIVERY_ORDER";

    private static final Logger log = LoggerFactory.getLogger(DeliveryOrderServiceImpl.class);
    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "number", "scheduledDate", "stopNumber", "status");
    private static final String NOT_FOUND = "La commande à livrer est introuvable.";

    private final DeliveryOrderRepository orderRepository;
    private final DeliveryMapper deliveryMapper;
    private final DeliveryOrderPolicy policy;
    private final JournalService journalService;
    private final UserService userService;
    private final AuditorAware<UUID> auditorAware;

    DeliveryOrderServiceImpl(
            DeliveryOrderRepository orderRepository,
            DeliveryMapper deliveryMapper,
            DeliveryOrderPolicy policy,
            JournalService journalService,
            UserService userService,
            AuditorAware<UUID> auditorAware) {
        this.orderRepository = orderRepository;
        this.deliveryMapper = deliveryMapper;
        this.policy = policy;
        this.journalService = journalService;
        this.userService = userService;
        this.auditorAware = auditorAware;
    }

    @Override
    @Transactional
    public DeliveryOrderRDto createOrder(DeliveryOrderWDto request) {
        var command = policy.check(request);
        orderRepository.lockRound("delivery-round:" + command.driverId() + ":" + command.scheduledDate());
        var stopNumber = orderRepository.nextStopNumber(command.driverId(), command.scheduledDate());
        var number = "LIV-%04d".formatted(orderRepository.nextNumber());
        var order = orderRepository.save(DeliveryOrder.create(number, stopNumber, command));
        journalService.record(JournalAction.CREATE, RESOURCE_TYPE, order.getId(), describe(order));
        log.info("Commande à livrer {} créée (arrêt {} du {})", number, stopNumber, order.getScheduledDate());
        return deliveryMapper.toRDto(order);
    }

    @Override
    public DeliveryOrderRDto getOrder(UUID orderId) {
        return deliveryMapper.toRDto(findVisible(orderId));
    }

    @Override
    public PageResponse<DeliveryOrderRDto> getOrders(PageQuery query, DeliveryOrderFilter filter) {
        var scoped = ownDriverId().map(filter::forDriver).orElse(filter);
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        return PageResponse.from(orderRepository
                .search(scoped.driverId(), scoped.scheduledDate(), scoped.status(), pageable)
                .map(deliveryMapper::toRDto));
    }

    @Override
    public PageResponse<DeliveryDriverRDto> getDrivers(PageQuery query) {
        var drivers = userService.getActiveDrivers(query);
        return new PageResponse<>(
                drivers.items().stream().map(deliveryMapper::toDriverRDto).toList(),
                drivers.page(),
                drivers.pageSize(),
                drivers.totalCount(),
                drivers.totalPages());
    }

    @Override
    public PageResponse<DeliveryEventRDto> getHistory(UUID orderId, PageQuery query) {
        findVisible(orderId);
        var entries = journalService.getResourceEntries(RESOURCE_TYPE, orderId, query);
        return new PageResponse<>(
                entries.items().stream().map(deliveryMapper::toEventRDto).toList(),
                entries.page(),
                entries.pageSize(),
                entries.totalCount(),
                entries.totalPages());
    }

    /** The order, or 404 if it is unknown or, for a delivery driver, assigned to another driver. */
    private DeliveryOrder findVisible(UUID orderId) {
        var order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException(NOT_FOUND));
        var ownDriverId = ownDriverId();
        if (ownDriverId.isPresent() && !ownDriverId.get().equals(order.getDriverId())) {
            throw new NotFoundException(NOT_FOUND);
        }
        return order;
    }

    /** The caller's id when he is an active delivery driver: he only sees the orders assigned to him. */
    private Optional<UUID> ownDriverId() {
        return auditorAware.getCurrentAuditor().filter(userService::isActiveDriver);
    }

    private static String describe(DeliveryOrder order) {
        return "Création de la commande à livrer %s : %d article(s), %s Ar, tournée du %s (arrêt %d)"
                .formatted(
                        order.getNumber(),
                        order.getLines().size(),
                        order.getAmount().toPlainString(),
                        order.getScheduledDate(),
                        order.getStopNumber());
    }
}
