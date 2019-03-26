package uk.ac.ebi.uniprot.view.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import uk.ac.ebi.uniprot.rest.output.header.HttpCommonHeaderConfig;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.RepositoryConfig;
import uk.ac.ebi.uniprot.view.api.service.ViewByServiceConfig;




@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, ViewByServiceConfig.class})

public class ViewByRestApplication {
	   @Bean
	    static PropertySourcesPlaceholderConfigurer propertyPlaceHolderConfigurer() {
	        return new PropertySourcesPlaceholderConfigurer();
	    }
	public static void main(String[] args) {
		SpringApplication.run(ViewByRestApplication.class, args);
	}
}