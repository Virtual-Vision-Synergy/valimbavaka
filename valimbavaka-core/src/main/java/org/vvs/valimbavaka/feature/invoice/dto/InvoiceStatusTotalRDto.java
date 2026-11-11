package org.vvs.valimbavaka.feature.invoice.dto;

import java.math.BigDecimal;

import org.vvs.valimbavaka.shared.operation.OperationStatus;

/**
 * Invoices of one status (FAC-05): {@code VALIDATED} is paid, {@code REVERSED} is credited by a credit note,
 * {@code CANCELLED} is an invoice of a sale cancelled before credit notes existed.
 */
public record InvoiceStatusTotalRDto(OperationStatus status, long invoiceCount, BigDecimal amount) {}
