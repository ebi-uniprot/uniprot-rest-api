package org.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.uniprotkb.view.ViewBy;
import org.uniprot.core.cv.ec.ECEntry;
import org.uniprot.core.cv.ec.impl.ECEntryBuilder;
import org.uniprot.cv.ec.ECRepo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class UniProtViewByECServiceTest {
    @Mock private SolrClient solrClient;

    @Mock private ECRepo ecRepo;
    private UniProtViewByECService service;

    @BeforeEach
    void setup() {
        solrClient = Mockito.mock(SolrClient.class);
        ecRepo = Mockito.mock(ECRepo.class);
        mockEcService();
        service = null;
    }

    @Test
    void test() throws IOException, SolrServerException {
        Map<String, Long> counts = new HashMap<>();
        counts.put("1.1.1.-", 346L);
        counts.put("1.1.3.-", 1L);
        MockServiceHelper.mockServiceQueryResponse(solrClient, "ec", counts);
        List<ViewBy> viewBys = service.getViewBys("", "1.1");
        assertEquals(2, viewBys.size());
        ViewBy viewBy1 =
                MockServiceHelper.createViewBy(
                        "1.1.1.-",
                        "With NAD(+) or NADP(+) as acceptor",
                        346L,
                        UniProtViewByECService.URL_PREFIX + "1.1.1.-",
                        true);
        assertTrue(viewBys.contains(viewBy1));
        ViewBy viewBy2 =
                MockServiceHelper.createViewBy(
                        "1.1.3.-",
                        "With oxygen as acceptor",
                        1L,
                        UniProtViewByECService.URL_PREFIX + "1.1.3.-",
                        true);
        assertTrue(viewBys.contains(viewBy2));
    }

    void mockEcService() {
        ECEntry ec1 =
                new ECEntryBuilder()
                        .id("1.1.1.-")
                        .label("With NAD(+) or NADP(+) as acceptor")
                        .build();
        ECEntry ec2 = new ECEntryBuilder().id("1.1.3.-").label("With oxygen as acceptor").build();
        doReturn(Optional.of(ec1)).when(ecRepo).getEC("1.1.1.-");
        doReturn(Optional.of(ec2)).when(ecRepo).getEC("1.1.3.-");
    }
}
