package org.uniprot.api.support.data.subcellular.request;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.SUBCELLULAR_LOCATION_ID_REGEX;

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
import org.uniprot.api.support.data.subcellular.service.SubcellularLocationService;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@Configuration
public class SubcellularLocationSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/subcellular-query.config";
    private static final Pattern SUBCELLULAR_LOCATION_ID_REGEX_PATTERN =
            Pattern.compile(SUBCELLULAR_LOCATION_ID_REGEX);

    @Bean
    public SolrQueryConfig subcellSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig subcellSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.SUBCELLLOCATION);
    }

    @Bean
    public UniProtQueryProcessorConfig subcellQueryProcessorConfig(
            WhitelistFieldConfig whiteListFieldConfig, SearchFieldConfig subcellSearchFieldConfig) {
        Map<String, String> subcellWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.SUBCELLLOCATION.toString().toLowerCase(),
                                new HashMap<>());
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(getDefaultSearchOptimisedFieldItems(subcellSearchFieldConfig))
                .whiteListFields(subcellWhiteListFields)
                .searchFieldConfig(subcellSearchFieldConfig)
                .build();
    }

    @Bean
    public RequestConverter subcellRequestConverter(
            SolrQueryConfig subcellSolrQueryConf,
            SubcellularLocationSortClause subcellSortClause,
            UniProtQueryProcessorConfig subcellQueryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties) {
        return new RequestConverterImpl(
                subcellSolrQueryConf,
                subcellSortClause,
                subcellQueryProcessorConfig,
                requestConverterConfigProperties,
                null,
                SUBCELLULAR_LOCATION_ID_REGEX_PATTERN);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig searchFieldConfig) {
        return Collections.singletonList(
                searchFieldConfig.getSearchFieldItemByName(
                        SubcellularLocationService.SUBCELL_ID_FIELD));
    }
}
