package org.vvs.valimbavaka.feature.delivery.service.impl;

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
import org.vvs.valimbavaka.feature.delivery.service.DeliveryCompletionService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;

@Service
@Transactional(readOnly = true)
public class DeliveryCompletionServiceImpl implements DeliveryCompletionService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryCompletionServiceImpl.class);
    private static final String NOT_FOUND = "La commande à livrer est introuvable.";

    private final DeliveryOrderRepository orderRepository;
    private final DeliveryMapper deliveryMapper;
    private final DeliverySale deliverySale;
    private final JournalService journalService;
    private final UserService userService;
    private final AuditorAware<UUID> auditorAware;

    DeliveryCompletionServiceImpl(
            DeliveryOrderRepository orderRepository,
            DeliveryMapper deliveryMapper,
            DeliverySale deliverySale,
            JournalService journalService,
            UserService userService,
            AuditorAware<UUID> auditorAware) {
        this.orderRepository = orderRepository;
        this.deliveryMapper = deliveryMapper;
        this.deliverySale = deliverySale;
        this.journalService = journalService;
        this.userService = userService;
        this.auditorAware = auditorAware;
    }

    @Override
    @Transactional
    public DeliveryOrderRDto markDelivered(UUID orderId) {
        var order = findVisible(orderId);
        order.getStatus().ensureCanMoveTo(DeliveryStatus.DELIVERED);
        var sale = deliverySale.record(order);
        order.deliver(sale.id());
        journalService.record(
                JournalAction.UPDATE, DeliveryOrderServiceImpl.RESOURCE_TYPE, order.getId(), describe(order, sale));
        log.info("Commande à livrer {} livrée, vente {} enregistrée", order.getNumber(), sale.number());
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

    /** No customer data: the order and sale numbers lead to them. */
    private static String describe(DeliveryOrder order, SaleRDto sale) {
        return "Commande %s %s : vente %s, %d article(s), %s Ar à la livraison"
                .formatted(
                        order.getNumber(),
                        DeliveryStatus.DELIVERED.label(),
                        sale.number(),
                        order.getLines().size(),
                        sale.total().toPlainString());
    }
}
