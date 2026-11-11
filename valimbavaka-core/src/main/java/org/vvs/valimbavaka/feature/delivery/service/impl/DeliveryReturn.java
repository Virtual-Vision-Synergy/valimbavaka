package org.vvs.valimbavaka.feature.delivery.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.delivery.domain.DeliveryOrder;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLineCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.service.TransferService;

/**
 * Brings the articles of a failed order back to stock (LIV-04): a stock return (TRF-03, {@code RET-…}) from the
 * delivery location, where the morning transfers put them, to the warehouse they came from.
 */
@Component
class DeliveryReturn {

    static final String FROM_LOCATION = "DELIVERY";
    static final String TO_LOCATION = "WAREHOUSE";

    private final TransferService transferService;
    private final ReferentialService referentialService;

    DeliveryReturn(TransferService transferService, ReferentialService referentialService) {
        this.transferService = transferService;
        this.referentialService = referentialService;
    }

    /** @return id of the recorded stock return; 409 if the delivery location does not hold the articles */
    UUID record(DeliveryOrder order) {
        var leg = new Leg(TransferOrigin.RETURN, FROM_LOCATION, TO_LOCATION);
        return transfer(order, leg, "Retour de la livraison en échec " + order.getNumber());
    }

    /**
     * Undo of a failure (LIV-05): the articles go back from the warehouse to the delivery location, as a transfer
     * that cancels the return (a recorded transfer is never deleted); 409 if the warehouse no longer holds them.
     */
    void undoReturn(DeliveryOrder order) {
        var leg = new Leg(TransferOrigin.MANUAL, TO_LOCATION, FROM_LOCATION);
        transfer(order, leg, "Annulation du retour de la livraison " + order.getNumber());
    }

    private UUID transfer(DeliveryOrder order, Leg leg, String note) {
        var lines = order.getLines().stream()
                .map(line -> new TransferLineCommand(line.getArticleId(), line.getQuantity()))
                .toList();
        var command = new TransferCommand(
                leg.origin(),
                referentialService.requireActiveStockLocationId(leg.from()),
                referentialService.requireActiveStockLocationId(leg.to()),
                lines,
                note);
        return transferService.record(command).id();
    }

    /** Kind of transfer and the codes of the locations it leaves and reaches. */
    private record Leg(TransferOrigin origin, String from, String to) {}
}
