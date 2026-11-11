package org.vvs.valimbavaka.feature.sale.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.sale.dto.SaleFormRDto;
import org.vvs.valimbavaka.feature.sale.dto.SalePromotionRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleRDto;
import org.vvs.valimbavaka.feature.sale.dto.SaleWDto;
import org.vvs.valimbavaka.feature.sale.service.SaleService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Sales on every channel (shop, Facebook, phone): entered by a seller or the administrator (VTE-01). */
@Tag(name = "Sales")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/sales")
@Validated
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @GetMapping("/form")
    @Operation(summary = "Channels, active sellers and sellable articles with their stock, for the sale form")
    @ApiResponse(responseCode = "200", description = "Sale form")
    @HasPermission("sales.create")
    public SaleFormRDto getForm() {
        return saleService.getForm();
    }

    @GetMapping("/promo-codes/{code}")
    @Operation(summary = "Checks a promo code for a channel and returns its promotion with the articles it reaches")
    @ApiResponse(responseCode = "200", description = "Promotion of the code, usable today on the channel")
    @ApiResponse(responseCode = "404", description = "Unknown code (NOT_FOUND)")
    @ApiResponse(responseCode = "422", description = "Code expired, other channel or limit reached (VALIDATION_FAILED)")
    @HasPermission("sales.create")
    public SalePromotionRDto getPromoCode(@PathVariable String code, @RequestParam UUID salesChannelId) {
        return saleService.getPromoCode(code, salesChannelId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "One sale with its lines")
    @ApiResponse(responseCode = "200", description = "Sale")
    @ApiResponse(responseCode = "404", description = "Sale not found (NOT_FOUND)")
    @HasPermission("sales.read")
    public SaleRDto getSaleById(@PathVariable UUID id) {
        return saleService.getSaleById(id);
    }

    @PostMapping
    @Operation(summary = "Records a sale in today's opened day and takes the stock out of the channel's location")
    @ApiResponse(responseCode = "201", description = "Sale recorded with the VALIDATED status")
    @ApiResponse(responseCode = "404", description = "Article not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Day not opened, not enough stock or promotion limit (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data or promo code refused (VALIDATION_FAILED)")
    @HasPermission("sales.create")
    public ResponseEntity<SaleRDto> createSale(@Valid @RequestBody SaleWDto request) {
        var created = saleService.createSale(request);
        return ResponseEntity.created(URI.create("/api/v1/sales/" + created.id()))
                .body(created);
    }
}
