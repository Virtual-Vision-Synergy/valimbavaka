package org.vvs.valimbavaka.feature.commission.controller;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
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

import org.vvs.valimbavaka.feature.commission.dto.CommissionRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionValidationRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionValidationWDto;
import org.vvs.valimbavaka.feature.commission.dto.SellerCommissionRDto;
import org.vvs.valimbavaka.feature.commission.service.CommissionService;
import org.vvs.valimbavaka.feature.commission.service.CommissionValidationService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/**
 * Commissions computed automatically on sales and salon services (COM-02). Read only: they are computed by the server
 * when the operation is recorded; the administrator consults those of every seller ({@code commissions.read-all}) and
 * validates them seller by seller before payout ({@code commissions.validate}, COM-03).
 */
@Tag(name = "Commissions")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/commissions")
@Validated
public class CommissionController {

    private final CommissionService commissionService;
    private final CommissionValidationService validationService;

    public CommissionController(CommissionService commissionService, CommissionValidationService validationService) {
        this.commissionService = commissionService;
        this.validationService = validationService;
    }

    @GetMapping
    @Operation(summary = "Paged list of computed commissions, filterable by seller and business days (inclusive)")
    @ApiResponse(responseCode = "200", description = "Page of commissions")
    @ApiResponse(responseCode = "422", description = "Invalid page or period (VALIDATION_FAILED)")
    @HasPermission("commissions.read-all")
    public PageResponse<CommissionRDto> getCommissions(
            @Valid PageQuery query,
            @RequestParam(required = false) @Nullable UUID sellerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to) {
        return commissionService.getCommissions(query, sellerId, from, to);
    }

    @GetMapping("/sellers")
    @Operation(summary = "Sellers with their commissions to validate and validated over business days (inclusive)")
    @ApiResponse(responseCode = "200", description = "Page of sellers, by name")
    @ApiResponse(responseCode = "422", description = "Invalid page or period (VALIDATION_FAILED)")
    @HasPermission("commissions.read-all")
    public PageResponse<SellerCommissionRDto> getSellers(
            @Valid PageQuery query,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return validationService.getSellerSummaries(query, from, to);
    }

    @PostMapping("/validation")
    @Operation(summary = "Validates the computed commissions of a seller over business days (inclusive)")
    @ApiResponse(responseCode = "200", description = "Commissions validated")
    @ApiResponse(responseCode = "404", description = "No commission for this seller over the period (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Already validated (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data or period (VALIDATION_FAILED)")
    @HasPermission("commissions.validate")
    public CommissionValidationRDto validate(@Valid @RequestBody CommissionValidationWDto request) {
        return validationService.validate(request);
    }
}
