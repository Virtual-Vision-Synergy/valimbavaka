package org.vvs.valimbavaka.feature.invoice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.vvs.valimbavaka.feature.invoice.domain.Invoice;
import org.vvs.valimbavaka.shared.operation.OperationRepository;

/** An issued invoice is never deleted (JNL-02); its number is gapless inside a year. */
public interface InvoiceRepository extends OperationRepository<Invoice, UUID> {

    Optional<Invoice> findBySaleId(UUID saleId);

    /**
     * Next number of the year, in one atomic statement: the counter row stays locked until the end of the caller's
     * transaction, so two concurrent sales are serialised and a rolled back sale gives its number back (no gap).
     */
    @Query(
            value = "insert into invoice_counters (year, last_value) values (:year, 1) "
                    + "on conflict (year) do update set last_value = invoice_counters.last_value + 1 "
                    + "returning last_value",
            nativeQuery = true)
    long nextNumber(@Param("year") int year);

    /** Count and amount of the invoices per status over the business days {@code [from, to]} (FAC-05). */
    @Query("""
            select new org.vvs.valimbavaka.feature.invoice.repository.InvoiceStatusTotal(
                i.status, count(i), sum(i.total))
            from Invoice i
            where i.businessDate between :from and :to
            group by i.status
            """)
    List<InvoiceStatusTotal> totalsByStatus(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
