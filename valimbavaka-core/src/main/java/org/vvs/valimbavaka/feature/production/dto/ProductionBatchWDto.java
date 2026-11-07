package org.vvs.valimbavaka.feature.production.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Creation of a production batch; the number, the status and the author are set by the server. */
public record ProductionBatchWDto(
        @NotNull(message = "Le produit obtenu est obligatoire")
        UUID outputArticleId,

        @NotNull(message = "La quantité produite est obligatoire")
        @Positive(message = "La quantité produite doit être strictement positive")
        @Digits(integer = 9, fraction = 3, message = "La quantité produite a trop de chiffres")
        BigDecimal quantity,

        @NotNull(message = "La vendeuse responsable est obligatoire")
        UUID operatorId,

        @NotEmpty(message = "Le lot doit consommer au moins un entrant")
        @Size(max = 50, message = "Un lot ne peut pas dépasser {max} entrants")
        List<@NotNull(message = "Entrant vide") @Valid ProductionInputWDto> inputs) {}
