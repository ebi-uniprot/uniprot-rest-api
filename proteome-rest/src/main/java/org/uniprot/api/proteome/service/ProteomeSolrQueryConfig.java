package org.uniprot.api.proteome.service;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.PROTEOME_ID_REGEX;

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

@Configuration
@EnableConfigurationProperties(value = RequestConverterConfigProperties.class)
public class ProteomeSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/proteome-query.config";
    private static final Pattern PROTEOME_ID_REGEX_PATTERN = Pattern.compile(PROTEOME_ID_REGEX);

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

    @Bean
    public RequestConverter proteomeRequestConverter(
            SolrQueryConfig proteomeSolrQueryConf,
            ProteomeSortClause proteomeSortClause,
            UniProtQueryProcessorConfig proteomeQueryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties,
            FacetConfig proteomeFacetConfig) {
        return new RequestConverterImpl(
                proteomeSolrQueryConf,
                proteomeSortClause,
                proteomeQueryProcessorConfig,
                requestConverterConfigProperties,
                proteomeFacetConfig,
                PROTEOME_ID_REGEX_PATTERN);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig proteomeSearchFieldConfig) {
        return Collections.singletonList(
                proteomeSearchFieldConfig.getSearchFieldItemByName(
                        ProteomeQueryService.PROTEOME_ID_FIELD));
    }
}
