package org.vvs.valimbavaka.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.vvs.valimbavaka.support.IntegrationTest;

@IntegrationTest
class PasswordEncoderIT {

    private static final String PASSWORD = "mot-de-passe-long-et-valide";

    @Autowired
    private PasswordEncoder sut;

    @Test
    void encode_newPassword_usesArgon2id() {
        // Act
        var hash = sut.encode(PASSWORD);

        // Assert
        assertThat(hash).startsWith("{argon2}");
        assertThat(sut.matches(PASSWORD, hash)).isTrue();
        assertThat(sut.matches("autre-mot-de-passe", hash)).isFalse();
    }

    @Test
    void matches_legacyBcryptHash_stillVerifies() {
        // Arrange
        var legacy = "{bcrypt}" + new BCryptPasswordEncoder().encode(PASSWORD);

        // Act + Assert
        assertThat(sut.matches(PASSWORD, legacy)).isTrue();
    }
}
