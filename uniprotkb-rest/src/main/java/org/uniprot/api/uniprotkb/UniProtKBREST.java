package org.uniprot.api.uniprotkb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.uniprotkb.view.service.ViewByServiceConfig;

/**
 * Starts advanced-search REST application.
 *
 * @author lgonzales
 */
@SpringBootApplication
//@EnableSolrRepositories(basePackages = {"org.uniprot.api.uniprotkb.repository.search.impl"})
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, ViewByServiceConfig.class })
@ComponentScan(basePackages = {"org.uniprot.api.uniprotkb","org.uniprot.api.rest"})
public class UniProtKBREST {
    public static void main(String[] args) {
        SpringApplication.run(UniProtKBREST.class, args);
    }
}
