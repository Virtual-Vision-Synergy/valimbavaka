package org.vvs.valimbavaka.feature.delivery.service.impl;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;
import org.vvs.valimbavaka.feature.sale.domain.SaleCommand;
import org.vvs.valimbavaka.feature.sale.domain.SaleLineCommand;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.feature.user.service.UserService;

/**
 * Turns a delivered order into a sale (LIV-03, decision of the product owner of 07/11/2026) through the port of
 * VTE-01: delivery channel, so the stock leaves the delivery location; prices frozen when the order was entered;
 * paid on delivery; credited to the seller who entered the order, nobody if she is not an active seller any more
 * (order entered by the administrator). The invoice follows the {@code SaleRecordedEvent} (FAC-01).
 */
@Component
class DeliverySale {

    static final String CHANNEL = "DELIVERY";

    private final SaleService saleService;
    private final ReferentialService referentialService;
    private final UserService userService;

    DeliverySale(SaleService saleService, ReferentialService referentialService, UserService userService) {
        this.saleService = saleService;
        this.referentialService = referentialService;
        this.userService = userService;
    }

    /** @return the recorded sale; 409 if today is not opened or the delivery location lacks the articles */
    SaleRDto record(DeliveryOrder order) {
        var channelId = referentialService
                .findSalesChannelId(CHANNEL)
                .orElseThrow(() -> new BusinessException("Le canal de vente Livraison est introuvable."));
        var lines = order.getLines().stream()
                .map(line -> new SaleLineCommand(line.getArticleId(), line.getQuantity(), line.getUnitPrice()))
                .toList();
        var command = new SaleCommand(channelId, seller(order.getCreatedBy()), PaymentMethod.ON_DELIVERY, lines);
        return saleService.record(command);
    }

    private @Nullable UUID seller(@Nullable UUID author) {
        if (author == null || !userService.isActiveSeller(author)) {
            return null;
        }
        return author;
    }
}
