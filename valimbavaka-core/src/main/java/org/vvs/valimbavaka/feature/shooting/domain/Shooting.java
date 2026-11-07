package org.vvs.valimbavaka.feature.shooting.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.domain.BaseEntity;

/**
 * Products taken out of a stock location for a photo shooting (SHO-01), so that they are not mistaken for losses.
 * A validated operation (JNL-02): never deleted; it ends with the return of the products (SHO-02).
 */
@Entity
@Table(name = "shootings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shooting extends BaseEntity {

    /** Server-assigned number, {@code SHO-0001}, never reused. */
    @Column(name = "number", nullable = false, updatable = false, length = 20)
    private String number;

    @Column(name = "location_id", nullable = false, updatable = false)
    private UUID locationId;

    @Column(name = "taken_by_name", nullable = false, updatable = false)
    private String takenByName;

    @Column(name = "expected_return_at", nullable = false)
    private Instant expectedReturnAt;

    @Column(name = "note")
    private @Nullable String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShootingStatus status;

    @ElementCollection
    @CollectionTable(name = "shooting_items", joinColumns = @JoinColumn(name = "shooting_id"))
    @OrderColumn(name = "line_no")
    @BatchSize(size = 100)
    private List<ShootingItem> items = new ArrayList<>();

    public static Shooting open(String number, ShootingDraft draft) {
        var shooting = new Shooting();
        shooting.number = number;
        shooting.locationId = draft.locationId();
        shooting.takenByName = draft.takenByName();
        shooting.expectedReturnAt = draft.expectedReturnAt();
        shooting.note = draft.note();
        shooting.status = ShootingStatus.OUT;
        shooting.items.addAll(draft.items());
        return shooting;
    }

    /** Still out after its expected return (shown as late, counted by SHO-03). */
    public boolean isLateAt(Instant now) {
        return status == ShootingStatus.OUT && expectedReturnAt.isBefore(now);
    }
}
