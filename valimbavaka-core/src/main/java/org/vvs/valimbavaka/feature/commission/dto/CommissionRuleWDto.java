package org.vvs.valimbavaka.feature.commission.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.commission.domain.CommissionBase;

/** Write DTO of a commission rule (create and update); no identifier or status here. */
public record CommissionRuleWDto(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser {max} caractères")
        String name,

        @NotNull(message = "Le canal de vente est obligatoire")
        UUID salesChannelId,

        @NotNull(message = "Le taux de commission est obligatoire")
        @DecimalMin(value = "0.00", message = "Le taux de commission ne peut pas être négatif")
        @DecimalMax(value = "100.00", message = "Le taux de commission ne peut pas dépasser {value} %")
        @Digits(integer = 3, fraction = 2, message = "Le taux de commission accepte 2 décimales au maximum")
        BigDecimal rate,

        @NotNull(message = "La base de calcul est obligatoire")
        CommissionBase base,

        // Version read by the client (CommissionRuleRDto.version): a stale value is refused with 409. Optional.
        @Nullable Long version) {}
