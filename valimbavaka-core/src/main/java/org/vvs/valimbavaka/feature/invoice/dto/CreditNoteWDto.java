package org.vvs.valimbavaka.feature.invoice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Reason of a credit note (FAC-04): the only input; the invoice comes from the URL, the content from the invoice. */
public record CreditNoteWDto(
        @NotBlank(message = "Le motif est obligatoire")
        @Size(max = 500, message = "Le motif ne doit pas dépasser {max} caractères")
        String reason) {}
