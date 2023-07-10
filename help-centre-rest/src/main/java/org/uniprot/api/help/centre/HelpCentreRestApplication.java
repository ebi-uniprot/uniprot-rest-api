package org.uniprot.api.help.centre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;

@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, ErrorHandlerConfig.class})
@ComponentScan(basePackages = {"org.uniprot.api.help.centre", "org.uniprot.api.rest"})
public class HelpCentreRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(HelpCentreRestApplication.class, args);
    }
}
