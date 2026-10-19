package org.vvs.valimbavaka.feature.user.controller;

import jakarta.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vvs.valimbavaka.feature.user.dto.RoleRDto;
import org.vvs.valimbavaka.feature.user.service.RoleService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Lecture seule : alimente la sélection du rôle dans le formulaire utilisateur. */
@Tag(name = "Rôles")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Requête mal formée (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Non authentifié (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Permission manquante (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/roles")
@Validated
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des rôles")
    @ApiResponse(responseCode = "200", description = "Page de rôles")
    @HasPermission("roles.read")
    public PageResponse<RoleRDto> getRoles(@Valid PageQuery query) {
        return roleService.getRoles(query);
    }
}
