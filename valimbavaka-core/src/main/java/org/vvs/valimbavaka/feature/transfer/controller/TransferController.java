package org.vvs.valimbavaka.feature.transfer.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.transfer.dto.StockReturnWDto;
import org.vvs.valimbavaka.feature.transfer.dto.TransferRDto;
import org.vvs.valimbavaka.feature.transfer.service.TransferService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Transfers of stock between locations; a stock return is entered by a seller or the administrator. */
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
