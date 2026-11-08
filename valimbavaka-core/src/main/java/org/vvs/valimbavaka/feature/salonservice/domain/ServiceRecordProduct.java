package org.vvs.valimbavaka.feature.salonservice.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Product consumed by a performed service (SRV-02), taken out of the salon stock. Code and name are copied when the
 * service is recorded, so that it stays readable if the article sheet changes; the article is referenced by id.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceRecordProduct {

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "article_code", nullable = false, updatable = false, length = 20)
    private String articleCode;

    @Column(name = "article_name", nullable = false, updatable = false, length = 120)
    private String articleName;

    /** Positive quantity consumed, in the article's unit (the stock movement carries the negative sign). */
    @Column(name = "quantity", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    public static ServiceRecordProduct of(UUID articleId, String articleCode, String articleName, BigDecimal quantity) {
        var product = new ServiceRecordProduct();
        product.articleId = articleId;
        product.articleCode = articleCode;
        product.articleName = articleName;
        product.quantity = quantity;
        return product;
    }
}
