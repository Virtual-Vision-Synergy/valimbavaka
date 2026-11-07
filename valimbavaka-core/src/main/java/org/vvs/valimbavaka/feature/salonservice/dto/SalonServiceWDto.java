package org.vvs.valimbavaka.feature.salonservice.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/** Write DTO of a salon service (create and update); no identifier or status here. */
public record SalonServiceWDto(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser {max} caractères")
        String name,

        @NotNull(message = "La durée est obligatoire")
        @Min(value = 1, message = "La durée doit être d'au moins {value} minute")
        @Max(value = 600, message = "La durée ne doit pas dépasser {value} minutes")
        Integer durationMinutes,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.00", message = "Le prix ne peut pas être négatif")
        @Digits(integer = 12, fraction = 2, message = "Le prix accepte 2 décimales au maximum")
        BigDecimal price,

        // Optional: without a rate of its own, the channel rule then the seller's rate apply (COM-01 resolver).
        @DecimalMin(value = "0.00", message = "Le taux de commission ne peut pas être négatif")
        @DecimalMax(value = "100.00", message = "Le taux de commission ne peut pas dépasser {value} %")
        @Digits(integer = 3, fraction = 2, message = "Le taux de commission accepte 2 décimales au maximum")
        @Nullable BigDecimal commissionRate,

        // Optional: absent or empty = no product usually consumed.
        @Size(max = 20, message = "Un service ne peut pas lister plus de {max} produits consommés")
        @Nullable List<@Valid @NotNull(message = "Ligne de produit consommé vide") ConsumedProductWDto> products,

        // Version read by the client (SalonServiceRDto.version): a stale value is refused with 409. Optional.
        @Nullable Long version) {}
