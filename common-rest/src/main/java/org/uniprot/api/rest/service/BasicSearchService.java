package org.uniprot.api.rest.service;

import static org.uniprot.api.rest.output.PredefinedAPIStatus.LEADING_WILDCARD_IGNORED;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.*;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.SolrQueryUtil;
import org.uniprot.store.search.document.Document;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

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
    protected final FacetConfig facetConfig;
    private static final Pattern CLEAN_QUERY_REGEX =
            Pattern.compile(FieldRegexConstants.CLEAN_QUERY_REGEX);

    // If this property is not set then it is set to empty and later it is set to
    // DEFAULT_SOLR_BATCH_SIZE
    @Value("${solr.query.batchSize:#{null}}")
    private Integer solrBatchSize;

    @Value("${search.default.page.size:#{null}}")
    private Integer defaultPageSize;

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
        Set<ProblemPair> warnings = getWarnings(request.getQuery(), Set.of());
        return QueryResult.of(
                converted,
                results.getPage(),
                results.getFacets(),
                null,
                null,
                results.getSuggestions(),
                warnings);
    }

    public Stream<R> stream(StreamRequest request) {
        SolrRequest solrRequest =
                createSolrRequestBuilder(request, this.solrSortClause, this.queryBoosts)
                        .rows(getDefaultBatchSize())
                        .totalRows(Integer.MAX_VALUE)
                        .build();

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

    protected abstract UniProtQueryProcessorConfig getQueryProcessorConfig();

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

        String query =
                UniProtQueryProcessor.newInstance(getQueryProcessorConfig())
                        .processQuery(requestedQuery);
        requestBuilder.query(query);

        if (solrSortClause != null) {
            requestBuilder.sorts(solrSortClause.getSort(request.getSort()));
        }

        requestBuilder.queryField(getQueryFields(query));
        requestBuilder.queryConfig(queryBoosts);

        return requestBuilder;
    }

    private String getQueryFields(String query) {
        String queryFields = "";
        Optional<String> optimisedQueryField = validateOptimisableField(query);
        if (optimisedQueryField.isPresent()) {
            queryFields = optimisedQueryField.get();
            if (queryBoosts.getExtraOptmisableQueryFields() != null) {
                queryFields = queryFields + " " + queryBoosts.getExtraOptmisableQueryFields();
            }
        } else {
            queryFields = queryBoosts.getQueryFields();
        }
        return queryFields;
    }

    private boolean isSizeLessOrEqualToSolrBatchSize(Integer requestedSize) {
        return requestedSize > NumberUtils.INTEGER_ZERO && requestedSize <= getDefaultBatchSize();
    }

    private Integer getDefaultBatchSize() {
        return this.solrBatchSize == null ? DEFAULT_SOLR_BATCH_SIZE : this.solrBatchSize;
    }

    public Stream<String> streamRdf(StreamRequest streamRequest, String dataType, String format) {
        SolrRequest solrRequest =
                createSolrRequestBuilder(streamRequest, solrSortClause, queryBoosts)
                        .rows(getDefaultBatchSize())
                        .totalRows(Integer.MAX_VALUE)
                        .build();
        List<String> idStream =
                getDocumentIdStream().fetchIds(solrRequest).collect(Collectors.toList());
        return getRdfStreamer().stream(idStream.stream(), dataType, format);
    }

    protected DefaultDocumentIdStream<D> getDocumentIdStream() {
        throw new UnsupportedOperationException("Override this method");
    }

    public String getRdf(String id, String dataType, String format) {
        return getRdfStreamer().stream(Stream.of(id), dataType, format)
                .collect(Collectors.joining());
    }

    protected RdfStreamer getRdfStreamer() {
        throw new UnsupportedOperationException("Override this method");
    }

    protected Integer getDefaultPageSize() {
        return this.defaultPageSize;
    }

    protected Set<ProblemPair> getWarnings(String query, Set<String> leadWildcardSupportedFields) {
        ProblemPair warning = getLeadingWildcardIgnoredWarning(query, leadWildcardSupportedFields);
        Set<ProblemPair> warnings = Objects.isNull(warning) ? null : Set.of(warning);
        return warnings;
    }

    private ProblemPair getLeadingWildcardIgnoredWarning(
            String query, Set<String> leadWildcardSupportedFields) {
        if (SolrQueryUtil.ignoreLeadingWildcard(query, leadWildcardSupportedFields)) {
            return new ProblemPair(
                    LEADING_WILDCARD_IGNORED.getCode(), LEADING_WILDCARD_IGNORED.getMessage());
        }
        return null;
    }

    private Optional<String> validateOptimisableField(String query) {
        String cleanQuery = CLEAN_QUERY_REGEX.matcher(query.strip()).replaceAll("");
        return getQueryProcessorConfig().getOptimisableFields().stream()
                .filter(
                        f ->
                                Utils.notNullNotEmpty(f.getValidRegex())
                                        && cleanQuery.matches(f.getValidRegex()))
                .map(SearchFieldItem::getFieldName)
                .findFirst();
    }
}
