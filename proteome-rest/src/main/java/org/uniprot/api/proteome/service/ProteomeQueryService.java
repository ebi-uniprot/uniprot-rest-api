package org.uniprot.api.proteome.service;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.proteome.repository.ProteomeFacetConfig;
import org.uniprot.api.proteome.repository.ProteomeQueryRepository;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

/**
 * @author jluo
 * @date: 26 Apr 2019
 */
@Service
@Import(ProteomeSolrQueryConfig.class)
public class ProteomeQueryService extends BasicSearchService<ProteomeDocument, ProteomeEntry> {
    public static final String PROTEOME_ID_FIELD = "upid";
    public static final String EXCLUDED_FIELD = "excluded";
    private final UniProtQueryProcessorConfig proteomeQueryProcessorConfig;
    private final SearchFieldConfig fieldConfig;

    public ProteomeQueryService(
            ProteomeQueryRepository repository,
            ProteomeFacetConfig facetConfig,
            ProteomeSortClause solrSortClause,
            SolrQueryConfig proteomeSolrQueryConf,
            UniProtQueryProcessorConfig proteomeQueryProcessorConfig,
            SearchFieldConfig proteomeSearchFieldConfig) {
        super(
                repository,
                new ProteomeEntryConverter(),
                solrSortClause,
                proteomeSolrQueryConf,
                facetConfig);
        this.proteomeQueryProcessorConfig = proteomeQueryProcessorConfig;
        fieldConfig = proteomeSearchFieldConfig;
    }

    @Override
    protected SolrRequest.SolrRequestBuilder createSolrRequestBuilder(
            BasicRequest request,
            AbstractSolrSortClause solrSortClause,
            SolrQueryConfig queryBoosts) {
        SolrRequest.SolrRequestBuilder builder =
                super.createSolrRequestBuilder(request, solrSortClause, queryBoosts);
        builder.filterQuery(getActiveProteomeFilterQuery());
        return builder;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return fieldConfig.getSearchFieldItemByName(PROTEOME_ID_FIELD);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return proteomeQueryProcessorConfig;
    }

    private String getActiveProteomeFilterQuery() {
        return EXCLUDED_FIELD + ":false";
    }
}
