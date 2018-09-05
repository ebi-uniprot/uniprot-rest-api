package uk.ac.ebi.uniprot.uuw.advanced.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;
import uk.ac.ebi.uniprot.rest.http.HttpCommonHeaderConfig;

/**
 * Starts advanced-search REST application.
 *
 * @author lgonzales
 */
@SpringBootApplication
@EnableSolrRepositories(basePackages = {"uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl"})
@Import({HttpCommonHeaderConfig.class})
public class AdvancedSearchREST {
    public static void main(String[] args) {
        SpringApplication.run(AdvancedSearchREST.class, args);
    }
}
