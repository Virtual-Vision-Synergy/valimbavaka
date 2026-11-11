package org.vvs.valimbavaka.feature.invoice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Credit note of an invoice (FAC-04): the invoice it reverses, its reason, and the frozen amounts and lines. */
public record CreditNoteRDto(
        UUID id,
        String number,
        UUID invoiceId,
        String invoiceNumber,
        UUID saleId,
        String saleNumber,
        Instant issuedAt,
        LocalDate businessDate,
        String reason,
        String issuerName,
        @Nullable String issuerNif,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal total,
        List<InvoiceLineRDto> lines) {}
