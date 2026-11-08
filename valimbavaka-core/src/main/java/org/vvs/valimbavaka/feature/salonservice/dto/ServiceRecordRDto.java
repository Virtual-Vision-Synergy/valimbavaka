package org.vvs.valimbavaka.feature.salonservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Performed salon service (SRV-02). {@code serviceName}, {@code durationMinutes} and {@code price} are copies of the
 * catalogue when recorded; {@code price} is the revenue used by the commission (COM-02).
 *
 * @param sellerId account that recorded the service (seller of the commission)
 * @param sellerName full name of that account when recorded
 * @param salesChannelId channel passed to the commission resolver (the store channel)
 * @param locationId stock location the products left (the salon)
 */
public record ServiceRecordRDto(
        UUID id,
        String number,
        UUID salonServiceId,
        String serviceName,
        int durationMinutes,
        BigDecimal price,
        @Nullable String customerName,
        UUID sellerId,
        String sellerName,
        UUID salesChannelId,
        UUID locationId,
        List<ServiceRecordProductRDto> products,
        @Nullable String note,
        Instant performedAt) {}
