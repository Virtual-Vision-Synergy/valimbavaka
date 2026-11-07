package org.vvs.valimbavaka.feature.stock.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * Generation of a restock order (STK-05).
 *
 * @param articleIds articles to restock; {@code null} or empty = every article under its threshold. An article that
 *     is no longer under its threshold is left out.
 */
public record RestockOrderWDto(
        @Nullable @Size(max = 500, message = "Un bon ne peut pas viser plus de {max} articles")
        List<@NotNull(message = "Identifiant d'article manquant") UUID> articleIds) {}
