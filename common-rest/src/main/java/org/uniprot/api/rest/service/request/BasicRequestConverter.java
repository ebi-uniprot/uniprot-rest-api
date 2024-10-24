package org.uniprot.api.rest.service.request;

import java.util.Optional;
import java.util.regex.Pattern;

import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

public class BasicRequestConverter {
    private static final Integer DEFAULT_SOLR_BATCH_SIZE = 100;
    private static final Integer DEFAULT_PAGE_SIZE = 25;
    private static final Pattern CLEAN_QUERY_REGEX =
            Pattern.compile(FieldRegexConstants.CLEAN_QUERY_REGEX);
    private final SolrQueryConfig queryConfig;
    private final AbstractSolrSortClause solrSortClause;
    private final UniProtQueryProcessorConfig queryProcessorConfig;
    private final RequestConverterConfigProperties requestConverterConfigProperties;
    private final Pattern idRequestRegex;

    public BasicRequestConverter(
            SolrQueryConfig queryConfig,
            AbstractSolrSortClause solrSortClause,
            UniProtQueryProcessorConfig queryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties,
            Pattern idPattern) {
        this.queryConfig = queryConfig;
        this.solrSortClause = solrSortClause;
        this.queryProcessorConfig = queryProcessorConfig;
        this.requestConverterConfigProperties = requestConverterConfigProperties;
        this.idRequestRegex = idPattern;
    }

    public SolrRequest.SolrRequestBuilder createSearchSolrRequest(SearchRequest request) {
        SolrRequest.SolrRequestBuilder requestBuilder = createBasicSolrRequestBuilder(request);

        if (request.getSize() == null) { // set the default result size
            request.setSize(getDefaultPageSize());
        }

        if (request.hasFacets()) {
            requestBuilder.facets(request.getFacetList());
        }
        requestBuilder.rows(request.getSize());
        requestBuilder.totalRows(request.getSize());

        return requestBuilder;
    }

    public SolrRequest.SolrRequestBuilder createStreamSolrRequest(StreamRequest request) {
        SolrRequest.SolrRequestBuilder requestBuilder = createBasicSolrRequestBuilder(request);
        requestBuilder.rows(getDefaultBatchSize());
        requestBuilder.totalRows(Integer.MAX_VALUE);
        return requestBuilder;
    }

    public String getQueryFields(String query) {
        String queryFields = "";
        Optional<String> optimisedQueryField = validateOptimisableField(query);
        if (optimisedQueryField.isPresent()) {
            queryFields = optimisedQueryField.get();
            if (queryConfig.getExtraOptmisableQueryFields() != null) {
                queryFields = queryFields + " " + queryConfig.getExtraOptmisableQueryFields();
            }
        } else {
            queryFields = queryConfig.getQueryFields();
        }
        return queryFields;
    }

    private SolrRequest.SolrRequestBuilder createBasicSolrRequestBuilder(BasicRequest request) {
        SolrRequest.SolrRequestBuilder requestBuilder = SolrRequest.builder();
        String requestedQuery = request.getQuery();
        if (idRequestRegex != null) {
            String cleanQuery =
                    CLEAN_QUERY_REGEX.matcher(request.getQuery().strip()).replaceAll("");
            if (idRequestRegex.matcher(cleanQuery.toUpperCase()).matches()) {
                requestedQuery = cleanQuery.toUpperCase();
            }
        }

        String query = requestedQuery;
        if (queryProcessorConfig != null) {
            query =
                    UniProtQueryProcessor.newInstance(queryProcessorConfig)
                            .processQuery(requestedQuery);
        }
        requestBuilder.query(query);

        if (solrSortClause != null) {
            requestBuilder.sorts(solrSortClause.getSort(request.getSort()));
        }

        requestBuilder.queryField(getQueryFields(query));
        requestBuilder.boostFunctions(queryConfig.getBoostFunctions());
        requestBuilder.fieldBoosts(queryConfig.getFieldBoosts());
        requestBuilder.staticBoosts(queryConfig.getStaticBoosts());
        requestBuilder.highlightFields(queryConfig.getHighlightFields());

        return requestBuilder;
    }

    private Optional<String> validateOptimisableField(String query) {
        String cleanQuery = CLEAN_QUERY_REGEX.matcher(query.strip()).replaceAll("");
        return queryProcessorConfig.getOptimisableFields().stream()
                .filter(
                        f ->
                                Utils.notNullNotEmpty(f.getValidRegex())
                                        && cleanQuery.matches(f.getValidRegex()))
                .map(SearchFieldItem::getFieldName)
                .findFirst();
    }

    public AbstractSolrSortClause getSolrSortClause() {
        return solrSortClause;
    }

    public Integer getDefaultPageSize() {
        return requestConverterConfigProperties.getDefaultPageSize() == null
                ? DEFAULT_PAGE_SIZE
                : requestConverterConfigProperties.getDefaultPageSize();
    }

    public Integer getDefaultBatchSize() {
        return requestConverterConfigProperties.getDefaultSolrBatchSize() == null
                ? DEFAULT_SOLR_BATCH_SIZE
                : requestConverterConfigProperties.getDefaultSolrBatchSize();
    }
}
