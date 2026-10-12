package org.vvs.valimbavaka.iam.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.vvs.valimbavaka.iam.domain.RoleName;
import org.vvs.valimbavaka.iam.repository.RoleRepository;
import org.vvs.valimbavaka.iam.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private AdminBootstrap bootstrap(AdminBootstrapProperties properties) {
        return new AdminBootstrap(properties, userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void refusesToStartWithoutAdminAndWithoutVariables() {
        when(userRepository.existsByRoleName(RoleName.ADMINISTRATEUR)).thenReturn(false);
        var properties = new AdminBootstrapProperties("", "", "", "");

        assertThatIllegalStateException().isThrownBy(() -> bootstrap(properties).run(null))
                .withMessageContaining("ADMIN_EMAIL manquant")
                .withMessageContaining("ADMIN_PASSWORD manquant");
        verify(userRepository, never()).save(any());
    }

    @Test
    void refusesTooShortPasswordWithoutLeakingIt() {
        when(userRepository.existsByRoleName(RoleName.ADMINISTRATEUR)).thenReturn(false);
        var properties = new AdminBootstrapProperties("admin@valimbavaka.local", "court", "Admin", "Test");

        assertThatIllegalStateException().isThrownBy(() -> bootstrap(properties).run(null))
                .withMessageContaining("ADMIN_PASSWORD doit contenir au moins 8")
                .withMessageNotContaining("court");
    }

    @Test
    void doesNothingWhenAnAdminAlreadyExists() {
        when(userRepository.existsByRoleName(RoleName.ADMINISTRATEUR)).thenReturn(true);

        bootstrap(new AdminBootstrapProperties(null, null, null, null)).run(null);

        verify(userRepository, never()).save(any());
    }
}
