package org.uniprot.api.uniprotkb.common.service.request;

import static org.uniprot.api.rest.service.request.BasicRequestConverter.getIdsTermQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.BasicRequestConverter;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.uniprotkb.common.repository.search.UniProtTermsConfig;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBBasicRequest;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBStreamRequest;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.SolrQueryUtil;

public class UniProtKBRequestConverterImpl implements UniProtKBRequestConverter {
    public static final String ACCESSION_ID = "accession_id";
    public static final String ACCESSION = "accession";
    public static final String PROTEIN_ID = "id";
    public static final String IS_ISOFORM = "is_isoform";
    public static final String ACTIVE = "active";
    public static final String CANONICAL_ISOFORM = "-1";
    private final BasicRequestConverter basicConverter;
    private final UniProtTermsConfig uniProtTermsConfig;
    private final SearchFieldConfig searchFieldConfig;

    public UniProtKBRequestConverterImpl(
            SolrQueryConfig queryConfig,
            AbstractSolrSortClause solrSortClause,
            UniProtQueryProcessorConfig queryProcessorConfig,
            RequestConverterConfigProperties requestConverterConfigProperties,
            UniProtTermsConfig uniProtTermsConfig,
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
        this.uniProtTermsConfig = uniProtTermsConfig;
        searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);
    }

    @Override
    public SolrRequest createSearchSolrRequest(SearchRequest request) {
        UniProtKBSearchRequest uniProtRequest = (UniProtKBSearchRequest) request;

        if (isSearchAll(uniProtRequest)) {
            uniProtRequest.setQuery(getQueryFieldName(ACTIVE) + ":" + true);
        } else if (needToAddActiveFilter(uniProtRequest)) {
            uniProtRequest.setQuery(
                    uniProtRequest.getQuery() + " AND " + getQueryFieldName(ACTIVE) + ":" + true);
        }

        // fill the common params from the basic service class
        SolrRequest.SolrRequestBuilder builder =
                basicConverter.createSearchSolrRequest(uniProtRequest);

        updateIsoformsFilterQuery(builder, uniProtRequest);

        if (uniProtRequest.getShowSingleTermMatchedFields()) {
            builder.termQuery(uniProtRequest.getQuery());
            List<String> termFields = new ArrayList<>(uniProtTermsConfig.getFields());
            builder.termFields(termFields);
        }

        return builder.build();
    }

    @Override
    public SolrRequest createStreamSolrRequest(StreamRequest request) {
        UniProtKBStreamRequest uniProtRequest = (UniProtKBStreamRequest) request;
        SolrRequest.SolrRequestBuilder builder =
                basicConverter.createStreamSolrRequest(uniProtRequest);

        updateIsoformsFilterQuery(builder, uniProtRequest);

        if (isSearchAll(uniProtRequest)) {
            builder.query(getQueryFieldName(ACTIVE) + ":" + true);
        }
        builder.largeSolrStreamRestricted(uniProtRequest.isLargeSolrStreamRestricted());
        return builder.build();
    }

    @Override
    public SolrRequest createSearchIdsSolrRequest(
            SearchRequest request, List<String> idsList, String idField) {
        SolrRequest.SolrRequestBuilder requestBuilder =
                createSearchIdsSolrRequestBuilder(request, idField);
        requestBuilder.ids(idsList);
        requestBuilder.idsQuery(getUniProtKBIdsTermQuery(idsList, idField));
        return requestBuilder.build();
    }

    @Override
    public SolrRequest createStreamIdsSolrRequest(
            StreamRequest request, List<String> idsList, String idField) {
        SolrRequest.SolrRequestBuilder requestBuilder =
                createStreamIdsSolrRequestBuilder(request, idField);
        requestBuilder.ids(idsList);
        requestBuilder.idsQuery(getUniProtKBIdsTermQuery(idsList, idField));
        return requestBuilder.build();
    }

    @Override
    public int getDefaultPageSize() {
        return basicConverter.getDefaultPageSize();
    }

    @Override
    public SolrRequest createProteinIdSolrRequest(String proteinId) {
        return SolrRequest.builder()
                .query(
                        PROTEIN_ID
                                + ":"
                                + proteinId.toUpperCase()
                                + " AND  "
                                + IS_ISOFORM
                                + ":false")
                .sorts(basicConverter.getSolrSortClause().getSort(null))
                .rows(NumberUtils.INTEGER_TWO)
                .build();
    }

    @Override
    public SolrRequest createAccessionSolrRequest(String accession) {
        return SolrRequest.builder()
                .query(ACCESSION_ID + ":" + accession)
                .rows(NumberUtils.INTEGER_ONE)
                .build();
    }

    @Override
    public String getQueryFields(String query) {
        return basicConverter.getQueryFields(query);
    }

    protected SolrRequest.SolrRequestBuilder createSearchIdsSolrRequestBuilder(
            SearchRequest request, String idField) {
        return basicConverter.createSearchIdsSolrRequest(request, idField);
    }

    protected SolrRequest.SolrRequestBuilder createStreamIdsSolrRequestBuilder(
            StreamRequest request, String idField) {
        return basicConverter.createStreamIdsSolrRequest(request, idField);
    }

    protected String getUniProtKBIdsTermQuery(List<String> idsList, String idField) {
        List<String> result = new ArrayList<>();
        List<String> isoformList = idsList.stream().filter(id -> id.contains("-")).toList();
        if (Utils.notNullNotEmpty(isoformList)) {
            result.add(BasicRequestConverter.getIdsTermQuery(isoformList, ACCESSION));
        }
        List<String> accessionList = idsList.stream().filter(id -> !id.contains("-")).toList();
        if (Utils.notNullNotEmpty(accessionList)) {
            result.add(getIdsTermQuery(accessionList, idField));
        }
        return String.join(" OR ", result);
    }

    private void updateIsoformsFilterQuery(
            SolrRequest.SolrRequestBuilder builder, UniProtKBBasicRequest uniProtRequest) {
        boolean filterIsoform =
                UniProtKBRequestUtil.needsToFilterIsoform(
                        getQueryFieldName(ACCESSION_ID),
                        getQueryFieldName(IS_ISOFORM),
                        builder.getQuery(),
                        uniProtRequest.isIncludeIsoform());
        if (filterIsoform) {
            builder.filterQuery(getQueryFieldName(IS_ISOFORM) + ":" + false);
        }
    }

    private boolean isSearchAll(UniProtKBBasicRequest uniProtRequest) {
        String query = uniProtRequest.getQuery().strip();
        return "*".equals(query)
                || "(*)".equals(query)
                || "*:*".equals(query)
                || "(*:*)".equals(query);
    }

    private boolean needToAddActiveFilter(UniProtKBSearchRequest uniProtRequest) {
        return SolrQueryUtil.hasNegativeTerm(uniProtRequest.getQuery());
    }

    private String getQueryFieldName(String fieldName) {
        return searchFieldConfig.getSearchFieldItemByName(fieldName).getFieldName();
    }
}
