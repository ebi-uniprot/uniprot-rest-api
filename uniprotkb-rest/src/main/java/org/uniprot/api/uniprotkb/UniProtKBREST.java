package org.uniprot.api.uniprotkb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByServiceConfig;

/**
 * Starts advanced-search REST application.
 *
 * @author lgonzales
 */
@SpringBootApplication
@EnableCaching
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, GroupByServiceConfig.class})
@ComponentScan(
        basePackages = {
            "org.uniprot.api.uniprotkb",
            "org.uniprot.api.rest",
            "org.uniprot.api.common.repository.stream.store.uniprotkb",
            "org.uniprot.api.common.repository.stream.rdf",
            "org.uniprot.api.support.data.common.keyword",
            "org.uniprot.api.support.data.common.taxonomy"
        })
public class UniProtKBREST {
    public static void main(String[] args) {
        SpringApplication.run(UniProtKBREST.class, args);
    }
}
