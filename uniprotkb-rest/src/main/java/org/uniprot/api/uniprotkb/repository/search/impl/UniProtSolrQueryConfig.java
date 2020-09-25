package org.uniprot.api.uniprotkb.repository.search.impl;

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
import org.uniprot.api.uniprotkb.service.UniProtEntryService;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;

/**
 * Created 05/09/19
 *
 * @author Edd
 */
@Configuration
public class UniProtSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/uniprotkb-query.config";

    @Bean
    public SolrQueryConfig uniProtKBSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public QueryProcessor uniProtKBQueryProcessor(WhitelistFieldConfig whiteListFieldConfig) {
        Map<String, String> uniprotWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.UNIPROTKB.toString().toLowerCase(),
                                new HashMap<>());
        return new UniProtQueryProcessor(
                getDefaultSearchOptimisedFieldItems(), uniprotWhiteListFields);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        SearchFieldConfig searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);
        return Collections.singletonList(
                searchFieldConfig.getSearchFieldItemByName(UniProtEntryService.ACCESSION));
    }
}
