package org.uniprot.api.help.centre.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Configuration
public class HelpCentreSolrQueryConfig {

    private static final String RESOURCE_LOCATION = "/help-centre-query.config";

    @Bean
    public SolrQueryConfig helpCentreSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig helpCentreSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.HELP);
    }

    @Bean
    public UniProtQueryProcessorConfig helpCentreQueryProcessorConfig(
            WhitelistFieldConfig whiteListFieldConfig,
            SearchFieldConfig helpCentreSearchFieldConfig) {
        Map<String, String> helpCentreWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.HELP.toString().toLowerCase(), new HashMap<>());
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(Collections.emptyList())
                .whiteListFields(helpCentreWhiteListFields)
                .searchFieldConfig(helpCentreSearchFieldConfig)
                .build();
    }

    @Bean
    public RequestConverter helpCentreRequestConverter(
            SolrQueryConfig helpCentreSolrQueryConf,
            HelpCentreSortClause helpCentreSortClause,
            UniProtQueryProcessorConfig helpCentreQueryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties,
            FacetConfig helpCentreFacetConfig) {
        return new RequestConverterImpl(
                helpCentreSolrQueryConf,
                helpCentreSortClause,
                helpCentreQueryProcessorConfig,
                requestConverterConfigProperties,
                helpCentreFacetConfig);
    }
}
