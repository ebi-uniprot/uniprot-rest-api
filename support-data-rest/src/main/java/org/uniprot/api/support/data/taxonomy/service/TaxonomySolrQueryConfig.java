package org.uniprot.api.support.data.taxonomy.service;

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
public class TaxonomySolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/taxonomy-query.config";

    @Bean
    public SolrQueryConfig taxonomySolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public QueryProcessor taxonomyQueryProcessor(WhitelistFieldConfig whiteListFieldConfig) {
        Map<String, String> taxonomyWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.TAXONOMY.toString().toLowerCase(), new HashMap<>());
        return new UniProtQueryProcessor(
                getDefaultSearchOptimisedFieldItems(), taxonomyWhiteListFields);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        SearchFieldConfig searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.TAXONOMY);
        return Collections.singletonList(
                searchFieldConfig.getSearchFieldItemByName(TaxonomyService.TAXONOMY_ID_FIELD));
    }
}
