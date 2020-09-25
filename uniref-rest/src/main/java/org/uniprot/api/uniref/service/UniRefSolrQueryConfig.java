package org.uniprot.api.uniref.service;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

@Configuration
public class UniRefSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/uniref-query.config";

    @Bean
    public SolrQueryConfig uniRefSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public QueryProcessor uniRefQueryProcessor(WhitelistFieldConfig whiteListFieldConfig) {
        Map<String, String> uniRefWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.UNIREF.toString().toLowerCase(), new HashMap<>());
        return new UniProtQueryProcessor(
                getDefaultSearchOptimisedFieldItems(), uniRefWhiteListFields);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        SearchFieldConfig searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIREF);
        return asList(
                searchFieldConfig.getSearchFieldItemByName(UniRefLightSearchService.UNIREF_ID),
                searchFieldConfig.getSearchFieldItemByName(UniRefLightSearchService.UNIREF_UPI));
    }
}
