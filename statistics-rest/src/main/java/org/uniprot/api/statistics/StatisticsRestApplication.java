package org.uniprot.api.statistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
// @Import({HttpCommonHeaderConfig.class, ErrorHandlerConfig.class})
/*@ComponentScan(
basePackages = {"org.uniprot.api", "org.uniprot.api.rest", "org.uniprot.api.statistics"})*/
public class StatisticsRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatisticsRestApplication.class, args);
    }
}
