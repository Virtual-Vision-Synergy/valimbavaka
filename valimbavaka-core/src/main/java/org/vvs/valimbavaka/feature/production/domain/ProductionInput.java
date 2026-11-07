package org.vvs.valimbavaka.feature.production.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

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

    public ProductionInput(UUID articleId, BigDecimal quantity) {
        this.articleId = articleId;
        this.quantity = quantity;
    }
}
