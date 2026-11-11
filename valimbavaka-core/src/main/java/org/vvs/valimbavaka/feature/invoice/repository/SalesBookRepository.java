package org.vvs.valimbavaka.feature.invoice.repository;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import org.vvs.valimbavaka.feature.invoice.domain.SalesBookEntry;

/** Read-only access to the sales book (FAC-06): nothing can be saved nor deleted through it. */
public interface SalesBookRepository extends Repository<SalesBookEntry, UUID> {

    /**
     * Lines whose business day is in [from, to], with a case-insensitive search on the document numbers, the sale
     * number and the seller; empty {@code search} = all.
     */
    @Query(value = """
            select e from SalesBookEntry e
            where e.businessDate >= :from and e.businessDate <= :to
              and (lower(e.number) like lower(concat('%', :search, '%'))
                   or lower(e.saleNumber) like lower(concat('%', :search, '%'))
                   or lower(coalesce(e.invoiceNumber, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(e.sellerName, '')) like lower(concat('%', :search, '%')))
            """, countQuery = """
            select count(e) from SalesBookEntry e
            where e.businessDate >= :from and e.businessDate <= :to
              and (lower(e.number) like lower(concat('%', :search, '%'))
                   or lower(e.saleNumber) like lower(concat('%', :search, '%'))
                   or lower(coalesce(e.invoiceNumber, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(e.sellerName, '')) like lower(concat('%', :search, '%')))
            """)
    Page<SalesBookEntry> search(LocalDate from, LocalDate to, String search, Pageable pageable);
}
