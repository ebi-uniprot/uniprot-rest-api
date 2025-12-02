package org.uniprot.api.support.data.common.taxonomy.request;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.support.data.common.taxonomy.repository.TaxonomyTermsConfig;

class TaxonomyRequestConverterImplTest {

    @Test
    void testCreateSearchSolrRequestWithMatchedTerms() {
        String query = "12345";
        List<String> termFields = List.of("field1", "field2", "field3");

        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        TaxonomyTermsConfig termsConfig = Mockito.mock(TaxonomyTermsConfig.class);
        Mockito.when(termsConfig.getFields()).thenReturn(termFields);
        TaxonomyRequestConverterImpl converter =
                new TaxonomyRequestConverterImpl(
                        queryConfig, null, queryProcessorConfig, convertProps, null, termsConfig);

        TaxonomySearchRequest request = Mockito.mock(TaxonomySearchRequest.class);
        Mockito.when(request.getQuery()).thenReturn(query);
        Mockito.when(request.getShowSingleTermMatchedFields()).thenReturn(true);

        SolrRequest result = converter.createSearchSolrRequest(request);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertEquals(query, result.getTermQuery());
        assertEquals(termFields, result.getTermFields());
    }

    @Test
    void testCreateSearchSolrRequestWithoutMatchedTerms() {
        String query = "12345";
        List<String> termFields = List.of("field1", "field2", "field3");

        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        TaxonomyTermsConfig termsConfig = Mockito.mock(TaxonomyTermsConfig.class);
        Mockito.when(termsConfig.getFields()).thenReturn(termFields);
        TaxonomyRequestConverterImpl converter =
                new TaxonomyRequestConverterImpl(
                        queryConfig, null, queryProcessorConfig, convertProps, null, termsConfig);

        TaxonomySearchRequest request = Mockito.mock(TaxonomySearchRequest.class);
        Mockito.when(request.getQuery()).thenReturn(query);
        Mockito.when(request.getShowSingleTermMatchedFields()).thenReturn(false);

        SolrRequest result = converter.createSearchSolrRequest(request);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertNull(result.getTermQuery());
        assertTrue(result.getTermFields().isEmpty());
    }

    @Test
    void testCreateSearchIdsSolrRequest() {
        String query = "12345";
        List<String> termFields = List.of("field1", "field2", "field3");

        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        TaxonomyTermsConfig termsConfig = Mockito.mock(TaxonomyTermsConfig.class);
        Mockito.when(termsConfig.getFields()).thenReturn(termFields);
        TaxonomyRequestConverterImpl converter =
                new TaxonomyRequestConverterImpl(
                        queryConfig, null, queryProcessorConfig, convertProps, null, termsConfig);

        TaxonomySearchRequest request = Mockito.mock(TaxonomySearchRequest.class);
        Mockito.when(request.getQuery()).thenReturn(query);
        Mockito.when(request.getShowSingleTermMatchedFields()).thenReturn(false);

        SolrRequest result = converter.createSearchIdsSolrRequest(request, null, null);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
    }
}
