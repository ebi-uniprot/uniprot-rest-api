package org.uniprot.api.aa.service;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.ARBA_ID_REGEX;

import java.util.*;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.rest.service.request.RequestConverterImpl;
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
@EnableConfigurationProperties(value = RequestConverterConfigProperties.class)
public class ArbaSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/arba-query.config";
    private static final Pattern ARBA_REGEX_PATTERN = Pattern.compile(ARBA_ID_REGEX);

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
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(getDefaultSearchOptimisedFieldItems(arbaSearchFieldConfig))
                .whiteListFields(uniRuleWhiteListFields)
                .searchFieldConfig(arbaSearchFieldConfig)
                .build();
    }

    @Bean
    public RequestConverter arbaRequestConverter(
            SolrQueryConfig arbaSolrQueryConf,
            ArbaSortClause arbaSortClause,
            UniProtQueryProcessorConfig arbaQueryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties,
            FacetConfig arbaFacetConfig) {
        return new RequestConverterImpl(
                arbaSolrQueryConf,
                arbaSortClause,
                arbaQueryProcessorConfig,
                requestConverterConfigProperties,
                arbaFacetConfig,
                ARBA_REGEX_PATTERN);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig arbaSearchFieldConfig) {
        return Collections.singletonList(
                arbaSearchFieldConfig.getSearchFieldItemByName(ArbaService.ARBA_ID_FIELD));
    }
}
