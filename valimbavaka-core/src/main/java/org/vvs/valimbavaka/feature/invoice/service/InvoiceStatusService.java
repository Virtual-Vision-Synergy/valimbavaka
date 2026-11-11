package org.vvs.valimbavaka.feature.invoice.service;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.invoice.dto.InvoiceStatusTotalsRDto;

/** Follow-up of the invoice statuses (FAC-05): paid, credited and cancelled invoices, consolidated for the owner. */
public interface InvoiceStatusService {

    /**
     * Number and amount of the invoices of each status issued over the business days {@code [from, to]} inclusive.
     * {@code to} is today (Indian/Antananarivo) and {@code from} the first day of that month by default. Every status
     * is listed, at zero when empty, and the grand total is the sum of the statuses. 422 if {@code from} is after
     * {@code to}.
     */
    InvoiceStatusTotalsRDto getStatusTotals(@Nullable LocalDate from, @Nullable LocalDate to);
}
