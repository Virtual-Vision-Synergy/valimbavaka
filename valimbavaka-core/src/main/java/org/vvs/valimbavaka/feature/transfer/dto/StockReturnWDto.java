package org.vvs.valimbavaka.feature.transfer.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/** Stock return entered by a seller or the administrator; number, origin and status are set by the server. */
public record StockReturnWDto(
        @NotNull(message = "L'emplacement d'origine est obligatoire")
        UUID fromLocationId,

        @NotNull(message = "L'emplacement de destination est obligatoire")
        UUID toLocationId,

        @NotNull(message = "Les lignes sont obligatoires")
        @Size(min = 1, max = 100, message = "Un retour compte de {min} à {max} lignes")
        @Valid
        List<@NotNull(message = "Une ligne vide n'est pas acceptée") TransferLineWDto> lines,

        @Size(max = 500, message = "La note ne doit pas dépasser {max} caractères") @Nullable
        String note) {}
