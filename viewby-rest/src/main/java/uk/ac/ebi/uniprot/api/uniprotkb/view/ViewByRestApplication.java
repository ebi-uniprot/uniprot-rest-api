package uk.ac.ebi.uniprot.api.uniprotkb.view;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import uk.ac.ebi.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
//import uk.ac.ebi.uniprot.api.uniprotkb.repository.search.RepositoryConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.view.service.ViewByServiceConfig;




@SpringBootApplication
//@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, ViewByServiceConfig.class})

public class ViewByRestApplication {
	   @Bean
	    static PropertySourcesPlaceholderConfigurer propertyPlaceHolderConfigurer() {
	        return new PropertySourcesPlaceholderConfigurer();
	    }
	public static void main(String[] args) {
		SpringApplication.run(ViewByRestApplication.class, args);
	}
}