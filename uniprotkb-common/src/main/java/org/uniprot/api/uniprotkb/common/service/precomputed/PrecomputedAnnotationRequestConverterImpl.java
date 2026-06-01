package org.uniprot.api.uniprotkb.common.service.precomputed;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.BasicRequestConverter;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.rest.service.request.RequestConverterImpl;
import org.uniprot.store.search.SolrQueryUtil;

public class PrecomputedAnnotationRequestConverterImpl extends RequestConverterImpl {
    private static final String ACCESSION = "accession";
    private static final String UNIPARC = "uniparc";
    private static final String TAXONOMY_ID = "taxonomy_id";
    private static final String ALL_QUERY = "*:*";

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
                basicConverter.createSearchSolrRequest(precomputedAnnotationSearchByProteomeRequest);
        builder.query(createQuery(precomputedAnnotationSearchByProteomeRequest));
        return builder.build();
    }

    private String createQuery(PrecomputedAnnotationSearchByProteomeRequest request) {
        List<String> queryParts = new ArrayList<>();
        addQueryPart(queryParts, ACCESSION, request.getAccession());
        addQueryPart(queryParts, UNIPARC, request.getUniparc());
        addQueryPart(queryParts, TAXONOMY_ID, request.getTaxonomyId());
        return queryParts.isEmpty() ? ALL_QUERY : String.join(" AND ", queryParts);
    }

    private void addQueryPart(List<String> queryParts, String field, String value) {
        if (value != null && !value.isBlank()) {
            String escapedValue =
                    SolrQueryUtil.escapeSpecialCharacters(value.strip().toUpperCase(Locale.ROOT));
            queryParts.add(field + ":" + escapedValue);
        }
    }
}
