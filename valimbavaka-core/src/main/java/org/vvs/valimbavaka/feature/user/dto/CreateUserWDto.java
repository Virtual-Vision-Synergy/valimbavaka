package org.vvs.valimbavaka.feature.user.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTO d'écriture (création) d'un utilisateur. */
public record CreateUserWDto(
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email doit être une adresse email valide")
        @Size(max = 255, message = "L'email ne doit pas dépasser {max} caractères")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 128, message = "Le mot de passe doit contenir entre {min} et {max} caractères")
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
