package org.vvs.valimbavaka.feature.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Consolidated invoices by status over the business days {@code [from, to]} (FAC-05): one entry per status (at zero
 * when there is no invoice), and the count and amount of all the invoices, which are the sum of the entries.
 */
public record InvoiceStatusTotalsRDto(
        LocalDate from, LocalDate to, long invoiceCount, BigDecimal amount, List<InvoiceStatusTotalRDto> statuses) {}
