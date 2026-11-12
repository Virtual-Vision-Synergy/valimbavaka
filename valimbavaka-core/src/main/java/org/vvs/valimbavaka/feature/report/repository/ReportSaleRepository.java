package org.vvs.valimbavaka.feature.report.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    /** Number of articles of the ranking; the {@code limit} of {@link #topArticles} has the same value. */
    int RANKING_SIZE = 10;

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

    /**
     * Turnover of every product range in one query (same {@code where} as {@link #totalsByRange}, without the range
     * restriction), biggest first; the line totals are before the discount of the whole sale.
     */
    @Query("""
            select new org.vvs.valimbavaka.feature.report.repository.RangeRevenue(
                pr.id, pr.code, pr.label, sum(l.lineTotal))
            from Sale s
            join s.lines l
            join Article a on a.id = l.articleId
            join ProductRange pr on pr.id = a.rangeId
            where s.businessDate between :from and :to
              and s.status = org.vvs.valimbavaka.shared.operation.OperationStatus.VALIDATED
              and (:salesChannelId is null or s.salesChannelId = :salesChannelId)
            group by pr.id, pr.code, pr.label
            order by sum(l.lineTotal) desc, pr.label
            """)
    List<RangeRevenue> revenueByRange(LocalDate from, LocalDate to, @Nullable UUID salesChannelId);

    /**
     * Best-selling articles of the period (RPT-05), one aggregate grouped by article, biggest turnover first and cut
     * in the database ({@code limit} = {@link #RANKING_SIZE}, keep both in step). Same {@code where} as
     * {@link #totalsByRange}, the range restriction being optional ({@code null} = every range). Ties on the turnover
     * are broken by the article name then its code, so the order never changes between two calls.
     */
    @Query("""
            select new org.vvs.valimbavaka.feature.report.repository.ArticleSales(
                a.id, a.code, a.name, sum(l.quantity), sum(l.lineTotal),
                sum(l.quantity * l.unitCost), count(l.articleId), count(l.unitCost))
            from Sale s
            join s.lines l
            join Article a on a.id = l.articleId
            where s.businessDate between :from and :to
              and s.status = org.vvs.valimbavaka.shared.operation.OperationStatus.VALIDATED
              and (:rangeId is null or a.rangeId = :rangeId)
              and (:salesChannelId is null or s.salesChannelId = :salesChannelId)
            group by a.id, a.code, a.name
            order by sum(l.lineTotal) desc, a.name, a.code
            limit 10
            """)
    List<ArticleSales> topArticles(LocalDate from, LocalDate to, @Nullable UUID salesChannelId, @Nullable UUID rangeId);

    /** Turnover of all the lines of the period (before the discount of the sales), the base of the ranking shares. */
    @Query("""
            select sum(l.lineTotal)
            from Sale s
            join s.lines l
            join Article a on a.id = l.articleId
            where s.businessDate between :from and :to
              and s.status = org.vvs.valimbavaka.shared.operation.OperationStatus.VALIDATED
              and (:rangeId is null or a.rangeId = :rangeId)
              and (:salesChannelId is null or s.salesChannelId = :salesChannelId)
            """)
    @Nullable
    BigDecimal linesRevenue(LocalDate from, LocalDate to, @Nullable UUID salesChannelId, @Nullable UUID rangeId);

    /** Label of a sales channel (the export of the report prints the filter it applied, RPT-06). */
    @Query("select c.label from SalesChannel c where c.id = :id")
    Optional<String> salesChannelLabel(UUID id);

    /** Label of a product range (the export of the report prints the filter it applied, RPT-06). */
    @Query("select r.label from ProductRange r where r.id = :id")
    Optional<String> productRangeLabel(UUID id);
}
