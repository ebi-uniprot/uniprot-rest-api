package org.uniprot.api.uniprotkb.view.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.cv.pathway.UniPathway;
import org.uniprot.cv.pathway.UniPathwayService;

@ExtendWith(MockitoExtension.class)
class UniProtViewByPathwayServiceTest {
    @Mock private SolrClient solrClient;

    @Mock private UniPathwayService unipathwayService;
    private UniProtViewByPathwayService service;

    @BeforeEach
    void setup() {
        solrClient = Mockito.mock(SolrClient.class);
        unipathwayService = Mockito.mock(UniPathwayService.class);
        mockPathwayService();
        service = new UniProtViewByPathwayService(solrClient, "uniprot", unipathwayService);
    }

    @Test
    void test() throws IOException, SolrServerException {
        Map<String, Long> counts = new HashMap<>();
        counts.put("289", 36L);
        counts.put("456", 1L);
        MockServiceHelper.mockServiceQueryResponse(solrClient, "ec", counts);
        List<ViewBy> viewBys = service.get("", "1");
        assertEquals(2, viewBys.size());
        ViewBy viewBy1 =
                MockServiceHelper.createViewBy(
                        "289", "Amine and polyamine biosynthesis", 36L, null, false);
        assertTrue(viewBys.contains(viewBy1));
        ViewBy viewBy2 =
                MockServiceHelper.createViewBy(
                        "456", "Amine and polyamine degradation", 1L, null, false);
        assertTrue(viewBys.contains(viewBy2));
    }

    void mockPathwayService() {
        List<UniPathway> nodes = new ArrayList<>();
        UniPathway node1 = new UniPathway("289", "Amine and polyamine biosynthesis");
        UniPathway node2 = new UniPathway("456", "Amine and polyamine degradation");
        nodes.add(node1);
        nodes.add(node2);

        when(unipathwayService.getChildrenById(any())).thenReturn(nodes);
    }
}
