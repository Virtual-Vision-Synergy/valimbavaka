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

/** Réservé à l'administrateur : chaque action exige la permission {@code users.*} correspondante. */
@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @HasPermission("users.read")
    public PageResponse<UserRDto> getUsers(@Valid PageQuery query) {
        return userService.getUsers(query);
    }

    @GetMapping("/{id}")
    @HasPermission("users.read")
    public UserRDto getUserById(@PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @PostMapping
    @HasPermission("users.create")
    public ResponseEntity<UserRDto> createUser(@Valid @RequestBody CreateUserWDto request) {
        var created = userService.createUser(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @HasPermission("users.update")
    public UserRDto updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserWDto request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @HasPermission("users.delete")
    public ResponseEntity<Void> deleteUserById(@PathVariable UUID id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
