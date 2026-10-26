package org.vvs.valimbavaka.feature.article.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
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

/** Catalogue article. The range and the supplier are referenced by UUID (no association between features). */
@Entity
@Table(name = "articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseEntity {

    private static final int MONEY_SCALE = 2;
    private static final int QUANTITY_SCALE = 3;

    @Column(name = "code", nullable = false, updatable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "barcode", length = 13)
    private @Nullable String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ArticleType type;

    @Column(name = "range_id")
    private @Nullable UUID rangeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 20)
    private ArticleUnit unit;

    @Column(name = "cost", precision = 14, scale = 2)
    private @Nullable BigDecimal cost;

    @Column(name = "price", precision = 14, scale = 2)
    private @Nullable BigDecimal price;

    @Column(name = "threshold", nullable = false, precision = 12, scale = 3)
    private BigDecimal threshold;

    @Column(name = "supplier_id")
    private @Nullable UUID supplierId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public static Article create(String code, ArticleDetails details) {
        var article = new Article();
        article.code = code;
        article.apply(details);
        article.active = true;
        return article;
    }

    /** Applies the sheet once normalized and reports whether anything changed (nothing to trace otherwise). */
    public boolean update(ArticleDetails details) {
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

    /** Range and price only make sense for a finished product (sold at the till): dropped for the others. */
    private void apply(ArticleDetails details) {
        var sold = details.type() == ArticleType.FINISHED;
        this.name = details.name().strip();
        this.barcode = blankToNull(details.barcode());
        this.type = Objects.requireNonNull(details.type());
        this.rangeId = sold ? details.rangeId() : null;
        this.unit = Objects.requireNonNull(details.unit());
        this.cost = scaled(details.cost(), MONEY_SCALE);
        this.price = sold ? scaled(details.price(), MONEY_SCALE) : null;
        this.threshold = Objects.requireNonNull(scaled(details.threshold(), QUANTITY_SCALE));
        this.supplierId = details.supplierId();
    }

    private ArticleDetails details() {
        return new ArticleDetails(name, barcode, type, rangeId, unit, cost, price, threshold, supplierId);
    }

    private static @Nullable BigDecimal scaled(@Nullable BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
