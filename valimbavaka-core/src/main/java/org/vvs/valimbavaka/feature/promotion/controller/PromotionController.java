package org.vvs.valimbavaka.feature.promotion.controller;

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

import org.vvs.valimbavaka.feature.promotion.dto.PromotionRDto;
import org.vvs.valimbavaka.feature.promotion.dto.PromotionWDto;
import org.vvs.valimbavaka.feature.promotion.service.PromotionService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Promotions (PRO-01): reading for every role allowed to sell, creation for the administrator only. */
@Tag(name = "Promotions")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/promotions")
@Validated
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Promotion sheet, with its status deduced from the period")
    @ApiResponse(responseCode = "200", description = "Promotion")
    @ApiResponse(responseCode = "404", description = "Promotion not found (NOT_FOUND)")
    @HasPermission("promotions.read")
    public PromotionRDto getPromotionById(@PathVariable UUID id) {
        return promotionService.getPromotionById(id);
    }

    @PostMapping
    @Operation(summary = "Creates a promotion (kind, value, scope and targets, channels, period, optional code)")
    @ApiResponse(responseCode = "201", description = "Promotion created")
    @ApiResponse(responseCode = "409", description = "Promo code already used (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("promotions.create")
    public ResponseEntity<PromotionRDto> createPromotion(@Valid @RequestBody PromotionWDto request) {
        var created = promotionService.createPromotion(request);
        return ResponseEntity.created(URI.create("/api/v1/promotions/" + created.id()))
                .body(created);
    }
}
