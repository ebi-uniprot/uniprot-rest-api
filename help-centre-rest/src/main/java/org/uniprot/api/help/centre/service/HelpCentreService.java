package org.uniprot.api.help.centre.service;

import java.util.function.Function;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.help.centre.model.HelpCentreEntry;
import org.uniprot.api.help.centre.repository.HelpCentreFacetConfig;
import org.uniprot.api.help.centre.request.HelpCentreSearchRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.SolrQueryUtil;
import org.uniprot.store.search.document.help.HelpDocument;

/**
 * @author lgonzales
 * @since 07/07/2021
 */
@Service
@Import(HelpCentreSolrQueryConfig.class)
public class HelpCentreService extends BasicSearchService<HelpDocument, HelpCentreEntry> {

    static final String HELP_CENTRE_ID_FIELD =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.HELP)
                    .getSearchFieldItemByName("id")
                    .getFieldName();
    static final String HELP_CENTRE_TITLE_FIELD =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.HELP)
                    .getSearchFieldItemByName("title")
                    .getFieldName();
    static final String HELP_CENTRE_CONTENT_FIELD =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.HELP)
                    .getSearchFieldItemByName("content")
                    .getFieldName();
    static final String HELP_CENTRE_CATEGORY_FIELD =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.HELP)
                    .getSearchFieldItemByName("category")
                    .getFieldName();
    private final SearchFieldConfig searchFieldConfig;
    private final UniProtQueryProcessorConfig helpCentreQueryProcessorConfig;

    public HelpCentreService(
            SolrQueryRepository<HelpDocument> repository,
            Function<HelpDocument, HelpCentreEntry> entryConverter,
            AbstractSolrSortClause solrSortClause,
            SolrQueryConfig helpCentreSolrQueryConf,
            HelpCentreFacetConfig facetConfig,
            SearchFieldConfig helpCentreSearchFieldConfig,
            UniProtQueryProcessorConfig helpCentreQueryProcessorConfig) {
        super(repository, entryConverter, solrSortClause, helpCentreSolrQueryConf, facetConfig);
        this.searchFieldConfig = helpCentreSearchFieldConfig;
        this.helpCentreQueryProcessorConfig = helpCentreQueryProcessorConfig;
    }

    @Override
    public QueryResult<HelpCentreEntry> search(SearchRequest request) {
        HelpCentreSearchRequest searchRequest = (HelpCentreSearchRequest) request;
        String query = searchRequest.getQuery();
        if (simpleSearch(query)) {
            // default simple search (query OR "query") will boost exact search
            searchRequest.setQuery(query + " OR \"" + query + "\"");
        }
        QueryResult<HelpCentreEntry> result = super.search(searchRequest);
        if (simpleSearch(query) && Utils.notNullNotEmpty(result.getSuggestions())) {
            searchRequest.setQuery(query);
            QueryResult<HelpCentreEntry> suggester = super.search(request);
            result =
                    QueryResult.of(
                            result.getContent(),
                            result.getPage(),
                            result.getFacets(),
                            null,
                            null,
                            suggester.getSuggestions());
        }
        return result;
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(HELP_CENTRE_ID_FIELD);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return helpCentreQueryProcessorConfig;
    }

    private boolean simpleSearch(String query) {
        return !query.contains("\"")
                && !SolrQueryUtil.hasFieldTerms(query, HELP_CENTRE_TITLE_FIELD)
                && !SolrQueryUtil.hasFieldTerms(query, HELP_CENTRE_CATEGORY_FIELD)
                && !SolrQueryUtil.hasFieldTerms(query, HELP_CENTRE_CONTENT_FIELD);
    }
}
