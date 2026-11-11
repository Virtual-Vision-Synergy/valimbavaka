package org.vvs.valimbavaka.feature.invoice.controller;

import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.invoice.dto.InvoiceRDto;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/**
 * Invoices issued by the sales (FAC-01). An invoice is issued by the recording of a sale, never by a request with
 * free lines, and is never modified nor deleted; the only write is the re-edition of the invoice of an existing sale
 * (FAC-02), which takes no body.
 */
@Tag(name = "Invoices")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)"),
    @ApiResponse(responseCode = "404", description = "Invoice not found (NOT_FOUND)")
})
@RestController
@RequestMapping("/api/v1/invoices")
@Validated
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "One invoice with its lines")
    @ApiResponse(responseCode = "200", description = "Invoice")
    @HasPermission("sales.read")
    public InvoiceRDto getInvoiceById(@PathVariable UUID id) {
        return invoiceService.getInvoiceById(id);
    }

    @GetMapping("/sales/{saleId}")
    @Operation(summary = "The invoice of a sale")
    @ApiResponse(responseCode = "200", description = "Invoice of the sale")
    @HasPermission("sales.read")
    public InvoiceRDto getInvoiceBySaleId(@PathVariable UUID saleId) {
        return invoiceService.getInvoiceBySaleId(saleId);
    }

    @PostMapping("/sales/{saleId}/regenerate")
    @Operation(summary = "Regenerate the invoice of an existing sale (same number and content)")
    @ApiResponse(responseCode = "200", description = "Invoice of the sale")
    @HasPermission("invoices.regenerate")
    public InvoiceRDto regenerateForSale(@PathVariable UUID saleId) {
        return invoiceService.regenerateForSale(saleId);
    }
}
