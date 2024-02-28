package org.uniprot.api.proteome.service;

import java.util.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@Configuration
public class ProteomeSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/proteome-query.config";

    @Bean
    public SolrQueryConfig proteomeSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig proteomeSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.PROTEOME);
    }

    @Bean
    public UniProtQueryProcessorConfig proteomeQueryProcessorConfig(
            WhitelistFieldConfig whiteListFieldConfig,
            SearchFieldConfig proteomeSearchFieldConfig) {
        Map<String, String> proteomeWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.PROTEOME.toString().toLowerCase(), new HashMap<>());
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(getDefaultSearchOptimisedFieldItems(proteomeSearchFieldConfig))
                .whiteListFields(proteomeWhiteListFields)
                .searchFieldConfig(proteomeSearchFieldConfig)
                .build();
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig proteomeSearchFieldConfig) {
        return Collections.singletonList(
                proteomeSearchFieldConfig.getSearchFieldItemByName(
                        ProteomeQueryService.PROTEOME_ID_FIELD));
    }
}
