package org.uniprot.api.unisave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@SpringBootApplication
@Import({HttpCommonHeaderConfig.class})
@EntityScan(basePackages = {"org.uniprot.api.unisave"})
@ComponentScan(
        basePackages = {"org.uniprot.api.unisave", "org.uniprot.api.rest"},
        excludeFilters = {
            @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    value = RepositoryConfig.class),
            @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "org\\.uniprot\\.api\\.rest\\.service\\.query\\..*")
        })
public class UniSaveRESTApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniSaveRESTApplication.class, args);
    }
}
