package org.vvs.valimbavaka.feature.invoice.controller;

import java.time.LocalDate;

import jakarta.validation.Valid;

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

import org.vvs.valimbavaka.feature.invoice.dto.SalesBookRowRDto;
import org.vvs.valimbavaka.feature.invoice.service.SalesBookService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.excel.annotation.ExcelExport;

/**
 * Sales book (FAC-06), administrator only ({@code invoices.export}). The list is paged and /export, /export/pdf
 * download the same lines as xlsx and PDF (SYS-01); a period is required and bounded.
 */
@Tag(name = "Invoices")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Missing or malformed period (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)"),
    @ApiResponse(responseCode = "422", description = "Invalid period or export too large (VALIDATION_FAILED)")
})
@RestController
@RequestMapping("/api/v1/invoices/sales-book")
@Validated
public class SalesBookController {

    private final SalesBookService salesBookService;

    public SalesBookController(SalesBookService salesBookService) {
        this.salesBookService = salesBookService;
    }

    @GetMapping
    @Operation(summary = "Sales book of a period: invoices and credit notes (negative); /export downloads the xlsx")
    @ApiResponse(responseCode = "200", description = "Page of lines of the book")
    @HasPermission("invoices.export")
    @ExcelExport(
            target = "sales-book",
            filename = "livre-des-ventes",
            sheet = "Livre des ventes",
            title = "Livre des ventes")
    public PageResponse<SalesBookRowRDto> getSalesBook(
            @Valid PageQuery query,
            @Parameter(description = "First business day, yyyy-MM-dd")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @Parameter(description = "Last business day (included), yyyy-MM-dd; 366 days after from at most")
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {
        return salesBookService.getSalesBook(query, from, to);
    }
}
