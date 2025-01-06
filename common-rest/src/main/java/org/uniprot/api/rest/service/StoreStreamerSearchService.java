package org.uniprot.api.rest.service;

import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetTupleStreamConverter;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.search.document.Document;

public abstract class StoreStreamerSearchService<D extends Document, R>
        extends BasicSearchService<D, R> {
    protected final StoreStreamer<R> storeStreamer;
    protected final TupleStreamDocumentIdStream solrIdStreamer;
    private final FacetTupleStreamTemplate tupleStreamTemplate;
    private final FacetTupleStreamConverter tupleStreamConverter;
    private final SolrQueryConfig solrQueryConfig;
    private final FacetConfig facetConfig;

    public StoreStreamerSearchService(
            SolrQueryRepository<D> repository,
            FacetConfig facetConfig,
            StoreStreamer<R> storeStreamer,
            SolrQueryConfig solrQueryConfig,
            FacetTupleStreamTemplate tupleStreamTemplate,
            TupleStreamDocumentIdStream solrIdStreamer,
            RequestConverter requestConverter) {

        this(
                repository,
                null,
                facetConfig,
                storeStreamer,
                solrQueryConfig,
                tupleStreamTemplate,
                solrIdStreamer,
                requestConverter);
    }

    public StoreStreamerSearchService(
            SolrQueryRepository<D> repository,
            Function<D, R> entryConverter,
            FacetConfig facetConfig,
            StoreStreamer<R> storeStreamer,
            SolrQueryConfig solrQueryConfig,
            FacetTupleStreamTemplate tupleStreamTemplate,
            TupleStreamDocumentIdStream solrIdStreamer,
            RequestConverter requestConverter) {

        super(repository, entryConverter, requestConverter);
        this.storeStreamer = storeStreamer;
        this.solrQueryConfig = solrQueryConfig;
        this.tupleStreamTemplate = tupleStreamTemplate;
        this.tupleStreamConverter = new FacetTupleStreamConverter(getSolrIdField(), facetConfig);
        this.solrIdStreamer = solrIdStreamer;
        this.facetConfig = facetConfig;
    }

    public abstract R findByUniqueId(final String uniqueId, final String filters);

    protected abstract R mapToThinEntry(String entryId);

    @Override
    public Stream<R> stream(StreamRequest request) {
        SolrRequest query = getRequestConverter().createStreamSolrRequest(request);
        if (LIST_MEDIA_TYPE_VALUE.equals(request.getFormat())) {
            return this.solrIdStreamer
                    .fetchIds(query)
                    .map(this::mapToThinEntry)
                    .filter(Objects::nonNull);
        } else {
            StoreRequest storeRequest = getStoreRequest(request);
            return this.storeStreamer.idsToStoreStream(query, storeRequest);
        }
    }

    public Stream<String> streamIdsForDownload(StreamRequest request) {
        SolrRequest solrRequest = getRequestConverter().createStreamSolrRequest(request);
        return this.storeStreamer.idsStream(solrRequest);
    }

    public QueryResult<R> getByIds(IdsSearchRequest idsRequest) {
        boolean hasIsoformIds = hasIsoformIds(idsRequest.getIdList());
        SolrStreamFacetResponse solrStreamResponse =
                solrStreamNeeded(idsRequest, hasIsoformIds)
                        ? searchBySolrStream(idsRequest)
                        : new SolrStreamFacetResponse();

        // use the ids returned by solr stream if query filter is passed or sort field is passed
        // otherwise use the passed ids
        List<String> ids =
                needSolrReturnedAccessions(idsRequest, hasIsoformIds)
                        ? solrStreamResponse.getIds()
                        : idsRequest.getIdList();
        // default page size to number of ids passed
        int pageSize = Objects.isNull(idsRequest.getSize()) ? ids.size() : idsRequest.getSize();

        // compute the cursor and get subset of ids as per cursor
        CursorPage cursorPage = CursorPage.of(idsRequest.getCursor(), pageSize, ids.size());

        List<String> idsInPage =
                ids.subList(
                        cursorPage.getOffset().intValue(), CursorPage.getNextOffset(cursorPage));

        // get n entries from store
        Stream<R> entries = streamEntries(idsInPage, idsRequest);

        // facets may be set when facetList is passed but that should not be returned with cursor
        List<Facet> facets = solrStreamResponse.getFacets();
        if (Objects.nonNull(idsRequest.getCursor())) {
            facets = null; // do not return facet in case of next page and facetFilter
        }

        return QueryResult.<R>builder().content(entries).page(cursorPage).facets(facets).build();
    }

    protected Stream<R> streamEntries(List<String> idsInPage, IdsSearchRequest request) {
        StoreRequest storeRequest = getStoreRequest(request);
        return this.storeStreamer.streamEntries(idsInPage, storeRequest);
    }

    public StoreRequest getStoreRequest(BasicRequest request) {
        return StoreRequest.builder().fields(request.getFields()).build();
    }

    protected abstract UniProtDataType getUniProtDataType();

    protected abstract String getSolrIdField();

    protected String getTermsQueryField() {
        return getSolrIdField();
    }

    @Override
    protected Stream<R> convertDocumentsToEntries(SearchRequest request, QueryResult<D> results) {
        Stream<R> converted;
        if (LIST_MEDIA_TYPE_VALUE.equals(request.getFormat())) {
            converted =
                    results.getContent()
                            .map(Document::getDocumentId)
                            .map(this::mapToThinEntry)
                            .filter(Objects::nonNull);
        } else {
            converted = super.convertDocumentsToEntries(request, results);
        }
        return converted;
    }

    private SolrStreamFacetResponse searchBySolrStream(IdsSearchRequest idsRequest) {
        SolrRequest solrRequest =
                getRequestConverter()
                        .createSearchIdsSolrRequest(
                                idsRequest, idsRequest.getIdList(), getSolrIdField());
        TupleStream tupleStream = this.tupleStreamTemplate.create(solrRequest);
        return this.tupleStreamConverter.convert(tupleStream, idsRequest.getFacetList());
    }

    private boolean solrStreamNeeded(IdsSearchRequest idsRequest, boolean hasIsoformIds) {
        return (Utils.nullOrEmpty(idsRequest.getCursor())
                        && Utils.notNullNotEmpty(idsRequest.getFacetList())
                        && !idsRequest.isDownload())
                || Utils.notNullNotEmpty(idsRequest.getQuery())
                || Utils.notNullNotEmpty(idsRequest.getSort())
                || hasIsoformIds;
    }

    private boolean needSolrReturnedAccessions(IdsSearchRequest idsRequest, boolean hasIsoformIds) {
        return Utils.notNullNotEmpty(idsRequest.getQuery())
                || Utils.notNullNotEmpty(idsRequest.getSort())
                || hasIsoformIds;
    }

    protected boolean hasIsoformIds(List<String> ids) {
        return false;
    }
}
