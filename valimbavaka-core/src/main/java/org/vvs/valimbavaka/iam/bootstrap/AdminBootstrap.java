package org.vvs.valimbavaka.iam.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.vvs.valimbavaka.iam.domain.Role;
import org.vvs.valimbavaka.iam.domain.RoleName;
import org.vvs.valimbavaka.iam.domain.User;
import org.vvs.valimbavaka.iam.repository.RoleRepository;
import org.vvs.valimbavaka.iam.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Au démarrage, crée le premier administrateur si la base n'en contient aucun.
 * Le compte vient des variables ADMIN_* (jamais du code ni d'un changelog) et doit changer
 * son mot de passe à la première connexion. Sans ces variables, le démarrage échoue.
 */
@Component
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int NAME_MAX_LENGTH = 100;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(AdminBootstrapProperties properties, UserRepository userRepository,
                          RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRoleName(RoleName.ADMINISTRATEUR)) {
            log.info("Un administrateur existe déjà : pas de création du compte initial.");
            return;
        }
        validate();
        String email = properties.email().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Impossible de créer l'administrateur initial : l'email "
                    + email + " est déjà utilisé par un autre compte (ADMIN_EMAIL).");
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMINISTRATEUR)
                .orElseThrow(() -> new IllegalStateException(
                        "Rôle " + RoleName.ADMINISTRATEUR + " introuvable : migrations Liquibase incomplètes."));

        User admin = new User();
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(properties.password()));
        admin.setFirstName(properties.firstName().trim());
        admin.setLastName(properties.lastName().trim());
        admin.setRole(adminRole);
        admin.setActive(true);
        admin.setMustChangePassword(true);
        userRepository.save(admin);
        log.info("Administrateur initial créé ({}) : changement de mot de passe exigé à la première connexion.",
                email);
    }

    private void validate() {
        List<String> errors = new ArrayList<>();
        if (!StringUtils.hasText(properties.email())) {
            errors.add("ADMIN_EMAIL manquant");
        } else if (!EMAIL_PATTERN.matcher(properties.email().trim()).matches()) {
            errors.add("ADMIN_EMAIL n'est pas une adresse email valide");
        }
        if (!StringUtils.hasText(properties.password())) {
            errors.add("ADMIN_PASSWORD manquant");
        } else if (properties.password().length() < PASSWORD_MIN_LENGTH) {
            errors.add("ADMIN_PASSWORD doit contenir au moins " + PASSWORD_MIN_LENGTH + " caractères");
        }
        checkName(properties.firstName(), "ADMIN_FIRST_NAME", errors);
        checkName(properties.lastName(), "ADMIN_LAST_NAME", errors);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Aucun administrateur en base et compte initial invalide : "
                    + String.join(", ", errors) + ". Renseigner les variables ADMIN_* puis redémarrer.");
        }
    }

    private static void checkName(String value, String variable, List<String> errors) {
        if (!StringUtils.hasText(value)) {
            errors.add(variable + " manquant");
        } else if (value.trim().length() > NAME_MAX_LENGTH) {
            errors.add(variable + " ne doit pas dépasser " + NAME_MAX_LENGTH + " caractères");
        }
    }
}
