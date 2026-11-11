package org.vvs.valimbavaka.feature.invoice.service;

import java.time.LocalDate;

import org.vvs.valimbavaka.feature.invoice.dto.SalesBookRowRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/**
 * Sales book (FAC-06): the invoices and the credit notes of a period, one line each, credit notes in negative. The
 * list is paged and the period is bounded, so a book is never loaded whole in memory.
 */
public interface SalesBookService {

    /** Longest period of a book, in days, both ends included. */
    int MAX_PERIOD_DAYS = 366;

    /**
     * Lines whose business day is in [from, to], oldest first by default. 422 if {@code from} is after {@code to} or
     * the period is longer than {@link #MAX_PERIOD_DAYS} days.
     */
    PageResponse<SalesBookRowRDto> getSalesBook(PageQuery query, LocalDate from, LocalDate to);
}
