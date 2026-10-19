package org.vvs.valimbavaka.feature.user.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vvs.valimbavaka.feature.user.dto.CreateUserWDto;
import org.vvs.valimbavaka.feature.user.dto.UpdateUserWDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.security.annotation.HasPermission;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Réservé à l'administrateur : chaque action exige la permission {@code users.*} correspondante. */
@Tag(name = "Utilisateurs")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Requête mal formée (MALFORMED_REQUEST)"),
    @ApiResponse(responseCode = "401", description = "Non authentifié (UNAUTHENTICATED)"),
    @ApiResponse(responseCode = "403", description = "Permission manquante (FORBIDDEN)")
})
@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Liste paginée des utilisateurs")
    @ApiResponse(responseCode = "200", description = "Page d'utilisateurs")
    @HasPermission("users.read")
    public PageResponse<UserRDto> getUsers(@Valid PageQuery query) {
        return userService.getUsers(query);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un utilisateur")
    @ApiResponse(responseCode = "200", description = "Utilisateur")
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable (NOT_FOUND)")
    @HasPermission("users.read")
    public UserRDto getUserById(@PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @PostMapping
    @Operation(summary = "Crée un utilisateur")
    @ApiResponse(responseCode = "201", description = "Utilisateur créé")
    @ApiResponse(responseCode = "409", description = "Email déjà utilisé (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Données invalides (VALIDATION_FAILED)")
    @HasPermission("users.create")
    public ResponseEntity<UserRDto> createUser(@Valid @RequestBody CreateUserWDto request) {
        var created = userService.createUser(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifie un utilisateur")
    @ApiResponse(responseCode = "200", description = "Utilisateur modifié")
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable (NOT_FOUND)")
    @ApiResponse(responseCode = "409", description = "Conflit de version ou d'email (CONFLICT)")
    @ApiResponse(responseCode = "422", description = "Données invalides (VALIDATION_FAILED)")
    @HasPermission("users.update")
    public UserRDto updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserWDto request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un utilisateur")
    @ApiResponse(responseCode = "204", description = "Utilisateur supprimé")
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable (NOT_FOUND)")
    @HasPermission("users.delete")
    public ResponseEntity<Void> deleteUserById(@PathVariable UUID id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
