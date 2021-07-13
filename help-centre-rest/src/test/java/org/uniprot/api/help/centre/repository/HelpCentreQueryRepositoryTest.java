package org.uniprot.api.help.centre.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.store.search.document.help.HelpDocument;

/**
 * @author lgonzales
 * @since 13/07/2021
 */
class HelpCentreQueryRepositoryTest {

    @Test
    void getResponseWithHighLightDocuments() {
        HelpDocument doc =
                HelpDocument.builder().id("id").title("title").content("content").build();
        Map<String, List<String>> highlightField = Map.of("content", List.of("content highlight"));
        Map<String, Map<String, List<String>>> highlight = Map.of("id", highlightField);

        QueryResponse queryResults = Mockito.mock(QueryResponse.class);
        Mockito.when(queryResults.getBeans(HelpDocument.class)).thenReturn(List.of(doc));
        Mockito.when(queryResults.getHighlighting()).thenReturn(highlight);
        HelpCentreQueryRepository repository = new HelpCentreQueryRepository(null, null, null);
        List<HelpDocument> result = repository.getResponseDocuments(queryResults);
        assertNotNull(result);
        assertEquals(1, result.size());
        HelpDocument docResult = result.get(0);
        assertEquals("id", docResult.getId());
        assertEquals("title", docResult.getTitle());
        assertEquals("content", docResult.getContent());
        assertEquals(highlightField, docResult.getMatches());
    }

    @Test
    void getResponseWithoutHighLightDocuments() {
        HelpDocument doc =
                HelpDocument.builder().id("id").title("title").content("content").build();

        QueryResponse queryResults = Mockito.mock(QueryResponse.class);
        Mockito.when(queryResults.getBeans(HelpDocument.class)).thenReturn(List.of(doc));

        HelpCentreQueryRepository repository = new HelpCentreQueryRepository(null, null, null);
        List<HelpDocument> result = repository.getResponseDocuments(queryResults);

        assertNotNull(result);
        assertEquals(1, result.size());
        HelpDocument docResult = result.get(0);
        assertEquals("id", docResult.getId());
        assertEquals("title", docResult.getTitle());
        assertEquals("content", docResult.getContent());
        assertNull(docResult.getMatches());
    }
}
