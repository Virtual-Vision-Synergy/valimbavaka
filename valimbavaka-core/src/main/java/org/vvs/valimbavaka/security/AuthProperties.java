package org.vvs.valimbavaka.security;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Règles de connexion : verrouillage de compte et cookie du refresh token (convention §9.2). */
@Validated
@ConfigurationProperties(prefix = "vvs.auth")
public record AuthProperties(
        @DefaultValue("5") @Min(1) int maxFailedAttempts,
        @DefaultValue("5m") @NotNull Duration lockDuration,
        @DefaultValue("1h") @NotNull Duration maxLockDuration,
        @DefaultValue("true") boolean refreshCookieSecure) {

    public LockPolicy lockPolicy() {
        return new LockPolicy(maxFailedAttempts, lockDuration, maxLockDuration);
    }
}
