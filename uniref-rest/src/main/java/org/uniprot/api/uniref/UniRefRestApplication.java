package org.uniprot.api.uniref;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.download.config.RedisConfig;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;

/** @author jluo */
@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, ErrorHandlerConfig.class})
@ComponentScan(
        basePackages = {
            "org.uniprot.api.uniref",
            "org.uniprot.api.uniref.common",
            "org.uniprot.api.rest",
            "org.uniprot.api.common.repository.stream.rdf"
        },
        excludeFilters = {
            @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {RedisConfig.class})
        })
public class UniRefRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniRefRestApplication.class, args);
    }
}
