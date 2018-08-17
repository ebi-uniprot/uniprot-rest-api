package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.CloudSolrStream;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.uniprot.uuw.advanced.search.service.UniprotAdvancedSearchService.cloudResultStreamToStream;

/**
 * Created 17/08/18
 *
 * @author Edd
 */
@RunWith(MockitoJUnitRunner.class)
public class UniprotAdvancedSearchServiceTest {
    @Mock
    private CloudSolrStream cloudSolrStream;

    @Test
    public void cloudStreamToStreamWithElements() throws IOException {
        when(cloudSolrStream.read())
                .thenReturn(tuple("anything"))    // when calling hasNext() 1st time
                .thenReturn(tuple("accession1"))  // when calling next()
                .thenReturn(tuple("anything"))    // when calling hasNext() 2nd time
                .thenReturn(tuple("accession2"))  // when calling next() 2nd time
                .thenReturn(endTuple());
        List<String> accessionsFromStream = cloudResultStreamToStream(cloudSolrStream).collect(Collectors.toList());
        assertThat(accessionsFromStream, IsCollectionContaining.hasItems("accession1", "accession2"));
    }

    @Test
    public void cloudStreamToIteratorWhenEmpty() throws IOException {
        when(cloudSolrStream.read()).thenReturn(endTuple());
        List<String> collect = cloudResultStreamToStream(cloudSolrStream).collect(Collectors.toList());
        assertThat(collect, hasSize(0));
    }

    private Tuple tuple(String accession) {
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("accession", accession);
        return new Tuple(valueMap);
    }

    private Tuple endTuple() {
        Map<String, String> eofMap = new HashMap<>();
        eofMap.put("EOF", "");
        return new Tuple(eofMap);
    }
}