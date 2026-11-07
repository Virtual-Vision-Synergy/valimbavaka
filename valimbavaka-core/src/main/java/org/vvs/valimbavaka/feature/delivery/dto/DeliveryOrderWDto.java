package org.vvs.valimbavaka.feature.delivery.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * Order to deliver entered by a seller or the administrator. Number, status, position in the round, prices and
 * amount are set by the server: sent in the body, they are ignored.
 */
public record DeliveryOrderWDto(
        @NotBlank(message = "Le nom du client est obligatoire")
        @Size(max = 120, message = "Le nom du client ne doit pas dépasser {max} caractères")
        String customerName,

        @NotBlank(message = "Le téléphone du client est obligatoire")
        @Pattern(
                regexp = "^\\s*\\+?[0-9][0-9 .-]{6,24}\\s*$",
                message = "Le téléphone accepte des chiffres, des espaces et un + initial (7 chiffres au moins)")
        String customerPhone,

        @NotBlank(message = "L'adresse de livraison est obligatoire")
        @Size(max = 255, message = "L'adresse ne doit pas dépasser {max} caractères")
        String address,

        @Size(max = 80, message = "Le quartier ne doit pas dépasser {max} caractères") @Nullable
        String district,

        @NotNull(message = "Le livreur est obligatoire") UUID driverId,

        @NotNull(message = "La date de livraison est obligatoire")
        LocalDate scheduledDate,

        @NotNull(message = "Les articles sont obligatoires")
        @Size(min = 1, max = 100, message = "Une commande compte de {min} à {max} lignes")
        @Valid
        List<@NotNull(message = "Une ligne vide n'est pas acceptée") DeliveryOrderLineWDto> lines,

        @Size(max = 500, message = "La note ne doit pas dépasser {max} caractères") @Nullable
        String note) {}
