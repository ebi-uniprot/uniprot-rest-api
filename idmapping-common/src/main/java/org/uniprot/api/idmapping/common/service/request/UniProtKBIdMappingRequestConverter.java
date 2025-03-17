package org.uniprot.api.idmapping.common.service.request;

import static org.uniprot.api.rest.service.request.BasicRequestConverter.*;

import java.util.List;
import java.util.regex.Pattern;

import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.idmapping.common.request.uniprotkb.UniProtKBIdMappingBasicRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.uniprotkb.common.repository.search.UniProtTermsConfig;
import org.uniprot.api.uniprotkb.common.service.request.UniProtKBRequestConverterImpl;
import org.uniprot.core.util.Utils;
import org.uniprot.store.search.SolrQueryUtil;

public class UniProtKBIdMappingRequestConverter extends UniProtKBRequestConverterImpl {
    public UniProtKBIdMappingRequestConverter(
            SolrQueryConfig queryConfig,
            AbstractSolrSortClause solrSortClause,
            UniProtQueryProcessorConfig queryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties,
            UniProtTermsConfig uniProtTermsConfig,
            FacetConfig facetConfig,
            Pattern idPattern) {
        super(
                queryConfig,
                solrSortClause,
                queryProcessorConfig,
                requestConverterConfigProperties,
                uniProtTermsConfig,
                facetConfig,
                idPattern);
    }

    @Override
    public SolrRequest createSearchIdsSolrRequest(
            SearchRequest request, List<String> idsList, String idField) {
        SolrRequest.SolrRequestBuilder builder =
                createSearchIdsSolrRequestBuilder(request, idField);
        UniProtKBIdMappingBasicRequest uniProtKbRequest = (UniProtKBIdMappingBasicRequest) request;
        updateIdsSolrRequestBuilder(uniProtKbRequest, builder, idsList, idField);
        return builder.build();
    }

    @Override
    public SolrRequest createStreamIdsSolrRequest(
            StreamRequest request, List<String> idsList, String idField) {
        SolrRequest.SolrRequestBuilder builder =
                createStreamIdsSolrRequestBuilder(request, idField);
        UniProtKBIdMappingBasicRequest uniProtKbRequest = (UniProtKBIdMappingBasicRequest) request;
        updateIdsSolrRequestBuilder(uniProtKbRequest, builder, idsList, idField);
        return builder.build();
    }

    private void updateIdsSolrRequestBuilder(
            UniProtKBIdMappingBasicRequest uniProtKbRequest,
            SolrRequest.SolrRequestBuilder builder,
            List<String> idsList,
            String idField) {
        boolean hasIsIsoForm = false;
        if (Utils.notNullNotEmpty(uniProtKbRequest.getQuery())) {
            hasIsIsoForm = SolrQueryUtil.hasFieldTerms(uniProtKbRequest.getQuery(), IS_ISOFORM);
        }
        builder.ids(idsList);
        if (uniProtKbRequest.isIncludeIsoform() || hasIsIsoForm) {
            builder.idsQuery(getIdsTermQuery(idsList, ACCESSION));
        } else {
            builder.idsQuery(super.getUniProtKBIdsTermQuery(idsList, idField));
        }
    }
}
