package org.uniprot.api.aa.service;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.UNIRULE_ALL_ID_REGEX;

import java.util.*;
import java.util.regex.Pattern;

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
 * @created 11/11/2020
 */
@Configuration
public class UniRuleSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/unirule-query.config";
    private static final Pattern UNIRULE_REGEX_PATTERN = Pattern.compile(UNIRULE_ALL_ID_REGEX);

    @Bean
    public SolrQueryConfig uniRuleSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig uniRuleSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIRULE);
    }

    @Bean
    public UniProtQueryProcessorConfig uniRuleQueryProcessorConfig(
            WhitelistFieldConfig whiteListFieldConfig, SearchFieldConfig uniRuleSearchFieldConfig) {
        Map<String, String> uniRuleWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.UNIRULE.toString().toLowerCase(), new HashMap<>());
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(getDefaultSearchOptimisedFieldItems(uniRuleSearchFieldConfig))
                .whiteListFields(uniRuleWhiteListFields)
                .searchFieldConfig(uniRuleSearchFieldConfig)
                .build();
    }

    @Bean
    public RequestConverter uniRuleRequestConverter(
            SolrQueryConfig uniRuleSolrQueryConf,
            UniRuleSortClause uniRuleSortClause,
            UniProtQueryProcessorConfig uniRuleQueryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties,
            FacetConfig uniRuleFacetConfig) {
        return new RequestConverterImpl(
                uniRuleSolrQueryConf,
                uniRuleSortClause,
                uniRuleQueryProcessorConfig,
                requestConverterConfigProperties,
                uniRuleFacetConfig,
                UNIRULE_REGEX_PATTERN);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig uniRuleSearchFieldConfig) {
        return Collections.singletonList(
                uniRuleSearchFieldConfig.getSearchFieldItemByName(
                        UniRuleService.ALL_RULE_ID_FIELD));
    }
}
