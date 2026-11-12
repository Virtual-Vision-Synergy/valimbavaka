package org.vvs.valimbavaka.feature.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Turnover of one day of the trend: validated sales only, 0 for a day without any sale. */
public record RevenueTrendPointRDto(LocalDate date, BigDecimal revenue, long salesCount) {}
