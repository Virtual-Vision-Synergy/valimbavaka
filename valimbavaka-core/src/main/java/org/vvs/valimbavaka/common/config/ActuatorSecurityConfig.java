package org.vvs.valimbavaka.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Ouvre uniquement /actuator/health (et ses sondes liveness/readiness) pour le health check
 * du deploiement. Chaine separee, evaluee avant la chaine principale de SecurityConfig, qui
 * reste en "deny by default" pour tout le reste. Le detail de sante n'est jamais expose
 * (management.endpoint.health.show-details=never), et nginx ne proxifie pas /actuator.
 */
@Configuration
public class ActuatorSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorHealthFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/health", "/actuator/health/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
