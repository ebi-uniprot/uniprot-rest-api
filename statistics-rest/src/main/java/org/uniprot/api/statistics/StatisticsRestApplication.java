package org.uniprot.api.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;

@SpringBootApplication
@EnableJpaRepositories
@Import({HttpCommonHeaderConfig.class})
@ComponentScan(basePackages = {"org.uniprot.api.statistics", "org.uniprot.api.rest.validation"})
public class StatisticsRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatisticsRestApplication.class, args);
    }
}
