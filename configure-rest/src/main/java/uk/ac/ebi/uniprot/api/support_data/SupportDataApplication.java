package uk.ac.ebi.uniprot.api.support_data;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import uk.ac.ebi.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import uk.ac.ebi.uniprot.api.rest.respository.RepositoryConfig;
import uk.ac.ebi.uniprot.api.suggester.service.SuggesterServiceConfig;

@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, SuggesterServiceConfig.class})
@ComponentScan(basePackages = {"uk.ac.ebi.uniprot.api","uk.ac.ebi.uniprot.api.rest"})
public class SupportDataApplication {
	public static void main(String[] args) {
		SpringApplication.run(SupportDataApplication.class, args);
	}
}
