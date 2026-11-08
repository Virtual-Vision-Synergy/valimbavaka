package org.vvs.valimbavaka.feature.inventory.domain;

import java.time.LocalDate;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** The four pieces of information of an opening, once checked: location, scope, responsible and count date. */
public record InventoryOpening(
        UUID locationId,
        InventoryScope scope,
        @Nullable UUID rangeId,
        UUID responsibleId,
        String responsibleName,
        LocalDate countDate) {}
