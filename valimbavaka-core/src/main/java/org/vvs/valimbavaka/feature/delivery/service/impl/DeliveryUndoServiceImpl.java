package org.vvs.valimbavaka.feature.delivery.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryStatus;
import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;
import org.vvs.valimbavaka.feature.delivery.mapper.DeliveryMapper;
import org.vvs.valimbavaka.feature.delivery.repository.DeliveryOrderRepository;
import org.vvs.valimbavaka.feature.delivery.service.DeliveryUndoService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.feature.user.service.UserService;

@Service
@Transactional(readOnly = true)
public class DeliveryUndoServiceImpl implements DeliveryUndoService {

    /** Time during which the last action of a delivery can be undone. */
    static final Duration UNDO_WINDOW = Duration.ofMinutes(5);

    private static final Logger log = LoggerFactory.getLogger(DeliveryUndoServiceImpl.class);
    private static final String NOT_FOUND = "La commande à livrer est introuvable.";

    private final DeliveryOrderRepository orderRepository;
    private final DeliveryMapper deliveryMapper;
    private final DeliveryReturn deliveryReturn;
    private final SaleService saleService;
    private final JournalService journalService;
    private final UserService userService;
    private final AuditorAware<UUID> auditorAware;
    private final Clock clock;

    DeliveryUndoServiceImpl(
            DeliveryOrderRepository orderRepository,
            DeliveryMapper deliveryMapper,
            DeliveryReturn deliveryReturn,
            SaleService saleService,
            JournalService journalService,
            UserService userService,
            AuditorAware<UUID> auditorAware,
            Clock clock) {
        this.orderRepository = orderRepository;
        this.deliveryMapper = deliveryMapper;
        this.deliveryReturn = deliveryReturn;
        this.saleService = saleService;
        this.journalService = journalService;
        this.userService = userService;
        this.auditorAware = auditorAware;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DeliveryOrderRDto undoLastAction(UUID orderId) {
        var order = findVisible(orderId);
        var undone = order.getStatus();
        order.ensureCanUndo(clock.instant(), UNDO_WINDOW);
        cancelEffects(order);
        order.undo(clock.instant(), UNDO_WINDOW);
        journalService.record(
                JournalAction.CANCEL, DeliveryOrderServiceImpl.RESOURCE_TYPE, order.getId(), describe(order, undone));
        log.info("Action « {} » de la commande à livrer {} annulée", undone.label(), order.getNumber());
        return deliveryMapper.toRDto(order);
    }

    /** The sale of a delivery is cancelled, the return of a failure is cancelled: both put the articles back. */
    private void cancelEffects(DeliveryOrder order) {
        var saleId = order.getSaleId();
        if (order.getStatus() == DeliveryStatus.DELIVERED && saleId != null) {
            saleService.cancel(saleId, "annulation de la livraison " + order.getNumber());
        } else if (order.getStatus() == DeliveryStatus.FAILED) {
            deliveryReturn.undoReturn(order);
        }
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

    /** No customer data: only the order number and the two statuses. */
    private static String describe(DeliveryOrder order, DeliveryStatus undone) {
        return "Annulation de l'action sur la commande %s : de « %s » à « %s »"
                .formatted(order.getNumber(), undone.label(), order.getStatus().label());
    }
}
