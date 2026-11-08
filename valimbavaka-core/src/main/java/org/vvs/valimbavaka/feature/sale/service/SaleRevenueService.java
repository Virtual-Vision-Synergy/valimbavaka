package org.vvs.valimbavaka.feature.sale.service;

import java.time.LocalDate;
import java.util.List;

import org.vvs.valimbavaka.feature.sale.dto.DailySalesRevenueRDto;

/** Read port on the revenue of the sales, for the dashboard and the reports (DSH-02). */
public interface SaleRevenueService {

    /**
     * Revenue of the {@code VALIDATED} sales per business date in {@code [from, to]}, oldest first; a date without
     * any validated sale is absent (cancelled and reversed sales never count).
     */
    List<DailySalesRevenueRDto> getDailyRevenue(LocalDate from, LocalDate to);
}
