package org.vvs.valimbavaka.feature.inventory.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.inventory.dto.InventoryCountWDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryLineRDto;
import org.vvs.valimbavaka.feature.inventory.service.InventoryCountService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Physical count of an inventory session (INV-02), with {@code inventory.update}. */
@Tag(name = "Inventories")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/inventories")
@Validated
public class InventoryCountController {

    private final InventoryCountService inventoryCountService;

    public InventoryCountController(InventoryCountService inventoryCountService) {
        this.inventoryCountService = inventoryCountService;
    }

    @PutMapping("/{id}/lines/{lineId}/count")
    @Operation(summary = "Records the counted quantity of a line; the gap is computed against the frozen stock")
    @ApiResponse(responseCode = "200", description = "Counted line with its gap")
    @ApiResponse(responseCode = "404", description = "Session or line not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Session no longer in progress (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid quantity (VALIDATION_FAILED)")
    @HasPermission("inventory.update")
    public InventoryLineRDto countLine(
            @PathVariable UUID id, @PathVariable UUID lineId, @Valid @RequestBody InventoryCountWDto request) {
        return inventoryCountService.countLine(id, lineId, request);
    }
}
