package org.uniprot.api.aa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.aa.repository.UniRuleFacetConfig;
import org.uniprot.api.aa.repository.UniRuleQueryRepository;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.unirule.UniRuleDocument;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@Service
public class UniRuleService extends BasicSearchService<UniRuleDocument, UniRuleEntry> {

    public static final String ALL_RULE_ID_FIELD = "all_rule_id";
    private final UniProtQueryProcessorConfig uniRuleQueryProcessorConfig;
    private final SearchFieldConfig searchFieldConfig;

    @Autowired
    public UniRuleService(
            UniRuleQueryRepository repository,
            UniRuleFacetConfig facetConfig,
            UniRuleEntryConverter uniRuleEntryConverter,
            UniRuleSortClause solrSortClause,
            SolrQueryConfig uniRuleSolrQueryConf,
            UniProtQueryProcessorConfig uniRuleQueryProcessorConfig,
            SearchFieldConfig uniRuleSearchFieldConfig) {
        super(repository, uniRuleEntryConverter, solrSortClause, uniRuleSolrQueryConf, facetConfig);
        this.uniRuleQueryProcessorConfig = uniRuleQueryProcessorConfig;
        this.searchFieldConfig = uniRuleSearchFieldConfig;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(ALL_RULE_ID_FIELD);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return uniRuleQueryProcessorConfig;
    }
}
