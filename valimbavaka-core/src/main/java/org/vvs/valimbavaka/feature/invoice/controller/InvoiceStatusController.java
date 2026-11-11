package org.vvs.valimbavaka.feature.invoice.controller;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.invoice.dto.InvoiceStatusTotalsRDto;
import org.vvs.valimbavaka.feature.invoice.service.InvoiceStatusService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Follow-up of the invoice statuses (FAC-05): a read, for the administrator only. */
@Tag(name = "Invoices")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/invoices/status-totals")
@Validated
public class InvoiceStatusController {

    private final InvoiceStatusService invoiceStatusService;

    public InvoiceStatusController(InvoiceStatusService invoiceStatusService) {
        this.invoiceStatusService = invoiceStatusService;
    }

    @GetMapping
    @Operation(
            summary = "Number and amount of the invoices of each status (paid, credited, cancelled)",
            description = "Business days from..to inclusive; to = today (Indian/Antananarivo) and from = first day of"
                    + " that month by default. Every status is listed, at zero when empty.")
    @ApiResponse(responseCode = "200", description = "One entry per status and the consolidated total")
    @ApiResponse(responseCode = "422", description = "from after to (VALIDATION_FAILED)")
    @HasPermission("reports.read")
    public InvoiceStatusTotalsRDto getStatusTotals(
            @Parameter(description = "First business day, yyyy-MM-dd")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable
                    LocalDate from,
            @Parameter(description = "Last business day, yyyy-MM-dd")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable
                    LocalDate to) {
        return invoiceStatusService.getStatusTotals(from, to);
    }
}
