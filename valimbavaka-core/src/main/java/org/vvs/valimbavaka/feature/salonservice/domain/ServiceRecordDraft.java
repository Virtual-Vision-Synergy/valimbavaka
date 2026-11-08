package org.vvs.valimbavaka.feature.salonservice.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Checked content of a new performed service (SRV-02), ready to be saved.
 *
 * @param salonServiceId catalogue service performed; name, duration and price are copies taken when recording
 * @param seller account recording the service and its full name, copied for display
 * @param place sales channel (commission) and stock location (salon) of the service
 * @param products articles already copied from their sheet, each article once
 */
public record ServiceRecordDraft(
        UUID salonServiceId,
        ServiceCopy service,
        Seller seller,
        Place place,
        @Nullable String customerName,
        @Nullable String note,
        List<ServiceRecordProduct> products) {

    /** Catalogue values copied into the performed service. */
    public record ServiceCopy(String name, int durationMinutes, BigDecimal price) {}

    public record Seller(UUID id, String name) {}

    public record Place(UUID salesChannelId, UUID locationId) {}
}
