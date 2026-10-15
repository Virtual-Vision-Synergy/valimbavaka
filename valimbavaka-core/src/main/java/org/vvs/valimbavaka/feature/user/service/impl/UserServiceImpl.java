package org.vvs.valimbavaka.feature.user.service.impl;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.user.config.AdminAccountProperties;
import org.vvs.valimbavaka.feature.user.domain.Role;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.dto.CreateUserWDto;
import org.vvs.valimbavaka.feature.user.dto.UpdateUserWDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.exception.AdminAccountConfigurationException;
import org.vvs.valimbavaka.feature.user.mapper.UserMapper;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final String EMAIL_ALREADY_USED = "Un utilisateur avec cet email existe déjà.";
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "email", "firstName", "lastName");

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

    @Override
    public PageResponse<UserRDto> getUsers(PageQuery query) {
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        return PageResponse.from(userRepository.search(query.searchTerm(), pageable).map(userMapper::toRDto));
    }

    @Override
    public UserRDto getUserById(UUID userId) {
        return userMapper.toRDto(findUserWithRole(userId));
    }

    @Override
    @Transactional
    public UserRDto createUser(CreateUserWDto request) {
        if (userRepository.existsByEmail(User.normalizeEmail(request.email()))) {
            throw new ConflictException(EMAIL_ALREADY_USED);
        }
        var profile = new UserProfile(request.email(), request.firstName(), request.lastName());
        var user = User.create(profile, passwordEncoder.encode(request.password()), findRole(request.roleId()));
        User saved = userRepository.save(user);
        log.info("Utilisateur {} créé", saved.getId());
        return userMapper.toRDto(saved);
    }

    @Override
    @Transactional
    public UserRDto updateUser(UUID userId, UpdateUserWDto request) {
        User user = findUserWithRole(userId);
        if (userRepository.existsByEmailAndIdNot(User.normalizeEmail(request.email()), userId)) {
            throw new ConflictException(EMAIL_ALREADY_USED);
        }
        var profile = new UserProfile(request.email(), request.firstName(), request.lastName());
        user.update(profile, findRole(request.roleId()), request.active());
        return userMapper.toRDto(user);
    }

    @Override
    @Transactional
    public void deleteUserById(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw userNotFound(userId);
        }
        userRepository.deleteById(userId);
        log.info("Utilisateur {} supprimé", userId);
    }

    @Override
    @Transactional
    public boolean createInitialAdministratorIfMissing(AdminAccountProperties account) {
        if (userRepository.existsByRoleName(RoleName.ADMINISTRATEUR)) {
            return false;
        }
        var violations = account.violations();
        if (!violations.isEmpty()) {
            throw new AdminAccountConfigurationException("Aucun administrateur en base : renseigner les variables "
                    + "d'environnement du premier compte. " + String.join(" ; ", violations));
        }
        var profile = new UserProfile(account.email(), account.firstName(), account.lastName());
        var administrator = User.createWithTemporaryPassword(profile, passwordEncoder.encode(account.password()),
                roleRepository.findByName(RoleName.ADMINISTRATEUR)
                        .orElseThrow(() -> new AdminAccountConfigurationException(
                                "Rôle ADMINISTRATEUR absent : vérifier les migrations Liquibase")));
        User saved = userRepository.save(administrator);
        log.info("Administrateur initial {} créé, changement de mot de passe requis", saved.getId());
        return true;
    }

    private User findUserWithRole(UUID userId) {
        return userRepository.findByIdWithRole(userId).orElseThrow(() -> userNotFound(userId));
    }

    private Role findRole(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Rôle introuvable : " + roleId));
    }

    private static NotFoundException userNotFound(UUID userId) {
        return new NotFoundException("Utilisateur introuvable : " + userId);
    }
}
