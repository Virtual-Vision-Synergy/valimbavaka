package org.vvs.valimbavaka.security.config;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.vvs.valimbavaka.core.config.CorsProperties;
import org.vvs.valimbavaka.security.handler.ProblemDetailWriter;
import org.vvs.valimbavaka.security.handler.RestAccessDeniedHandler;
import org.vvs.valimbavaka.security.handler.RestAuthenticationEntryPoint;
import org.vvs.valimbavaka.security.ratelimit.LoginRateLimitFilter;
import org.vvs.valimbavaka.security.ratelimit.RateLimitProperties;

import tools.jackson.databind.json.JsonMapper;

/** Deny by default : toute route est protégée sauf déclaration explicite (convention §9.3). */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ROUTES = {
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout"
    };

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsProperties corsProperties;
    private final LoginRateLimitFilter loginRateLimitFilter;

    public SecurityConfig(RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler, CorsProperties corsProperties,
            RateLimitProperties rateLimitProperties, ProblemDetailWriter problemDetailWriter, JsonMapper jsonMapper) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsProperties = corsProperties;
        this.loginRateLimitFilter = new LoginRateLimitFilter(rateLimitProperties, problemDetailWriter, jsonMapper);
    }

    /**
     * Chaîne dédiée à /actuator/health (sondes du déploiement), évaluée avant la chaîne principale.
     * Le détail de santé n'est jamais exposé (management.endpoint.health.show-details=never).
     */
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorHealthFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/health", "/actuator/health/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtConverter)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> { })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new OriginCheckFilter(Set.copyOf(corsProperties.allowedOrigins()),
                        accessDeniedHandler), BasicAuthenticationFilter.class)
                .addFilterBefore(loginRateLimitFilter, BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    /**
     * Claim {@code permissions} sans préfixe (testé par {@code @HasPermission}), claim {@code roles}
     * préfixé {@code ROLE_} ; sans cela Spring lirait {@code scope} avec le préfixe {@code SCOPE_} (§9.3).
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var permissions = new JwtGrantedAuthoritiesConverter();
        permissions.setAuthoritiesClaimName("permissions");
        permissions.setAuthorityPrefix("");
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> Stream
                .concat(permissions.convert(jwt).stream(), roles.convert(jwt).stream()).toList());
        return converter;
    }

    /**
     * Argon2id (OWASP : 19 Mio, 2 itérations, parallélisme 1) pour les nouveaux hachages ; bcrypt reste
     * vérifiable en héritage. Le préfixe {id} permet de migrer sans casser les comptes (§9.2).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        var argon2 = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
        return new DelegatingPasswordEncoder("argon2", Map.of("argon2", argon2, "bcrypt", new BCryptPasswordEncoder()));
    }

    @Bean
    static AnnotationTemplateExpressionDefaults templateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }
}
