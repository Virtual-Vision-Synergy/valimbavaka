package org.vvs.valimbavaka.feature.report.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Raw turnover of one product range over a period (RPT-04); ranges that sold nothing have no row. */
public record RangeRevenue(UUID rangeId, String code, String label, @Nullable BigDecimal revenue) {}
