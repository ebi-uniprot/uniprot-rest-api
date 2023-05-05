package org.uniprot.api.uniref;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;

/** @author jluo */
@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, ErrorHandlerConfig.class})
@ComponentScan(basePackages = {"org.uniprot.api.uniref", "org.uniprot.api.rest", "org.uniprot.api"})
public class UniRefRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniRefRestApplication.class, args);
    }
}
