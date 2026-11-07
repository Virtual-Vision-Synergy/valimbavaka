package org.vvs.valimbavaka.feature.sale.domain;

import java.util.List;
import java.util.UUID;

/**
 * A sale requested by the counter (VTE-01) or by another feature (an order delivered, LIV-03).
 *
 * @param salesChannelId active sales channel of the referential (SYS-05); it decides the stock location
 * @param sellerId active account with the seller role, credited with the sale (commission COM-02)
 * @param lines 1 to 100 lines, each article once
 */
public record SaleCommand(
        UUID salesChannelId, UUID sellerId, PaymentMethod paymentMethod, List<SaleLineCommand> lines) {}
