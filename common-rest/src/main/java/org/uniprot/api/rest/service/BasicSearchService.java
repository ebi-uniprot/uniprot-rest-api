package org.uniprot.api.rest.service;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
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
    protected final AbstractSolrSortClause solrSortClause;
    protected final SolrQueryConfig queryBoosts;
    private final FacetConfig facetConfig;

    // If this property is not set then it is set to empty and later it is set to
    // DEFAULT_SOLR_BATCH_SIZE
    @Value("${solr.query.batchSize:#{null}}")
    private Integer solrBatchSize;

    @Value("${search.default.page.size:#{null}}")
    private Integer defaultPageSize;

    public BasicSearchService(SolrQueryRepository<D> repository, Function<D, R> entryConverter) {
        this(repository, entryConverter, null, null, null);
    }

    public BasicSearchService(
            SolrQueryRepository<D> repository,
            Function<D, R> entryConverter,
            AbstractSolrSortClause solrSortClause,
            SolrQueryConfig queryBoosts,
            FacetConfig facetConfig) {
        this.repository = repository;
        this.entryConverter = entryConverter;
        this.solrSortClause = solrSortClause;
        this.queryBoosts = queryBoosts;
        this.facetConfig = facetConfig;
    }

    public R findByUniqueId(final String uniqueId) {
        return getEntity(getIdField().getFieldName(), uniqueId);
    }

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
        Stream<R> converted = results.getContent().map(entryConverter).filter(Objects::nonNull);
        return QueryResult.of(converted, results.getPage(), results.getFacets());
    }

    public Stream<R> download(SearchRequest request) {
        SolrRequest solrRequest = createDownloadSolrRequest(request);

        return repository
                .getAll(solrRequest)
                .map(entryConverter)
                .filter(Objects::nonNull)
                .limit(solrRequest.getTotalRows());
    }

    /*
    to create request for search api.
    include facets true for search api
    */
    public SolrRequest createSearchSolrRequest(SearchRequest request) {
        if (request.getSize() == null) { // set the default result size
            request.setSize(defaultPageSize);
        }
        SolrRequest.SolrRequestBuilder builder =
                createSolrRequestBuilder(request, this.solrSortClause, this.queryBoosts);

        if (request.hasFacets()) {
            builder.facets(request.getFacetList());
            builder.facetConfig(facetConfig);
        }
        builder.rows(request.getSize());
        builder.totalRows(request.getSize());
        return builder.build();
    }

    protected abstract SearchFieldItem getIdField();

    protected abstract QueryProcessor getQueryProcessor();

    /*
       case 1. size is not passed, use  DEFAULT_RESULTS_SIZE(25) then set rows and totalRows as DEFAULT_RESULTS_SIZE
       case 2. size is less than or equal solrBatchSize(10,000 or 100) then set SolrRequest's rows, totalRows value to size
       case 3. size is greater than solrBatchSize(10,000 or 100) then then set SolrRequest's rows, totalRows value to solrBatchSize
       For the search rows, size and totalRows should be same. We should restrict size value less than solrBatchSize.
    */

    public SolrRequest createDownloadSolrRequest(SearchRequest request) {
        if (request.getSize() == null) { // set -1 to download all if not passed
            request.setSize(NumberUtils.INTEGER_MINUS_ONE);
        }

        SolrRequest.SolrRequestBuilder builder =
                createSolrRequestBuilder(request, this.solrSortClause, this.queryBoosts);

        // If the requested size is less than batch size, set the batch size as requested size
        Integer requestedSize = request.getSize();
        if (isSizeLessOrEqualToSolrBatchSize(requestedSize)) {
            builder.rows(requestedSize); // add the batch size for the solr query
        } else { // Else set the solr batch size if passed else use DEFAULT_SOLR_BATCH_SIZE(25)
            builder.rows(getDefaultBatchSize());
        }

        if (requestedSize.equals(
                NumberUtils.INTEGER_MINUS_ONE)) { // special case for download, -1 to get everything
            builder.totalRows(Integer.MAX_VALUE);
        } else { // total number of rows requested by the client
            builder.totalRows(requestedSize);
        }

        return builder.build();
    }

    protected SolrRequest.SolrRequestBuilder createSolrRequestBuilder(
            BasicRequest request,
            AbstractSolrSortClause solrSortClause,
            SolrQueryConfig queryBoosts) {

        SolrRequest.SolrRequestBuilder requestBuilder = SolrRequest.builder();

        String requestedQuery = request.getQuery();

        requestBuilder.query(getQueryProcessor().processQuery(requestedQuery));

        if (solrSortClause != null) {
            requestBuilder.sorts(solrSortClause.getSort(request.getSort()));
        }

        requestBuilder.queryConfig(queryBoosts);

        return requestBuilder;
    }

    private boolean isSizeLessOrEqualToSolrBatchSize(Integer requestedSize) {
        return requestedSize > NumberUtils.INTEGER_ZERO && requestedSize <= getDefaultBatchSize();
    }

    private Integer getDefaultBatchSize() {
        return this.solrBatchSize == null ? DEFAULT_SOLR_BATCH_SIZE : this.solrBatchSize;
    }
}
