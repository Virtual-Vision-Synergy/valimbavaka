package org.vvs.valimbavaka.security.ratelimit;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Limitation de débit des routes sensibles : par adresse IP et par identifiant (convention §9.5). */
@Validated
@ConfigurationProperties(prefix = "vvs.rate-limit")
public record RateLimitProperties(
        @DefaultValue("20") @Min(1) int loginPerIp,
        @DefaultValue("10") @Min(1) int loginPerIdentifier,
        @DefaultValue("1m") @NotNull Duration window) {
}
