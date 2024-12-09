package org.uniprot.api.rest.service.query.config;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.UNIPARC_UPI_REGEX;

import java.util.*;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.query.sort.UniParcSortClause;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.rest.service.request.RequestConverterImpl;
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@ComponentScan(basePackages = "org.uniprot.api.rest.validation.config")
@EnableConfigurationProperties(value = RequestConverterConfigProperties.class)
@Configuration
public class UniParcSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/uniparc-query.config";
    private static final Pattern UNIPARC_UPI_REGEX_PATTERN = Pattern.compile(UNIPARC_UPI_REGEX);

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
                .searchFieldConfig(uniParcSearchFieldConfig)
                .build();
    }

    @Bean
    public RequestConverter uniParcRequestConverter(
            SolrQueryConfig uniParcSolrQueryConf,
            UniParcSortClause uniParcSortClause,
            UniProtQueryProcessorConfig uniParcQueryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties,
            FacetConfig uniParcFacetConfig) {
        return new RequestConverterImpl(
                uniParcSolrQueryConf,
                uniParcSortClause,
                uniParcQueryProcessorConfig,
                requestConverterConfigProperties,
                uniParcFacetConfig,
                UNIPARC_UPI_REGEX_PATTERN);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig uniParcSearchFieldConfig) {
        return Collections.singletonList(uniParcSearchFieldConfig.getSearchFieldItemByName("upi"));
    }
}
