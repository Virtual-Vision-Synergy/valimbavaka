package org.vvs.valimbavaka.feature.transfer.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.dto.StockReturnWDto;
import org.vvs.valimbavaka.feature.transfer.dto.TransferRDto;
import org.vvs.valimbavaka.feature.transfer.dto.TransferSummaryRDto;
import org.vvs.valimbavaka.feature.transfer.service.TransferService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/**
 * Transfers of stock between locations: history for the administrator (TRF-01); a stock return is entered by a seller
 * or the administrator (TRF-03).
 */
@Tag(name = "Transfers")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/transfers")
@Validated
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping
    @Operation(summary = "Paged history of the transfers, newest first, filterable by origin")
    @ApiResponse(responseCode = "200", description = "Page of transfers")
    @ApiResponse(responseCode = "422", description = "Invalid paging (VALIDATION_FAILED)")
    @HasPermission("transfers.read")
    public PageResponse<TransferSummaryRDto> getTransfers(
            @Valid PageQuery query, @RequestParam(required = false) @Nullable TransferOrigin origin) {
        return transferService.getTransfers(query, origin);
    }

    @PostMapping("/returns")
    @Operation(summary = "Records a stock return between two locations and moves the stock")
    @ApiResponse(responseCode = "201", description = "Return recorded with the RETURN status")
    @ApiResponse(responseCode = "404", description = "Article not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Not enough stock at the origin (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("transfers.return")
    public ResponseEntity<TransferRDto> recordReturn(@Valid @RequestBody StockReturnWDto request) {
        var created = transferService.recordReturn(request);
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + created.id()))
                .body(created);
    }
}
