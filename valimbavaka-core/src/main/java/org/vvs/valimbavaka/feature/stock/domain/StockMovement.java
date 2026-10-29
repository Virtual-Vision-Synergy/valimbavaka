package org.vvs.valimbavaka.feature.stock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stock movement: written once, never modified (the database refuses UPDATE and DELETE). A correction is a
 * new, opposite movement. Article and location are referenced by id (no JPA association between features).
 */
@Entity
@Immutable
@Table(name = "stock_movements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(name = "location_id", nullable = false, updatable = false)
    private UUID locationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 20)
    private StockMovementType type;

    @Column(name = "quantity", nullable = false, updatable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "reference_id", updatable = false)
    private @Nullable UUID referenceId;

    /** Set on insert; the author of the movement is in the journal. */
    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    public static StockMovement of(StockMovementCommand command) {
        var movement = new StockMovement();
        movement.articleId = command.articleId();
        movement.locationId = command.locationId();
        movement.type = command.type();
        movement.quantity = command.quantity();
        movement.referenceId = command.referenceId();
        return movement;
    }
}
