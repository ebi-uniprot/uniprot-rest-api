package org.uniprot.api.rest.service.request;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.search.FakeSolrSortClause;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

class BasicRequestConverterTest {

    private static final SearchFieldConfig searchFieldConfig =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.DISEASE);

    @Test
    void createBasicSolrRequestWithQueryFields() {
        String query = "DI-12345";
        String queryField = "queryField1,queryField2";

        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        Mockito.when(queryConfig.getQueryFields()).thenReturn(queryField);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        BasicRequestConverter converter =
                new BasicRequestConverter(
                        queryConfig, null, queryProcessorConfig, convertProps, null);

        SearchRequest request = Mockito.mock(SearchRequest.class);
        Mockito.when(request.getQuery()).thenReturn(query);

        SolrRequest result = converter.createSearchSolrRequest(request).build();
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertEquals(queryField, result.getQueryField());
    }

    @Test
    void createBasicSolrRequestCanCleanQueryAndUpperCaseIdField() {
        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        Pattern diseaseIdPattern = Pattern.compile(FieldRegexConstants.DISEASE_REGEX);
        BasicRequestConverter converter =
                new BasicRequestConverter(
                        queryConfig, null, queryProcessorConfig, convertProps, diseaseIdPattern);

        SearchRequest request = Mockito.mock(SearchRequest.class);
        Mockito.when(request.getQuery()).thenReturn("(di-12345)");

        SolrRequest result = converter.createSearchSolrRequest(request).build();
        assertNotNull(result);
        assertEquals("DI-12345", result.getQuery());
    }

    @Test
    void createBasicSolrRequestWithOptimisedQueryFields() {
        String query = "di-12345";
        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        Mockito.when(queryConfig.getExtraOptmisableQueryFields())
                .thenReturn("extraOptmisableQueryField");

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        Mockito.when(queryProcessorConfig.getSearchFieldConfig()).thenReturn(searchFieldConfig);
        Mockito.when(queryProcessorConfig.getOptimisableFields())
                .thenReturn(List.of(searchFieldConfig.getSearchFieldItemByName("id")));

        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        Pattern diseaseIdPattern = Pattern.compile(FieldRegexConstants.DISEASE_REGEX);
        BasicRequestConverter converter =
                new BasicRequestConverter(
                        queryConfig, null, queryProcessorConfig, convertProps, diseaseIdPattern);

        SearchRequest request = Mockito.mock(SearchRequest.class);
        Mockito.when(request.getQuery()).thenReturn(query);

        SolrRequest result = converter.createSearchSolrRequest(request).build();
        assertNotNull(result);
        assertEquals(query.toUpperCase(), result.getQuery());
        assertEquals("id extraOptmisableQueryField", result.getQueryField());
    }

    @Test
    void createBasicSolrRequestWithDefaultPageSizeOnly() {
        String query = "queryValue";
        Integer defaultPageSize = 18;
        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        Mockito.when(queryProcessorConfig.getSearchFieldConfig()).thenReturn(searchFieldConfig);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        Mockito.when(convertProps.getDefaultPageSize()).thenReturn(defaultPageSize);
        BasicRequestConverter converter =
                new BasicRequestConverter(
                        queryConfig, null, queryProcessorConfig, convertProps, null);

        SearchRequest request = Mockito.mock(SearchRequest.class);
        Mockito.when(request.getQuery()).thenReturn(query);
        Mockito.when(request.getSize()).thenReturn(null).thenReturn(defaultPageSize);

        SolrRequest result = converter.createSearchSolrRequest(request).build();
        assertNotNull(result);
        Mockito.verify(request, times(1)).setSize(defaultPageSize);
        assertEquals(query, result.getQuery());
        assertEquals(defaultPageSize, result.getRows());
        assertEquals(defaultPageSize, result.getTotalRows());
    }

    @Test
    void createBasicSolrRequestWillPreferRequestPageSize() {
        String query = "query";
        Integer defaultPageSize = 18;
        Integer requestPageSize = 23;
        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        Mockito.when(convertProps.getDefaultPageSize()).thenReturn(defaultPageSize);
        BasicRequestConverter converter =
                new BasicRequestConverter(
                        queryConfig, null, queryProcessorConfig, convertProps, null);

        SearchRequest request = Mockito.mock(SearchRequest.class);
        Mockito.when(request.getQuery()).thenReturn(query);
        Mockito.when(request.getSize()).thenReturn(requestPageSize);

        SolrRequest result = converter.createSearchSolrRequest(request).build();
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertEquals(requestPageSize, result.getRows());
        assertEquals(requestPageSize, result.getTotalRows());
    }

    @Test
    void createSolrRequestWithBoosts() {
        String query = "query:value";
        String boostFunctions = "boostFunctions";
        List<String> fieldBoosts = List.of("field:{query}^100.0", "field2:{query}^20.0");
        List<String> staticBoosts = List.of("field:10^3.0", "field2:20^4.0");
        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        Mockito.when(queryConfig.getBoostFunctions()).thenReturn(boostFunctions);
        Mockito.when(queryConfig.getFieldBoosts()).thenReturn(fieldBoosts);
        Mockito.when(queryConfig.getStaticBoosts()).thenReturn(staticBoosts);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        BasicRequestConverter converter =
                new BasicRequestConverter(
                        queryConfig, null, queryProcessorConfig, convertProps, null);

        SearchRequest request = Mockito.mock(SearchRequest.class);
        Mockito.when(request.getQuery()).thenReturn(query);

        SolrRequest result = converter.createSearchSolrRequest(request).build();
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertEquals(boostFunctions, result.getBoostFunctions());
        assertEquals(fieldBoosts, result.getFieldBoosts());
        assertEquals(staticBoosts, result.getStaticBoosts());
    }

    @Test
    void createSolrRequestWithHighlightFields() {
        String query = "query:value";
        String highlightFields = "highlightFields";
        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        Mockito.when(queryConfig.getHighlightFields()).thenReturn(highlightFields);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        BasicRequestConverter converter =
                new BasicRequestConverter(
                        queryConfig, null, queryProcessorConfig, convertProps, null);

        SearchRequest request = Mockito.mock(SearchRequest.class);
        Mockito.when(request.getQuery()).thenReturn(query);

        SolrRequest result = converter.createSearchSolrRequest(request).build();
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertEquals(highlightFields, result.getHighlightFields());
    }

    @Test
    void createSolrRequestWithFacets() {
        String query = "query:value";
        List<String> facets = List.of("facet1", "facet2");

        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        Mockito.when(queryProcessorConfig.getSearchFieldConfig()).thenReturn(searchFieldConfig);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        BasicRequestConverter converter =
                new BasicRequestConverter(
                        queryConfig, null, queryProcessorConfig, convertProps, null);

        SearchRequest request = Mockito.mock(SearchRequest.class);
        Mockito.when(request.getQuery()).thenReturn(query);
        Mockito.when(request.hasFacets()).thenReturn(true);
        Mockito.when(request.getFacetList()).thenReturn(facets);

        SolrRequest result = converter.createSearchSolrRequest(request).build();
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertEquals(facets, result.getFacets());
    }

    @Test
    void createSolrRequestWithSort() {
        String query = "query:value";
        SolrQuery.SortClause sort = new SolrQuery.SortClause("sort", SolrQuery.ORDER.asc);

        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        FakeSolrSortClause solrClause = Mockito.mock(FakeSolrSortClause.class);
        Mockito.when(solrClause.getSort(Mockito.anyString())).thenReturn(List.of(sort));

        BasicRequestConverter converter =
                new BasicRequestConverter(
                        queryConfig, solrClause, queryProcessorConfig, convertProps, null);

        SearchRequest request = Mockito.mock(SearchRequest.class);
        Mockito.when(request.getQuery()).thenReturn(query);
        Mockito.when(request.getSort()).thenReturn(sort.toString());

        SolrRequest result = converter.createSearchSolrRequest(request).build();
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertEquals(List.of(sort), result.getSorts());
    }

    @Test
    void createStreamBasicSolrRequest() {
        String query = "query";
        Integer defaultSolrBatchSize = 45;
        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        Mockito.when(convertProps.getDefaultSolrBatchSize()).thenReturn(defaultSolrBatchSize);
        BasicRequestConverter converter =
                new BasicRequestConverter(
                        queryConfig, null, queryProcessorConfig, convertProps, null);

        StreamRequest request = Mockito.mock(StreamRequest.class);
        Mockito.when(request.getQuery()).thenReturn(query);

        SolrRequest result = converter.createStreamSolrRequest(request).build();
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertEquals(defaultSolrBatchSize, result.getRows());
        assertEquals(Integer.MAX_VALUE, result.getTotalRows());
    }
}
