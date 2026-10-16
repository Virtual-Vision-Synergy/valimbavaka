package org.vvs.valimbavaka.feature.user.exception;

import java.io.Serial;

/** Base sans administrateur et variables ADMIN_* absentes ou invalides : démarrage refusé (fail closed). */
public class AdminAccountConfigurationException extends IllegalStateException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AdminAccountConfigurationException(String message) {
        super(message);
    }
}
