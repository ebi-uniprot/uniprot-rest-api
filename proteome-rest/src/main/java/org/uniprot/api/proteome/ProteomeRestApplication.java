package org.uniprot.api.proteome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;

/**
 *
 * @author jluo
 * @date: 24 Apr 2019
 *
*/


@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, ErrorHandlerConfig.class})
@ComponentScan(basePackages = {"org.uniprot.api.proteome","org.uniprot.api.rest"})
public class ProteomeRestApplication {
	public static void main(String[] args) {
		SpringApplication.run(ProteomeRestApplication.class, args);
	}
}
