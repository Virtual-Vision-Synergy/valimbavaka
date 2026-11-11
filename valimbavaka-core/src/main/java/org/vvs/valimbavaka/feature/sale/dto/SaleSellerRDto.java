package org.vvs.valimbavaka.feature.sale.dto;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Active seller who can be credited with a sale; no email nor commission rate is exposed. */
public record SaleSellerRDto(
        UUID id,
        String firstName,
        String lastName,
        @Nullable UUID salesChannelId) {}
