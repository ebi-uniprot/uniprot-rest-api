package org.uniprot.api.support.data.disease.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.api.support.data.disease.service.DiseaseService;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@Configuration
public class DiseaseSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/disease-query.config";

    @Bean
    public SolrQueryConfig diseaseSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig diseaseSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.DISEASE);
    }

    @Bean
    public QueryProcessor diseaseQueryProcessor(
            WhitelistFieldConfig whiteListFieldConfig, SearchFieldConfig diseaseSearchFieldConfig) {
        Map<String, String> diseaseWhitelistFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.DISEASE.toString().toLowerCase(), new HashMap<>());
        return UniProtQueryProcessor.newInstance(
                UniProtQueryProcessorConfig.builder()
                        .optimisableFields(
                                getDefaultSearchOptimisedFieldItems(diseaseSearchFieldConfig))
                        .whiteListFields(diseaseWhitelistFields)
                        .build());
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig diseaseSearchFieldConfig) {
        return Collections.singletonList(
                diseaseSearchFieldConfig.getSearchFieldItemByName(DiseaseService.DISEASE_ID_FIELD));
    }
}
