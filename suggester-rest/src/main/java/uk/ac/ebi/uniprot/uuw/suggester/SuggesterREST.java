package uk.ac.ebi.uniprot.uuw.suggester;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Starts the REST application.
 *
 * Created 18/07/18
 *
 * @author Edd
 */
@SpringBootApplication
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
