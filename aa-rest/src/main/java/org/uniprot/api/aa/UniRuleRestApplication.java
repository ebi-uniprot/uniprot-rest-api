package org.uniprot.api.aa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, ErrorHandlerConfig.class})
@ComponentScan(basePackages = {"org.uniprot.api.aa", "org.uniprot.api.rest"})
public class UniRuleRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniRuleRestApplication.class, args);
    }
}
