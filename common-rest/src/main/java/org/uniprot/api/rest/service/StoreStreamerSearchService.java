package org.uniprot.api.rest.service;

import java.util.function.Function;
import java.util.stream.Stream;

import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.Document;

public abstract class StoreStreamerSearchService<T, R extends Document>
        extends BasicSearchService<T, R> {
    private final StoreStreamer<R, T> storeStreamer;

    public StoreStreamerSearchService(
            SolrQueryRepository<R> repository,
            FacetConfig facetConfig,
            AbstractSolrSortClause solrSortClause,
            StoreStreamer<R, T> storeStreamer) {

        this(repository, null, solrSortClause, null, facetConfig, storeStreamer);
    }

    public StoreStreamerSearchService(
            SolrQueryRepository<R> repository,
            Function<R, T> entryConverter,
            AbstractSolrSortClause solrSortClause,
            DefaultSearchHandler defaultSearchHandler,
            FacetConfig facetConfig,
            StoreStreamer<R, T> storeStreamer) {
        super(repository, entryConverter, solrSortClause, defaultSearchHandler, facetConfig);
        this.storeStreamer = storeStreamer;
    }

    public abstract T findByUniqueId(final String uniqueId, final String filters);

    public Stream<T> stream(SearchRequest request) {
        SolrRequest query = createSolrRequest(request);
        return this.storeStreamer.idsToStoreStream(query);
    }

    public Stream<String> streamIds(SearchRequest request) {
        SolrRequest solrRequest = createSolrRequest(request);
        return this.storeStreamer.idsStream(solrRequest);
    }

    public Stream<String> streamRDF(SearchRequest searchRequest) {
        SolrRequest solrRequest = createSolrRequest(searchRequest);
        return this.storeStreamer.idsToRDFStoreStream(solrRequest);
    }
}
