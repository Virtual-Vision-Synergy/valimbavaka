package org.vvs.valimbavaka.feature.promotion.domain;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Promotion data, normalized on construction: name stripped, blank description or code set to null, code in upper
 * case, duplicates removed from the targets and channels.
 */
public record PromotionDetails(
        String name,
        @Nullable String description,
        PromotionKind kind,
        BigDecimal value,
        @Nullable String code,
        PromotionScope scope,
        Set<UUID> targetIds,
        PromotionPeriod period,
        Set<UUID> channelIds,
        @Nullable Integer usageLimit) {

    public PromotionDetails {
        name = name.strip();
        description = blankToNull(description);
        code = normalizeCode(code);
        targetIds = Set.copyOf(targetIds);
        channelIds = Set.copyOf(channelIds);
    }

    private static @Nullable String normalizeCode(@Nullable String code) {
        var value = blankToNull(code);
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
