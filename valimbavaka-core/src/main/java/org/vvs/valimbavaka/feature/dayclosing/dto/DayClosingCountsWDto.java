package org.vvs.valimbavaka.feature.dayclosing.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Counts of today's closing (JR-05): the lines are saved together, or none is. */
public record DayClosingCountsWDto(
        @NotEmpty(message = "Au moins un comptage est obligatoire")
        @Size(max = 1000, message = "Le comptage accepte {max} lignes au maximum")
        @Valid
        List<@NotNull(message = "Une ligne vide n'est pas acceptée") ClosingCountWDto> counts) {}
