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
 * @param promoCode promo code typed by the customer at the counter (VTE-04), {@code null} if none; the server checks
 *     it and decides the discount
 */
public record SaleCommand(
        UUID salesChannelId,
        @Nullable UUID sellerId,
        PaymentMethod paymentMethod,
        List<SaleLineCommand> lines,
        @Nullable String promoCode) {

    /** A sale without promo code (another feature, or a customer who gave none). */
    public SaleCommand(
            UUID salesChannelId, @Nullable UUID sellerId, PaymentMethod paymentMethod, List<SaleLineCommand> lines) {
        this(salesChannelId, sellerId, paymentMethod, lines, null);
    }
}
