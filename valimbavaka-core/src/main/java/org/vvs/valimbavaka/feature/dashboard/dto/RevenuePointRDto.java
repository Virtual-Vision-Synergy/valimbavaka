package org.vvs.valimbavaka.feature.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One bar of the revenue chart (DSH-02).
 *
 * @param date the day, or the Monday of the week for a weekly point
 * @param amount revenue of that day or week (validated sales and salon services), 0 when nothing was sold
 */
public record RevenuePointRDto(LocalDate date, BigDecimal amount) {}
