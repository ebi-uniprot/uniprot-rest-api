package org.uniprot.api.rest.service;

import java.util.function.Function;
import java.util.stream.Stream;

import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.Document;

public abstract class StoreStreamerSearchService<D extends Document, R>
        extends BasicSearchService<D, R> {
    private final StoreStreamer<D, R> storeStreamer;

    public StoreStreamerSearchService(
            SolrQueryRepository<D> repository,
            FacetConfig facetConfig,
            AbstractSolrSortClause solrSortClause,
            StoreStreamer<D, R> storeStreamer,
            QueryBoosts queryBoosts) {

        this(repository, null, solrSortClause, null, facetConfig, storeStreamer, queryBoosts);
    }

    public StoreStreamerSearchService(
            SolrQueryRepository<D> repository,
            Function<D, R> entryConverter,
            AbstractSolrSortClause solrSortClause,
            DefaultSearchHandler defaultSearchHandler,
            FacetConfig facetConfig,
            StoreStreamer<D, R> storeStreamer,
            QueryBoosts queryBoosts) {

        super(
                repository,
                entryConverter,
                solrSortClause,
                defaultSearchHandler,
                queryBoosts,
                facetConfig);
        this.storeStreamer = storeStreamer;
    }

    public abstract R findByUniqueId(final String uniqueId, final String filters);

    public Stream<R> stream(SearchRequest request) {
        SolrRequest query = createDownloadSolrRequest(request);
        return this.storeStreamer.idsToStoreStream(query);
    }

    public Stream<String> streamIds(SearchRequest request) {
        SolrRequest solrRequest = createDownloadSolrRequest(request);
        return this.storeStreamer.idsStream(solrRequest);
    }
}
