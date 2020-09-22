package org.uniprot.api.rest.service;

import java.util.function.Function;
import java.util.stream.Stream;

import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.document.Document;

public abstract class StoreStreamerSearchService<D extends Document, R>
        extends BasicSearchService<D, R> {
    private final StoreStreamer<R> storeStreamer;

    public StoreStreamerSearchService(
            SolrQueryRepository<D> repository,
            FacetConfig facetConfig,
            AbstractSolrSortClause solrSortClause,
            StoreStreamer<R> storeStreamer,
            SolrQueryConfig solrQueryConfig) {

        this(repository, null, solrSortClause, facetConfig, storeStreamer, solrQueryConfig);
    }

    public StoreStreamerSearchService(
            SolrQueryRepository<D> repository,
            Function<D, R> entryConverter,
            AbstractSolrSortClause solrSortClause,
            FacetConfig facetConfig,
            StoreStreamer<R> storeStreamer,
            SolrQueryConfig solrQueryConfig) {

        super(repository, entryConverter, solrSortClause, solrQueryConfig, facetConfig);
        this.storeStreamer = storeStreamer;
    }

    public abstract R findByUniqueId(final String uniqueId, final String filters);

    public Stream<R> stream(StreamRequest request) {
        SolrRequest query = createDownloadSolrRequest(request);
        return this.storeStreamer.idsToStoreStream(query);
    }

    public Stream<String> streamIds(StreamRequest request) {
        SolrRequest solrRequest = createDownloadSolrRequest(request);
        return this.storeStreamer.idsStream(solrRequest);
    }

    protected SolrRequest createDownloadSolrRequest(StreamRequest request) {
        return createSolrRequestBuilder(request, solrSortClause, queryBoosts).build();
    }
}
