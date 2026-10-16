package org.vvs.valimbavaka.feature.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.vvs.valimbavaka.feature.user.domain.Role;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.support.IntegrationTest;

/** SYS-05 sur une base neuve : vrais rôles + premier administrateur créé depuis l'environnement. */
@IntegrationTest
@Transactional
class AdminAccountInitializerIT {

    @Autowired
    private AdminAccountInitializer sut;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void migrations_freshDatabase_seedExactlyTheThreeBusinessRoles() {
        // when
        var roleNames = roleRepository.findAll().stream().map(Role::getName).toList();
        // then
        assertThat(roleNames).containsExactlyInAnyOrder(RoleName.ADMIN, RoleName.SELLER,
                RoleName.DELIVERY_DRIVER);
    }

    @Test
    void run_atStartupWithoutAdministrator_createsAdministratorRequiringPasswordChange() {
        // when (exécuté au démarrage du contexte)
        var administrators = administrators();
        // then
        assertThat(administrators).singleElement().satisfies(admin -> {
            assertThat(admin.getEmail()).isEqualTo("admin.test@valimbavaka.mg");
            assertThat(admin.isPasswordChangeRequired()).isTrue();
            assertThat(admin.getPasswordHash()).doesNotContain("mot-de-passe-de-test-tres-long");
            assertThat(passwordEncoder.matches("mot-de-passe-de-test-tres-long", admin.getPasswordHash())).isTrue();
        });
    }

    @Test
    void run_administratorAlreadyExists_isIdempotent() {
        // when
        sut.run(new DefaultApplicationArguments());
        // then
        assertThat(administrators()).hasSize(1);
    }

    private List<User> administrators() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole().getName() == RoleName.ADMIN)
                .toList();
    }
}
