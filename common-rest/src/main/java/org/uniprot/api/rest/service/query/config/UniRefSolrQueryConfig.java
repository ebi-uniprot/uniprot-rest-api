package org.uniprot.api.rest.service.query.config;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@ComponentScan(basePackages = "org.uniprot.api.rest.validation.config")
@Configuration
public class UniRefSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/uniref-query.config";

    @Bean
    public SolrQueryConfig uniRefSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig uniRefSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIREF);
    }

    @Bean
    public UniProtQueryProcessorConfig uniRefQueryProcessorConfig(
            WhitelistFieldConfig whiteListFieldConfig, SearchFieldConfig uniRefSearchFieldConfig) {
        Map<String, String> uniRefWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.UNIREF.toString().toLowerCase(), new HashMap<>());
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(getDefaultSearchOptimisedFieldItems(uniRefSearchFieldConfig))
                .whiteListFields(uniRefWhiteListFields)
                .searchFieldConfig(uniRefSearchFieldConfig)
                .build();
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig uniRefSearchFieldConfig) {
        return asList(
                uniRefSearchFieldConfig.getSearchFieldItemByName("id"),
                uniRefSearchFieldConfig.getSearchFieldItemByName("upi"));
    }
}
