package org.vvs.valimbavaka.feature.report.repository;

import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import org.vvs.valimbavaka.feature.sale.domain.Sale;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

/**
 * Read side of the reports (RPT-01): sales aggregated in SQL, never loaded in memory. Only the sales with the given
 * status count, over the business days {@code [from, to]}; a {@code null} channel = all channels. Read only.
 */
public interface ReportSaleRepository extends Repository<Sale, UUID> {

    /** Turnover (sale totals, after discount) and number of sales. */
    @Query("""
            select new org.vvs.valimbavaka.feature.report.repository.SalesTotal(sum(s.total), count(s))
            from Sale s
            where s.businessDate between :from and :to and s.status = :status
              and (:salesChannelId is null or s.salesChannelId = :salesChannelId)
            """)
    SalesTotal totals(LocalDate from, LocalDate to, OperationStatus status, @Nullable UUID salesChannelId);

    /**
     * Turnover of the lines of the articles of one range (line totals, before the discount of the whole sale) and
     * number of sales that contain at least one such line.
     */
    @Query("""
            select new org.vvs.valimbavaka.feature.report.repository.SalesTotal(
                sum(l.lineTotal), count(distinct s.id))
            from Sale s
            join s.lines l
            join Article a on a.id = l.articleId
            where s.businessDate between :from and :to and s.status = :status and a.rangeId = :rangeId
              and (:salesChannelId is null or s.salesChannelId = :salesChannelId)
            """)
    SalesTotal totalsByRange(
            LocalDate from, LocalDate to, OperationStatus status, @Nullable UUID salesChannelId, UUID rangeId);
}
