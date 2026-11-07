package org.vvs.valimbavaka.feature.inventory.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
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

import org.vvs.valimbavaka.feature.inventory.domain.InventoryStatus;
import org.vvs.valimbavaka.feature.inventory.dto.InventoryLineRDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionRDto;
import org.vvs.valimbavaka.feature.inventory.dto.InventorySessionWDto;
import org.vvs.valimbavaka.feature.inventory.service.InventoryService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Inventory sessions (INV-01): reading with {@code inventory.read}, opening with {@code inventory.create}. */
@Tag(name = "Inventories")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/inventories")
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "Paged inventory sessions with their gap count, filterable by status and location")
    @ApiResponse(responseCode = "200", description = "Page of sessions, most recent first")
    @ApiResponse(responseCode = "422", description = "Invalid paging (VALIDATION_FAILED)")
    @HasPermission("inventory.read")
    public PageResponse<InventorySessionRDto> getSessions(
            @Valid PageQuery query,
            @RequestParam(required = false) @Nullable InventoryStatus status,
            @RequestParam(required = false) @Nullable UUID locationId) {
        return inventoryService.getSessions(query, status, locationId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Inventory session")
    @ApiResponse(responseCode = "200", description = "Session")
    @ApiResponse(responseCode = "404", description = "Session not found (NOT_FOUND)")
    @HasPermission("inventory.read")
    public InventorySessionRDto getSession(@PathVariable UUID id) {
        return inventoryService.getSession(id);
    }

    @GetMapping("/{id}/lines")
    @Operation(summary = "Paged lines of a session, sorted by article code")
    @ApiResponse(responseCode = "200", description = "Page of lines")
    @ApiResponse(responseCode = "404", description = "Session not found (NOT_FOUND)")
    @ApiResponse(responseCode = "422", description = "Invalid paging (VALIDATION_FAILED)")
    @HasPermission("inventory.read")
    public PageResponse<InventoryLineRDto> getLines(@PathVariable UUID id, @Valid PageQuery query) {
        return inventoryService.getLines(id, query);
    }

    @PostMapping
    @Operation(summary = "Opens an inventory session: location, scope, responsible and count date")
    @ApiResponse(responseCode = "201", description = "Session opened with its lines to count")
    @ApiResponse(responseCode = "409", description = "A session is already in progress in the location (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("inventory.create")
    public ResponseEntity<InventorySessionRDto> openSession(@Valid @RequestBody InventorySessionWDto request) {
        var opened = inventoryService.openSession(request);
        return ResponseEntity.created(URI.create("/api/v1/inventories/" + opened.id()))
                .body(opened);
    }
}
