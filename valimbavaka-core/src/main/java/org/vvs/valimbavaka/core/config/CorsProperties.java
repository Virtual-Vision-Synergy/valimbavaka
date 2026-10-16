package org.vvs.valimbavaka.core.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

/** Origines autorisées par profil, jamais de wildcard "*" (convention §9.4). */
@Validated
@ConfigurationProperties(prefix = "vvs.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
}
