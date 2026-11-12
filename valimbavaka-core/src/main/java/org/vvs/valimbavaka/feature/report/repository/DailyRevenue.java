package org.vvs.valimbavaka.feature.report.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/** Raw turnover of one business day that has sales (RPT-03); days without sale have no row. */
public record DailyRevenue(LocalDate day, @Nullable BigDecimal revenue) {}
