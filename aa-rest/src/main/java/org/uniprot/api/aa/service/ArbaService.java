package org.uniprot.api.aa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uniprot.api.aa.repository.ArbaFacetConfig;
import org.uniprot.api.aa.repository.ArbaQueryRepository;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.arba.ArbaDocument;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@Service
public class ArbaService extends BasicSearchService<ArbaDocument, UniRuleEntry> {

    public static final String ARBA_ID_FIELD = "rule_id";
    private final SearchFieldConfig searchFieldConfig;
    private final QueryProcessor queryProcessor;

    @Autowired
    public ArbaService(
            ArbaQueryRepository repository,
            ArbaFacetConfig facetConfig,
            ArbaDocumentConverter arbaDocumentConverter,
            ArbaSortClause solrSortClause,
            SolrQueryConfig arbaSolrQueryConf,
            QueryProcessor arbaQueryProcessor,
            SearchFieldConfig arbaSearchFieldConfig) {
        super(repository, arbaDocumentConverter, solrSortClause, arbaSolrQueryConf, facetConfig);
        this.searchFieldConfig = arbaSearchFieldConfig;
        this.queryProcessor = arbaQueryProcessor;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(ARBA_ID_FIELD);
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return this.queryProcessor;
    }
}
