package org.vvs.valimbavaka.support;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Profil « test » : le premier administrateur créé au démarrage (SYS-05) doit changer son mot de passe et n'a donc
 * aucune permission. Les tests d'intégration qui l'utilisent le trouvent dans l'état d'après sa première connexion.
 * Le changement obligatoire lui-même est couvert par {@code PasswordChangeIT} et {@code UserServiceImplTest}.
 */
@Component
@Profile("test")
class InitialAdministratorReadyForTests {

    static final String ADMIN_EMAIL = "admin.test@valimbavaka.mg";

    private final JdbcTemplate jdbcTemplate;

    InitialAdministratorReadyForTests(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    void liftPasswordChangeRequirement() {
        jdbcTemplate.update("UPDATE users SET is_password_change_required = FALSE WHERE email = ?", ADMIN_EMAIL);
    }
}
