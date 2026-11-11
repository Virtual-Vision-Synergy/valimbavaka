package org.vvs.valimbavaka.feature.saledetail.dto;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Promotion that gave the discount of a sale; {@code code} is {@code null} for an automatic promotion. */
public record SaleDetailPromotionRDto(UUID id, String name, @Nullable String code) {}
