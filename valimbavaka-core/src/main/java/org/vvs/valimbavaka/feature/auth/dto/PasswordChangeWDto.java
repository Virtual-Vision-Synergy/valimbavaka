package org.vvs.valimbavaka.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Changement de son propre mot de passe : l'ancien est exigé, le nouveau suit la convention §9.2. */
public record PasswordChangeWDto(
        @NotBlank(message = "Le mot de passe actuel est obligatoire")
        @Size(max = 128, message = "Le mot de passe ne doit pas dépasser {max} caractères")
        String currentPassword,
        // 15 caractères minimum sans MFA (NIST SP 800-63B, convention §9.2).
        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 15, max = 128, message = "Le mot de passe doit contenir entre {min} et {max} caractères")
        String newPassword) {}
