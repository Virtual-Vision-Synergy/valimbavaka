package org.vvs.valimbavaka.feature.inventory.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** Physical count of one line (INV-02): only the counted quantity; the theoretical stock is set by the server. */
public record InventoryCountWDto(
        @NotNull(message = "La quantité comptée est obligatoire")
        @DecimalMin(value = "0", message = "La quantité comptée ne peut pas être négative")
        @Digits(integer = 9, fraction = 3, message = "La quantité comptée accepte 3 décimales au maximum")
        BigDecimal countedQuantity) {}
