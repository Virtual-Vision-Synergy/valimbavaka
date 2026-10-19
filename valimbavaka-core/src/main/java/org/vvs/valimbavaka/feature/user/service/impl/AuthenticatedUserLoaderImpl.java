package org.vvs.valimbavaka.feature.user.service.impl;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.user.domain.Permission;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.AuthenticatedUserLoader;
import org.vvs.valimbavaka.security.LockPolicy;

/** Adapte les comptes (feature user) au port d'authentification défini par {@code security}. */
@Service
@Transactional(readOnly = true)
public class AuthenticatedUserLoaderImpl implements AuthenticatedUserLoader {

    private final UserRepository userRepository;
    private final Clock clock;

    public AuthenticatedUserLoaderImpl(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Override
    public Optional<AuthenticatedUser> findByEmail(String email) {
        return userRepository
                .findByEmailWithRoleAndPermissions(User.normalizeEmail(email))
                .map(this::toAuthenticated);
    }

    @Override
    public Optional<AuthenticatedUser> findById(UUID userId) {
        return userRepository.findByIdWithRoleAndPermissions(userId).map(this::toAuthenticated);
    }

    @Override
    @Transactional
    public void recordFailure(UUID userId, LockPolicy policy) {
        userRepository.findById(userId).ifPresent(user -> user.registerFailedLogin(clock.instant(), policy));
    }

    @Override
    @Transactional
    public void recordSuccess(UUID userId) {
        userRepository.findById(userId).ifPresent(User::registerSuccessfulLogin);
    }

    private AuthenticatedUser toAuthenticated(User user) {
        var role = user.getRole();
        var permissions =
                role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toUnmodifiableSet());
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                role.getName().name(),
                role.getDescription() == null ? role.getName().name() : role.getDescription(),
                user.getPasswordHash(),
                permissions,
                user.isActive(),
                user.isPasswordChangeRequired(),
                user.getLockedUntil());
    }
}
