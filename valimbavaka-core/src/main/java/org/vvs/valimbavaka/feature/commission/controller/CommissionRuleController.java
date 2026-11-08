package org.vvs.valimbavaka.feature.commission.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.vvs.valimbavaka.feature.commission.dto.CommissionRuleRDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRuleStatusWDto;
import org.vvs.valimbavaka.feature.commission.dto.CommissionRuleWDto;
import org.vvs.valimbavaka.feature.commission.service.CommissionRuleService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Administrator only: each action requires the matching {@code commission-rules.*} permission (COM-01). */
@Tag(name = "Commission rules")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Malformed request (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Not authenticated (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Missing permission (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/commission-rules")
@Validated
public class CommissionRuleController {

    private final CommissionRuleService ruleService;

    public CommissionRuleController(CommissionRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    @Operation(summary = "Paged list of commission rules, filterable by sales channel and status")
    @ApiResponse(responseCode = "200", description = "Page of commission rules")
    @HasPermission("commission-rules.read")
    public PageResponse<CommissionRuleRDto> getRules(
            @Valid PageQuery query,
            @RequestParam(required = false) @Nullable UUID salesChannelId,
            @RequestParam(required = false) @Nullable Boolean active) {
        return ruleService.getRules(query, salesChannelId, active);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Commission rule")
    @ApiResponse(responseCode = "200", description = "Commission rule")
    @ApiResponse(responseCode = "404", description = "Rule not found (NOT_FOUND)")
    @HasPermission("commission-rules.read")
    public CommissionRuleRDto getRuleById(@PathVariable UUID id) {
        return ruleService.getRuleById(id);
    }

    @PostMapping
    @Operation(summary = "Creates an active commission rule for a sales channel")
    @ApiResponse(responseCode = "201", description = "Rule created")
    @ApiResponse(responseCode = "409", description = "Name used or channel already covered (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data or unknown channel (VALIDATION_FAILED)")
    @HasPermission("commission-rules.create")
    public ResponseEntity<CommissionRuleRDto> createRule(@Valid @RequestBody CommissionRuleWDto request) {
        var created = ruleService.createRule(request);
        return ResponseEntity.created(URI.create("/api/v1/commission-rules/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Updates a commission rule")
    @ApiResponse(responseCode = "200", description = "Rule updated")
    @ApiResponse(responseCode = "404", description = "Rule not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Version, name or channel conflict (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data or unknown channel (VALIDATION_FAILED)")
    @HasPermission("commission-rules.update")
    public CommissionRuleRDto updateRule(@PathVariable UUID id, @Valid @RequestBody CommissionRuleWDto request) {
        return ruleService.updateRule(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activates or deactivates a commission rule")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "404", description = "Rule not found (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Channel already covered by an active rule (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Invalid data (VALIDATION_FAILED)")
    @HasPermission("commission-rules.update")
    public CommissionRuleRDto setActive(@PathVariable UUID id, @Valid @RequestBody CommissionRuleStatusWDto request) {
        return ruleService.setActive(id, request.active());
    }
}
