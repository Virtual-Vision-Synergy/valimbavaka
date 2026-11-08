package org.vvs.valimbavaka.feature.sale.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import org.vvs.valimbavaka.feature.sale.domain.Sale;
import org.vvs.valimbavaka.feature.sale.dto.DailySalesRevenueRDto;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

/** Read-only aggregates on the sales (DSH-02): nothing is saved nor deleted through this repository. */
public interface SaleRevenueRepository extends Repository<Sale, UUID> {

    /** Sum of the totals of the sales in {@code status}, per business date in {@code [from, to]}, oldest first. */
    @Query("""
            select new org.vvs.valimbavaka.feature.sale.dto.DailySalesRevenueRDto(s.businessDate, sum(s.total))
            from Sale s
            where s.status = :status and s.businessDate between :from and :to
            group by s.businessDate
            order by s.businessDate
            """)
    List<DailySalesRevenueRDto> sumTotalPerBusinessDate(OperationStatus status, LocalDate from, LocalDate to);
}
