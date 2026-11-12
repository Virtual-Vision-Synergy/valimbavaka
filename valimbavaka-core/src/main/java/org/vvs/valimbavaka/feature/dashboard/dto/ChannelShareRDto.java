package org.vvs.valimbavaka.feature.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * One sales channel of the revenue split (DSH-04): turnover of its validated sales of the day (after discount) and
 * share of the turnover of all channels in percent, one decimal. The label is {@code null} for a channel deactivated
 * since.
 */
public record ChannelShareRDto(
        UUID salesChannelId, @Nullable String label, BigDecimal revenue, BigDecimal sharePercent) {}
