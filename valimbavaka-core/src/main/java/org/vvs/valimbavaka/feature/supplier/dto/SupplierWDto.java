package org.vvs.valimbavaka.feature.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;
import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;

/** Write DTO for the supplier sheet (create and update); no identifier or status here. */
public record SupplierWDto(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed {max} characters")
        String name,

        @NotNull(message = "Type is required")
        SupplierType type,

        @Size(max = 150, message = "Contact name must not exceed {max} characters")
        @Nullable String contactName,

        @Size(max = 50, message = "Phone must not exceed {max} characters")
        @Nullable String phone,

        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed {max} characters")
        @Nullable String email,

        @Size(max = 500, message = "Address must not exceed {max} characters")
        @Nullable String address,

        @Size(max = 2000, message = "Notes must not exceed {max} characters")
        @Nullable String notes) {
}
