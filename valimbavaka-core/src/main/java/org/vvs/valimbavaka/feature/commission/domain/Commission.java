package org.vvs.valimbavaka.feature.commission.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
 * Commission earned by a seller on a sale or a salon service (COM-02), computed once when the operation is recorded:
 * base amount × rate, rounded half up to the ariary. The operation, the seller, the channel, the service and the rule
 * are referenced by their identifiers, never by a JPA association (D8); the seller's name is frozen.
 */
@Entity
@Table(name = "commissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Commission extends BaseEntity {

    private static final int AMOUNT_SCALE = 2;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, updatable = false, length = 20)
    private CommissionSourceType sourceType;

    @Column(name = "source_id", nullable = false, updatable = false)
    private UUID sourceId;

    @Column(name = "source_number", nullable = false, updatable = false, length = 20)
    private String sourceNumber;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "seller_name", nullable = false, updatable = false)
    private String sellerName;

    @Column(name = "sales_channel_id", nullable = false, updatable = false)
    private UUID salesChannelId;

    @Column(name = "salon_service_id", updatable = false)
    private @Nullable UUID salonServiceId;

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    /** Revenue of the operation (sale total after discounts, price of the service). */
    @Column(name = "revenue", nullable = false, precision = 19, scale = 2)
    private BigDecimal revenue;

    /** Amount the rate applied to: the revenue, or the gross margin (never below zero) for a margin rule. */
    @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_base", nullable = false, length = 20)
    private CommissionBase base;

    /** Rate in percent, 0 to 100. */
    @Column(name = "rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_source", nullable = false, length = 20)
    private CommissionRateSource rateSource;

    @Column(name = "commission_rule_id")
    private @Nullable UUID ruleId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CommissionStatus status = CommissionStatus.CALCULATED;

    /** The operation was cancelled: the commission is kept for the trace but no longer counts. */
    public void cancel() {
        this.status = CommissionStatus.CANCELLED;
    }

    /**
     * A margin rule applies to the revenue minus the purchase cost (zero if the operation was sold below cost); when
     * the purchase cost is unknown (salon service), the rate applies to the revenue and the base says so.
     */
    public static Commission calculate(CommissionSource source, String sellerName, CommissionTerms terms) {
        var commission = new Commission();
        commission.sourceType = source.type();
        commission.sourceId = source.id();
        commission.sourceNumber = source.number();
        commission.sellerId = source.sellerId();
        commission.sellerName = sellerName;
        commission.salesChannelId = source.salesChannelId();
        commission.salonServiceId = source.salonServiceId();
        commission.businessDate = source.businessDate();
        commission.revenue = source.revenue().setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        commission.applyTerms(source.purchaseCost(), terms);
        return commission;
    }

    private void applyTerms(@Nullable BigDecimal purchaseCost, CommissionTerms terms) {
        var cost = terms.base() == CommissionBase.MARGIN ? purchaseCost : null;
        if (cost == null) {
            this.base = CommissionBase.REVENUE;
            this.baseAmount = revenue;
        } else {
            this.base = CommissionBase.MARGIN;
            this.baseAmount = revenue.subtract(cost).max(BigDecimal.ZERO).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        this.rate = terms.rate().setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        this.rateSource = terms.rateSource();
        this.ruleId = terms.ruleId();
        this.amount = baseAmount
                .multiply(rate)
                .divide(HUNDRED, 0, RoundingMode.HALF_UP)
                .setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
    }
}
