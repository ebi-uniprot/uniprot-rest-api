package org.uniprot.api.unirule.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.unirule.repository.UniRuleFacetConfig;
import org.uniprot.api.unirule.repository.UniRuleQueryRepository;
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

    public static final String UNIRULE_ID_FIELD = "unirule_id";
    private final SearchFieldConfig searchFieldConfig;
    private final QueryProcessor queryProcessor;

    @Autowired
    public UniRuleService(
            UniRuleQueryRepository repository,
            UniRuleFacetConfig facetConfig,
            UniRuleEntryConverter uniRuleEntryConverter,
            UniRuleSortClause solrSortClause,
            SolrQueryConfig solrQueryConfig,
            QueryProcessor queryProcessor,
            SearchFieldConfig searchFieldConfig) {
        super(repository, uniRuleEntryConverter, solrSortClause, solrQueryConfig, facetConfig);
        this.searchFieldConfig = searchFieldConfig;
        this.queryProcessor = queryProcessor;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(UNIRULE_ID_FIELD);
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return this.queryProcessor;
    }
}
