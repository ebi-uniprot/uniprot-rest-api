package uk.ac.ebi.uniprot.api.proteome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import uk.ac.ebi.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import uk.ac.ebi.uniprot.api.rest.respository.RepositoryConfig;

/**
 *
 * @author jluo
 * @date: 24 Apr 2019
 *
*/


@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class})
@ComponentScan(basePackages = {"uk.ac.ebi.uniprot.api.proteome","uk.ac.ebi.uniprot.api.rest"})
public class ProteomeRestApplication {
	public static void main(String[] args) {
		SpringApplication.run(ProteomeRestApplication.class, args);
	}
}
