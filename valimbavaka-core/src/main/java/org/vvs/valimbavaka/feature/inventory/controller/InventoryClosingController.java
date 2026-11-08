package org.vvs.valimbavaka.feature.inventory.controller;

import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionRDto;
import org.vvs.valimbavaka.feature.inventory.service.InventoryClosingService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Closing of an inventory session (INV-04), with {@code inventory.close}. */
@Tag(name = "Inventories")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/inventories")
@Validated
public class InventoryClosingController {

    private final InventoryClosingService inventoryClosingService;

    public InventoryClosingController(InventoryClosingService inventoryClosingService) {
        this.inventoryClosingService = inventoryClosingService;
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Closes the session and adjusts the stock of the location to the counted quantities")
    @ApiResponse(responseCode = "200", description = "Closed session")
    @ApiResponse(responseCode = "404", description = "Session not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Session no longer in progress, or stock moved (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Line not counted, or gap without reason (VALIDATION_FAILED)")
    @HasPermission("inventory.close")
    public InventorySessionRDto closeSession(@PathVariable UUID id) {
        return inventoryClosingService.closeSession(id);
    }
}
