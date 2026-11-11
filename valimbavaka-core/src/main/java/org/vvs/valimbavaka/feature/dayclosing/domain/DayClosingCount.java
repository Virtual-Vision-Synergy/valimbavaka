package org.vvs.valimbavaka.feature.dayclosing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.domain.BaseEntity;

/**
 * Evening count of one article in one location for a business day (JR-05). The theoretical quantity is frozen at the
 * count, so that a later sale does not rewrite what the administrator saw. A reason exists if and only if the count
 * differs from the theoretical quantity. The day, the location and the article are referenced by id (no association
 * between features); JR-06 locks the day on these lines.
 */
@Entity
@Table(name = "day_closing_counts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DayClosingCount extends BaseEntity {

    @Column(name = "business_day_id", nullable = false, updatable = false)
    private UUID businessDayId;

    @Column(name = "location_id", nullable = false, updatable = false)
    private UUID locationId;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "theoretical_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal theoreticalQuantity;

    @Column(name = "counted_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal countedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 30)
    private @Nullable ClosingGapReason reason;

    @Column(name = "counted_at", nullable = false)
    private Instant countedAt;

    /** First count of a line; the reason is kept only if the count has a gap. */
    public static DayClosingCount create(UUID businessDayId, UUID locationId, UUID articleId, CountResult result) {
        var count = new DayClosingCount();
        count.businessDayId = businessDayId;
        count.locationId = locationId;
        count.articleId = articleId;
        count.apply(result);
        return count;
    }

    /**
     * A recount replaces the quantities and the reason; a recount without gap clears the reason.
     *
     * @return false when the same count is sent again (offline replay): nothing changes
     */
    public boolean recount(CountResult result) {
        if (result.sameAs(countedQuantity, theoreticalQuantity, reason)) {
            return false;
        }
        apply(result);
        return true;
    }

    /** True when the counted quantity differs from the theoretical quantity frozen at the count. */
    public boolean hasGap() {
        return countedQuantity.compareTo(theoreticalQuantity) != 0;
    }

    private void apply(CountResult result) {
        theoreticalQuantity = result.theoretical();
        countedQuantity = result.counted();
        reason = result.hasGap() ? result.reason() : null;
        countedAt = result.countedAt();
    }
}
