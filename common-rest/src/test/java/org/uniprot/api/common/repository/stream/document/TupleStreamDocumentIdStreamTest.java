package org.uniprot.api.common.repository.stream.document;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.when;
import static org.uniprot.api.common.repository.stream.store.TupleStreamUtils.tupleStream;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;

/**
 * @author sahmad
 * @created 27/01/2021
 */
@ExtendWith(MockitoExtension.class)
@Slf4j
class TupleStreamDocumentIdStreamTest {

    private static final String ID = "id";

    @Test
    void testFetchIds() {
        List<String> ids = asList("a", "b", "c", "d", "e");
        TupleStream tupleStream = tupleStream(ids);
        TupleStreamTemplate mockTupleStreamTemplate = Mockito.mock(TupleStreamTemplate.class);
        when(mockTupleStreamTemplate.create(ArgumentMatchers.any())).thenReturn(tupleStream);
        StreamerConfigProperties streamConfig = new StreamerConfigProperties();
        streamConfig.setIdFieldName(ID);
        streamConfig.setStoreBatchSize(10);

        DocumentIdStream idStream =
                TupleStreamDocumentIdStream.builder()
                        .tupleStreamTemplate(mockTupleStreamTemplate)
                        .streamConfig(streamConfig)
                        .build();

        Stream<String> idsStream = idStream.fetchIds(SolrRequest.builder().build());
        List<String> returnedIds = idsStream.collect(Collectors.toList());
        Assertions.assertEquals(ids, returnedIds);
    }
}
