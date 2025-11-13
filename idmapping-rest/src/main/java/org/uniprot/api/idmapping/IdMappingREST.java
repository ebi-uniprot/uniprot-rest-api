package org.uniprot.api.idmapping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.uniprotkb.common.repository.search.PublicationRepository;
import org.uniprot.api.uniprotkb.common.repository.search.PublicationSolrQueryConfig;
import org.uniprot.api.uniprotkb.common.repository.store.protnlm.ProtNLMStoreConfig;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtKBEntryVersionService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniSaveClient;

/**
 * Starts the Id mapping service.
 *
 * <p>Created 15/02/2021
 *
 * @author Edd
 */
@SpringBootApplication
@Import({HttpCommonHeaderConfig.class})
@ComponentScan(
        basePackages = {
            "org.uniprot.api.idmapping",
            "org.uniprot.api.rest",
            "org.uniprot.api.common.repository.stream.rdf",
            "org.uniprot.api.common.repository.stream.store.uniprotkb",
            "org.uniprot.api.uniprotkb.common.service.groupby",
            "org.uniprot.api.uniprotkb.common.service.uniprotkb",
            "org.uniprot.api.uniprotkb.common.repository.search",
            "org.uniprot.api.uniprotkb.common.repository.store",
            "org.uniprot.api.support.data.common.rdf",
            "org.uniprot.api.uniprotkb.common.service.ec",
            "org.uniprot.api.uniprotkb.common.service.go",
            "org.uniprot.api.support.data.common.keyword.service",
            "org.uniprot.api.support.data.common.keyword.repository",
            "org.uniprot.api.support.data.common.taxonomy"
        },
        excludeFilters = {
            @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {
                        UniProtKBEntryVersionService.class,
                        UniSaveClient.class,
                        PublicationRepository.class,
                        PublicationSolrQueryConfig.class,
                        ProtNLMStoreConfig.class
                    })
        })
public class IdMappingREST {
    public static void main(String[] args) {
        SpringApplication.run(IdMappingREST.class, args);
    }
}
