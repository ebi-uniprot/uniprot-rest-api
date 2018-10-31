package uk.ac.ebi.uniprot.uuw.advanced.search.controller;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.MessageConverterConfig;
import uk.ac.ebi.uniprot.uuw.advanced.search.mockers.UniProtEntryMocker;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.DataStoreManager;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.DataStoreTestConfig;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.RepositoryConfig;
import uk.ac.ebi.uniprot.uuw.advanced.search.repository.impl.uniprot.UniprotQueryRepository;
import uk.ac.ebi.uniprot.uuw.advanced.search.results.ResultsConfig;
import uk.ac.ebi.uniprot.uuw.advanced.search.results.StoreStreamer;
import uk.ac.ebi.uniprot.uuw.advanced.search.results.TupleStreamTemplate;
import uk.ac.ebi.uniprot.uuw.advanced.search.service.UniProtEntryService;
import uk.ac.ebi.uniprot.uuw.advanced.search.store.UniProtStoreConfig;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.ac.ebi.uniprot.uuw.advanced.search.controller.UniprotAdvancedSearchController.UNIPROTKB_RESOURCE;
import static uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType.FF_MEDIA_TYPE;
/**
 * Created 21/09/18
 *
 * @author Edd
 */
@RunWith(SpringRunner.class)
@WebMvcTest({UniprotAdvancedSearchController.class})
@Import({DataStoreTestConfig.class, RepositoryConfig.class, UniProtEntryService.class, UniprotQueryRepository.class,
         UniProtStoreConfig.class, ResultsConfig.class, MessageConverterConfig.class})
public class UniProtAdvancedSearchDownloadTest {
    private static final String DOWNLOAD_RESOURCE = UNIPROTKB_RESOURCE + "/download/";
    private static final String QUERY = "query";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;

    @MockBean
    private TupleStreamTemplate tupleStreamTemplate;

    @MockBean
    private StoreStreamer<UniProtEntry> uniProtEntryStoreStreamer;

    @Before
    public void setUp() {
        when(tupleStreamTemplate.create(any())).thenReturn(mock(TupleStream.class));
    }

    @Test
    public void canReachDownloadEndpoint() throws Exception {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryUniProtAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);

        mockStreamerResponseOf(entry);

        ResultActions response = mockMvc.perform(
                get(DOWNLOAD_RESOURCE)
                        .header(ACCEPT, FF_MEDIA_TYPE)
                        .param(QUERY, accessionQuery(acc)));

        response.andExpect(
                request().asyncStarted())
                .andDo(MvcResult::getAsyncResult)
                .andDo(print())
                .andExpect(content().contentType(FF_MEDIA_TYPE))
                .andExpect(content().string(containsString("AC   Q8DIA7;")))
                .andExpect(header().stringValues(VARY, ACCEPT, ACCEPT_ENCODING))
                .andExpect(header().exists(CONTENT_DISPOSITION));
    }

    private String accessionQuery(String acc) {
        return "accession:" + acc;
    }

    private void mockStreamerResponseOf(UniProtEntry... entries) {
        when(uniProtEntryStoreStreamer.idsToStoreStream(any(), any()))
                .thenReturn(Stream.of(entries));
    }
}
