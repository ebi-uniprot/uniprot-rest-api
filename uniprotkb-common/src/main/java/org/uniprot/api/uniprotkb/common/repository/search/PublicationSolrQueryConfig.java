package org.uniprot.api.uniprotkb.common.repository.search;

import static java.util.Collections.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

@Configuration
public class PublicationSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/publication-query.config";

    @Bean(name = "publicationQueryConfig")
    public SolrQueryConfig publicationSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig publicationSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.PUBLICATION);
    }

    @Bean
    public UniProtQueryProcessorConfig publicationQueryProcessorConfig(
            SearchFieldConfig publicationSearchFieldConfig) {
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(emptyList())
                .whiteListFields(emptyMap())
                .searchFieldConfig(publicationSearchFieldConfig)
                .build();
    }
}
