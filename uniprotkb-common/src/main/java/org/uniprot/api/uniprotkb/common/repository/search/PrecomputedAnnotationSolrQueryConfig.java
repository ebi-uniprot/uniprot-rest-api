package org.uniprot.api.uniprotkb.common.repository.search;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.uniprotkb.common.service.precomputed.PrecomputedAnnotationRequestConverterImpl;
import org.uniprot.api.uniprotkb.common.service.precomputed.PrecomputedAnnotationSortClause;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

@Configuration
@EnableConfigurationProperties(value = RequestConverterConfigProperties.class)
public class PrecomputedAnnotationSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/precomputed-annotation-query.config";

    @Bean(name = "precomputedAnnotationQueryConfig")
    public SolrQueryConfig precomputedAnnotationSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig precomputedAnnotationSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(
                UniProtDataType.PRECOMPUTED_ANNOTATION);
    }

    @Bean
    public UniProtQueryProcessorConfig precomputedAnnotationQueryProcessorConfig(
            @Qualifier("precomputedAnnotationSearchFieldConfig")
                    SearchFieldConfig precomputedAnnotationSearchFieldConfig) {
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(emptyList())
                .whiteListFields(emptyMap())
                .searchFieldConfig(precomputedAnnotationSearchFieldConfig)
                .build();
    }

    @Bean
    public RequestConverter precomputedAnnotationRequestConverter(
            @Qualifier("precomputedAnnotationQueryConfig")
                    SolrQueryConfig precomputedAnnotationSolrQueryConf,
            PrecomputedAnnotationSortClause precomputedAnnotationSortClause,
            @Qualifier("precomputedAnnotationQueryProcessorConfig")
                    UniProtQueryProcessorConfig precomputedAnnotationQueryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties) {
        return new PrecomputedAnnotationRequestConverterImpl(
                precomputedAnnotationSolrQueryConf,
                precomputedAnnotationSortClause,
                precomputedAnnotationQueryProcessorConfig,
                requestConverterConfigProperties);
    }
}
