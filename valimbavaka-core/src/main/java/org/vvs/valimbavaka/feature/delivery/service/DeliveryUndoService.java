package org.vvs.valimbavaka.feature.delivery.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.delivery.dto.DeliveryOrderRDto;

/**
 * Time-limited undo of the last action on a delivery (LIV-05): a delivered or failed order goes back to the status it
 * had before, so that the driver corrects a slip of the finger.
 */
public interface DeliveryUndoService {

    /**
     * Restores the status before the last action, if it is recent enough (5 minutes). A delivered order cancels its
     * sale (the articles come back to the delivery location); a failed order cancels its stock return (the articles
     * go back from the warehouse to the delivery location). The undo is traced in the journal, all or nothing. 404 if
     * the order is unknown (or assigned to another driver for a driver); 409 if there is no recent action to undo
     * (to prepare, on round, cancelled, already undone, delay over), if the day of the sale is closed or if the
     * warehouse no longer holds the articles of a failure.
     */
    DeliveryOrderRDto undoLastAction(UUID orderId);
}
