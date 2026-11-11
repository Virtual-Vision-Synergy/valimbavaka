package org.vvs.valimbavaka.feature.sale.controller;

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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.sale.domain.SaleFilter;
import org.vvs.valimbavaka.feature.sale.dto.SaleSummaryRDto;
import org.vvs.valimbavaka.feature.sale.service.SaleQueryService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** The day's sales (VTE-07): the administrator reads every sale, a seller the sales credited to her. */
@Tag(name = "Sales")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/sales")
@Validated
public class SaleQueryController {

    private final SaleQueryService saleQueryService;

    public SaleQueryController(SaleQueryService saleQueryService) {
        this.saleQueryService = saleQueryService;
    }

    @GetMapping
    @Operation(
            summary = "Paged sales of one business day, most recent first; a seller only sees her own sales",
            description = "Day in Madagascar time (Indian/Antananarivo), today by default; search is not applied.")
    @ApiResponse(responseCode = "200", description = "Page of sales with their articles, payment, promo and total")
    @ApiResponse(responseCode = "422", description = "Invalid paging (VALIDATION_FAILED)")
    @HasPermission("sales.read")
    public PageResponse<SaleSummaryRDto> getSales(
            @Valid PageQuery query,
            @Parameter(description = "Business day, yyyy-MM-dd (today by default)")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    @Nullable
                    LocalDate date,
            @RequestParam(required = false) @Nullable UUID salesChannelId,
            @RequestParam(required = false) @Nullable UUID sellerId) {
        return saleQueryService.getSales(query, new SaleFilter(date, salesChannelId, sellerId));
    }
}
