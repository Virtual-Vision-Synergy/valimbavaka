package org.vvs.valimbavaka.feature.day.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.domain.BaseEntity;

/**
 * Business day (JR-01): one per date, opened by the morning dispatch of the stock from the warehouse to the points
 * of sale, with the morning transfers it generated (one per destination). Opening date and author are the audit
 * fields ({@code createdAt}, {@code createdBy}).
 */
@Entity
@Table(name = "business_days")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessDay extends BaseEntity {

    @Column(name = "business_date", nullable = false, updatable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DayStatus status;

    @ElementCollection
    @CollectionTable(name = "business_day_transfers", joinColumns = @JoinColumn(name = "business_day_id"))
    @OrderColumn(name = "line_number")
    private List<DayTransfer> transfers = new ArrayList<>();

    /** A day is opened once, on its own date; it starts {@link DayStatus#OPEN}. */
    public static BusinessDay open(LocalDate businessDate) {
        var day = new BusinessDay();
        day.businessDate = businessDate;
        day.status = DayStatus.OPEN;
        return day;
    }

    /**
     * Keeps the morning transfer recorded for one destination.
     *
     * @param quantities quantity of each line of the transfer (its size is the line count)
     */
    public void addTransfer(UUID transferId, String transferNumber, UUID locationId, List<BigDecimal> quantities) {
        var total = quantities.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        transfers.add(new DayTransfer(transferId, transferNumber, locationId, quantities.size(), total));
    }

    /** Locks the day (JR-06): nothing more is recorded for this date. Only an open day can be closed. */
    public void close() {
        if (!isOpen()) {
            throw new IllegalStateException("La journée du " + businessDate + " est déjà clôturée.");
        }
        status = DayStatus.CLOSED;
    }

    /** True while sales can be recorded for this date. */
    public boolean isOpen() {
        return status == DayStatus.OPEN;
    }

    /** Morning transfers in the order they were generated; they never change once recorded. */
    public List<DayTransfer> getTransfers() {
        return Collections.unmodifiableList(transfers);
    }
}
