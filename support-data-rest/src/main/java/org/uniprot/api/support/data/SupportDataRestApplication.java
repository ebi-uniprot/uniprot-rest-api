package org.uniprot.api.support.data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.support.data.suggester.service.SuggesterServiceConfig;

@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, SuggesterServiceConfig.class})
@ComponentScan(basePackages = {"org.uniprot.api", "org.uniprot.api.rest"})
public class SupportDataRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupportDataRestApplication.class, args);
    }
}
