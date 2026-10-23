package org.vvs.valimbavaka.feature.user.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/** DTO d'écriture (création) d'un utilisateur ; canal et commission sont réservés au rôle vendeuse. */
public record CreateUserWDto(
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email doit être une adresse email valide")
        @Size(max = 255, message = "L'email ne doit pas dépasser {max} caractères")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        // 15 caractères minimum sans MFA (NIST SP 800-63B, convention §9.2).
        @Size(min = 15, max = 128, message = "Le mot de passe doit contenir entre {min} et {max} caractères")
        String password,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 100, message = "Le prénom ne doit pas dépasser {max} caractères")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser {max} caractères")
        String lastName,

        @NotNull(message = "Le rôle est obligatoire") UUID roleId,

        @Nullable UUID salesChannelId,

        @DecimalMin(value = "0.00", message = "Le taux de commission ne peut pas être négatif")
        @DecimalMax(value = "100.00", message = "Le taux de commission ne peut pas dépasser {value} %")
        @Digits(integer = 3, fraction = 2, message = "Le taux de commission accepte 2 décimales au maximum")
        @Nullable
        BigDecimal commissionRate) {}
