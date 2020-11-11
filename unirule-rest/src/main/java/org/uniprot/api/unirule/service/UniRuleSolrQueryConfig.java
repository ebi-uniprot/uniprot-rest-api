package org.uniprot.api.unirule.service;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * @author sahmad
 * @created 11/11/2020
 */
@Configuration
public class UniRuleSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/unirule-query.config";

    @Bean
    public SolrQueryConfig uniRuleSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig uniRuleSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIRULE);
    }

    @Bean
    public QueryProcessor uniRuleQueryProcessor(
            WhitelistFieldConfig whiteListFieldConfig, SearchFieldConfig uniRuleSearchFieldConfig) {
        Map<String, String> uniParcWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.UNIRULE.toString().toLowerCase(), new HashMap<>());
        return new UniProtQueryProcessor(
                getDefaultSearchOptimisedFieldItems(uniRuleSearchFieldConfig),
                uniParcWhiteListFields);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems(
            SearchFieldConfig uniRuleSearchFieldConfig) {
        return Collections.singletonList(
                uniRuleSearchFieldConfig.getSearchFieldItemByName(
                        UniRuleService.UNIRULE_ID_FIELD));
    }
}
