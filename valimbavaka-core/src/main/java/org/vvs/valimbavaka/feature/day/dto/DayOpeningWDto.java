package org.vvs.valimbavaka.feature.day.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Morning dispatch that opens the day. The date is always today's (set by the server), as are the transfers: one
 * per destination, the same article sent twice to the same destination being added up. No line: the day is
 * opened without transfer.
 */
public record DayOpeningWDto(
        @NotNull(message = "Les lignes sont obligatoires")
        @Size(max = 300, message = "La répartition compte {max} lignes au maximum")
        @Valid
        List<@NotNull(message = "Une ligne vide n'est pas acceptée") DispatchLineWDto> lines) {}
