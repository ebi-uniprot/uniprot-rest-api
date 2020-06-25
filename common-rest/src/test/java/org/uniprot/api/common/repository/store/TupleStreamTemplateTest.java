package org.uniprot.api.common.repository.store;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.uniprot.api.common.repository.store.TupleStreamTemplate.TupleStreamBuilder.fieldsToReturn;
import static org.uniprot.api.common.repository.store.TupleStreamTemplate.TupleStreamBuilder.sortToString;

/**
 * Created 23/10/18
 *
 * @author Edd
 */
class TupleStreamTemplateTest {

    @Test
    void givenFieldAndSingleSort_whenFindFieldsToReturn_thenGetCorrectResults() {
        String key = "key";
        String field1 = "field1";
        List<SolrQuery.SortClause> order = new ArrayList<>();
        order.add(new SolrQuery.SortClause(field1, SolrQuery.ORDER.asc));
        assertThat(fieldsToReturn(key, order), is(String.join(",", key, field1)));
    }

    @Test
    void givenFieldAndMultipleSort_whenFindFieldsToReturn_thenGetCorrectResults() {
        String key = "key";
        String field1 = "field1";
        String field2 = "field2";
        List<SolrQuery.SortClause> order = new ArrayList<>();
        order.add(new SolrQuery.SortClause(field1, SolrQuery.ORDER.asc));
        order.add(new SolrQuery.SortClause(field2, SolrQuery.ORDER.desc));
        assertThat(fieldsToReturn(key, order), is(String.join(",", key, field1, field2)));
    }

    @Test
    void givenSingleSort_whenSortToString_thenGetCorrectResults() {
        String field1 = "field1";
        List<SolrQuery.SortClause> order = new ArrayList<>();
        order.add(new SolrQuery.SortClause(field1, SolrQuery.ORDER.asc));
        assertThat(sortToString(order), is(field1 + " asc"));
    }

    @Test
    void givenMultipleSort_whenSortToString_thenGetCorrectResults() {
        String field1 = "field1";
        String field2 = "field2";
        List<SolrQuery.SortClause> order = new ArrayList<>();
        order.add(new SolrQuery.SortClause(field1, SolrQuery.ORDER.asc));
        order.add(new SolrQuery.SortClause(field2, SolrQuery.ORDER.desc));
        assertThat(sortToString(order), is(String.join(",", field1 + " asc", field2 + " desc")));
    }

    @Test
    void givenRequest_whenValidResponse_thenGetCorrectResults()
            throws IOException, SolrServerException {
        // given
        SolrRequest request =
                SolrRequest.builder()
                        .query("protein")
                        .filterQueries(Collections.emptyList())
                        .build();
        SolrClient solrClient = mock(SolrClient.class);

        // when
        long queryHits = 9L;
        int acceptableHitsToRetrieve = 10;
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);

        StreamerConfigProperties streamConfig = new StreamerConfigProperties();
        streamConfig.setStoreMaxCountToRetrieve(acceptableHitsToRetrieve);
        TupleStreamTemplate streamTemplate =
                TupleStreamTemplate.builder()
                        .collection(SolrCollection.uniprot)
                        .solrClient(solrClient)
                        .streamConfig(streamConfig)
                        .solrRequestConverter(new SolrRequestConverter())
                        .build();

        // then
        assertDoesNotThrow(() -> streamTemplate.validateResponse(request));
    }

    @Test
    void givenRequest_whenNotRequestedMaxHits_thenDoNotQuerySolrForHits()
            throws IOException, SolrServerException {
        // given
        SolrRequest request =
                SolrRequest.builder()
                        .query("protein")
                        .filterQueries(Collections.emptyList())
                        .build();
        SolrClient solrClient = mock(SolrClient.class);

        // when
        int acceptableHitsToRetrieve = -1;

        StreamerConfigProperties streamConfig = new StreamerConfigProperties();
        streamConfig.setStoreMaxCountToRetrieve(acceptableHitsToRetrieve);
        TupleStreamTemplate streamTemplate =
                TupleStreamTemplate.builder()
                        .solrClient(solrClient)
                        .streamConfig(streamConfig)
                        .build();

        streamTemplate.validateResponse(request);

        // then
        verify(solrClient, times(0)).query(anyString(), any());
    }

    @Test
    void givenRequest_whenInvalidResponse_thenThrowException()
            throws IOException, SolrServerException {
        // given
        SolrRequest request =
                SolrRequest.builder()
                        .query("protein")
                        .filterQueries(Collections.emptyList())
                        .build();
        SolrClient solrClient = mock(SolrClient.class);

        // when
        long queryHits = 11L;
        int acceptableHitsToRetrieve = 10;
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);

        StreamerConfigProperties streamConfig = new StreamerConfigProperties();
        streamConfig.setStoreMaxCountToRetrieve(acceptableHitsToRetrieve);
        TupleStreamTemplate streamTemplate =
                TupleStreamTemplate.builder()
                        .collection(SolrCollection.uniprot)
                        .solrClient(solrClient)
                        .streamConfig(streamConfig)
                        .solrRequestConverter(new SolrRequestConverter())
                        .build();

        // then
        assertThrows(ServiceException.class, () -> streamTemplate.validateResponse(request));
    }
}
