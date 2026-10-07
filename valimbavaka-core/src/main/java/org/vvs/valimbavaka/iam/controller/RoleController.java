package org.vvs.valimbavaka.iam.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vvs.valimbavaka.common.dto.ApiResponse;
import org.vvs.valimbavaka.common.dto.PageResponse;
import org.vvs.valimbavaka.iam.dto.RoleRDto;
import org.vvs.valimbavaka.iam.service.RoleService;

/** Lecture seule : alimente la sélection du rôle dans le formulaire utilisateur. */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RoleRDto>>> getRoles(
            @PageableDefault(size = 100, sort = "name") Pageable pageable) {
        PageResponse<RoleRDto> roles = PageResponse.from(roleService.getRoles(pageable));
        return ResponseEntity.ok(ApiResponse.success(roles, "OK"));
    }
}
