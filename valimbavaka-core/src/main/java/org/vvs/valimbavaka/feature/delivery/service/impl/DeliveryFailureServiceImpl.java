package org.vvs.valimbavaka.feature.delivery.service.impl;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryFailure;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryFailureWDto;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;
import org.vvs.valimbavaka.feature.delivery.mapper.DeliveryMapper;
import org.vvs.valimbavaka.feature.delivery.repository.DeliveryOrderRepository;
import org.vvs.valimbavaka.feature.delivery.service.DeliveryFailureService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.user.service.UserService;

@Service
@Transactional(readOnly = true)
public class DeliveryFailureServiceImpl implements DeliveryFailureService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryFailureServiceImpl.class);
    private static final String NOT_FOUND = "La commande à livrer est introuvable.";

    private final DeliveryOrderRepository orderRepository;
    private final DeliveryMapper deliveryMapper;
    private final DeliveryReturn deliveryReturn;
    private final JournalService journalService;
    private final UserService userService;
    private final AuditorAware<UUID> auditorAware;

    DeliveryFailureServiceImpl(
            DeliveryOrderRepository orderRepository,
            DeliveryMapper deliveryMapper,
            DeliveryReturn deliveryReturn,
            JournalService journalService,
            UserService userService,
            AuditorAware<UUID> auditorAware) {
        this.orderRepository = orderRepository;
        this.deliveryMapper = deliveryMapper;
        this.deliveryReturn = deliveryReturn;
        this.journalService = journalService;
        this.userService = userService;
        this.auditorAware = auditorAware;
    }

    @Override
    @Transactional
    public DeliveryOrderRDto reportFailure(UUID orderId, DeliveryFailureWDto request) {
        var order = findVisible(orderId);
        order.getStatus().ensureCanMoveTo(DeliveryStatus.FAILED);
        var failure = new DeliveryFailure(request.reason(), blankToNull(request.note()));
        var returnTransferId = deliveryReturn.record(order);
        order.fail(failure, returnTransferId);
        journalService.record(
                JournalAction.UPDATE, DeliveryOrderServiceImpl.RESOURCE_TYPE, order.getId(), describe(order, failure));
        log.info("Commande à livrer {} en échec ({}), articles remis en stock", order.getNumber(), failure.reason());
        return deliveryMapper.toRDto(order);
    }

    /** The order, or 404 if it is unknown or, for a driver, assigned to another driver. */
    private DeliveryOrder findVisible(UUID orderId) {
        var order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException(NOT_FOUND));
        var callerId = auditorAware.getCurrentAuditor();
        if (callerId.isPresent()
                && !callerId.get().equals(order.getDriverId())
                && userService.isActiveDriver(callerId.get())) {
            throw new NotFoundException(NOT_FOUND);
        }
        return order;
    }

    /** Reason only: the driver's comment may name the customer and stays out of the journal. */
    private static String describe(DeliveryOrder order, DeliveryFailure failure) {
        return "Commande %s %s : %s, %d article(s) remis en stock"
                .formatted(
                        order.getNumber(),
                        DeliveryStatus.FAILED.label(),
                        failure.reason().label(),
                        order.getLines().size());
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
