package uk.ac.ebi.uniprot.api.suggester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import uk.ac.ebi.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import uk.ac.ebi.uniprot.api.rest.respository.RepositoryConfig;
import uk.ac.ebi.uniprot.api.suggester.service.SuggesterServiceConfig;

/**
 * Starts the REST application.
 *
 * Created 18/07/18
 *
 * @author Edd
 */
@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, SuggesterServiceConfig.class})
public class SuggesterREST {
    /**
     * Ensures that placeholders are replaced with property values
     */
    @Bean
    static PropertySourcesPlaceholderConfigurer propertyPlaceHolderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public static void main(String[] args) {
        SpringApplication.run(SuggesterREST.class, args);
    }
}
