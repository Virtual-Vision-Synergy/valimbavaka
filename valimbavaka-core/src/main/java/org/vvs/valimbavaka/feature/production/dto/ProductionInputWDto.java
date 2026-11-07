package org.vvs.valimbavaka.feature.production.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** One consumed input of a batch: raw material or semi-finished article (ART-04 usage INPUT). */
public record ProductionInputWDto(
        @NotNull(message = "L'entrant est obligatoire") UUID articleId,

        @NotNull(message = "La quantité de l'entrant est obligatoire")
        @Positive(message = "La quantité de l'entrant doit être strictement positive")
        @Digits(integer = 9, fraction = 3, message = "La quantité de l'entrant a trop de chiffres")
        BigDecimal quantity) {}
