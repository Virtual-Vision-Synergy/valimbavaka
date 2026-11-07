package org.vvs.valimbavaka.feature.sale.domain;

import java.util.UUID;

/**
 * Published when a sale is recorded, in the transaction of the sale: the invoice (FAC-01) and the commission
 * (COM-02) listen to it without the sale depending on them.
 */
public record SaleRecordedEvent(UUID saleId, String number) {}
