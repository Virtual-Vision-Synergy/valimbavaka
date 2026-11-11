package org.vvs.valimbavaka.feature.saledetail.controller;

import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.saledetail.dto.SaleDetailRDto;
import org.vvs.valimbavaka.feature.saledetail.service.SaleDetailService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Detail of a sale with its lines, promotion, invoice and commission (VTE-09): read only, one call per screen. */
@Tag(name = "Sales")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/sales")
@Validated
public class SaleDetailController {

    private final SaleDetailService saleDetailService;

    public SaleDetailController(SaleDetailService saleDetailService) {
        this.saleDetailService = saleDetailService;
    }

    @GetMapping("/{saleId}/detail")
    @Operation(
            summary = "Detail of a sale",
            description = "Lines with article names, promotion, invoice and commission rule applied. A seller only "
                    + "reads her own sales.")
    @ApiResponse(responseCode = "200", description = "Detail of the sale")
    @ApiResponse(responseCode = "404", description = "Unknown sale, or sale of another seller (NOT_FOUND)")
    @HasPermission("sales.read")
    public SaleDetailRDto getDetail(@PathVariable UUID saleId) {
        return saleDetailService.getDetail(saleId);
    }
}
