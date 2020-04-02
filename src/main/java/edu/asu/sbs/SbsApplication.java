package edu.asu.sbs;

import edu.asu.sbs.config.ApplicationProperties;
import edu.asu.sbs.config.SbsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class, SbsProperties.class})
public class SbsApplication {

    public static void main(String[] args) {
        System.setProperty("org.hyperledger.fabric.sdk.service_discovery.as_localhost", "true");
        SpringApplication.run(SbsApplication.class, args);
    }

}
