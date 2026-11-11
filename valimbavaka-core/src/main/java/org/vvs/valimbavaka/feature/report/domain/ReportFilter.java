package org.vvs.valimbavaka.feature.report.domain;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Filters of a report (RPT-01): the period, plus an optional sales channel and an optional product range. A
 * {@code null} channel or range means no restriction.
 */
public record ReportFilter(ReportPeriod period, @Nullable UUID salesChannelId, @Nullable UUID rangeId) {}
