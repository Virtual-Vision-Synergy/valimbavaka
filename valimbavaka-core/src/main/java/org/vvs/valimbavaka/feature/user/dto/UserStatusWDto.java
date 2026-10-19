package org.vvs.valimbavaka.feature.user.dto;

import jakarta.validation.constraints.NotNull;

/** DTO d'écriture du statut d'un compte : seul l'état actif/désactivé est modifiable ici. */
public record UserStatusWDto(
        @NotNull(message = "Le statut est obligatoire") Boolean active) {}
