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

import org.vvs.valimbavaka.feature.inventory.dto.InventoryLineRDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryReasonWDto;
import org.vvs.valimbavaka.feature.inventory.service.InventoryReasonService;
import org.vvs.valimbavaka.security.annotation.HasPermission;

/** Gap reason of a line of an inventory session (INV-03), with {@code inventory.update}. */
@Tag(name = "Inventories")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request or unknown reason (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/inventories")
@Validated
public class InventoryReasonController {

    private final InventoryReasonService inventoryReasonService;

    public InventoryReasonController(InventoryReasonService inventoryReasonService) {
        this.inventoryReasonService = inventoryReasonService;
    }

    @PutMapping("/{id}/lines/{lineId}/reason")
    @Operation(summary = "Qualifies the reason of the gap of a counted line")
    @ApiResponse(responseCode = "200", description = "Line with its reason")
    @ApiResponse(responseCode = "404", description = "Session or line not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Session no longer in progress (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Missing reason, or line without gap (VALIDATION_FAILED)")
    @HasPermission("inventory.update")
    public InventoryLineRDto qualifyLine(
            @PathVariable UUID id, @PathVariable UUID lineId, @Valid @RequestBody InventoryReasonWDto request) {
        return inventoryReasonService.qualifyLine(id, lineId, request);
    }
}
