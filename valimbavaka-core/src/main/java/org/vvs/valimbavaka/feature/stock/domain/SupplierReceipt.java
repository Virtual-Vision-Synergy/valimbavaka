package org.vvs.valimbavaka.feature.stock.domain;

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
import org.vvs.valimbavaka.shared.operation.OperationStatus;

/**
 * Receipt of a supplier delivery (STK-06): the stock enters through one {@code RECEIPT} movement per line,
 * referencing this receipt. A validated receipt is never deleted (JNL-02): an error is corrected by a reversal,
 * which records the inverse movements and marks it {@link OperationStatus#REVERSED}.
 */
@Entity
@Table(name = "supplier_receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplierReceipt extends BaseEntity {

    @Column(name = "number", nullable = false, updatable = false, length = 20)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OperationStatus status;

    @Column(name = "supplier_id", nullable = false, updatable = false)
    private UUID supplierId;

    @Column(name = "supplier_name", nullable = false, updatable = false, length = 150)
    private String supplierName;

    @Column(name = "location_id", nullable = false, updatable = false)
    private UUID locationId;

    @Column(name = "restock_order_id", updatable = false)
    private @Nullable UUID restockOrderId;

    @Column(name = "note", updatable = false, length = 500)
    private @Nullable String note;

    // Loaded lazily, by batches of pages, to keep the paged list free of N+1 queries.
    @ElementCollection
    @CollectionTable(name = "supplier_receipt_lines", joinColumns = @JoinColumn(name = "supplier_receipt_id"))
    @OrderColumn(name = "line_number")
    @BatchSize(size = 100)
    private List<SupplierReceiptLine> lines = new ArrayList<>();

    /** Validated receipt: the service records the stock movements in the same transaction. */
    public static SupplierReceipt create(String number, SupplierReceiptCommand command) {
        var receipt = new SupplierReceipt();
        receipt.number = number;
        receipt.status = OperationStatus.VALIDATED;
        receipt.supplierId = command.supplierId();
        receipt.supplierName = command.supplierName();
        receipt.locationId = command.locationId();
        receipt.restockOrderId = command.restockOrderId();
        receipt.note = command.note();
        command.lines().forEach(line -> receipt.lines.add(SupplierReceiptLine.of(line)));
        return receipt;
    }

    /**
     * Reversal: the receipt becomes {@link OperationStatus#REVERSED}; the service records the inverse movements.
     *
     * @throws org.vvs.valimbavaka.core.exception.ConflictException if the receipt is already reversed
     */
    public void reverse() {
        status.ensureCanReverse();
        this.status = OperationStatus.REVERSED;
    }

    /** Lines in the order they were entered; they never change once recorded. */
    public List<SupplierReceiptLine> getLines() {
        return List.copyOf(lines);
    }
}
