package org.vvs.valimbavaka.feature.salonservice.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Product usually consumed by a salon service (SRV-01): article referenced by its identifier (D8) and quantity in the
 * article's unit, three decimals. SRV-02 pre-fills the products of a performed service with these lines.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsumedProduct {

    private static final int QUANTITY_SCALE = 3;

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    public ConsumedProduct(UUID articleId, BigDecimal quantity) {
        this.articleId = articleId;
        this.quantity = quantity.setScale(QUANTITY_SCALE, RoundingMode.UNNECESSARY);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof ConsumedProduct product
                && articleId.equals(product.articleId)
                && quantity.compareTo(product.quantity) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(articleId, quantity.stripTrailingZeros());
    }
}
