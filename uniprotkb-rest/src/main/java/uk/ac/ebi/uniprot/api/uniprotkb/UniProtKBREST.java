package uk.ac.ebi.uniprot.api.uniprotkb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;

import uk.ac.ebi.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.search.RepositoryConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.view.service.ViewByServiceConfig;

/**
 * Starts advanced-search REST application.
 *
 * @author lgonzales
 */
@SpringBootApplication
@EnableSolrRepositories(basePackages = {"uk.ac.ebi.uniprot.api.uniprotkb.repository.search.impl"})
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, ViewByServiceConfig.class})
@ComponentScan(basePackages = {"uk.ac.ebi.uniprot.api.uniprotkb","uk.ac.ebi.uniprot.api.rest"})
public class UniProtKBREST {
    public static void main(String[] args) {
        SpringApplication.run(UniProtKBREST.class, args);
    }
}
