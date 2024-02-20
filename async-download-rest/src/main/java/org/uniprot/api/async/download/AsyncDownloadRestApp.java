package org.uniprot.api.async.download;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.uniprot.api.idmapping.common.service.config.UniParcIdMappingResultsConfig;
import org.uniprot.api.idmapping.common.service.config.UniProtKBIdMappingResultsConfig;
import org.uniprot.api.idmapping.common.service.config.UniRefIdMappingResultsConfig;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByServiceConfig;

@SpringBootApplication
@EnableCaching
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, GroupByServiceConfig.class})
@ComponentScan(
        basePackages = {
            "org.uniprot.api.uniparc.common",
            "org.uniprot.api.async.download",
            "org.uniprot.api.rest",
            "org.uniprot.api.uniprotkb.common",
            "org.uniprot.api.uniref.common",
            "org.uniprot.api.idmapping.common",
            "org.uniprot.api.common.repository.stream.store.uniprotkb",
            "org.uniprot.api.common.repository.stream.rdf",
            "org.uniprot.api.support.data.common.keyword",
            "org.uniprot.api.support.data.common.taxonomy"
        },
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {
                            UniProtKBIdMappingResultsConfig.class,
                            UniRefIdMappingResultsConfig.class,
                            UniParcIdMappingResultsConfig.class
                        }))
public class AsyncDownloadRestApp {
    public static void main(String[] args) {
        SpringApplication.run(AsyncDownloadRestApp.class, args);
    }
}
