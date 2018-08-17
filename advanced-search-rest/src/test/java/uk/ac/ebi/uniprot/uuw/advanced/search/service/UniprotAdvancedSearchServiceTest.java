package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.CloudSolrStream;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.uniprot.uuw.advanced.search.service.UniprotAdvancedSearchService.cloudStreamToIterator;

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
    public void cloudStreamToIteratorWithElements() throws IOException {
        when(cloudSolrStream.read())
                .thenReturn(tuple("anything"))    // when calling hasNext() 1st time
                .thenReturn(tuple("accession1"))  // when calling next()
                .thenReturn(tuple("anything"))    // when calling hasNext() 2nd time
                .thenReturn(tuple("accession2"))  // when calling next() 2nd time
                .thenReturn(endTuple());
        Iterable<String> stringIterable = () -> cloudStreamToIterator(cloudSolrStream);
        assertThat(stringIterable, IsIterableContainingInOrder.contains("accession1", "accession2"));
    }

    @Test
    public void cloudStreamToIteratorWhenEmpty() throws IOException {
        when(cloudSolrStream.read()).thenReturn(endTuple());
        Iterable<String> stringIterable = () -> cloudStreamToIterator(cloudSolrStream);
        assertThat(stringIterable, IsIterableWithSize.iterableWithSize(0));
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