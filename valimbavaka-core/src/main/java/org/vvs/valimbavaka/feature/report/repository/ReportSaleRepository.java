package org.vvs.valimbavaka.feature.report.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import org.vvs.valimbavaka.feature.sale.domain.Sale;

/**
 * Read side of the reports (RPT-01): sales aggregated in SQL, never loaded in memory. Only the validated sales count,
 * over the business days {@code [from, to]}; a {@code null} channel = all channels. Read only.
 */
public interface ReportSaleRepository extends Repository<Sale, UUID> {

    /** Turnover (sale totals, after discount) and number of sales. */
    @Query("""
            select new org.vvs.valimbavaka.feature.report.repository.SalesTotal(sum(s.total), count(s))
            from Sale s
            where s.businessDate between :from and :to
              and s.status = org.vvs.valimbavaka.shared.operation.OperationStatus.VALIDATED
              and (:salesChannelId is null or s.salesChannelId = :salesChannelId)
            """)
    SalesTotal totals(LocalDate from, LocalDate to, @Nullable UUID salesChannelId);

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
            where s.businessDate between :from and :to
              and s.status = org.vvs.valimbavaka.shared.operation.OperationStatus.VALIDATED
              and a.rangeId = :rangeId
              and (:salesChannelId is null or s.salesChannelId = :salesChannelId)
            """)
    SalesTotal totalsByRange(LocalDate from, LocalDate to, @Nullable UUID salesChannelId, UUID rangeId);

    /** Turnover of each business day that has at least one sale (same {@code where} as {@link #totals}). */
    @Query("""
            select new org.vvs.valimbavaka.feature.report.repository.DailyRevenue(s.businessDate, sum(s.total))
            from Sale s
            where s.businessDate between :from and :to
              and s.status = org.vvs.valimbavaka.shared.operation.OperationStatus.VALIDATED
              and (:salesChannelId is null or s.salesChannelId = :salesChannelId)
            group by s.businessDate
            order by s.businessDate
            """)
    List<DailyRevenue> dailyRevenue(LocalDate from, LocalDate to, @Nullable UUID salesChannelId);

    /** Turnover of the lines of one range for each business day (same {@code where} as {@link #totalsByRange}). */
    @Query("""
            select new org.vvs.valimbavaka.feature.report.repository.DailyRevenue(s.businessDate, sum(l.lineTotal))
            from Sale s
            join s.lines l
            join Article a on a.id = l.articleId
            where s.businessDate between :from and :to
              and s.status = org.vvs.valimbavaka.shared.operation.OperationStatus.VALIDATED
              and a.rangeId = :rangeId
              and (:salesChannelId is null or s.salesChannelId = :salesChannelId)
            group by s.businessDate
            order by s.businessDate
            """)
    List<DailyRevenue> dailyRevenueByRange(LocalDate from, LocalDate to, @Nullable UUID salesChannelId, UUID rangeId);
}
