package org.vvs.valimbavaka.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Conteneur PostgreSQL partagé par tous les tests d'un même contexte Spring (mis en cache). */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** Même version majeure qu'en production (docker-compose.yml, décision D2). */
    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
