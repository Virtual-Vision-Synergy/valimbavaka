package org.vvs.valimbavaka.feature.stock.controller;

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

import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptRDto;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptReversalWDto;
import org.vvs.valimbavaka.feature.stock.dto.SupplierReceiptWDto;
import org.vvs.valimbavaka.feature.stock.service.SupplierReceiptService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Supplier receipts (STK-06): administrator only, permission {@code stocks.receive}. No update, no delete. */
@Tag(name = "Supplier receipts")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/stocks/receipts")
@Validated
public class SupplierReceiptController {

    private final SupplierReceiptService supplierReceiptService;

    public SupplierReceiptController(SupplierReceiptService supplierReceiptService) {
        this.supplierReceiptService = supplierReceiptService;
    }

    @PostMapping
    @Operation(summary = "Records a supplier delivery: the received goods enter the stock")
    @ApiResponse(responseCode = "201", description = "Receipt recorded")
    @ApiResponse(responseCode = "404", description = "Unknown supplier, article or restock order (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Restock order already received (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("stocks.receive")
    public ResponseEntity<SupplierReceiptRDto> receive(@Valid @RequestBody SupplierReceiptWDto request) {
        var created = supplierReceiptService.receive(request);
        return ResponseEntity.created(URI.create("/api/v1/stocks/receipts/" + created.id()))
                .body(created);
    }

    @GetMapping
    @Operation(summary = "Paged list of the supplier receipts with their lines, newest first")
    @ApiResponse(responseCode = "200", description = "Page of receipts")
    @ApiResponse(responseCode = "422", description = "Invalid paging (VALIDATION_FAILED)")
    @HasPermission("stocks.receive")
    public PageResponse<SupplierReceiptRDto> getReceipts(@Valid PageQuery query) {
        return supplierReceiptService.getReceipts(query);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Supplier receipt with its lines")
    @ApiResponse(responseCode = "200", description = "Receipt")
    @ApiResponse(responseCode = "404", description = "Receipt not found (NOT_FOUND)")
    @HasPermission("stocks.receive")
    public SupplierReceiptRDto getReceiptById(@PathVariable UUID id) {
        return supplierReceiptService.getReceiptById(id);
    }

    @PostMapping("/{id}/reverse")
    @Operation(summary = "Reverses a receipt: inverse stock movements, the receipt becomes REVERSED")
    @ApiResponse(responseCode = "200", description = "Receipt reversed")
    @ApiResponse(responseCode = "404", description = "Receipt not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Already reversed or goods no longer in stock (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Missing reason (VALIDATION_FAILED)")
    @HasPermission("stocks.receive")
    public SupplierReceiptRDto reverse(@PathVariable UUID id, @Valid @RequestBody SupplierReceiptReversalWDto request) {
        return supplierReceiptService.reverse(id, request.reason());
    }
}
