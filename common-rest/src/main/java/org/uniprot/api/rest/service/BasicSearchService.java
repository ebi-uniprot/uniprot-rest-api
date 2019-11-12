package org.uniprot.api.rest.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.solr.core.query.Query;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.Document;

/**
 * @param <D> the type of the input to the class. a type of Document
 * @param <R> the type of the result of the class
 * @author lgonzales
 */
@PropertySource("classpath:common-message.properties")
public abstract class BasicSearchService<D extends Document, R> {
    public static final Integer DEFAULT_SOLR_BATCH_SIZE = 100;
    private final SolrQueryRepository<D> repository;
    private final Function<D, R> entryConverter;
    private AbstractSolrSortClause solrSortClause;
    private DefaultSearchHandler defaultSearchHandler;
    private FacetConfig facetConfig;

    @Value("${solr.query.batchSize:#{null}}") // if this property is not set then
    private Optional<Integer>
            solrBatchSize; // it is set to empty and later it is set to DEFAULT_SOLR_BATCH_SIZE

    public BasicSearchService(SolrQueryRepository<D> repository, Function<D, R> entryConverter) {
        this(repository, entryConverter, null, null, null);
    }

    public BasicSearchService(
            SolrQueryRepository<D> repository,
            Function<D, R> entryConverter,
            AbstractSolrSortClause solrSortClause,
            DefaultSearchHandler defaultSearchHandler,
            FacetConfig facetConfig) {
        this.repository = repository;
        this.entryConverter = entryConverter;
        this.solrSortClause = solrSortClause;
        this.defaultSearchHandler = defaultSearchHandler;
        this.facetConfig = facetConfig;
    }

    public R findByUniqueId(final String uniqueId) {
        return getEntity(getIdField(), uniqueId);
    }

    protected abstract String getIdField();

    public R getEntity(String idField, String value) {
        try {
            String query = idField + ":" + value;
            SolrRequest solrRequest =
                    SolrRequest.builder().query(query).rows(NumberUtils.INTEGER_ONE).build();
            D document =
                    repository
                            .getEntry(solrRequest)
                            .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));

            R entry = entryConverter.apply(document);
            if (entry == null) {
                String message =
                        entryConverter.getClass() + " can not convert object for: [" + value + "]";
                throw new ServiceException(message);
            } else {
                return entry;
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get entity for id: [" + value + "]";
            throw new ServiceException(message, e);
        }
    }

    public QueryResult<R> search(SearchRequest request) {
        SolrRequest solrRequest = createSearchSolrRequest(request);

        QueryResult<D> results = repository.searchPage(solrRequest, request.getCursor());
        List<R> converted =
                results.getContent().stream()
                        .map(entryConverter)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
        return QueryResult.of(converted, results.getPage(), results.getFacets());
    }

    public Stream<R> download(SearchRequest request) {
        setSizeForDownloadAllIfNeeded(request);

        SolrRequest solrRequest = createSolrRequest(request);

        return repository
                .getAll(solrRequest)
                .map(entryConverter)
                .filter(Objects::nonNull)
                .limit(solrRequest.getTotalRows());
    }

    /*
    to create request for search api.
    */
    protected SolrRequest createSearchSolrRequest(SearchRequest request) {
        return createSearchSolrRequest(request, true);
    }

    /*
     // case 1. size is not passed, use  DEFAULT_RESULTS_SIZE(25) then set rows and totalCount as DEFAULT_RESULTS_SIZE
    // case 2. size is less than solrBatchSize(10,000 or 100) then set SolrRequest's rows, totalCount value to size
    // case 3. size is greater than solrBatchSize(10,000 or 100) then then set SolrRequest's rows, totalCount value to solrBatchSize
    // for the search rows, size and total rows should be same. We should restrict size value less than solrBatchSize
     */

    protected SolrRequest createSearchSolrRequest(SearchRequest request, boolean includeFacets) {
        if (request.getSize() == null) { // set the default result size
            request.setSize(SearchRequest.DEFAULT_RESULTS_SIZE);
        } else if (request.getSize() > this.solrBatchSize.orElse(DEFAULT_SOLR_BATCH_SIZE)) {
            request.setSize(this.solrBatchSize.orElse(DEFAULT_SOLR_BATCH_SIZE));
        }

        return createSolrRequest(request, includeFacets);
    }

    protected SolrRequest createSolrRequest(SearchRequest request) {
        return createSolrRequest(request, true);
    }

    protected SolrRequest createSolrRequest(SearchRequest request, boolean includeFacets) {

        SolrRequest.SolrRequestBuilder builder =
                createSolrRequestBuilder(
                        request,
                        this.facetConfig,
                        this.solrSortClause,
                        this.defaultSearchHandler,
                        includeFacets);

        return builder.build();
    }

    protected void setSizeForDownloadAllIfNeeded(SearchRequest request) {
        if (request.getSize() == null) { // set -1 to download all if not passed
            request.setSize(NumberUtils.INTEGER_MINUS_ONE);
        }
    }

    private SolrRequest.SolrRequestBuilder createSolrRequestBuilder(
            SearchRequest request,
            FacetConfig facetConfig,
            AbstractSolrSortClause solrSortClause,
            DefaultSearchHandler defaultSearchHandler,
            boolean includeFacets) {

        SolrRequest.SolrRequestBuilder requestBuilder = SolrRequest.builder();

        String requestedQuery = request.getQuery();

        boolean hasScore = false;
        if (defaultSearchHandler != null && defaultSearchHandler.hasDefaultSearch(requestedQuery)) {
            requestedQuery = defaultSearchHandler.optimiseDefaultSearch(requestedQuery);
            hasScore = true;
            requestBuilder.defaultQueryOperator(Query.Operator.OR);
        }
        requestBuilder.query(requestedQuery);

        if (solrSortClause != null) {
            requestBuilder.addSort(solrSortClause.getSort(request.getSort(), hasScore));
        }

        if (includeFacets && request.hasFacets()) {
            requestBuilder.facets(request.getFacetList());
            requestBuilder.facetConfig(facetConfig);
        }

        // set the solr batch size if passed else use DEFAULT_SOLR_BATCH_SIZE(25)
        requestBuilder.rows(this.solrBatchSize.orElse(DEFAULT_SOLR_BATCH_SIZE));

        // if the requested size is less than batch size, set the batch size as requested size
        Integer requestedSize = request.getSize();
        if (isSizeLessOrEqualToSolrBatchSize(requestedSize)) {
            requestBuilder.rows(requestedSize); // add the batch size for the solr query
        }

        if (requestedSize
                == NumberUtils
                        .INTEGER_MINUS_ONE) { // special case for download, -1 to get everything
            requestBuilder.totalRows(Integer.MAX_VALUE);
        } else { // total number of rows requested by the client
            requestBuilder.totalRows(requestedSize);
        }

        return requestBuilder;
    }

    private boolean isSizeLessOrEqualToSolrBatchSize(Integer requestedSize) {
        return requestedSize > NumberUtils.INTEGER_ZERO
                && requestedSize <= this.solrBatchSize.orElse(DEFAULT_SOLR_BATCH_SIZE);
    }
}
