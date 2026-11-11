package org.vvs.valimbavaka.feature.commission.controller;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.commission.dto.CommissionRDto;
import org.vvs.valimbavaka.feature.commission.service.CommissionService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/**
 * Commissions computed automatically on sales and salon services (COM-02). Read only: they are computed by the server
 * when the operation is recorded; the administrator consults those of every seller ({@code commissions.read-all}).
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

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
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
}
