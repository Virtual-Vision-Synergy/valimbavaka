package org.vvs.valimbavaka.feature.shooting.controller;

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

import org.vvs.valimbavaka.feature.shooting.domain.ShootingStatus;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingRDto;
import org.vvs.valimbavaka.feature.shooting.dto.ShootingWDto;
import org.vvs.valimbavaka.feature.shooting.service.ShootingService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Administrator and seller: reading with {@code services.read}, recording an exit with {@code shootings.create}. */
@Tag(name = "Shootings")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/shootings")
@Validated
public class ShootingController {

    private final ShootingService shootingService;

    public ShootingController(ShootingService shootingService) {
        this.shootingService = shootingService;
    }

    @GetMapping
    @Operation(summary = "Paged list of shooting exits, most recent first, filterable by status")
    @ApiResponse(responseCode = "200", description = "Page of shooting exits")
    @HasPermission("services.read")
    public PageResponse<ShootingRDto> getShootings(
            @Valid PageQuery query, @RequestParam(required = false) @Nullable ShootingStatus status) {
        return shootingService.getShootings(query, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Shooting exit")
    @ApiResponse(responseCode = "200", description = "Shooting exit")
    @ApiResponse(responseCode = "404", description = "Shooting exit not found (NOT_FOUND)")
    @HasPermission("services.read")
    public ShootingRDto getShootingById(@PathVariable UUID id) {
        return shootingService.getShootingById(id);
    }

    @PostMapping
    @Operation(summary = "Records a shooting exit: the products leave the stock of the location")
    @ApiResponse(responseCode = "201", description = "Shooting exit recorded, status OUT")
    @ApiResponse(responseCode = "404", description = "Article not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Insufficient stock in the location (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("shootings.create")
    public ResponseEntity<ShootingRDto> createShooting(@Valid @RequestBody ShootingWDto request) {
        var created = shootingService.createShooting(request);
        return ResponseEntity.created(URI.create("/api/v1/shootings/" + created.id()))
                .body(created);
    }
}
