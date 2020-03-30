package org.uniprot.api.unisave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@SpringBootApplication
@Import({HttpCommonHeaderConfig.class})
@EntityScan(basePackages = {"org.uniprot.api.unisave"})
@ComponentScan(basePackages = {"org.uniprot.api.unisave", "org.uniprot.api.rest"})
public class UniSaveRESTApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniSaveRESTApplication.class, args);
    }
}
