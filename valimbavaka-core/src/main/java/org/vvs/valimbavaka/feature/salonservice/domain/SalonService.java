package org.vvs.valimbavaka.feature.salonservice.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.domain.BaseEntity;

/**
 * Salon service of the catalogue (SRV-01): name, duration, price, optional commission rate and products usually
 * consumed. The commission rate, when set, takes precedence over the channel rule and the seller's rate (PO decision
 * of 07/11/2026, no accumulation). A service is never deleted: it is deactivated.
 */
@Entity
@Table(name = "salon_services")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalonService extends BaseEntity {

    private static final int MONEY_SCALE = 2;
    private static final Comparator<ConsumedProduct> BY_ARTICLE = Comparator.comparing(ConsumedProduct::getArticleId);

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "price", nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    /** Rate in percent, 0 to 100, two decimals; {@code null} = no rate of its own. */
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private @Nullable BigDecimal commissionRate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    /** Sorted by article identifier: same order in the database and after an update, so no false change. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "salon_service_products", joinColumns = @JoinColumn(name = "salon_service_id"))
    @OrderBy("articleId ASC")
    @BatchSize(size = 100)
    private List<ConsumedProduct> products = new ArrayList<>();

    public static SalonService create(SalonServiceDetails details) {
        var service = new SalonService();
        service.apply(details);
        service.active = true;
        return service;
    }

    /** Applies the normalized fields and reports whether anything changed (nothing to trace otherwise). */
    public boolean update(SalonServiceDetails details) {
        var before = details();
        var after = normalize(details);
        if (before.equals(after)) {
            return false;
        }
        apply(after);
        return true;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    /** Read-only copy of the consumed products, sorted by article identifier. */
    public List<ConsumedProduct> getProducts() {
        return List.copyOf(products);
    }

    private void apply(SalonServiceDetails details) {
        var normalized = normalize(details);
        this.name = normalized.name();
        this.durationMinutes = normalized.durationMinutes();
        this.price = normalized.price();
        this.commissionRate = normalized.commissionRate();
        // The managed collection is kept: Hibernate tracks its content, never a new list.
        this.products.clear();
        this.products.addAll(normalized.products());
    }

    private SalonServiceDetails details() {
        return new SalonServiceDetails(name, durationMinutes, price, commissionRate, getProducts());
    }

    private static SalonServiceDetails normalize(SalonServiceDetails details) {
        var rate = details.commissionRate();
        return new SalonServiceDetails(
                details.name().strip(),
                details.durationMinutes(),
                details.price().setScale(MONEY_SCALE, RoundingMode.UNNECESSARY),
                rate == null ? null : rate.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY),
                details.products().stream().sorted(BY_ARTICLE).toList());
    }
}
