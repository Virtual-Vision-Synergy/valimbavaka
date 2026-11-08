package org.vvs.valimbavaka.feature.salonservice.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
 * Salon service performed for a customer (SRV-02): copy of the catalogue service (name, duration, price), seller,
 * sales channel and products consumed from the salon stock. A validated operation (JNL-02): never updated nor
 * deleted; its time is {@code createdAt}.
 */
@Entity
@Table(name = "service_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceRecord extends BaseEntity {

    /** Server-assigned number, {@code PRE-0001}, never reused. */
    @Column(name = "number", nullable = false, updatable = false, length = 20)
    private String number;

    @Column(name = "salon_service_id", nullable = false, updatable = false)
    private UUID salonServiceId;

    @Column(name = "service_name", nullable = false, updatable = false, length = 100)
    private String serviceName;

    @Column(name = "duration_minutes", nullable = false, updatable = false)
    private int durationMinutes;

    /** Price charged, in ariary: revenue of the service for the commission (COM-02). */
    @Column(name = "price", nullable = false, updatable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @Column(name = "customer_name", updatable = false, length = 100)
    private @Nullable String customerName;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "seller_name", nullable = false, updatable = false)
    private String sellerName;

    @Column(name = "sales_channel_id", nullable = false, updatable = false)
    private UUID salesChannelId;

    /** Stock location the products leave: the salon. */
    @Column(name = "location_id", nullable = false, updatable = false)
    private UUID locationId;

    @Column(name = "note", updatable = false)
    private @Nullable String note;

    @ElementCollection
    @CollectionTable(name = "service_record_products", joinColumns = @JoinColumn(name = "service_record_id"))
    @OrderColumn(name = "line_no")
    @BatchSize(size = 100)
    private List<ServiceRecordProduct> products = new ArrayList<>();

    public static ServiceRecord record(String number, ServiceRecordDraft draft) {
        var serviceRecord = new ServiceRecord();
        serviceRecord.number = number;
        serviceRecord.salonServiceId = draft.salonServiceId();
        serviceRecord.serviceName = draft.service().name();
        serviceRecord.durationMinutes = draft.service().durationMinutes();
        serviceRecord.price = draft.service().price();
        serviceRecord.customerName = draft.customerName();
        serviceRecord.sellerId = draft.seller().id();
        serviceRecord.sellerName = draft.seller().name();
        serviceRecord.salesChannelId = draft.place().salesChannelId();
        serviceRecord.locationId = draft.place().locationId();
        serviceRecord.note = draft.note();
        serviceRecord.products.addAll(draft.products());
        return serviceRecord;
    }
}
