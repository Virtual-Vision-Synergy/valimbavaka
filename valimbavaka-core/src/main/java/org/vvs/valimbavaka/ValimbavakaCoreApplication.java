package org.vvs.valimbavaka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ValimbavakaCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValimbavakaCoreApplication.class, args);
    }
}
