package org.vvs.valimbavaka.security.jwt;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.validator.constraints.time.DurationMax;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Secret HS256 d'au moins 256 bits fourni par l'environnement (JWT_SECRET), jamais versionné. */
@Validated
@ConfigurationProperties(prefix = "vvs.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "le secret JWT doit contenir au moins 32 caractères (256 bits)")
        String secret,
        @DefaultValue("15m") @NotNull Duration accessTokenTtl,
        @DefaultValue("7d") @NotNull Duration refreshTokenTtl,
        @DefaultValue("30s") @NotNull @DurationMax(seconds = 30) Duration clockSkew) {
}
