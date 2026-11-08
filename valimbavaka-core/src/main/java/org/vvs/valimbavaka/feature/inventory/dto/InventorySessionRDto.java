package org.vvs.valimbavaka.feature.inventory.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.inventory.domain.InventoryScope;
import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;

/**
 * Inventory session; {@code openedAt} / {@code openedBy} are the creation audit, {@code lineCount} the articles and
 * {@code gapCount} the counted lines whose count differs from the theoretical stock (INV-05).
 */
public record InventorySessionRDto(
        UUID id,
        String number,
        UUID locationId,
        String locationCode,
        String locationLabel,
        InventoryScope scope,
        @Nullable UUID rangeId,
        UUID responsibleId,
        String responsibleName,
        LocalDate countDate,
        InventoryStatus status,
        Instant openedAt,
        @Nullable UUID openedBy,
        @Nullable Instant closedAt,
        long lineCount,
        long gapCount,
        long version) {}
