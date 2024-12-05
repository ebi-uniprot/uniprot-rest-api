package org.uniprot.api.rest.service.query.config;

import static java.util.Collections.emptyMap;

import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.query.sort.LiteratureSortClause;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.rest.service.request.RequestConverterImpl;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@Configuration
public class LiteratureSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/literature-query.config";

    @Bean
    public SolrQueryConfig literatureSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig literatureSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.LITERATURE);
    }

    @Bean
    public UniProtQueryProcessorConfig literatureQueryProcessorConfig(
            SearchFieldConfig literatureSearchFieldConfig) {
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(getDefaultSearchOptimisedFieldItems(literatureSearchFieldConfig))
                .whiteListFields(emptyMap())
                .searchFieldConfig(literatureSearchFieldConfig)
                .build();
    }

    @Bean
    public RequestConverter literatureRequestConverter(
            SolrQueryConfig literatureSolrQueryConf,
            LiteratureSortClause literatureSortClause,
            UniProtQueryProcessorConfig literatureQueryProcessorConfig,
            FacetConfig literatureFacetConfig,
            RequestConverterConfigProperties requestConverterConfigProperties) {
        return new RequestConverterImpl(
                literatureSolrQueryConf,
                literatureSortClause,
                literatureQueryProcessorConfig,
                requestConverterConfigProperties,
                literatureFacetConfig);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig literatureSearchFieldConfig) {
        return Collections.singletonList(
                literatureSearchFieldConfig.getSearchFieldItemByName("id"));
    }
}
