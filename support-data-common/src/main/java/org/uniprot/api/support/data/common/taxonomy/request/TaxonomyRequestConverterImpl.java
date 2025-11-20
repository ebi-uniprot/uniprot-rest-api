package org.uniprot.api.support.data.common.taxonomy.request;

import java.util.ArrayList;
import java.util.List;

import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.BasicRequestConverter;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.rest.service.request.RequestConverterImpl;
import org.uniprot.api.support.data.common.taxonomy.repository.TaxonomyTermsConfig;

public class TaxonomyRequestConverterImpl extends RequestConverterImpl {
    private final BasicRequestConverter basicConverter;
    private final TaxonomyTermsConfig taxonomyTermsConfig;

    public TaxonomyRequestConverterImpl(
            SolrQueryConfig queryConfig,
            AbstractSolrSortClause solrSortClause,
            UniProtQueryProcessorConfig queryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties,
            FacetConfig facetConfig,
            TaxonomyTermsConfig taxonomyTermsConfig) {
        super(
                queryConfig,
                solrSortClause,
                queryProcessorConfig,
                requestConverterConfigProperties,
                facetConfig);
        this.taxonomyTermsConfig = taxonomyTermsConfig;
        this.basicConverter =
                new BasicRequestConverter(
                        queryConfig,
                        solrSortClause,
                        queryProcessorConfig,
                        requestConverterConfigProperties,
                        facetConfig,
                        null);
    }

    @Override
    public SolrRequest createSearchSolrRequest(SearchRequest request) {
        TaxonomySearchRequest taxonomySearchRequest = (TaxonomySearchRequest) request;
        // fill the common params from the basic service class
        SolrRequest.SolrRequestBuilder builder =
                basicConverter.createSearchSolrRequest(taxonomySearchRequest);

        if (taxonomySearchRequest.getShowSingleTermMatchedFields()) {
            builder.termQuery(taxonomySearchRequest.getQuery());
            List<String> termFields = new ArrayList<>(taxonomyTermsConfig.getFields());
            builder.termFields(termFields);
        }

        return builder.build();
    }

    @Override
    public SolrRequest createSearchIdsSolrRequest(
            SearchRequest request, List<String> idsList, String idField) {
        SolrRequest.SolrRequestBuilder requestBuilder =
                basicConverter.createSearchSolrRequest(request);
        return requestBuilder.build();
    }
}
