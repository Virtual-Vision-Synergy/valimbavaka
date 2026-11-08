package org.vvs.valimbavaka.feature.shooting.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One article taken out for a shooting. Code and name are copied when the exit is recorded, so that the exit stays
 * readable even if the article sheet changes later; the article itself is referenced by id (no JPA association).
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShootingItem {

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "article_code", nullable = false, updatable = false, length = 20)
    private String articleCode;

    @Column(name = "article_name", nullable = false, updatable = false, length = 120)
    private String articleName;

    /** Positive quantity taken out (the stock movement carries the negative sign). */
    @Column(name = "quantity", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    public static ShootingItem of(UUID articleId, String articleCode, String articleName, BigDecimal quantity) {
        var item = new ShootingItem();
        item.articleId = articleId;
        item.articleCode = articleCode;
        item.articleName = articleName;
        item.quantity = quantity;
        return item;
    }
}
