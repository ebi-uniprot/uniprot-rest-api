package uk.ac.ebi.uniprot.api.uniprotkb.view.service;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.ac.ebi.uniprot.api.uniprotkb.view.ViewBy;
import uk.ac.ebi.uniprot.cv.ec.EC;
import uk.ac.ebi.uniprot.cv.ec.ECRepo;
import uk.ac.ebi.uniprot.cv.ec.impl.ECImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
class UniProtViewByECServiceTest {
    @Mock
    private SolrClient solrClient;

    @Mock
    private ECRepo ecRepo;
    private UniProtViewByECService service;


    @BeforeEach
    void setup() {
        solrClient = Mockito.mock(SolrClient.class);
        ecRepo = Mockito.mock(ECRepo.class);
        mockEcService();
        service = new UniProtViewByECService(solrClient, "uniprot", ecRepo);
    }

    @Test
    void test() throws IOException, SolrServerException {
        Map<String, Long> counts = new HashMap<>();
        counts.put("1.1.1.-", 346l);
        counts.put("1.1.3.-", 1l);
        MockServiceHelper.mockServiceQueryResponse(solrClient, "ec", counts);
        List<ViewBy> viewBys = service.get("", "1.1");
        assertEquals(2, viewBys.size());
        ViewBy viewBy1 = MockServiceHelper
                .createViewBy("1.1.1.-", "With NAD(+) or NADP(+) as acceptor", 346l, UniProtViewByECService.URL_PREFIX + "1.1.1.-", true);
        assertTrue(viewBys.contains(viewBy1));
        ViewBy viewBy2 = MockServiceHelper
                .createViewBy("1.1.3.-", "With oxygen as acceptor", 1l, UniProtViewByECService.URL_PREFIX + "1.1.3.-", true);
        assertTrue(viewBys.contains(viewBy2));
    }

    void mockEcService() {
        EC ec1 = new ECImpl("1.1.1.-", "With NAD(+) or NADP(+) as acceptor");
        EC ec2 = new ECImpl("1.1.3.-", "With oxygen as acceptor");
        when(ecRepo.getEC("1.1.1.-")).thenReturn(Optional.of(ec1));
        when(ecRepo.getEC("1.1.3.-")).thenReturn(Optional.of(ec2));
    }
}
