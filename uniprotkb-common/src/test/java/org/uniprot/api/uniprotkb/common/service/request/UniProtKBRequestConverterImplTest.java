package org.uniprot.api.uniprotkb.common.service.request;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.search.FakeSolrSortClause;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.uniprotkb.common.repository.search.UniProtTermsConfig;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBSearchRequest;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBStreamRequest;

class UniProtKBRequestConverterImplTest {

    @Test
    void createUniProtKBSearchSolrRequestWithSearchAllReturnActiveEntriesOnly() {
        String query = "*:*";
        String queryField = "queryField1,queryField2";

        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        Mockito.when(queryConfig.getQueryFields()).thenReturn(queryField);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        UniProtTermsConfig termsConfig = Mockito.mock(UniProtTermsConfig.class);
        UniProtKBRequestConverterImpl converter =
                new UniProtKBRequestConverterImpl(
                        queryConfig, null, queryProcessorConfig, convertProps, termsConfig, null);

        FakeUniProtKBSearchRequest request = new FakeUniProtKBSearchRequest();
        request.setQuery(query);

        SolrRequest result = converter.createSearchSolrRequest(request);
        assertNotNull(result);
        assertEquals("active:true", result.getQuery());
        assertEquals(List.of("is_isoform:false"), result.getFilterQueries());
        assertEquals(queryField, result.getQueryField());
    }

    @Test
    void createUniProtKBSearchSolrRequestWithSearchNegativeFilterActiveEntriesOnly() {
        String query = "NOT query";
        String queryField = "queryField1,queryField2";

        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        Mockito.when(queryConfig.getQueryFields()).thenReturn(queryField);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        UniProtTermsConfig termsConfig = Mockito.mock(UniProtTermsConfig.class);
        UniProtKBRequestConverterImpl converter =
                new UniProtKBRequestConverterImpl(
                        queryConfig, null, queryProcessorConfig, convertProps, termsConfig, null);

        FakeUniProtKBSearchRequest request = new FakeUniProtKBSearchRequest();
        request.setQuery(query);

        SolrRequest result = converter.createSearchSolrRequest(request);
        assertNotNull(result);
        assertEquals("NOT query AND active:true", result.getQuery());
        assertEquals(List.of("is_isoform:false"), result.getFilterQueries());
        assertEquals(queryField, result.getQueryField());
    }

    @Test
    void createUniProtKBSearchSolrRequestWithIsIsoformTrue() {
        String query = "is_isoform:true";
        String queryField = "queryField1,queryField2";

        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        Mockito.when(queryConfig.getQueryFields()).thenReturn(queryField);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        UniProtTermsConfig termsConfig = Mockito.mock(UniProtTermsConfig.class);
        UniProtKBRequestConverterImpl converter =
                new UniProtKBRequestConverterImpl(
                        queryConfig, null, queryProcessorConfig, convertProps, termsConfig, null);

        FakeUniProtKBSearchRequest request = new FakeUniProtKBSearchRequest();
        request.setQuery(query);

        SolrRequest result = converter.createSearchSolrRequest(request);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertTrue(result.getFilterQueries().isEmpty());
        assertEquals(queryField, result.getQueryField());
    }

    @Test
    void createUniProtKBSearchSolrRequestWithUniProtTermsConfig() {
        String query = "singleTerm";
        String queryField = "queryField1,queryField2";
        List<String> termFields = List.of("termField1", "termField2");

        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        Mockito.when(queryConfig.getQueryFields()).thenReturn(queryField);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        UniProtTermsConfig termsConfig = Mockito.mock(UniProtTermsConfig.class);
        Mockito.when(termsConfig.getFields()).thenReturn(termFields);
        UniProtKBRequestConverterImpl converter =
                new UniProtKBRequestConverterImpl(
                        queryConfig, null, queryProcessorConfig, convertProps, termsConfig, null);

        FakeUniProtKBSearchRequest request = new FakeUniProtKBSearchRequest();
        request.setQuery(query);
        request.setShowSingleTermMatchedFields("true");

        SolrRequest result = converter.createSearchSolrRequest(request);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertEquals(query, result.getTermQuery());
        assertEquals(termFields, result.getTermFields());
        assertEquals(queryField, result.getQueryField());
    }

