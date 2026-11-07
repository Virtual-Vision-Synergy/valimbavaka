package org.vvs.valimbavaka.feature.inventory.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.inventory.domain.InventoryScope;

/**
 * Opening of an inventory session (INV-01): the four required pieces of information. {@code rangeId} is required
 * with the {@code RANGE} scope and refused with {@code ALL}. No identifier, status or number here.
 */
public record InventorySessionWDto(
        @NotNull(message = "L'emplacement est obligatoire") UUID locationId,
        @NotNull(message = "Le périmètre est obligatoire") InventoryScope scope,
        @Nullable UUID rangeId,
        @NotNull(message = "Le responsable est obligatoire") UUID responsibleId,
        @NotNull(message = "La date de comptage est obligatoire") LocalDate countDate) {}
