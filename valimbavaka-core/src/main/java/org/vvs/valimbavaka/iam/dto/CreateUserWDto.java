package org.vvs.valimbavaka.iam.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO d'écriture (création) pour {@link org.vvs.valimbavaka.iam.domain.User}
 */
public record CreateUserWDto(
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email doit être une adresse email valide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins {min} caractères")
        String password,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(max = 100, message = "Le prénom ne doit pas dépasser {max} caractères")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas dépasser {max} caractères")
        String lastName,

        @NotNull(message = "Le rôle est obligatoire")
        UUID roleId) {
}
