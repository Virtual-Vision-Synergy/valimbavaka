package org.vvs.valimbavaka.feature.sale.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Revenue of the validated sales of one business date (DSH-02): sum of {@code sales.total}, after discount.
 *
 * @param businessDate business date of the sales (day they were recorded in)
 * @param revenue sum of the totals of the {@code VALIDATED} sales of that date
 */
public record DailySalesRevenueRDto(LocalDate businessDate, BigDecimal revenue) {}
