package org.uniprot.api.idmapping.common.service.request;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.uniprotkb.common.service.request.UniProtKBRequestConverterImpl.ACCESSION_ID;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.idmapping.common.request.uniprotkb.UniProtKBIdMappingSearchRequest;
import org.uniprot.api.idmapping.common.request.uniprotkb.UniProtKBIdMappingStreamRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;
import org.uniprot.api.uniprotkb.common.repository.search.UniProtTermsConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

class UniProtKBIdMappingRequestConverterTest {

    private static final SearchFieldConfig searchFieldConfig =
            SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);

    @Test
    void createSearchIdsSolrRequest() {
        String query = "queryValue";
        String queryField = "queryField1,queryField2";
        List<String> ids = List.of("P21802", "P12345", "P05067");

        UniProtKBIdMappingRequestConverter converter =
                getUniProtKBIdMappingRequestConverter(queryField);

        FakeUniProtKBIdMappingSearchRequest request = new FakeUniProtKBIdMappingSearchRequest();
        request.setQuery(query);

        SolrRequest result = converter.createSearchIdsSolrRequest(request, ids, ACCESSION_ID);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertFalse(result.isLargeSolrStreamRestricted());
        assertTrue(result.getFilterQueries().isEmpty());
        assertTrue(result.getSorts().isEmpty());
        assertEquals(queryField, result.getQueryField());
        assertEquals(query, result.getQuery());
        assertEquals("({!terms f=accession_id}P21802,P12345,P05067)", result.getIdsQuery());
        assertEquals(ACCESSION_ID, result.getIdField());
        assertEquals(ids, result.getIds());
    }

    @Test
    void createSearchIdsSolrRequestWithSort() {
        String query = "queryValue";
        String queryField = "queryField1,queryField2";
        List<String> ids = List.of("P21802", "P12345", "P05067");
        SolrQuery.SortClause sort =
                SolrQuery.SortClause.create("annotation_score", SolrQuery.ORDER.asc);
        SolrQuery.SortClause idSort =
                SolrQuery.SortClause.create("accession_id", SolrQuery.ORDER.asc);

        UniProtKBIdMappingRequestConverter converter =
                getUniProtKBIdMappingRequestConverter(queryField);

        FakeUniProtKBIdMappingSearchRequest request = new FakeUniProtKBIdMappingSearchRequest();
        request.setQuery(query);
        request.setSort(sort.getItem() + " " + sort.getOrder());

        SolrRequest result = converter.createSearchIdsSolrRequest(request, ids, ACCESSION_ID);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertFalse(result.isLargeSolrStreamRestricted());
        assertTrue(result.getFilterQueries().isEmpty());
        assertEquals(List.of(sort, idSort), result.getSorts());
        assertEquals(queryField, result.getQueryField());
        assertEquals(query, result.getQuery());
        assertEquals("({!terms f=accession_id}P21802,P12345,P05067)", result.getIdsQuery());
        assertEquals(ACCESSION_ID, result.getIdField());
        assertEquals(ids, result.getIds());
    }

    @Test
    void createSearchIdsSolrRequestWithIncludeIsoform() {
        String query = "queryValue";
        String queryField = "queryField1,queryField2";
        List<String> ids = List.of("P21802", "P12345", "P05067");

        UniProtKBIdMappingRequestConverter converter =
                getUniProtKBIdMappingRequestConverter(queryField);

        FakeUniProtKBIdMappingSearchRequest request = new FakeUniProtKBIdMappingSearchRequest();
        request.setQuery(query);
        request.setIncludeIsoform("true");

        SolrRequest result = converter.createSearchIdsSolrRequest(request, ids, ACCESSION_ID);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertFalse(result.isLargeSolrStreamRestricted());
        assertTrue(result.getFilterQueries().isEmpty());
        assertTrue(result.getSorts().isEmpty());
        assertEquals(queryField, result.getQueryField());
        assertEquals(query, result.getQuery());
        assertEquals("({!terms f=accession}P21802,P12345,P05067)", result.getIdsQuery());
        assertEquals(ACCESSION_ID, result.getIdField());
        assertEquals(ids, result.getIds());
    }

    @Test
    void createSearchIdsSolrRequestWithMixedIds() {
        String query = "queryValue";
        String queryField = "queryField1,queryField2";
        List<String> ids = List.of("P21802", "P12345", "P05067", "P05067-3", "P21802-2");

        UniProtKBIdMappingRequestConverter converter =
                getUniProtKBIdMappingRequestConverter(queryField);

        FakeUniProtKBIdMappingSearchRequest request = new FakeUniProtKBIdMappingSearchRequest();
        request.setQuery(query);

        SolrRequest result = converter.createSearchIdsSolrRequest(request, ids, ACCESSION_ID);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertFalse(result.isLargeSolrStreamRestricted());
        assertTrue(result.getFilterQueries().isEmpty());
        assertTrue(result.getSorts().isEmpty());
        assertEquals(queryField, result.getQueryField());
        assertEquals(query, result.getQuery());
        assertEquals(
                "({!terms f=accession}P05067-3,P21802-2) OR ({!terms f=accession_id}P21802,P12345,P05067)",
                result.getIdsQuery());
        assertEquals(ACCESSION_ID, result.getIdField());
        assertEquals(ids, result.getIds());
    }

    @Test
    void createStreamIdsSolrRequest() {
        String query = "queryValue";
        String queryField = "queryField1,queryField2";
        List<String> ids = List.of("P21802", "P12345", "P05067");

        UniProtKBIdMappingRequestConverter converter =
                getUniProtKBIdMappingRequestConverter(queryField);

        FakeUniProtKBIdMappingStreamRequest request = new FakeUniProtKBIdMappingStreamRequest();
        request.setQuery(query);

        SolrRequest result = converter.createStreamIdsSolrRequest(request, ids, ACCESSION_ID);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertFalse(result.isLargeSolrStreamRestricted());
        assertTrue(result.getFilterQueries().isEmpty());
        assertEquals(queryField, result.getQueryField());
        assertTrue(result.getSorts().isEmpty());
        assertEquals(query, result.getQuery());
        assertEquals("({!terms f=accession_id}P21802,P12345,P05067)", result.getIdsQuery());
        assertEquals(ACCESSION_ID, result.getIdField());
    }

    @Test
    void createStreamIdsSolrRequestWithIncludeIsoform() {
        String query = "queryValue";
        String queryField = "queryField1,queryField2";
        List<String> ids = List.of("P21802", "P12345", "P05067");

        UniProtKBIdMappingRequestConverter converter =
                getUniProtKBIdMappingRequestConverter(queryField);

        FakeUniProtKBIdMappingStreamRequest request = new FakeUniProtKBIdMappingStreamRequest();
        request.setQuery(query);
        request.setIncludeIsoform("true");

        SolrRequest result = converter.createStreamIdsSolrRequest(request, ids, ACCESSION_ID);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertFalse(result.isLargeSolrStreamRestricted());
        assertTrue(result.getFilterQueries().isEmpty());
        assertTrue(result.getSorts().isEmpty());
        assertEquals(queryField, result.getQueryField());
        assertEquals(query, result.getQuery());
        assertEquals("({!terms f=accession}P21802,P12345,P05067)", result.getIdsQuery());
        assertEquals(ACCESSION_ID, result.getIdField());
    }

    @Test
    void createStreamIdsSolrRequestWithMixedIds() {
        String query = "queryValue";
        String queryField = "queryField1,queryField2";
        List<String> ids = List.of("P21802", "P12345", "P05067", "P05067-3", "P21802-2");

        UniProtKBIdMappingRequestConverter converter =
                getUniProtKBIdMappingRequestConverter(queryField);

        FakeUniProtKBIdMappingStreamRequest request = new FakeUniProtKBIdMappingStreamRequest();
        request.setQuery(query);

        SolrRequest result = converter.createStreamIdsSolrRequest(request, ids, ACCESSION_ID);
        assertNotNull(result);
        assertEquals(query, result.getQuery());
        assertFalse(result.isLargeSolrStreamRestricted());
        assertTrue(result.getFilterQueries().isEmpty());
        assertTrue(result.getSorts().isEmpty());
        assertEquals(queryField, result.getQueryField());
        assertEquals(query, result.getQuery());
        assertEquals(
                "({!terms f=accession}P05067-3,P21802-2) OR ({!terms f=accession_id}P21802,P12345,P05067)",
                result.getIdsQuery());
        assertEquals(ACCESSION_ID, result.getIdField());
    }

    private static UniProtKBIdMappingRequestConverter getUniProtKBIdMappingRequestConverter(
            String queryField) {
        SolrQueryConfig queryConfig = Mockito.mock(SolrQueryConfig.class);
        Mockito.when(queryConfig.getQueryFields()).thenReturn(queryField);

        UniProtQueryProcessorConfig queryProcessorConfig =
                Mockito.mock(UniProtQueryProcessorConfig.class);
        Mockito.when(queryProcessorConfig.getSearchFieldConfig()).thenReturn(searchFieldConfig);
        RequestConverterConfigProperties convertProps =
                Mockito.mock(RequestConverterConfigProperties.class);
        UniProtTermsConfig termsConfig = Mockito.mock(UniProtTermsConfig.class);
        return new UniProtKBIdMappingRequestConverter(
                queryConfig,
                new FakeIdMappingSolrSortClause(),
                queryProcessorConfig,
                convertProps,
                termsConfig,
                null,
                null);
    }

    private static class FakeUniProtKBIdMappingSearchRequest
            extends UniProtKBIdMappingSearchRequest {}

    private static class FakeUniProtKBIdMappingStreamRequest
            extends UniProtKBIdMappingStreamRequest {}

    private static class FakeIdMappingSolrSortClause extends AbstractSolrSortClause {

        public FakeIdMappingSolrSortClause() {
            addDefaultFieldOrderPair("score", SolrQuery.ORDER.asc);
            addDefaultFieldOrderPair(ACCESSION_ID, SolrQuery.ORDER.asc);
        }

        @Override
        protected String getSolrDocumentIdFieldName() {
            return ACCESSION_ID;
        }

        @Override
        protected UniProtDataType getUniProtDataType() {
            return UniProtDataType.UNIPROTKB;
        }
    }
}
