package org.uniprot.api.rest.service.request;

import static org.uniprot.api.rest.service.request.BasicRequestConverter.*;

import java.util.List;
import java.util.regex.Pattern;

import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;

public class RequestConverterImpl implements RequestConverter {

    private final BasicRequestConverter basicConverter;

    public RequestConverterImpl(
            SolrQueryConfig queryConfig,
            AbstractSolrSortClause solrSortClause,
            UniProtQueryProcessorConfig queryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties,
            FacetConfig facetConfig) {
        this(
                queryConfig,
                solrSortClause,
                queryProcessorConfig,
                requestConverterConfigProperties,
                facetConfig,
                null);
    }

    public RequestConverterImpl(
            SolrQueryConfig queryConfig,
            AbstractSolrSortClause solrSortClause,
            UniProtQueryProcessorConfig queryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties,
            FacetConfig facetConfig,
            Pattern idPattern) {
        basicConverter =
                new BasicRequestConverter(
                        queryConfig,
                        solrSortClause,
                        queryProcessorConfig,
                        requestConverterConfigProperties,
                        facetConfig,
                        idPattern);
    }

    public SolrRequest createSearchSolrRequest(SearchRequest request) {
        SolrRequest.SolrRequestBuilder requestBuilder =
                basicConverter.createSearchSolrRequest(request);
        return requestBuilder.build();
    }

    public SolrRequest createStreamSolrRequest(StreamRequest request) {
        SolrRequest.SolrRequestBuilder requestBuilder =
                basicConverter.createStreamSolrRequest(request);
        return requestBuilder.build();
    }

    @Override
    public SolrRequest createSearchIdsSolrRequest(
            SearchRequest request, List<String> idsList, String idField) {
        SolrRequest.SolrRequestBuilder requestBuilder =
                basicConverter.createSearchIdsSolrRequest(request, idField);
        requestBuilder.idsQuery(getIdsTermQuery(idsList, idField));
        return requestBuilder.build();
    }

    @Override
    public SolrRequest createStreamIdsSolrRequest(
            StreamRequest request, List<String> idsList, String idField) {
        SolrRequest.SolrRequestBuilder requestBuilder =
                basicConverter.createStreamIdsSolrRequest(request, idField);
        requestBuilder.idsQuery(getIdsTermQuery(idsList, idField));
        return requestBuilder.build();
    }

    @Override
    public int getDefaultPageSize() {
        return basicConverter.getDefaultPageSize();
    }
}
