package org.vvs.valimbavaka.feature.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.inventory.domain.InventoryReason;

/**
 * Line of an inventory session. {@code theoreticalQuantity} is the quantity frozen at the count, or the current
 * stock of the location while the article is not counted; {@code gap} = counted − theoretical, null if not counted.
 */
public record InventoryLineRDto(
        UUID id,
        UUID articleId,
        String articleCode,
        String articleName,
        @Nullable String barcode,
        @Nullable BigDecimal unitCost,
        BigDecimal theoreticalQuantity,
        @Nullable BigDecimal countedQuantity,
        @Nullable BigDecimal gap,
        @Nullable InventoryReason reason) {}
