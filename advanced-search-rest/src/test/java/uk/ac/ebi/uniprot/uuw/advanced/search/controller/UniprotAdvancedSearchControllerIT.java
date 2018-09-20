package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.uniprot.dataservice.document.Document;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.uuw.advanced.search.AdvancedSearchREST;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.SolrClientTestConfig;

import java.io.IOException;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.ac.ebi.uniprot.uuw.advanced.search.repository.UniProtDocMocker.createDocs;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AdvancedSearchREST.class, properties = "spring.http.encoding.enabled=false")
@WebAppConfiguration
@Import({SolrClientTestConfig.class})
public class UniprotAdvancedSearchControllerIT {
    @Autowired
    private SolrClient uniProtSolrClient;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.
                webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    public void canReachDownloadEndpoint() throws Exception {
        saveDocuments(20);

        mockMvc.perform(get("/uniprot/search?query=accession:*")).andDo(print());
    }

    private void saveDocuments(Document... docs) {
        try {
            for (Document doc : docs) {
                uniProtSolrClient.addBean(doc);
            }
            uniProtSolrClient.commit();
        } catch (SolrServerException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<UniProtDocument> saveDocuments(int docCount) {
        List<UniProtDocument> addedDocs = createDocs(docCount);
        try {
            uniProtSolrClient.addBeans(addedDocs);
            uniProtSolrClient.commit();
        } catch (SolrServerException | IOException e) {
            throw new IllegalStateException(e);
        }
        return addedDocs;
    }
}