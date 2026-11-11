package org.vvs.valimbavaka.feature.invoice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.shared.operation.OperationStatus;

/**
 * Issued invoice (FAC-01). {@code paymentMethod} is the name of the payment method of the sale; {@code sellerName} is
 * {@code null} for a sale credited to nobody (order delivered, LIV-03); {@code issuerNif} is {@code null} until the tax
 * number is configured.
 */
public record InvoiceRDto(
        UUID id,
        String number,
        UUID saleId,
        String saleNumber,
        Instant issuedAt,
        LocalDate businessDate,
        OperationStatus status,
        @Nullable String salesChannelLabel,
        @Nullable UUID sellerId,
        @Nullable String sellerName,
        String paymentMethod,
        String issuerName,
        @Nullable String issuerNif,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal total,
        List<InvoiceLineRDto> lines) {}
