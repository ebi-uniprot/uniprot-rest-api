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
import org.uniprot.api.idmapping.common.service.impl.UniParcIdService;
import org.uniprot.api.idmapping.common.service.impl.UniProtKBIdService;
import org.uniprot.api.idmapping.common.service.impl.UniRefIdService;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.uniprotkb.common.service.ec.ECService;
import org.uniprot.api.uniprotkb.common.service.go.GOService;
import org.uniprot.api.uniprotkb.common.service.go.client.GOClientImpl;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByECService;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByGOService;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByServiceConfig;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtKBEntryVersionService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniSaveClient;

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
            "org.uniprot.api.support.data.common"
        },
        excludeFilters = {
            @ComponentScan.Filter(
                    type = FilterType.REGEX,
                    pattern = "org\\.uniprot\\.api\\.uniprotkb\\.common\\.service\\.groupby\\.*"),
            @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {
                        UniProtKBIdMappingResultsConfig.class,
                        UniRefIdMappingResultsConfig.class,
                        UniParcIdMappingResultsConfig.class,
                        UniParcIdService.class,
                        UniProtKBIdService.class,
                        UniRefIdService.class,
                        UniProtKBEntryVersionService.class,
                        UniSaveClient.class,
                        ECService.class,
                        GOService.class,
                        GOClientImpl.class,
                        GroupByECService.class,
                        GroupByGOService.class,
                        GroupByServiceConfig.class
                    })
        })
public class AsyncDownloadRestApp {
    public static void main(String[] args) {
        SpringApplication.run(AsyncDownloadRestApp.class, args);
    }
}
