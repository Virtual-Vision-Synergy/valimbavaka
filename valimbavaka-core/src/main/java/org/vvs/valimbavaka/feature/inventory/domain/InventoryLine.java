package org.vvs.valimbavaka.feature.inventory.domain;

import java.math.BigDecimal;
import java.time.Instant;
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
 * Article to count in a session, created when the session is opened (INV-01). The count (INV-02) fills
 * {@code countedQuantity}, {@code expectedQuantity} (theoretical stock frozen at the count) and {@code countedAt};
 * the gap reason is qualified by INV-03. The session is referenced by id.
 */
@Entity
@Table(name = "inventory_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryLine extends BaseEntity {

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "article_code", nullable = false, updatable = false, length = 20)
    private String articleCode;

    @Column(name = "article_name", nullable = false, updatable = false, length = 150)
    private String articleName;

    @Column(name = "barcode", updatable = false, length = 50)
    private @Nullable String barcode;

    @Column(name = "expected_quantity", precision = 12, scale = 3)
    private @Nullable BigDecimal expectedQuantity;

    @Column(name = "counted_quantity", precision = 12, scale = 3)
    private @Nullable BigDecimal countedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 30)
    private @Nullable InventoryReason reason;

    @Column(name = "counted_at")
    private @Nullable Instant countedAt;

    public static InventoryLine toCount(UUID sessionId, InventoryArticle article) {
        var line = new InventoryLine();
        line.sessionId = sessionId;
        line.articleId = article.articleId();
        line.articleCode = article.code();
        line.articleName = article.name();
        line.barcode = article.barcode();
        return line;
    }

    /**
     * Records the physical count (INV-02) and freezes the theoretical stock of the location at this instant, so
     * that later sales do not move the gap. A recount replaces both quantities.
     *
     * @return false when the same count is sent again (offline replay): nothing changes
     */
    public boolean count(BigDecimal counted, BigDecimal expected, Instant at) {
        if (isCountedAs(counted, expected)) {
            return false;
        }
        countedQuantity = counted;
        expectedQuantity = expected;
        countedAt = at;
        return true;
    }

    private boolean isCountedAs(BigDecimal counted, BigDecimal expected) {
        var currentCount = countedQuantity;
        var currentExpected = expectedQuantity;
        return currentCount != null
                && currentExpected != null
                && currentCount.compareTo(counted) == 0
                && currentExpected.compareTo(expected) == 0;
    }
}
