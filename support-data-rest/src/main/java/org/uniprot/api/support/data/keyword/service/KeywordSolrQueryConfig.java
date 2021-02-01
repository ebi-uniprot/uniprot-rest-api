package org.uniprot.api.support.data.keyword.service;

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
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@Configuration
public class KeywordSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/keyword-query.config";

    @Bean
    public SolrQueryConfig keywordSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig keywordSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.KEYWORD);
    }

    @Bean
    public QueryProcessor keywordQueryProcessor(
            WhitelistFieldConfig whiteListFieldConfig, SearchFieldConfig keywordSearchFieldConfig) {
        Map<String, String> keywordWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.KEYWORD.toString().toLowerCase(), new HashMap<>());
        return UniProtQueryProcessor.newInstance(
                UniProtQueryProcessorConfig.builder()
                        .optimisableFields(
                                getDefaultSearchOptimisedFieldItems(keywordSearchFieldConfig))
                        .whiteListFields(keywordWhiteListFields)
                        .build());
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig keywordSearchFieldConfig) {
        return Collections.singletonList(
                keywordSearchFieldConfig.getSearchFieldItemByName(KeywordService.KEYWORD_ID_FIELD));
    }
}
