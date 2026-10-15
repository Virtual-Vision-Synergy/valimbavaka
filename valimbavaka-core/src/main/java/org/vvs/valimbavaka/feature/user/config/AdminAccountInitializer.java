package org.vvs.valimbavaka.feature.user.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.vvs.valimbavaka.feature.user.service.UserService;

/** Au démarrage, garantit qu'un administrateur existe pour pouvoir se connecter sur une base neuve (SYS-05). */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserService userService;
    private final AdminAccountProperties adminAccountProperties;

    public AdminAccountInitializer(UserService userService, AdminAccountProperties adminAccountProperties) {
        this.userService = userService;
        this.adminAccountProperties = adminAccountProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        userService.createInitialAdministratorIfMissing(adminAccountProperties);
    }
}
