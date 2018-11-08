package uk.ac.ebi.uniprot.uniprotkb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;
import uk.ac.ebi.uniprot.rest.output.header.HttpCommonHeaderConfig;

/**
 * Starts advanced-search REST application.
 *
 * @author lgonzales
 */
@SpringBootApplication
@EnableSolrRepositories(basePackages = {"uk.ac.ebi.uniprot.uniprotkb.repository.search.impl"})
@Import({HttpCommonHeaderConfig.class})
@ComponentScan(basePackages = {"uk.ac.ebi.uniprot.uniprotkb","uk.ac.ebi.uniprot.rest"})
public class UniProtKbREST {
    public static void main(String[] args) {
        SpringApplication.run(UniProtKbREST.class, args);
    }
}
