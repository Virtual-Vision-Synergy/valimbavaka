package org.vvs.valimbavaka.feature.user.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Premier compte administrateur, fourni par variables d'environnement (ADMIN_EMAIL, ADMIN_PASSWORD,
 * ADMIN_FIRST_NAME, ADMIN_LAST_NAME). Valeurs facultatives : elles ne sont exigées que sur une base
 * sans administrateur, d'où une validation à la demande plutôt qu'au démarrage.
 */
@ConfigurationProperties(prefix = "vvs.bootstrap.admin")
public record AdminAccountProperties(
        @Nullable String email,
        @Nullable String password,
        @Nullable String firstName,
        @Nullable String lastName) {

    /** 15 caractères minimum sans MFA (NIST SP 800-63B, convention §9.2). */
    public static final int MIN_PASSWORD_LENGTH = 15;
    public static final int MAX_PASSWORD_LENGTH = 128;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final Pattern EMAIL_FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** Problèmes de configuration détectés ; ne contient jamais la valeur du mot de passe. */
    public List<String> violations() {
        var violations = new ArrayList<String>();
        if (!isValidEmail(email)) {
            violations.add("ADMIN_EMAIL doit être une adresse email valide");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || password.length() > MAX_PASSWORD_LENGTH) {
            violations.add("ADMIN_PASSWORD doit contenir entre " + MIN_PASSWORD_LENGTH + " et "
                    + MAX_PASSWORD_LENGTH + " caractères");
        }
        if (!isValidName(firstName)) {
            violations.add("ADMIN_FIRST_NAME est obligatoire (" + MAX_NAME_LENGTH + " caractères max)");
        }
        if (!isValidName(lastName)) {
            violations.add("ADMIN_LAST_NAME est obligatoire (" + MAX_NAME_LENGTH + " caractères max)");
        }
        return violations;
    }

    @Override
    public String toString() {
        return "AdminAccountProperties[email=" + email + ", password=****, firstName=" + firstName
                + ", lastName=" + lastName + "]";
    }

    private static boolean isValidEmail(@Nullable String value) {
        return value != null && value.length() <= MAX_EMAIL_LENGTH
                && EMAIL_FORMAT.matcher(value.strip()).matches();
    }

    private static boolean isValidName(@Nullable String value) {
        return value != null && !value.isBlank() && value.strip().length() <= MAX_NAME_LENGTH;
    }
}
