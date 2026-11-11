package org.vvs.valimbavaka.feature.sale.domain;

import java.util.UUID;

/**
 * Published when a sale is cancelled, in the transaction of the cancellation: the invoice (FAC-*) and the
 * commission (COM-*) listen to it, as they do to {@link SaleRecordedEvent}, without the sale depending on them.
 */
public record SaleCancelledEvent(UUID saleId, String number) {}
