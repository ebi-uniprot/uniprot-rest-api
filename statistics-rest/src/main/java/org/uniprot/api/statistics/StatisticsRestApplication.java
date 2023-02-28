package org.uniprot.api.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@ComponentScan(basePackages = {"org.uniprot.api.statistics", "org.uniprot.api.rest.validation"})
public class StatisticsRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatisticsRestApplication.class, args);
    }
}
