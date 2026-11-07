package org.vvs.valimbavaka.feature.sale.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Article that can be sold (active finished product with a price).
 *
 * @param stock quantity per stock location of the channels (location id → quantity); a location where the article
 *     never moved is absent (zero)
 */
public record SaleArticleRDto(
        UUID id,
        String code,
        @Nullable String barcode,
        String name,
        @Nullable UUID rangeId,
        BigDecimal price,
        Map<UUID, BigDecimal> stock) {}
