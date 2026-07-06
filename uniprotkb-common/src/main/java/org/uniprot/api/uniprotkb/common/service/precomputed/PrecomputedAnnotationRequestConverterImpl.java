package org.uniprot.api.uniprotkb.common.service.precomputed;

import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.BasicRequestConverter;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.rest.service.request.RequestConverterImpl;

public class PrecomputedAnnotationRequestConverterImpl extends RequestConverterImpl {
    private final BasicRequestConverter basicConverter;

    public PrecomputedAnnotationRequestConverterImpl(
            SolrQueryConfig queryConfig,
            AbstractSolrSortClause solrSortClause,
            UniProtQueryProcessorConfig queryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties) {
        super(
                queryConfig,
                solrSortClause,
                queryProcessorConfig,
                requestConverterConfigProperties,
                null);
        this.basicConverter =
                new BasicRequestConverter(
                        queryConfig,
                        solrSortClause,
                        queryProcessorConfig,
                        requestConverterConfigProperties,
                        null,
                        null);
    }

    @Override
    public SolrRequest createSearchSolrRequest(SearchRequest request) {
        PrecomputedAnnotationSearchByProteomeRequest precomputedAnnotationSearchByProteomeRequest =
                (PrecomputedAnnotationSearchByProteomeRequest) request;
        SolrRequest.SolrRequestBuilder builder =
                basicConverter.createSearchSolrRequest(
                        precomputedAnnotationSearchByProteomeRequest);
        return builder.build();
    }

    @Override
    public SolrRequest createStreamSolrRequest(StreamRequest request) {
        PrecomputedAnnotationStreamByProteomeRequest precomputedAnnotationStreamByProteomeRequest =
                (PrecomputedAnnotationStreamByProteomeRequest) request;
        SolrRequest.SolrRequestBuilder builder =
                basicConverter.createStreamSolrRequest(
                        precomputedAnnotationStreamByProteomeRequest);
        return builder.build();
    }
}
