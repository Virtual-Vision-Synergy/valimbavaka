package org.vvs.valimbavaka.feature.commission.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.domain.BaseEntity;

/**
 * Commission rule of a sales channel (COM-01). At most one active rule per channel (partial unique index); the sales
 * channel is referenced by its identifier, never by a JPA association (D8).
 */
@Entity
@Table(name = "commission_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommissionRule extends BaseEntity {

    private static final int RATE_SCALE = 2;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sales_channel_id", nullable = false)
    private UUID salesChannelId;

    /** Rate in percent, 0 to 100, two decimals. */
    @Column(name = "rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_base", nullable = false, length = 20)
    private CommissionBase base;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public static CommissionRule create(CommissionRuleDetails details) {
        var rule = new CommissionRule();
        rule.apply(details);
        rule.active = true;
        return rule;
    }

    /** Applies the normalized fields and reports whether anything changed (nothing to trace otherwise). */
    public boolean update(CommissionRuleDetails details) {
        var before = details();
        apply(details);
        return !before.equals(details());
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private void apply(CommissionRuleDetails details) {
        this.name = details.name().strip();
        this.salesChannelId = details.salesChannelId();
        this.rate = details.rate().setScale(RATE_SCALE, RoundingMode.UNNECESSARY);
        this.base = details.base();
    }

    private CommissionRuleDetails details() {
        return new CommissionRuleDetails(name, salesChannelId, rate, base);
    }
}
