package org.vvs.valimbavaka.feature.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.user.config.AdminAccountProperties;
import org.vvs.valimbavaka.feature.user.domain.Role;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.dto.CreateUserWDto;
import org.vvs.valimbavaka.feature.user.exception.AdminAccountConfigurationException;
import org.vvs.valimbavaka.feature.user.mapper.UserMapper;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String RAW_PASSWORD = "mot-de-passe-provisoire-long";
    private static final String PASSWORD_HASH = "{bcrypt}hash";

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private Role administratorRole;

    private UserServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new UserServiceImpl(userRepository, roleRepository, userMapper, passwordEncoder);
    }

    @Test
    void createInitialAdministratorIfMissing_administratorExists_createsNothing() {
        // given
        when(userRepository.existsByRoleName(RoleName.ADMINISTRATEUR)).thenReturn(true);
        // when
        boolean created = sut.createInitialAdministratorIfMissing(validAccount());
        // then
        assertThat(created).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void createInitialAdministratorIfMissing_variablesMissing_refusesStartup() {
        // given
        when(userRepository.existsByRoleName(RoleName.ADMINISTRATEUR)).thenReturn(false);
        var emptyAccount = new AdminAccountProperties("", "", "", "");
        // when / then
        assertThatThrownBy(() -> sut.createInitialAdministratorIfMissing(emptyAccount))
                .isInstanceOf(AdminAccountConfigurationException.class)
                .hasMessageContaining("ADMIN_EMAIL")
                .hasMessageContaining("ADMIN_PASSWORD");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createInitialAdministratorIfMissing_passwordTooShort_neverLeaksPasswordInMessage() {
        // given
        when(userRepository.existsByRoleName(RoleName.ADMINISTRATEUR)).thenReturn(false);
        var account = new AdminAccountProperties("admin@valimbavaka.mg", "secret-court", "Rasoa", "Rakoto");
        // when / then
        assertThatThrownBy(() -> sut.createInitialAdministratorIfMissing(account))
                .isInstanceOf(AdminAccountConfigurationException.class)
                .message().doesNotContain("secret-court");
    }

    @Test
    void createInitialAdministratorIfMissing_validVariables_createsAdministratorRequiringPasswordChange() {
        // given
        when(userRepository.existsByRoleName(RoleName.ADMINISTRATEUR)).thenReturn(false);
        when(roleRepository.findByName(RoleName.ADMINISTRATEUR)).thenReturn(Optional.of(administratorRole));
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // when
        boolean created = sut.createInitialAdministratorIfMissing(validAccount());
        // then
        var saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(created).isTrue();
        assertThat(saved.getValue())
                .extracting(User::getEmail, User::getPasswordHash, User::getRole, User::isPasswordChangeRequired,
                        User::isActive)
                .containsExactly("admin@valimbavaka.mg", PASSWORD_HASH, administratorRole, true, true);
    }

    @Test
    void createUser_emailAlreadyUsed_throwsConflict() {
        // given
        var request = new CreateUserWDto(" Vendeuse@Valimbavaka.mg ", "mot-de-passe", "Hery", "Rabe",
                UUID.randomUUID());
        when(userRepository.existsByEmail("vendeuse@valimbavaka.mg")).thenReturn(true);
        // when / then
        assertThatThrownBy(() -> sut.createUser(request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void getUserById_unknownId_throwsNotFound() {
        // given
        var unknownId = UUID.randomUUID();
        when(userRepository.findByIdWithRole(unknownId)).thenReturn(Optional.empty());
        // when / then
        assertThatThrownBy(() -> sut.getUserById(unknownId)).isInstanceOf(NotFoundException.class);
    }

    private static AdminAccountProperties validAccount() {
        return new AdminAccountProperties(" Admin@Valimbavaka.mg ", RAW_PASSWORD, "Rasoa", "Rakoto");
    }
}
