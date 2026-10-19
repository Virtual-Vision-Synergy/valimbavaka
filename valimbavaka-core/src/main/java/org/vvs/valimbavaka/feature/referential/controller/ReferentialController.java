package org.vvs.valimbavaka.feature.referential.controller;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vvs.valimbavaka.feature.referential.dto.ProductRangeRDto;
import org.vvs.valimbavaka.feature.referential.dto.SalesChannelRDto;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Référentiels métier en lecture seule (SYS-05), réservés aux utilisateurs authentifiés. */
@Tag(name = "Référentiels")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Requête mal formée (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Non authentifié (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Permission manquante (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1")
@Validated
public class ReferentialController {

    private final ReferentialService referentialService;

    public ReferentialController(ReferentialService referentialService) {
        this.referentialService = referentialService;
    }

    @GetMapping("/sales-channels")
    @Operation(summary = "Canaux de vente")
    @ApiResponse(responseCode = "200", description = "Page de canaux de vente")
    @HasPermission("referentials.read")
    public PageResponse<SalesChannelRDto> getSalesChannels(@Valid PageQuery query) {
        return referentialService.getSalesChannels(query);
    }

    @GetMapping("/product-ranges")
    @Operation(summary = "Gammes de produits")
    @ApiResponse(responseCode = "200", description = "Page de gammes")
    @HasPermission("referentials.read")
    public PageResponse<ProductRangeRDto> getProductRanges(@Valid PageQuery query) {
        return referentialService.getProductRanges(query);
    }

    @GetMapping("/stock-locations")
    @Operation(summary = "Emplacements de stock")
    @ApiResponse(responseCode = "200", description = "Page d'emplacements")
    @HasPermission("referentials.read")
    public PageResponse<StockLocationRDto> getStockLocations(@Valid PageQuery query) {
        return referentialService.getStockLocations(query);
    }
}
