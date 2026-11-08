package org.vvs.valimbavaka.feature.inventory.domain;

import java.time.Instant;
import java.time.LocalDate;
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
 * Inventory session of one location (INV-01). Opened {@link InventoryStatus#IN_PROGRESS}; at most one session in
 * progress per location (unique index). The opening instant and its author are the audit fields
 * {@code createdAt} / {@code createdBy}. Location, range and responsible are referenced by id (other features).
 */
@Entity
@Table(name = "inventory_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventorySession extends BaseEntity {

    @Column(name = "number", nullable = false, updatable = false, length = 20)
    private String number;

    @Column(name = "location_id", nullable = false, updatable = false)
    private UUID locationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, updatable = false, length = 20)
    private InventoryScope scope;

    @Column(name = "range_id", updatable = false)
    private @Nullable UUID rangeId;

    @Column(name = "responsible_id", nullable = false)
    private UUID responsibleId;

    /** Name of the responsible when the session was opened: the history keeps it even if the account changes. */
    @Column(name = "responsible_name", nullable = false, length = 201)
    private String responsibleName;

    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InventoryStatus status;

    /** Set by the closing (INV-04). */
    @Column(name = "closed_at")
    private @Nullable Instant closedAt;

    public static InventorySession open(String number, InventoryOpening opening) {
        var session = new InventorySession();
        session.number = number;
        session.locationId = opening.locationId();
        session.scope = opening.scope();
        session.rangeId = opening.rangeId();
        session.responsibleId = opening.responsibleId();
        session.responsibleName = opening.responsibleName();
        session.countDate = opening.countDate();
        session.status = InventoryStatus.IN_PROGRESS;
        return session;
    }

    /** Closes the session (INV-04) once the stock is adjusted; the caller checks it is still in progress. */
    public void close(Instant at) {
        status = InventoryStatus.CLOSED;
        closedAt = at;
    }
}
