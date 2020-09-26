package org.uniprot.api.uniparc.service;

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
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

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
    public QueryProcessor uniParcQueryProcessor(
            WhitelistFieldConfig whiteListFieldConfig, SearchFieldConfig uniParcSearchFieldConfig) {
        Map<String, String> uniParcWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.UNIPARC.toString().toLowerCase(), new HashMap<>());
        return new UniProtQueryProcessor(
                getDefaultSearchOptimisedFieldItems(uniParcSearchFieldConfig),
                uniParcWhiteListFields);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig uniParcSearchFieldConfig) {
        return Collections.singletonList(
                uniParcSearchFieldConfig.getSearchFieldItemByName(
                        UniParcQueryService.UNIPARC_ID_FIELD));
    }
}