    @Test
    void createUniProtKBStreamSolrRequestWithSearchAllReturnActiveEntriesOnly() {
        String query = "*:*";
        String queryField = "queryField1,queryField2";

        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        Mockito.when(queryConfig.getQueryFields()).thenReturn(queryField);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        UniProtTermsConfig termsConfig = Mockito.mock(UniProtTermsConfig.class);
        UniProtKBRequestConverterImpl converter =
                new UniProtKBRequestConverterImpl(
                        queryConfig, null, queryProcessorConfig, convertProps, termsConfig, null);

        FakeUniProtKBStreamRequest request = new FakeUniProtKBStreamRequest();
        request.setQuery(query);
        request.setLargeSolrStreamRestricted(true);

        SolrRequest result = converter.createStreamSolrRequest(request);
        assertNotNull(result);
        assertEquals("active:true", result.getQuery());
        assertTrue(result.isLargeSolrStreamRestricted());
        assertEquals(List.of("is_isoform:false"), result.getFilterQueries());
        assertEquals(queryField, result.getQueryField());
    }

    @Test
    void createUniProtKBStreamSolrRequestWithIsIsoformTrue() {
        String query = "is_isoform:true";
        String queryField = "queryField1,queryField2";

        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        Mockito.when(queryConfig.getQueryFields()).thenReturn(queryField);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        UniProtTermsConfig termsConfig = Mockito.mock(UniProtTermsConfig.class);
        UniProtKBRequestConverterImpl converter =
                new UniProtKBRequestConverterImpl(
                        queryConfig, null, queryProcessorConfig, convertProps, termsConfig, null);

        FakeUniProtKBStreamRequest request = new FakeUniProtKBStreamRequest();
        request.setQuery(query);
        request.setLargeSolrStreamRestricted(true);

        SolrRequest result = converter.createStreamSolrRequest(request);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertTrue(result.isLargeSolrStreamRestricted());
        assertTrue(result.getFilterQueries().isEmpty());
        assertEquals(queryField, result.getQueryField());
    }

    @Test
    void createProteinIdSolrRequest() {
        SolrQuery.SortClause sort = new SolrQuery.SortClause("sort", SolrQuery.ORDER.asc);
        FakeSolrSortClause solrClause = Mockito.mock(FakeSolrSortClause.class);
        Mockito.when(solrClause.getSort(Mockito.anyString())).thenReturn(List.of(sort));
        UniProtKBRequestConverterImpl converter =
                new UniProtKBRequestConverterImpl(null, solrClause, null, null, null, null);

        SolrRequest result = converter.createProteinIdSolrRequest("HUMAN_P21802");
        assertNotNull(result);
        assertEquals("id:HUMAN_P21802 AND  is_isoform:false", result.getQuery());
        assertTrue(result.getSorts().isEmpty());
        assertEquals(NumberUtils.INTEGER_TWO, result.getRows());
    }

    @Test
    void createAccessionSolrRequest() {
        UniProtKBRequestConverterImpl converter =
                new UniProtKBRequestConverterImpl(null, null, null, null, null, null);

        SolrRequest result = converter.createAccessionSolrRequest("P21802");
        assertNotNull(result);
        assertEquals("accession_id:P21802", result.getQuery());
        assertTrue(result.getSorts().isEmpty());
        assertEquals(NumberUtils.INTEGER_ONE, result.getRows());
    }

    private static class FakeUniProtKBSearchRequest extends UniProtKBSearchRequest {}

    private static class FakeUniProtKBStreamRequest extends UniProtKBStreamRequest {}
}