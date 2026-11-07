package org.vvs.valimbavaka.feature.transfer.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Article and quantity of a transfer line; the article is referenced by id (no association between features). */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferLine {

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "quantity", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    static TransferLine of(TransferLineCommand command) {
        var line = new TransferLine();
        line.articleId = command.articleId();
        line.quantity = command.quantity();
        return line;
    }
}
