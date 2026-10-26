package org.vvs.valimbavaka.feature.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;

/** Write DTO for the supplier sheet (create and update); no identifier or status here. */
public record SupplierWDto(
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 150, message = "Le nom ne doit pas dépasser {max} caractères")
        String name,

        @NotNull(message = "Le type est obligatoire") SupplierType type,

        @Size(max = 150, message = "Le nom du contact ne doit pas dépasser {max} caractères") @Nullable
        String contactName,

        @Size(max = 50, message = "Le téléphone ne doit pas dépasser {max} caractères") @Nullable
        String phone,

        @Email(message = "L'email doit être une adresse email valide")
        @Size(max = 255, message = "L'email ne doit pas dépasser {max} caractères")
        @Nullable
        String email,

        @Size(max = 500, message = "L'adresse ne doit pas dépasser {max} caractères") @Nullable
        String address,

        @Size(max = 2000, message = "Les notes ne doivent pas dépasser {max} caractères") @Nullable
        String notes,

        // Version read by the client (SupplierRDto.version): a stale value is refused with 409 instead of silently
        // overwriting a concurrent update. Optional on creation.
        @Nullable Long version) {}
