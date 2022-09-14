package org.uniprot.api.aa.service;

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

/**
 * @author sahmad
 * @created 19/07/2021
 */
@Configuration
public class ArbaSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/arba-query.config";

    @Bean
    public SolrQueryConfig arbaSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig arbaSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.ARBA);
    }

    @Bean
    public UniProtQueryProcessorConfig arbaQueryProcessorConfig(
            WhitelistFieldConfig whiteListFieldConfig, SearchFieldConfig arbaSearchFieldConfig) {
        Map<String, String> uniRuleWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.ARBA.toString().toLowerCase(), new HashMap<>());
        Set<String> searchFields = arbaSearchFieldConfig.getSearchFieldNames();
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(getDefaultSearchOptimisedFieldItems(arbaSearchFieldConfig))
                .whiteListFields(uniRuleWhiteListFields)
                .searchFieldsNames(searchFields)
                .build();
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig arbaSearchFieldConfig) {
        return Collections.singletonList(
                arbaSearchFieldConfig.getSearchFieldItemByName(ArbaService.ARBA_ID_FIELD));
    }
}
