package org.vvs.valimbavaka.feature.salonservice.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * Write DTO of a performed salon service (SRV-02). No number, price, seller, channel nor location here: the server
 * copies the price from the catalogue, takes the authenticated account as seller and consumes from the salon stock.
 *
 * @param products products actually consumed, adjusted by the seller; {@code null} = the catalogue recipe of the
 *     service, empty = nothing consumed
 */
public record ServiceRecordWDto(
        @NotNull(message = "Le service salon est obligatoire")
        UUID salonServiceId,

        @Size(max = 100, message = "Le nom de la cliente ne doit pas dépasser {max} caractères") @Nullable
        String customerName,

        @Size(max = 20, message = "Une prestation consomme {max} produits au maximum") @Nullable
        List<@Valid @NotNull ConsumedProductWDto> products,

        @Size(max = 255, message = "La note ne doit pas dépasser {max} caractères") @Nullable
        String note) {}
