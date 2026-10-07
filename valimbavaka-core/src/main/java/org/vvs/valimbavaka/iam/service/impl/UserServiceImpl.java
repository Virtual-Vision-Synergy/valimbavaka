package org.vvs.valimbavaka.iam.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vvs.valimbavaka.common.exception.specifics.ConflictException;
import org.vvs.valimbavaka.common.exception.specifics.ResourceNotFoundException;
import org.vvs.valimbavaka.iam.domain.Role;
import org.vvs.valimbavaka.iam.domain.User;
import org.vvs.valimbavaka.iam.dto.CreateUserWDto;
import org.vvs.valimbavaka.iam.dto.UpdateUserWDto;
import org.vvs.valimbavaka.iam.dto.UserRDto;
import org.vvs.valimbavaka.iam.mapper.UserMapper;
import org.vvs.valimbavaka.iam.repository.RoleRepository;
import org.vvs.valimbavaka.iam.repository.UserRepository;
import org.vvs.valimbavaka.iam.service.UserService;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    private static final String EMAIL_ALREADY_USED = "Un utilisateur avec cet email existe déjà.";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<UserRDto> getUsers(String search, Pageable pageable) {
        String term = search == null ? "" : search.trim();
        return userRepository.search(term, pageable).map(userMapper::toRDto);
    }

    @Transactional(readOnly = true)
    @Override
    public UserRDto getUserById(UUID userId) {
        return userMapper.toRDto(findUserWithRole(userId));
    }

    @Transactional
    @Override
    public UserRDto createUser(CreateUserWDto createUserWDto) {
        if (userRepository.existsByEmail(createUserWDto.email())) {
            throw new ConflictException(EMAIL_ALREADY_USED);
        }
        User user = userMapper.toEntity(createUserWDto);
        user.setPassword(passwordEncoder.encode(createUserWDto.password()));
        user.setRole(findRole(createUserWDto.roleId()));
        return userMapper.toRDto(userRepository.save(user));
    }

    @Transactional
    @Override
    public UserRDto updateUser(UUID userId, UpdateUserWDto updateUserWDto) {
        User user = findUserWithRole(userId);
        if (userRepository.existsByEmailAndIdNot(updateUserWDto.email(), userId)) {
            throw new ConflictException(EMAIL_ALREADY_USED);
        }
        userMapper.update(updateUserWDto, user);
        user.setRole(findRole(updateUserWDto.roleId()));
        return userMapper.toRDto(userRepository.save(user));
    }

    @Transactional
    @Override
    public void deleteUserById(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw userNotFound(userId);
        }
        userRepository.deleteById(userId);
    }

    private User findUserWithRole(UUID userId) {
        return userRepository.findByIdWithRole(userId).orElseThrow(() -> userNotFound(userId));
    }

    private Role findRole(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + roleId));
    }

    private static ResourceNotFoundException userNotFound(UUID userId) {
        return new ResourceNotFoundException("Utilisateur introuvable : " + userId);
    }
}
