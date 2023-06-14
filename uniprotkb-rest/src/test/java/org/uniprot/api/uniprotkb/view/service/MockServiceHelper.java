package org.uniprot.api.uniprotkb.view.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.api.uniprotkb.view.ViewByImpl;

public class MockServiceHelper {
    public static void mockServiceQueryResponse(
            SolrClient solrClient, String name, Map<String, Long> counts)
            throws SolrServerException, IOException {
        QueryResponse queryResponse = getQueryResponse(name, counts);
        when(solrClient.query(anyString(), any())).thenReturn(queryResponse);
    }

    private static QueryResponse getQueryResponse(String name, Map<String, Long> counts) {
        List<FacetField> facetResponse = new ArrayList<>();
        FacetField field1 = new FacetField(name);

        for (Map.Entry<String, Long> count : counts.entrySet()) {
            field1.add(count.getKey(), count.getValue());
        }
        facetResponse.add(field1);
        QueryResponse queryResponse = mock(QueryResponse.class);

        when(queryResponse.getFacetFields()).thenReturn(facetResponse);
        return queryResponse;
    }

    public static ViewBy createViewBy(String id, String label, long count, boolean expand) {
        return ViewByImpl.builder().id(id).label(label).count(count).expand(expand).build();
    }
}
