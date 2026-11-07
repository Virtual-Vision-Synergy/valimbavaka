package org.vvs.valimbavaka.feature.delivery.domain;

import org.jspecify.annotations.Nullable;

/**
 * Failure reported by the driver (LIV-04).
 *
 * @param reason required reason of the failure
 * @param note optional comment of the driver, trimmed, {@code null} when blank
 */
public record DeliveryFailure(DeliveryFailureReason reason, @Nullable String note) {}
