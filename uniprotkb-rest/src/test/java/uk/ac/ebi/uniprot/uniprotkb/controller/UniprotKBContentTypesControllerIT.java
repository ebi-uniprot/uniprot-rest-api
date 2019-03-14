package uk.ac.ebi.uniprot.uniprotkb.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.uniprot.common.repository.DataStoreManager;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.uniprotkb.UniProtKBREST;
import uk.ac.ebi.uniprot.uniprotkb.repository.DataStoreTestConfig;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.mockers.UniProtEntryMocker;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.ac.ebi.uniprot.uniprotkb.controller.UniprotKBController.UNIPROTKB_RESOURCE;

/**
 *
 * @author lgonzales
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@WebAppConfiguration
public class UniprotKBContentTypesControllerIT {

    private static final String SEARCH_RESOURCE = UNIPROTKB_RESOURCE + "/search";
    private static final String ACCESSION_RESOURCE = UNIPROTKB_RESOURCE + "/accession/";

    @Autowired
    private DataStoreManager storeManager;

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
    public void canReturnFastaFormat() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE)
                        .param("query", "accession:" + acc));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString(">sp|Q8DIA7|" +
                        "PURL_THEEB Phosphoribosylformylglycinamidine synthase subunit PurL " +
                        "OS=Thermosynechococcus elongatus (strain BP-1) OX=197221 GN=purL PE=3 SV=1")));
    }

    @Test
    public void fastaBadRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE)
                        .param("query", "invalid:invalid"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }

    @Test
    public void fastaNotFoundRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "P05067")
                        .header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE)
                        .param("query", "accession:P12345"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }


    private String saveEntry() {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);
        return acc;
    }
}
