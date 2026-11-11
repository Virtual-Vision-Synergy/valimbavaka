package org.vvs.valimbavaka.feature.invoice.repository;

import java.math.BigDecimal;

import org.vvs.valimbavaka.shared.operation.OperationStatus;

/** Number and amount of the invoices that have one status over a period (FAC-05). */
public record InvoiceStatusTotal(OperationStatus status, long invoiceCount, BigDecimal amount) {}
