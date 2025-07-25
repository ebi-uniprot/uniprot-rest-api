package org.uniprot.api.mapto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.uniprot.api.idmapping.common.response.IdMappingMessageConverterConfig;
import org.uniprot.api.idmapping.common.service.config.UniParcIdMappingResultsConfig;
import org.uniprot.api.idmapping.common.service.config.UniProtKBIdMappingResultsConfig;
import org.uniprot.api.idmapping.common.service.config.UniRefIdMappingResultsConfig;
import org.uniprot.api.idmapping.common.service.impl.UniParcLightIdService;
import org.uniprot.api.idmapping.common.service.impl.UniProtKBIdService;
import org.uniprot.api.idmapping.common.service.impl.UniRefIdService;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniprotkb.common.service.ec.ECService;
import org.uniprot.api.uniprotkb.common.service.go.GOService;
import org.uniprot.api.uniprotkb.common.service.go.client.GOClientImpl;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByECService;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByGOService;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByServiceConfig;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtKBEntryVersionService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniSaveClient;

@SpringBootApplication
@Import({HttpCommonHeaderConfig.class, RepositoryConfig.class, ErrorHandlerConfig.class})
@ComponentScan(
        basePackages = {
            "org.uniprot.api.mapto",
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
                    type = FilterType.REGEX,
                    pattern = "org\\.uniprot\\.api\\.uniprotkb\\.common\\.service\\.protnlm\\..*"),
            @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {
                        IdMappingMessageConverterConfig.class,
                        UniProtKBIdMappingResultsConfig.class,
                        UniRefIdMappingResultsConfig.class,
                        UniParcIdMappingResultsConfig.class,
                        UniParcLightIdService.class,
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
public class MapToREST {
    public static void main(String[] args) {
        SpringApplication.run(MapToREST.class, args);
    }
}
