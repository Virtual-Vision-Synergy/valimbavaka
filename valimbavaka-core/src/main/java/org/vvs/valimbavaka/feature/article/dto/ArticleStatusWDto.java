package org.vvs.valimbavaka.feature.article.dto;

import jakarta.validation.constraints.NotNull;

/** Write DTO for an article's status: only the active/inactive state can be changed here. */
public record ArticleStatusWDto(
        @NotNull(message = "L'état est obligatoire") Boolean active) {}
