package org.vvs.valimbavaka.feature.promotion.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.domain.BaseEntity;

/**
 * Promotion (PRO-01). Targets and channels are identifiers of other features (ranges or articles, sales channels):
 * no JPA association between features. The status is never stored, it is deduced from the period.
 */
@Entity
@Table(name = "promotions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Promotion extends BaseEntity {

    /** A page of the list (PRO-04, size ≤ 100) loads its targets and channels in one query each, not one per row. */
    private static final int LIST_BATCH_SIZE = 100;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private @Nullable String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private PromotionKind kind;

    @Column(name = "discount_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal value;

    /** Promo code typed at the till (VTE-04); null for an automatic promotion (PRO-02). */
    @Column(name = "code", length = 12)
    private @Nullable String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private PromotionScope scope;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    @Column(name = "usage_limit")
    private @Nullable Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    @Getter(AccessLevel.NONE)
    @ElementCollection
    @BatchSize(size = LIST_BATCH_SIZE)
    @CollectionTable(name = "promotion_targets", joinColumns = @JoinColumn(name = "promotion_id"))
    @Column(name = "target_id", nullable = false)
    private Set<UUID> targetIds = new HashSet<>();

    @Getter(AccessLevel.NONE)
    @ElementCollection
    @BatchSize(size = LIST_BATCH_SIZE)
    @CollectionTable(name = "promotion_channels", joinColumns = @JoinColumn(name = "promotion_id"))
    @Column(name = "sales_channel_id", nullable = false)
    private Set<UUID> channelIds = new HashSet<>();

    /** Creates a promotion from checked details; its usage counter starts at zero. */
    public static Promotion create(PromotionDetails details) {
        var promotion = new Promotion();
        promotion.name = details.name();
        promotion.description = details.description();
        promotion.kind = details.kind();
        promotion.value = details.value();
        promotion.code = details.code();
        promotion.scope = details.scope();
        promotion.startsOn = details.period().startsOn();
        promotion.endsOn = details.period().endsOn();
        promotion.usageLimit = details.usageLimit();
        promotion.targetIds.addAll(details.targetIds());
        promotion.channelIds.addAll(details.channelIds());
        return promotion;
    }

    /** Ranges (scope RANGES) or articles (scope ARTICLES) targeted; empty for the whole catalogue. */
    public Set<UUID> getTargetIds() {
        return Set.copyOf(targetIds);
    }

    /** Sales channels on which the promotion applies (at least one). */
    public Set<UUID> getChannelIds() {
        return Set.copyOf(channelIds);
    }

    public PromotionPeriod getPeriod() {
        return new PromotionPeriod(startsOn, endsOn);
    }
}
