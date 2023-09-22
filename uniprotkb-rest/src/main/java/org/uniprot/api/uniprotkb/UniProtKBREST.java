package org.uniprot.api.uniprotkb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.uniprotkb.configuration.UniProtKBCacheConfig;
import org.uniprot.api.uniprotkb.groupby.GroupByServiceConfig;

/**
 * Starts advanced-search REST application.
 *
 * @author lgonzales
 */
@SpringBootApplication
@Import({
    HttpCommonHeaderConfig.class,
    RepositoryConfig.class,
    GroupByServiceConfig.class,
    UniProtKBCacheConfig.class
})
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
