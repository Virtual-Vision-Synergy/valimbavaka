package org.vvs.valimbavaka.feature.transfer.domain;

import java.util.ArrayList;
import java.util.Collections;
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

import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.domain.BaseEntity;

/**
 * Transfer of stock between two locations (TRF-03): origin, destination, lines and status. The stock itself moves
 * through two {@code TRANSFER} movements per line, referencing this transfer; date and author are the audit fields.
 */
@Entity
@Table(name = "transfers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer extends BaseEntity {

    @Column(name = "number", nullable = false, updatable = false, length = 20)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, updatable = false, length = 20)
    private TransferOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "from_location_id", nullable = false, updatable = false)
    private UUID fromLocationId;

    @Column(name = "to_location_id", nullable = false, updatable = false)
    private UUID toLocationId;

    @Column(name = "note", length = 500)
    private @Nullable String note;

    @ElementCollection
    @CollectionTable(name = "transfer_lines", joinColumns = @JoinColumn(name = "transfer_id"))
    @OrderColumn(name = "line_number")
    private List<TransferLine> lines = new ArrayList<>();

    /** The status follows the origin: {@link TransferStatus#RETURN} for a return, validated otherwise. */
    public static Transfer create(String number, TransferCommand command) {
        var transfer = new Transfer();
        transfer.number = number;
        transfer.origin = command.origin();
        transfer.status = command.origin().initialStatus();
        transfer.fromLocationId = command.fromLocationId();
        transfer.toLocationId = command.toLocationId();
        transfer.note = blankToNull(command.note());
        command.lines().forEach(line -> transfer.lines.add(TransferLine.of(line)));
        return transfer;
    }

    /** Lines in the order they were entered; they never change once recorded. */
    public List<TransferLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
