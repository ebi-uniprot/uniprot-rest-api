package org.uniprot.api.common.repository.stream.document;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;

/**
 * @author sahmad
 * @created 27/01/2021
 */
@ExtendWith(MockitoExtension.class)
class DefaultDocumentIdStreamTest {
    @Mock private SolrQueryRepository<TestDocument> repository;

    @Test
    void testFetchIds() {
        // when
        SolrRequest solrRequest = SolrRequest.builder().query("*:*").rows(3).totalRows(3).build();
        TestDocument doc1 = new TestDocument("1", "name1");
        TestDocument doc2 = new TestDocument("2", "name2");
        TestDocument doc3 = new TestDocument("3", "name3");
        when(repository.getAll(solrRequest)).thenReturn(Stream.of(doc1, doc2, doc3));

        DefaultDocumentIdStream<TestDocument> idStream =
                DefaultDocumentIdStream.<TestDocument>builder()
                        .documentToId(TestDocument::getDocumentId)
                        .repository(repository)
                        .build();
        // then
        List<String> ids = idStream.fetchIds(solrRequest).collect(Collectors.toList());
        Assertions.assertNotNull(ids);
        Assertions.assertEquals(Arrays.asList("1", "2", "3"), ids);
    }
}
