package org.vvs.valimbavaka.feature.invoice.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.invoice.dto.CreditNoteRDto;
import org.vvs.valimbavaka.feature.invoice.dto.CreditNoteWDto;
import org.vvs.valimbavaka.feature.invoice.service.CreditNoteService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/**
 * Credit notes of the invoices (FAC-04): issued by the administrator for a reason, read like the invoice. No update,
 * no delete: a credit note is a legal document.
 */
@Tag(name = "Credit notes")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)"),
    @ApiResponse(responseCode = "404", description = "Invoice or credit note not found (NOT_FOUND)")
})
@RestController
@RequestMapping("/api/v1/invoices")
@Validated
public class CreditNoteController {

    private final CreditNoteService creditNoteService;

    public CreditNoteController(CreditNoteService creditNoteService) {
        this.creditNoteService = creditNoteService;
    }

    @PostMapping("/{invoiceId}/credit-note")
    @Operation(summary = "Issues the credit note of an invoice: reason required, the sale is cancelled and restocked")
    @ApiResponse(responseCode = "201", description = "Credit note issued")
    @ApiResponse(responseCode = "409", description = "Invoice already credited, or day of the sale closed (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Missing or too long reason (VALIDATION_FAILED)")
    @HasPermission("invoices.credit")
    public ResponseEntity<CreditNoteRDto> issueCreditNote(
            @PathVariable UUID invoiceId, @Valid @RequestBody CreditNoteWDto request) {
        var created = creditNoteService.issue(invoiceId, request.reason());
        return ResponseEntity.created(URI.create("/api/v1/invoices/" + invoiceId + "/credit-note"))
                .body(created);
    }

    @GetMapping("/{invoiceId}/credit-note")
    @Operation(summary = "The credit note of an invoice")
    @ApiResponse(responseCode = "200", description = "Credit note")
    @HasPermission("sales.read")
    public CreditNoteRDto getCreditNoteByInvoiceId(@PathVariable UUID invoiceId) {
        return creditNoteService.getCreditNoteByInvoiceId(invoiceId);
    }
}
