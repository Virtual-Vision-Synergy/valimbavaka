package org.vvs.valimbavaka.iam.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.vvs.valimbavaka.iam.dto.CreateUserWDto;
import org.vvs.valimbavaka.iam.dto.UpdateUserWDto;
import org.vvs.valimbavaka.iam.dto.UserRDto;

import java.util.UUID;

public interface UserService {
    Page<UserRDto> getUsers(String search, Pageable pageable);
    UserRDto getUserById(UUID userId);

    UserRDto createUser(CreateUserWDto createUserWDto);
    UserRDto updateUser(UUID userId, UpdateUserWDto updateUserWDto);
    void deleteUserById(UUID userId);
}
