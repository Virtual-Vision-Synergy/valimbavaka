package org.vvs.valimbavaka.feature.report.repository;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/** Raw sum and count of an aggregate; the revenue is {@code null} when no sale matches (SQL sum of nothing). */
public record SalesTotal(@Nullable BigDecimal revenue, long salesCount) {}
