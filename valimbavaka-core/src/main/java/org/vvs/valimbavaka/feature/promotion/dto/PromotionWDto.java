package org.vvs.valimbavaka.feature.promotion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.promotion.domain.PromotionKind;
import org.vvs.valimbavaka.feature.promotion.domain.PromotionScope;

/**
 * Creation of a promotion (PRO-01); no identifier, status or usage counter here. The rules that depend on the kind,
 * the scope or the other features (period, value, targets, channels) are checked by the service (422).
 */
public record PromotionWDto(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser {max} caractères")
        String name,

        @Size(max = 500, message = "La description ne doit pas dépasser {max} caractères") @Nullable
        String description,

        @NotNull(message = "Le type de remise est obligatoire") PromotionKind kind,

        @NotNull(message = "La valeur est obligatoire")
        @DecimalMin(value = "0", inclusive = false, message = "La valeur doit être positive")
        @Digits(integer = 12, fraction = 2, message = "La valeur a trop de chiffres")
        BigDecimal value,

        // Optional promo code (VTE-04): 3 to 12 letters or digits, stored in upper case.
        @Pattern(regexp = "^\\s*[A-Za-z0-9]{3,12}\\s*$|^\\s*$", message = "Le code compte 3 à 12 lettres ou chiffres")
        @Nullable
        String code,

        @NotNull(message = "Le périmètre est obligatoire") PromotionScope scope,

        // Ranges (scope RANGES) or articles (scope ARTICLES); empty or absent for the whole catalogue.
        @Size(max = 100, message = "Au plus {max} gammes ou articles ciblés") @Nullable
        List<@NotNull UUID> targetIds,

        @NotNull(message = "La date de début est obligatoire") LocalDate startsOn,

        @NotNull(message = "La date de fin est obligatoire") LocalDate endsOn,

        @NotEmpty(message = "Choisissez au moins un canal de vente")
        @Size(max = 10, message = "Au plus {max} canaux de vente")
        List<@NotNull UUID> channelIds,

        @Min(value = 1, message = "La limite d'utilisation est d'au moins 1")
        @Max(value = 1_000_000, message = "La limite d'utilisation ne doit pas dépasser {value}")
        @Nullable
        Integer usageLimit) {}
