package org.uniprot.api.support.data.crossref.request;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.CROSS_REF_REGEX;

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
import org.uniprot.api.support.data.crossref.service.CrossRefService;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@Configuration
public class CrossRefSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/crossref-query.config";
    private static final Pattern CROSS_REF_REGEX_PATTERN = Pattern.compile(CROSS_REF_REGEX);

    @Bean
    public SolrQueryConfig crossRefSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig crossRefSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.CROSSREF);
    }

    @Bean
    public UniProtQueryProcessorConfig crossRefQueryProcessorConfig(
            WhitelistFieldConfig whiteListFieldConfig,
            SearchFieldConfig crossRefSearchFieldConfig) {
        Map<String, String> crossRefWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.CROSSREF.toString().toLowerCase(), new HashMap<>());
        return UniProtQueryProcessorConfig.builder()
                .optimisableFields(getDefaultSearchOptimisedFieldItems(crossRefSearchFieldConfig))
                .whiteListFields(crossRefWhiteListFields)
                .searchFieldConfig(crossRefSearchFieldConfig)
                .build();
    }

    @Bean
    public RequestConverter crossRefRequestConverter(
            SolrQueryConfig crossRefSolrQueryConf,
            CrossRefSolrSortClause crossRefSortClause,
            UniProtQueryProcessorConfig crossRefQueryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties,
            FacetConfig crossRefFacetConfig) {
        return new RequestConverterImpl(
                crossRefSolrQueryConf,
                crossRefSortClause,
                crossRefQueryProcessorConfig,
                requestConverterConfigProperties,
                crossRefFacetConfig,
                CROSS_REF_REGEX_PATTERN);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig crossRefSearchFieldConfig) {
        return Collections.singletonList(
                crossRefSearchFieldConfig.getSearchFieldItemByName(
                        CrossRefService.CROSS_REF_ID_FIELD));
    }
}
