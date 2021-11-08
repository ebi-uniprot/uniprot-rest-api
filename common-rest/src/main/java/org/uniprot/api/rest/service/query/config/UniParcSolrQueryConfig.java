package org.uniprot.api.rest.service.query.config;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ComponentScan(basePackages = "org.uniprot.api.rest.validation.config")
@Configuration
public class UniParcSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/uniparc-query.config";

    @Bean
    public SolrQueryConfig uniParcSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig uniParcSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
    }

    @Bean
    public UniProtQueryProcessorConfig uniParcQueryProcessorConfig(
            WhitelistFieldConfig whiteListFieldConfig, SearchFieldConfig uniParcSearchFieldConfig) {
        Map<String, String> uniParcWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.UNIPARC.toString().toLowerCase(), new HashMap<>());
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(getDefaultSearchOptimisedFieldItems(uniParcSearchFieldConfig))
                .whiteListFields(uniParcWhiteListFields)
                .build();
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig uniParcSearchFieldConfig) {
        return Collections.singletonList(uniParcSearchFieldConfig.getSearchFieldItemByName("upi"));
    }
}
