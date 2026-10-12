package org.vvs.valimbavaka.iam.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Compte administrateur initial, lu depuis les variables d'environnement ADMIN_*.
 * Uniquement utilisé quand la base ne contient encore aucun administrateur.
 */
@ConfigurationProperties("app.bootstrap.admin")
public record AdminBootstrapProperties(String email, String password, String firstName, String lastName) {

    @Override
    public String toString() {
        return "AdminBootstrapProperties[email=" + email + ", password=****]";
    }
}
