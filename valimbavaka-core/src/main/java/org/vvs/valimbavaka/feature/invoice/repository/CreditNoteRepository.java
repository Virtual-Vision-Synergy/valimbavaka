package org.vvs.valimbavaka.feature.invoice.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.vvs.valimbavaka.feature.invoice.domain.CreditNote;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** An issued credit note is never deleted (JNL-02); its number is gapless inside a year. */
public interface CreditNoteRepository extends OperationRepository<CreditNote, UUID> {

    Optional<CreditNote> findByInvoiceId(UUID invoiceId);

    /**
     * Next number of the year, in one atomic statement (same mechanism as the invoice counter): the counter row stays
     * locked until the end of the caller's transaction, so two concurrent credit notes are serialised and a rolled
     * back one gives its number back (no gap).
     */
    @Query(
            value = "insert into credit_note_counters (year, last_value) values (:year, 1) "
                    + "on conflict (year) do update set last_value = credit_note_counters.last_value + 1 "
                    + "returning last_value",
            nativeQuery = true)
    long nextNumber(@Param("year") int year);
}
