package org.vvs.valimbavaka.feature.stock.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * Supplier delivery entered by the administrator (STK-06); number and status are set by the server.
 *
 * @param locationId destination of the stock; absent = the warehouse
 * @param restockOrderId restock order taken back (STK-05), absent for a free entry
 */
public record SupplierReceiptWDto(
        @NotNull(message = "Le fournisseur est obligatoire")
        UUID supplierId,

        @Nullable UUID locationId,

        @Nullable UUID restockOrderId,

        @NotNull(message = "Les lignes sont obligatoires")
        @Size(min = 1, max = 100, message = "Une réception compte de {min} à {max} lignes")
        @Valid
        List<@NotNull(message = "Une ligne vide n'est pas acceptée") SupplierReceiptLineWDto> lines,

        @Size(max = 500, message = "La note ne doit pas dépasser {max} caractères") @Nullable
        String note) {}
