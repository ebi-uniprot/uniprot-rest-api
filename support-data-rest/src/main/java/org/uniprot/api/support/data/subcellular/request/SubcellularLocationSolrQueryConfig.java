package org.uniprot.api.support.data.subcellular.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.api.support.data.subcellular.service.SubcellularLocationService;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@Configuration
public class SubcellularLocationSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/subcellular-query.config";

    @Bean
    public SolrQueryConfig subcellSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig subcellSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.SUBCELLLOCATION);
    }

    @Bean
    public QueryProcessor subcellQueryProcessor(
            WhitelistFieldConfig whiteListFieldConfig, SearchFieldConfig subcellSearchFieldConfig) {
        Map<String, String> subcellWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.SUBCELLLOCATION.toString().toLowerCase(),
                                new HashMap<>());
        return UniProtQueryProcessor.newInstance(
                UniProtQueryProcessorConfig.builder()
                        .optimisableFields(
                                getDefaultSearchOptimisedFieldItems(subcellSearchFieldConfig))
                        .whiteListFields(subcellWhiteListFields)
                        .build());
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig searchFieldConfig) {
        return Collections.singletonList(
                searchFieldConfig.getSearchFieldItemByName(
                        SubcellularLocationService.SUBCELL_ID_FIELD));
    }
}
