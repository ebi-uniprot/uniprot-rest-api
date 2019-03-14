package uk.ac.ebi.uniprot.uniprotkb.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
                        .header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }

    @Test
    public void canReturnFlatFileFormat() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, UniProtMediaType.FF_MEDIA_TYPE)
                        .param("query", "accession:" + acc));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.FF_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString("ID   PURL_THEEB              Reviewed;         761 AA.\n" +
                        "AC   Q8DIA7;\n" +
                        "DT   07-JUN-2005, integrated into UniProtKB/Swiss-Prot.\n" +
                        "DT   01-MAR-2003, sequence version 1.\n" +
                        "DT   05-DEC-2018, entry version 101.")));
    }

    @Test
    public void flatFileBadRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, UniProtMediaType.FF_MEDIA_TYPE)
                        .param("query", "invalid:invalid"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.FF_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }

    @Test
    public void flatFileNotFoundRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "P05067")
                        .header(ACCEPT, UniProtMediaType.FF_MEDIA_TYPE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.FF_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }

    @Test
    public void canReturnGffFormat() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, UniProtMediaType.GFF_MEDIA_TYPE)
                        .param("query", "accession:" + acc));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.GFF_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString("##gff-version 3\n" +
                        "##sequence-region Q8DIA7 1 761\n" +
                        "Q8DIA7\tUniProtKB\tChain\t1\t761\t.\t.\t.\tID=PRO_0000100496;Note=Phosphoribosylformylglycinamidine synthase subunit PurL")));
    }

    @Test
    public void gffBadRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, UniProtMediaType.GFF_MEDIA_TYPE)
                        .param("query", "invalid:invalid"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.GFF_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }

    @Test
    public void gffNotFoundRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "P05067")
                        .header(ACCEPT, UniProtMediaType.GFF_MEDIA_TYPE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.GFF_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }

    @Test
    public void canReturnListFormat() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, UniProtMediaType.LIST_MEDIA_TYPE)
                        .param("query", "accession:" + acc));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.LIST_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString("Q8DIA7")));
    }

    @Test
    public void listBadRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, UniProtMediaType.LIST_MEDIA_TYPE)
                        .param("query", "invalid:invalid"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.LIST_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }

    @Test
    public void listNotFoundRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "P12343")
                        .header(ACCEPT, UniProtMediaType.LIST_MEDIA_TYPE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.LIST_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }

    @Test
    public void canReturnTsvFormat() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, UniProtMediaType.TSV_MEDIA_TYPE)
                        .param("query", "accession:" + acc));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.TSV_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString("Q8DIA7")));
    }

    @Test
    public void tsvFormatBadRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, UniProtMediaType.TSV_MEDIA_TYPE)
                        .param("query", "invalid:invalid"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.TSV_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }

    @Test
    public void tsvFormatNotFoundRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "P12343")
                        .header(ACCEPT, UniProtMediaType.TSV_MEDIA_TYPE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.TSV_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }

    @Test
    public void canReturnXlsFormat() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, UniProtMediaType.XLS_MEDIA_TYPE)
                        .param("query", "accession:" + acc));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.XLS_MEDIA_TYPE_VALUE));
    }

    @Test
    public void xlsFormatBadRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, UniProtMediaType.XLS_MEDIA_TYPE)
                        .param("query", "invalid:invalid"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.XLS_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }

    @Test
    public void xlsFormatNotFoundRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "P12343")
                        .header(ACCEPT, UniProtMediaType.XLS_MEDIA_TYPE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,UniProtMediaType.XLS_MEDIA_TYPE_VALUE))
                .andExpect(content().string(isEmptyString()));
    }



    @Test
    public void canReturnXmlFormat() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, MediaType.APPLICATION_XML)
                        .param("query", "accession:" + acc));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_XML_VALUE))
                .andExpect(content().string(containsString("<accession>Q8DIA7</accession>")));
    }

    @Test
    public void xmlFormatBadRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, MediaType.APPLICATION_XML)
                        .param("query", "invalid:invalid"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_XML_VALUE))
                .andExpect(content().string(containsString("<messages>'invalid' is not a valid search field</messages>")));
    }

    @Test
    public void xmlFormatNotFoundRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "P12343")
                        .header(ACCEPT, MediaType.APPLICATION_XML));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_XML_VALUE))
                .andExpect(content().string(containsString("<messages>Resource not found</messages>")));
    }


    @Test
    public void canReturnJsonFormat() throws Exception {
        // given
        String acc = saveEntry();

        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "accession:" + acc));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("{\"entryType\":\"Swiss-Prot\"," +
                        "\"primaryAccession\":\"Q8DIA7\"," +
                        "\"uniProtId\":\"PURL_THEEB\"")));
    }

    @Test
    public void jsonFormatBadRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(SEARCH_RESOURCE)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "invalid:invalid"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"messages\":[\"'invalid' is not a valid search field\"]")));
    }

    @Test
    public void jsonFormatNotFoundRequest() throws Exception {
        // when
        ResultActions response = mockMvc.perform(
                get(ACCESSION_RESOURCE + "P12343")
                        .header(ACCEPT, MediaType.APPLICATION_JSON));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string(containsString("\"messages\":[\"Resource not found\"]")));
    }
    private String saveEntry() {
        UniProtEntry entry = UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);
        String acc = entry.getPrimaryAccession().getValue();
        storeManager.save(DataStoreManager.StoreType.UNIPROT, entry);
        return acc;
    }
}
