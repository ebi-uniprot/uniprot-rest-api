package org.uniprot.api.support.data.common.taxonomy.service;

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
public class TaxonomySolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/taxonomy-query.config";

    @Bean
    public SolrQueryConfig taxonomySolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig taxonomySearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.TAXONOMY);
    }

    @Bean
    public UniProtQueryProcessorConfig taxonomyQueryProcessorConfig(
            WhitelistFieldConfig whiteListFieldConfig,
            SearchFieldConfig taxonomySearchFieldConfig) {
        Map<String, String> taxonomyWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.TAXONOMY.toString().toLowerCase(), new HashMap<>());
        Set<String> searchFields = taxonomySearchFieldConfig.getSearchFieldNames();
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(getDefaultSearchOptimisedFieldItems(taxonomySearchFieldConfig))
                .whiteListFields(taxonomyWhiteListFields)
                .searchFieldsNames(searchFields)
                .searchFieldConfig(taxonomySearchFieldConfig)
                .build();
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig taxonomySearchFieldConfig) {
        return Collections.singletonList(
                taxonomySearchFieldConfig.getSearchFieldItemByName(
                        TaxonomyService.TAXONOMY_ID_FIELD));
    }
}
