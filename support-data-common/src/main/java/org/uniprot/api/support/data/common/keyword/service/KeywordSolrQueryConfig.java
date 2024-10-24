package org.uniprot.api.support.data.common.keyword.service;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.KEYWORD_ID_REGEX;

import java.util.*;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.rest.service.request.RequestConverterImpl;
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@Configuration
public class KeywordSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/keyword-query.config";
    private static final Pattern KEYWORD_ID_REGEX_PATTERN = Pattern.compile(KEYWORD_ID_REGEX);

    @Bean
    public SolrQueryConfig keywordSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig keywordSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.KEYWORD);
    }

    @Bean
    public UniProtQueryProcessorConfig keywordQueryProcessorConfig(
            WhitelistFieldConfig whiteListFieldConfig, SearchFieldConfig keywordSearchFieldConfig) {
        Map<String, String> keywordWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.KEYWORD.toString().toLowerCase(), new HashMap<>());
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(getDefaultSearchOptimisedFieldItems(keywordSearchFieldConfig))
                .whiteListFields(keywordWhiteListFields)
                .searchFieldConfig(keywordSearchFieldConfig)
                .build();
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig keywordSearchFieldConfig) {
        return Collections.singletonList(
                keywordSearchFieldConfig.getSearchFieldItemByName(KeywordService.KEYWORD_ID_FIELD));
    }

    @Bean
    public RequestConverter keywordRequestConverter(
            SolrQueryConfig keywordSolrQueryConf,
            KeywordSortClause keywordSolrClause,
            UniProtQueryProcessorConfig keywordQueryProcessorConfig,
            RequestConverterConfigProperties uniProtRequestConverterConfigProperties) {
        return new RequestConverterImpl(
                keywordSolrQueryConf,
                keywordSolrClause,
                keywordQueryProcessorConfig,
                uniProtRequestConverterConfigProperties,
                KEYWORD_ID_REGEX_PATTERN);
    }
}
