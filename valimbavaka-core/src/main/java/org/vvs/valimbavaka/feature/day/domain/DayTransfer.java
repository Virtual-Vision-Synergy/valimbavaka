package org.vvs.valimbavaka.feature.day.domain;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Morning transfer generated for one destination when the day was opened: the transfer is referenced by id (no
 * association between features), its number, line count and total quantity are kept to show the day at a glance.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DayTransfer {

    @Column(name = "transfer_id", nullable = false, updatable = false)
    private UUID transferId;

    @Column(name = "transfer_number", nullable = false, updatable = false, length = 20)
    private String transferNumber;

    @Column(name = "location_id", nullable = false, updatable = false)
    private UUID locationId;

    @Column(name = "line_count", nullable = false, updatable = false)
    private int lineCount;

    @Column(name = "quantity", nullable = false, updatable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    DayTransfer(UUID transferId, String transferNumber, UUID locationId, int lineCount, BigDecimal quantity) {
        this.transferId = transferId;
        this.transferNumber = transferNumber;
        this.locationId = locationId;
        this.lineCount = lineCount;
        this.quantity = quantity;
    }
}
