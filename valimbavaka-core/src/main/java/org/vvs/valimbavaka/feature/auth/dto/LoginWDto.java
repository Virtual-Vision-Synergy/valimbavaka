package org.vvs.valimbavaka.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Identifiants de connexion. Aucun champ rôle : le rôle est celui du compte, défini par l'administrateur. */
public record LoginWDto(
        @NotBlank(message = "L'email est obligatoire")
        @Size(max = 255, message = "L'email ne doit pas dépasser {max} caractères")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(max = 128, message = "Le mot de passe ne doit pas dépasser {max} caractères")
        String password) {
}
