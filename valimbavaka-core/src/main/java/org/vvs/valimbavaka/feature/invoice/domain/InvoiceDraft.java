package org.vvs.valimbavaka.feature.invoice.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Everything printed on an invoice, copied from the sale and from the sheets read when it is issued: later changes of
 * a name or a price never alter an issued invoice.
 *
 * @param paymentMethod name of the payment method of the sale ({@code CASH}, {@code MVOLA}...), kept as text so that
 *     this domain does not depend on the sale one
 */
public record InvoiceDraft(
        UUID saleId,
        String saleNumber,
        Instant issuedAt,
        LocalDate businessDate,
        @Nullable String salesChannelLabel,
        @Nullable UUID sellerId,
        @Nullable String sellerName,
        String paymentMethod,
        String issuerName,
        @Nullable String issuerNif,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal total,
        List<InvoiceLineDraft> lines) {}
