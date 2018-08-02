package uk.ac.ebi.uniprot.uuw.advanced.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

/**
 * Starts advanced-search REST application.
 *
 * @author lgonzales
 */
@SpringBootApplication
@EnableSolrRepositories(basePackages = { "uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl" })
public class AdvancedSearchREST {

    public static void main(String[] args) {
        SpringApplication.run(AdvancedSearchREST.class, args);
    }

}
