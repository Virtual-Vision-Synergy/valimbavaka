package org.vvs.valimbavaka.feature.article.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.article.domain.ArticleType;
import org.vvs.valimbavaka.feature.article.domain.ArticleUnit;

/** Write DTO for the article sheet (create and update); no identifier, code or status here. */
public record ArticleWDto(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 120, message = "Le nom ne doit pas dépasser {max} caractères")
        String name,

        @Pattern(regexp = "^$|^\\d{13}$", message = "Le code-barres doit contenir 13 chiffres") @Nullable
        String barcode,

        @NotNull(message = "Le type est obligatoire") ArticleType type,

        @Nullable UUID rangeId,

        @NotNull(message = "L'unité est obligatoire") ArticleUnit unit,

        @DecimalMin(value = "0", message = "Le coût ne peut pas être négatif")
        @Digits(integer = 12, fraction = 2, message = "Le coût a trop de chiffres")
        @Nullable
        BigDecimal cost,

        @DecimalMin(value = "0", message = "Le prix ne peut pas être négatif")
        @Digits(integer = 12, fraction = 2, message = "Le prix a trop de chiffres")
        @Nullable
        BigDecimal price,

        @NotNull(message = "Le seuil est obligatoire")
        @DecimalMin(value = "0", message = "Le seuil ne peut pas être négatif")
        @Digits(integer = 9, fraction = 3, message = "Le seuil a trop de chiffres")
        BigDecimal threshold,

        @Nullable UUID supplierId,

        // Version read by the client (ArticleRDto.version): a stale value is refused with 409 instead of silently
        // overwriting a concurrent update. Optional on creation.
        @Nullable Long version) {}
