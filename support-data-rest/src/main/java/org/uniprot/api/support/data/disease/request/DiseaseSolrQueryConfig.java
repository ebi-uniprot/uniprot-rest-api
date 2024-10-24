package org.uniprot.api.support.data.disease.request;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.DISEASE_REGEX;

import java.util.*;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.rest.service.request.RequestConverterImpl;
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.api.support.data.disease.service.DiseaseService;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@Configuration
@EnableConfigurationProperties(value = RequestConverterConfigProperties.class)
public class DiseaseSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/disease-query.config";
    private static final Pattern DISEASE_REGEX_PATTERN = Pattern.compile(DISEASE_REGEX);

    @Bean
    public SolrQueryConfig diseaseSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig diseaseSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.DISEASE);
    }

    @Bean
    public UniProtQueryProcessorConfig diseaseQueryProcessorConfig(
            WhitelistFieldConfig whiteListFieldConfig, SearchFieldConfig diseaseSearchFieldConfig) {
        Map<String, String> diseaseWhitelistFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.DISEASE.toString().toLowerCase(), new HashMap<>());
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(getDefaultSearchOptimisedFieldItems(diseaseSearchFieldConfig))
                .whiteListFields(diseaseWhitelistFields)
                .searchFieldConfig(diseaseSearchFieldConfig)
                .build();
    }

    @Bean
    public RequestConverter diseaseRequestConverter(
            SolrQueryConfig diseaseSolrQueryConf,
            DiseaseSolrSortClause diseaseSortClause,
            UniProtQueryProcessorConfig diseaseQueryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties) {
        return new RequestConverterImpl(
                diseaseSolrQueryConf,
                diseaseSortClause,
                diseaseQueryProcessorConfig,
                requestConverterConfigProperties,
                DISEASE_REGEX_PATTERN);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig diseaseSearchFieldConfig) {
        return Collections.singletonList(
                diseaseSearchFieldConfig.getSearchFieldItemByName(DiseaseService.DISEASE_ID_FIELD));
    }
}
