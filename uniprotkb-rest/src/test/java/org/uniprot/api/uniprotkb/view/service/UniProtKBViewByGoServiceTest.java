package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.uniprotkb.view.ViewBy;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class UniProtKBViewByGoServiceTest {
    @Mock
    private SolrClient solrClient;
    private UniProtKBViewByGoService service;

    @BeforeEach
    void setup() {
        solrClient = Mockito.mock(SolrClient.class);
        GoService goService = new GoService(new RestTemplate());
        service = null;
    }

    @Test
    void test() throws IOException, SolrServerException {
        Map<String, Long> counts = new HashMap<>();
        counts.put("GO:0008150", 78L);
        counts.put("GO:0005575", 70L);
        counts.put("GO:0003674", 73L);
        MockServiceHelper.mockServiceQueryResponse(solrClient, "go_id", counts);
        List<ViewBy> viewBys = service.getViewBys("", "");
        assertEquals(3, viewBys.size());
        ViewBy viewBy1 =
                MockServiceHelper.createViewBy(
                        "GO:0008150",
                        "biological_process",
                        78L,
                        UniProtKBViewByGoService.URL_PREFIX + "GO:0008150",
                        true);
        assertTrue(viewBys.contains(viewBy1));
        ViewBy viewBy2 =
                MockServiceHelper.createViewBy(
                        "GO:0005575",
                        "cellular_component",
                        70L,
                        UniProtKBViewByGoService.URL_PREFIX + "GO:0005575",
                        true);
        assertTrue(viewBys.contains(viewBy2));
        ViewBy viewBy3 =
                MockServiceHelper.createViewBy(
                        "GO:0003674",
                        "molecular_function",
                        73L,
                        UniProtKBViewByGoService.URL_PREFIX + "GO:0003674",
                        true);
        assertTrue(viewBys.contains(viewBy3));
    }
}
