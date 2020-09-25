package org.uniprot.api.support.data.literature.service;

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
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
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
    public QueryProcessor literatureQueryProcessor(WhitelistFieldConfig whiteListFieldConfig) {
        Map<String, String> literatureWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.LITERATURE.toString().toLowerCase(),
                                new HashMap<>());
        return new UniProtQueryProcessor(
                getDefaultSearchOptimisedFieldItems(), literatureWhiteListFields);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        SearchFieldConfig searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.LITERATURE);
        return Collections.singletonList(
                searchFieldConfig.getSearchFieldItemByName(LiteratureService.LITERATURE_ID_FIELD));
    }
}
