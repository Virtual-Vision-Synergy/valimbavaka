package org.vvs.valimbavaka;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.vvs.valimbavaka.support.IntegrationTest;

@IntegrationTest
class ValimbavakaCoreApplicationIT {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads_onEmptyDatabase_appliesMigrationsAndStarts() {
        // then : Liquibase et la validation du schéma Hibernate (ddl-auto=validate) sont passés
        assertThat(context.getBean(ValimbavakaCoreApplication.class)).isNotNull();
    }
}
