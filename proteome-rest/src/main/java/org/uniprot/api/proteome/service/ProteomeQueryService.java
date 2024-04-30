package org.uniprot.api.proteome.service;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.PROTEOME_ID_REGEX;

import java.util.regex.Pattern;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.proteome.repository.ProteomeFacetConfig;
import org.uniprot.api.proteome.repository.ProteomeQueryRepository;
import org.uniprot.api.proteome.request.ProteomeSearchRequest;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.request.SearchRequest;
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
    private final UniProtQueryProcessorConfig proteomeQueryProcessorConfig;
    private final SearchFieldConfig fieldConfig;

    private static final Pattern PROTEOME_ID_REGEX_PATTERN = Pattern.compile(PROTEOME_ID_REGEX);

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
        return super.createSolrRequestBuilder(request, solrSortClause, queryBoosts);
    }

    @Override
    protected SearchFieldItem getIdField() {
        return fieldConfig.getSearchFieldItemByName(PROTEOME_ID_FIELD);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return proteomeQueryProcessorConfig;
    }

    @Override
    public SolrRequest createSearchSolrRequest(SearchRequest request) {
        ProteomeSearchRequest searchRequest = (ProteomeSearchRequest) request;
        String cleanQuery = CLEAN_QUERY_REGEX.matcher(request.getQuery().strip()).replaceAll("");
        if (PROTEOME_ID_REGEX_PATTERN.matcher(cleanQuery.toUpperCase()).matches()) {
            searchRequest.setQuery(cleanQuery.toUpperCase());
        }
        return super.createSearchSolrRequest(searchRequest);
    }
}
