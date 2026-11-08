package org.vvs.valimbavaka.feature.production.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Raw material or semi-finished article consumed by a batch, referenced by its UUID (no JPA link to article). */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductionInput {

    @Column(name = "article_id", nullable = false)
    private UUID articleId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    /** Purchase cost frozen when the batch is completed (PRD-02); null before, or if the article has no cost. */
    @Column(name = "unit_cost", precision = 14, scale = 2)
    private @Nullable BigDecimal unitCost;

    public ProductionInput(UUID articleId, BigDecimal quantity) {
        this.articleId = articleId;
        this.quantity = quantity;
    }

    /** Same input with its cost frozen: a later change of the article cost does not alter the batch. */
    ProductionInput withUnitCost(@Nullable BigDecimal cost) {
        var input = new ProductionInput(articleId, quantity);
        input.unitCost = cost;
        return input;
    }
}
