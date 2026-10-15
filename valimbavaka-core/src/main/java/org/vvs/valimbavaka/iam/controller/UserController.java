package org.vvs.valimbavaka.iam.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.vvs.valimbavaka.common.dto.ApiResponse;
import org.vvs.valimbavaka.common.dto.PageResponse;
import org.vvs.valimbavaka.iam.dto.CreateUserWDto;
import org.vvs.valimbavaka.iam.dto.UpdateUserWDto;
import org.vvs.valimbavaka.iam.dto.UserRDto;
import org.vvs.valimbavaka.iam.service.UserService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserRDto>>> getUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<UserRDto> users = PageResponse.from(userService.getUsers(search, pageable));
        return ResponseEntity.ok(ApiResponse.success(users, "OK"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserRDto>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id), "OK"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserRDto>> createUser(@Valid @RequestBody CreateUserWDto createUserWDto) {
        UserRDto created = userService.createUser(createUserWDto);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.id()))
                .body(ApiResponse.success(created, "Utilisateur créé"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserRDto>> updateUser(@PathVariable UUID id,
                                                            @Valid @RequestBody UpdateUserWDto updateUserWDto) {
        UserRDto updated = userService.updateUser(id, updateUserWDto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Utilisateur modifié"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable UUID id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
