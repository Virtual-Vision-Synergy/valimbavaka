package org.vvs.valimbavaka.iam.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.vvs.valimbavaka.iam.domain.Role;
import org.vvs.valimbavaka.iam.domain.RoleName;
import org.vvs.valimbavaka.iam.domain.User;
import org.vvs.valimbavaka.iam.repository.RoleRepository;
import org.vvs.valimbavaka.iam.repository.UserRepository;
import org.vvs.valimbavaka.referential.repository.ProductRangeRepository;
import org.vvs.valimbavaka.referential.repository.SalesChannelRepository;
import org.vvs.valimbavaka.referential.repository.StockLocationRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Démarrage sur base vide : vrais rôles, référentiels et admin initial issu des variables ADMIN_*.
 * La base est partagée entre contextes de test : l'admin a pu être créé par un autre contexte.
 */
@SpringBootTest(properties = {
        "app.bootstrap.admin.email=admin.test@valimbavaka.local",
        "app.bootstrap.admin.password=mot-de-passe-test",
        "app.bootstrap.admin.first-name=Admin",
        "app.bootstrap.admin.last-name=Test"
})
@Transactional
class AdminBootstrapIntegrationTest {
    @Autowired
    private AdminBootstrap adminBootstrap;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private SalesChannelRepository salesChannelRepository;
    @Autowired
    private ProductRangeRepository productRangeRepository;
    @Autowired
    private StockLocationRepository stockLocationRepository;

    @Test
    void rolesAreExactlyTheBusinessRoles() {
        assertThat(roleRepository.findAll()).extracting(Role::getName)
                .containsExactlyInAnyOrder(RoleName.ADMINISTRATEUR, RoleName.VENDEUSE, RoleName.LIVREUR);
    }

    @Test
    void referentialsAreSeeded() {
        assertThat(salesChannelRepository.findAll()).extracting("code")
                .containsExactlyInAnyOrder("BOUTIQUE", "FACEBOOK", "TELEPHONE", "LIVRAISON");
        assertThat(productRangeRepository.findAll()).extracting("name")
                .containsExactlyInAnyOrder("Zezika", "Botanika", "Solika");
        assertThat(stockLocationRepository.findAll()).extracting("code")
                .containsExactlyInAnyOrder("ENTREPOT", "BOUTIQUE", "LIVRAISON", "SALON");
    }

    @Test
    void initialAdminExistsAndMustChangePassword() {
        List<User> admins = userRepository.findAll().stream()
                .filter(user -> RoleName.ADMINISTRATEUR.equals(user.getRole().getName()))
                .toList();
        assertThat(admins).hasSize(1);
        User admin = admins.getFirst();
        assertThat(admin.isActive()).isTrue();
        assertThat(admin.isMustChangePassword()).isTrue();
        assertThat(admin.getPassword()).startsWith("$2");
    }

    @Test
    void runningAgainDoesNotCreateASecondAdmin() throws Exception {
        long before = userRepository.count();
        adminBootstrap.run(null);
        assertThat(userRepository.count()).isEqualTo(before);
    }
}
