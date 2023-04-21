package org.uniprot.api.uniparc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;

/** Hello world! */
@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, ErrorHandlerConfig.class})
@ComponentScan(basePackages = {"org.uniprot.api.uniparc", "org.uniprot.api.rest", "org.uniprot.api"})
public class UniParcRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniParcRestApplication.class, args);
    }
}
