package org.vvs.valimbavaka.feature.shooting.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * Write DTO of a shooting exit (SHO-01). No number, status or author here: the server assigns them (the author is
 * the authenticated account).
 */
public record ShootingWDto(
        @NotNull(message = "L'emplacement de sortie est obligatoire") UUID locationId,

        @NotEmpty(message = "Ajoutez au moins un article")
        @Size(max = 50, message = "Une sortie compte {max} articles au maximum")
        List<@Valid @NotNull ShootingItemWDto> items,

        @NotNull(message = "Le retour prévu est obligatoire") Instant expectedReturnAt,

        @Size(max = 255, message = "La note ne doit pas dépasser {max} caractères") @Nullable String note) {}
