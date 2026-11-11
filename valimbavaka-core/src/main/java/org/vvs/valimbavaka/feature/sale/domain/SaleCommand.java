package org.vvs.valimbavaka.feature.sale.domain;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A sale requested by the counter (VTE-01) or by another feature (an order delivered, LIV-03).
 *
 * @param salesChannelId active sales channel of the referential (SYS-05); it decides the stock location
 * @param sellerId active account with the seller role, credited with the sale (commission COM-02); {@code null} only
 *     from another feature when nobody is to be credited (order entered by the administrator, LIV-03), never from
 *     the counter
 * @param lines 1 to 100 lines, each article once
 */
public record SaleCommand(
        UUID salesChannelId, @Nullable UUID sellerId, PaymentMethod paymentMethod, List<SaleLineCommand> lines) {}
